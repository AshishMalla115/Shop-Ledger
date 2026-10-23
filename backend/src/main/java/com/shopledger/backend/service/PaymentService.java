
package com.shopledger.backend.service;

import com.shopledger.backend.dto.request.PaymentRequest;
import com.shopledger.backend.dto.response.PaymentResponse;
import com.shopledger.backend.entity.LedgerEvent;
import com.shopledger.backend.entity.Party;
import com.shopledger.backend.entity.PartyBalance;
import com.shopledger.backend.enums.LedgerEventType;
import com.shopledger.backend.enums.PartyType;
import com.shopledger.backend.enums.PaymentDirection;
import com.shopledger.backend.exception.IdempotencyConflictException;
import com.shopledger.backend.exception.PartyNotFoundException;
import com.shopledger.backend.repository.LedgerEventRepository;
import com.shopledger.backend.repository.PartyBalanceRepository;
import com.shopledger.backend.repository.PartyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

@Service
public class PaymentService {

    private final PartyRepository partyRepository;
    private final LedgerEventRepository ledgerEventRepository;
    private final PartyBalanceRepository partyBalanceRepository;

    public PaymentService(
            PartyRepository partyRepository,
            LedgerEventRepository ledgerEventRepository,
            PartyBalanceRepository partyBalanceRepository) {
        this.partyRepository = partyRepository;
        this.ledgerEventRepository = ledgerEventRepository;
        this.partyBalanceRepository = partyBalanceRepository;
    }

    @Transactional
    public PaymentResult makePayment(PaymentRequest request) {

        var existingEvent = ledgerEventRepository
                .findByIdempotencyKey(request.idempotencyKey());

        if (existingEvent.isPresent()) {
            return replayExistingPayment(existingEvent.get(), request);
        }

        Party party = partyRepository.findById(request.partyId())
                .orElseThrow(() ->
                        new PartyNotFoundException(request.partyId()));

        validatePartyAndDirection(party, request.direction());

        if (request.amount().scale() > 2) {
            throw new IllegalArgumentException(
                    "Payment amount cannot have more than 2 decimal places.");
        }

        LedgerEventType eventType = getEventType(request.direction());

        BigDecimal ledgerAmount = request.amount()
                .negate()
                .setScale(2);

        partyBalanceRepository.initializeBalanceIfMissing(
                request.partyId());

        PartyBalance balance = partyBalanceRepository
                .findByPartyIdForUpdate(request.partyId())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Party balance could not be initialized."));

        // Recheck after acquiring the balance lock.
        existingEvent = ledgerEventRepository
                .findByIdempotencyKey(request.idempotencyKey());

        if (existingEvent.isPresent()) {
            return replayExistingPayment(existingEvent.get(), request);
        }

        BigDecimal newBalance = balance.getBalance()
                .add(ledgerAmount);

        LedgerEvent event = LedgerEvent.createPayment(
                party,
                eventType,
                ledgerAmount,
                request.idempotencyKey(),
                request.createdBy().trim(),
                request.notes());

        LedgerEvent savedEvent = ledgerEventRepository.save(event);

        balance.setBalance(newBalance);
        balance.setUpdatedAt(OffsetDateTime.now());
        partyBalanceRepository.save(balance);

        return new PaymentResult(
        new PaymentResponse(
                savedEvent.getId(),
                savedEvent.getEventType().name(),
                savedEvent.getAmount(),
                newBalance),
        true);
    }

    private void validatePartyAndDirection(
            Party party,
            PaymentDirection direction) {

        if (!party.isActive()) {
            throw new IllegalArgumentException(
                    "Cannot record a payment for an inactive party.");
        }

        if (direction == PaymentDirection.MADE
                && party.getPartyType() != PartyType.SUPPLIER) {
            throw new IllegalArgumentException(
                    "Payments made are allowed only for suppliers.");
        }

        if (direction == PaymentDirection.RECEIVED
                && party.getPartyType() != PartyType.PURCHASER
                && party.getPartyType() != PartyType.GARAGE) {
            throw new IllegalArgumentException(
                    "Payments received are allowed only from purchasers or garages.");
        }
    }

    private LedgerEventType getEventType(PaymentDirection direction) {
        return direction == PaymentDirection.MADE
                ? LedgerEventType.PAYMENT_MADE
                : LedgerEventType.PAYMENT_RECEIVED;
    }

    private PaymentResult replayExistingPayment(
            LedgerEvent event,
            PaymentRequest request) {

        LedgerEventType expectedType = getEventType(request.direction());

        boolean sameRequest =
                event.getEventType() == expectedType
                && event.getParty().getPartyId().equals(request.partyId())
                && event.getAmount().compareTo(
                        request.amount().negate()) == 0
                && Objects.equals(
                        event.getCreatedBy(), request.createdBy().trim())
                && Objects.equals(event.getNotes(), request.notes());

        if (!sameRequest) {
            throw new IdempotencyConflictException(
                    "This idempotency key has already been used for a different payment.");
        }

        PartyBalance balance = partyBalanceRepository
                .findByPartyId(request.partyId())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Party balance not found for an existing payment."));

        return new PaymentResult(
        new PaymentResponse(
                event.getId(),
                event.getEventType().name(),
                event.getAmount(),
                balance.getBalance()),
        false);
    }
}
