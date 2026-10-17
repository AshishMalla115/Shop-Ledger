
package com.shopledger.backend.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shopledger.backend.dto.request.SaleRequest;
import com.shopledger.backend.dto.response.SaleResponse;
import com.shopledger.backend.entity.Item;
import com.shopledger.backend.entity.ItemStock;
import com.shopledger.backend.entity.LedgerEvent;
import com.shopledger.backend.entity.Party;
import com.shopledger.backend.entity.PartyBalance;
import com.shopledger.backend.enums.LedgerEventType;
import com.shopledger.backend.enums.PartyType;
import com.shopledger.backend.exception.IdempotencyConflictException;
import com.shopledger.backend.exception.ItemNotFoundException;
import com.shopledger.backend.exception.PartyNotFoundException;
import com.shopledger.backend.repository.ItemRepository;
import com.shopledger.backend.repository.ItemStockRepository;
import com.shopledger.backend.repository.LedgerEventRepository;
import com.shopledger.backend.repository.PartyBalanceRepository;
import com.shopledger.backend.repository.PartyRepository;

@Service
public class SaleService {

    private final ItemRepository itemRepository;
    private final PartyRepository partyRepository;
    private final ItemStockRepository itemStockRepository;
    private final PartyBalanceRepository partyBalanceRepository;
    private final LedgerEventRepository ledgerEventRepository;

    public SaleService(
            ItemRepository itemRepository,
            PartyRepository partyRepository,
            ItemStockRepository itemStockRepository,
            PartyBalanceRepository partyBalanceRepository,
            LedgerEventRepository ledgerEventRepository) {
        this.itemRepository = itemRepository;
        this.partyRepository = partyRepository;
        this.itemStockRepository = itemStockRepository;
        this.partyBalanceRepository = partyBalanceRepository;
        this.ledgerEventRepository = ledgerEventRepository;
    }

    @Transactional
    public SaleResponse createSale(SaleRequest request) {

        // 1. Handle a previously committed request.
        var existingEvent = ledgerEventRepository
                .findByIdempotencyKey(request.getIdempotencyKey());

        if (existingEvent.isPresent()) {
            return replayExistingSale(existingEvent.get(), request);
        }

        // 2. Validate the customer or garage.
        Party party = partyRepository.findById(request.getPartyId())
                .orElseThrow(
                        () -> new PartyNotFoundException(request.getPartyId()));

        if (party.getPartyType() != PartyType.PURCHASER
                && party.getPartyType() != PartyType.GARAGE) {
            throw new IllegalArgumentException(
                    "Sales are allowed only for purchasers and garages.");
        }

        if (!party.isActive()) {
            throw new IllegalArgumentException(
                    "The selected party is inactive.");
        }

        // 3. Validate the item.
        Item item = itemRepository.findById(request.getItemId())
                .orElseThrow(
                        () -> new ItemNotFoundException(request.getItemId()));

        if (!item.isActive()) {
            throw new IllegalArgumentException(
                    "The selected item is inactive.");
        }

        if (request.getUnitPrice().scale() > 2) {
            throw new IllegalArgumentException(
                    "Unit selling price must have at most two decimal places.");
        }

        // 4. Calculate the total sale amount.
        BigDecimal amount = request.getUnitPrice()
                .multiply(BigDecimal.valueOf(request.getQuantity()))
                .setScale(2);

        // 5. Initialize missing rows.
        itemStockRepository.initializeStockIfMissing(item.getItemId());
        partyBalanceRepository.initializeBalanceIfMissing(
                party.getPartyId());

        // 6. Lock stock first, then party balance.
        ItemStock stock = itemStockRepository
                .findByItemIdForUpdate(item.getItemId())
                .orElseThrow(() -> new IllegalStateException(
                        "Stock row could not be initialized."));

        PartyBalance balance = partyBalanceRepository
                .findByPartyIdForUpdate(party.getPartyId())
                .orElseThrow(() -> new IllegalStateException(
                        "Party balance row could not be initialized."));

        // 7. Recheck idempotency after obtaining the locks.
        existingEvent = ledgerEventRepository
                .findByIdempotencyKey(request.getIdempotencyKey());

        if (existingEvent.isPresent()) {
            return replayExistingSale(existingEvent.get(), request);
        }

        // 8. Reject sales that exceed available stock.
        if (request.getQuantity() > stock.getAvailableQuantity()) {
            throw new IllegalArgumentException(
                    "Insufficient stock. Available quantity: "
                            + stock.getAvailableQuantity());
        }

        int newStockQuantity = stock.getAvailableQuantity()
                - request.getQuantity();

        // A sale increases the amount owed to the shop.
        BigDecimal newPartyBalance = balance.getBalance().add(amount);

        // 9. Create the immutable ledger event.
        LedgerEvent event = LedgerEvent.createSale(
                party,
                item,
                request.getQuantity(),
                request.getUnitPrice(),
                amount,
                request.getIdempotencyKey(),
                request.getCreatedBy().trim(),
                request.getNotes());

        LedgerEvent savedEvent = ledgerEventRepository.save(event);

        // 10. Update stock and party balance in the same transaction.
        stock.setAvailableQuantity(newStockQuantity);
        stock.setLastStockUpdatedAt(OffsetDateTime.now());

        balance.setBalance(newPartyBalance);
        balance.setUpdatedAt(OffsetDateTime.now());

        itemStockRepository.save(stock);
        partyBalanceRepository.save(balance);

        // 11. Return the result.
        SaleResponse response = new SaleResponse();
        response.setEventId(savedEvent.getId());
        response.setEventType(savedEvent.getEventType().name());
        response.setAmount(savedEvent.getAmount());
        response.setNewStockQuantity(newStockQuantity);
        response.setNewPartyBalance(newPartyBalance);

        return response;
    }

    private SaleResponse replayExistingSale(
            LedgerEvent event,
            SaleRequest request) {

        boolean sameRequest =
                event.getEventType() == LedgerEventType.SALE
                && event.getParty().getPartyId().equals(request.getPartyId())
                && event.getItem() != null
                && event.getItem().getItemId().equals(request.getItemId())
                && event.getQuantity().equals(request.getQuantity())
                && event.getUnitPrice().compareTo(request.getUnitPrice()) == 0;

        if (!sameRequest) {
            throw new IdempotencyConflictException(
                    "This idempotency key has already been used "
                    + "for a different sale.");
        }

        ItemStock stock = itemStockRepository
                .findById(request.getItemId())
                .orElseThrow(() -> new IllegalStateException(
                        "Stock record not found for the recorded sale."));

        PartyBalance balance = partyBalanceRepository
                .findById(request.getPartyId())
                .orElseThrow(() -> new IllegalStateException(
                        "Party balance record not found."));

        SaleResponse response = new SaleResponse();
        response.setEventId(event.getId());
        response.setEventType(event.getEventType().name());
        response.setAmount(event.getAmount());
        response.setNewStockQuantity(stock.getAvailableQuantity());
        response.setNewPartyBalance(balance.getBalance());

        return response;
    }
}
