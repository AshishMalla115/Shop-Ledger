
package com.shopledger.backend.service;

import com.shopledger.backend.dto.response.LedgerEventResponse;
import com.shopledger.backend.dto.response.LedgerHistoryResponse;
import com.shopledger.backend.entity.LedgerEvent;
import com.shopledger.backend.entity.Party;
import com.shopledger.backend.entity.PartyBalance;
import com.shopledger.backend.exception.PartyNotFoundException;
import com.shopledger.backend.repository.LedgerEventRepository;
import com.shopledger.backend.repository.PartyBalanceRepository;
import com.shopledger.backend.repository.PartyRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class LedgerHistoryService {

    private final PartyRepository partyRepository;
    private final LedgerEventRepository ledgerEventRepository;
    private final PartyBalanceRepository partyBalanceRepository;

    public LedgerHistoryService(
            PartyRepository partyRepository,
            LedgerEventRepository ledgerEventRepository,
            PartyBalanceRepository partyBalanceRepository) {
        this.partyRepository = partyRepository;
        this.ledgerEventRepository = ledgerEventRepository;
        this.partyBalanceRepository = partyBalanceRepository;
    }

    @Transactional(readOnly = true)
    public LedgerHistoryResponse getEventHistory(
            Long partyId,
            int page,
            int size) {

        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page number cannot be negative"
            );
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and 100"
            );
        }

        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new PartyNotFoundException(partyId));

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("occurredAt"),
                        Sort.Order.desc("id")
                )
        );

        Page<LedgerEvent> eventPage =
                ledgerEventRepository
                        .findByPartyPartyIdOrderByOccurredAtDesc(
                                partyId,
                                pageable
                        );

        List<LedgerEventResponse> events = eventPage
                .getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        BigDecimal calculatedBalance =
            ledgerEventRepository.calculateBalanceByPartyId(partyId);

        BigDecimal cachedBalance = partyBalanceRepository
                .findByPartyId(partyId)
                .map(PartyBalance::getBalance)
                .orElse(BigDecimal.ZERO);

        return new LedgerHistoryResponse(
                party.getPartyId(),
                party.getPartyName(),
                calculatedBalance,
                cachedBalance,
                calculatedBalance.compareTo(cachedBalance) == 0,
                events,
                page,
                size,
                eventPage.getTotalElements(),
                eventPage.getTotalPages()
        );
    }

    private LedgerEventResponse toResponse(LedgerEvent event) {
        return new LedgerEventResponse(
                event.getId(),
                event.getParty().getPartyId(),
                event.getParty().getPartyName(),
                event.getItem() != null
                        ? event.getItem().getItemId()
                        : null,
                event.getItem() != null
                        ? event.getItem().getItemName()
                        : null,
                event.getEventType(),
                event.getQuantity(),
                event.getUnitPrice(),
                event.getAmount(),
                event.getOccurredAt(),
                event.getCreatedBy(),
                event.getNotes()
        );
    }
}
