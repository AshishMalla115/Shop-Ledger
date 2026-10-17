package com.shopledger.backend.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shopledger.backend.dto.request.PurchaseRequest;
import com.shopledger.backend.dto.response.PurchaseResponse;
import com.shopledger.backend.entity.Item;
import com.shopledger.backend.entity.ItemStock;
import com.shopledger.backend.entity.LedgerEvent;
import com.shopledger.backend.entity.Party;
import com.shopledger.backend.entity.PartyBalance;
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
public class PurchaseService {

    private final ItemRepository itemRepository;
    private final PartyRepository partyRepository;
    private final ItemStockRepository itemStockRepository;
    private final PartyBalanceRepository partyBalanceRepository;
    private final LedgerEventRepository ledgerEventRepository;

    public PurchaseService(
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
    public PurchaseResponse receivePurchase(PurchaseRequest request) {

        // Fast path: handle an already committed request.
        var existingEvent = ledgerEventRepository
                .findByIdempotencyKey(request.getIdempotencyKey());

        if (existingEvent.isPresent()) {
            return replayExistingPurchase(existingEvent.get(), request);
        }

        Party supplier = partyRepository.findById(request.getSupplierId())
                .orElseThrow(
                        () -> new PartyNotFoundException(request.getSupplierId()));

        if (supplier.getPartyType() != PartyType.SUPPLIER) {
            throw new IllegalArgumentException(
                    "The selected party is not a supplier.");
        }

        if (!supplier.isActive()) {
            throw new IllegalArgumentException(
                    "The selected supplier is inactive.");
        }

        Item item = itemRepository.findById(request.getItemId())
                .orElseThrow(
                        () -> new ItemNotFoundException(request.getItemId()));

        if (!item.isActive()) {
            throw new IllegalArgumentException(
                    "The selected item is inactive.");
        }

        if (request.getUnitCost().scale() > 2) {
            throw new IllegalArgumentException(
                    "Unit cost must have at most two decimal places.");
        }

        BigDecimal amount = request.getUnitCost()
                .multiply(BigDecimal.valueOf(request.getQuantity()))
                .setScale(2);

        // Create missing rows safely.
        itemStockRepository.initializeStockIfMissing(item.getItemId());
        partyBalanceRepository.initializeBalanceIfMissing(
                supplier.getPartyId());

        // Lock rows in a consistent order.
        ItemStock stock = itemStockRepository
                .findByItemIdForUpdate(item.getItemId())
                .orElseThrow(() -> new IllegalStateException(
                        "Stock row could not be initialized."));

        PartyBalance balance = partyBalanceRepository
                .findByPartyIdForUpdate(supplier.getPartyId())
                .orElseThrow(() -> new IllegalStateException(
                        "Supplier balance row could not be initialized."));

        // Recheck after acquiring locks to avoid duplicate processing
        // when concurrent requests use the same key for this item.
        existingEvent = ledgerEventRepository
                .findByIdempotencyKey(request.getIdempotencyKey());

        if (existingEvent.isPresent()) {
            return replayExistingPurchase(existingEvent.get(), request);
        }

        int newStockQuantity;
        try {
            newStockQuantity = Math.addExact(
                    stock.getAvailableQuantity(),
                    request.getQuantity());
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "The resulting stock quantity is too large.");
        }

        BigDecimal newSupplierBalance = balance.getBalance().add(amount);

        LedgerEvent event = LedgerEvent.createPurchase(
            supplier,
            item,
            request.getQuantity(),
            request.getUnitCost(),
            amount,
            request.getIdempotencyKey(),
            request.getCreatedBy().trim(),
            request.getNotes()
        );

        LedgerEvent savedEvent = ledgerEventRepository.save(event);

        stock.setAvailableQuantity(newStockQuantity);
        stock.setLastStockUpdatedAt(OffsetDateTime.now());

        balance.setBalance(newSupplierBalance);
        balance.setUpdatedAt(OffsetDateTime.now());

        itemStockRepository.save(stock);
        partyBalanceRepository.save(balance);

        PurchaseResponse response = new PurchaseResponse();
        response.setEventId(savedEvent.getId());
        response.setEventType(savedEvent.getEventType().name());
        response.setAmount(savedEvent.getAmount());
        response.setNewStockQuantity(newStockQuantity);
        response.setNewSupplierBalance(newSupplierBalance);

        return response;
    }

    private PurchaseResponse replayExistingPurchase(
            LedgerEvent event,
            PurchaseRequest request) {

        boolean sameRequest =
                event.getParty().getPartyId().equals(request.getSupplierId())
                && event.getItem() != null
                && event.getItem().getItemId().equals(request.getItemId())
                && event.getQuantity().equals(request.getQuantity())
                && event.getUnitPrice().compareTo(request.getUnitCost()) == 0;

        if (!sameRequest) {
            throw new IdempotencyConflictException(
                    "This idempotency key has already been used "
                    + "for a different purchase.");
        }

        ItemStock stock = itemStockRepository
                .findById(request.getItemId())
                .orElseThrow(() -> new IllegalStateException(
                        "Stock record not found for the recorded purchase."));

        PartyBalance balance = partyBalanceRepository
                .findById(request.getSupplierId())
                .orElseThrow(() -> new IllegalStateException(
                        "Supplier balance record not found."));

        PurchaseResponse response = new PurchaseResponse();
        response.setEventId(event.getId());
        response.setEventType(event.getEventType().name());
        response.setAmount(event.getAmount());
        response.setNewStockQuantity(stock.getAvailableQuantity());
        response.setNewSupplierBalance(balance.getBalance());

        return response;
    }
}
