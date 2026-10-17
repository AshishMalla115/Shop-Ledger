
package com.shopledger.backend.service;

import com.shopledger.backend.dto.response.LedgerHistoryResponse;
import com.shopledger.backend.entity.LedgerEvent;
import com.shopledger.backend.entity.Party;
import com.shopledger.backend.entity.PartyBalance;
import com.shopledger.backend.exception.PartyNotFoundException;
import com.shopledger.backend.repository.LedgerEventRepository;
import com.shopledger.backend.repository.PartyBalanceRepository;
import com.shopledger.backend.repository.PartyRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerHistoryServiceTest {

    @Mock
    private PartyRepository partyRepository;

    @Mock
    private LedgerEventRepository ledgerEventRepository;

    @Mock
    private PartyBalanceRepository partyBalanceRepository;

    @InjectMocks
    private LedgerHistoryService ledgerHistoryService;

    private Party party;
    private PartyBalance partyBalance;

    @BeforeEach
    void setUp() {
        party = mock(Party.class);
        partyBalance = mock(PartyBalance.class);
    }
    @Test
    void getEventHistory_returnsHistoryAndMatchingBalances() {
        LedgerEvent event = mock(LedgerEvent.class);
        when(party.getPartyId()).thenReturn(3L);
        when(party.getPartyName()).thenReturn("Test Customer");
        when(event.getParty()).thenReturn(party);

        when(partyRepository.findById(3L))
                .thenReturn(Optional.of(party));

        when(ledgerEventRepository
                .findByPartyPartyIdOrderByOccurredAtDesc(
                        eq(3L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(event)));

        when(ledgerEventRepository.calculateBalanceByPartyId(3L))
                .thenReturn(new BigDecimal("240.00"));

        when(partyBalanceRepository.findByPartyId(3L))
                .thenReturn(Optional.of(partyBalance));

        when(partyBalance.getBalance())
                .thenReturn(new BigDecimal("240.00"));

        LedgerHistoryResponse response =
                ledgerHistoryService.getEventHistory(3L, 0, 20);

        assertEquals(3L, response.partyId());
        assertEquals("Test Customer", response.partyName());
        assertEquals(
                0,
                response.calculatedBalance().compareTo(
                        new BigDecimal("240.00"))
        );
        assertEquals(
                0,
                response.cachedBalance().compareTo(
                        new BigDecimal("240.00"))
        );
        assertTrue(response.balanceMatches());
        assertEquals(1, response.totalElements());
        assertEquals(1, response.events().size());
    }

    @Test
    void getEventHistory_detectsBalanceMismatch() {
        when(party.getPartyId()).thenReturn(3L);
        when(party.getPartyName()).thenReturn("Test Customer");
        when(partyRepository.findById(3L))
                .thenReturn(Optional.of(party));

        when(ledgerEventRepository
                .findByPartyPartyIdOrderByOccurredAtDesc(
                        eq(3L), any(Pageable.class)))
                .thenReturn(Page.empty());

        when(ledgerEventRepository.calculateBalanceByPartyId(3L))
                .thenReturn(new BigDecimal("240.00"));

        when(partyBalanceRepository.findByPartyId(3L))
                .thenReturn(Optional.of(partyBalance));

        when(partyBalance.getBalance())
                .thenReturn(new BigDecimal("200.00"));

        LedgerHistoryResponse response =
                ledgerHistoryService.getEventHistory(3L, 0, 20);

        assertFalse(response.balanceMatches());
    }

    @Test
    void getEventHistory_returnsZeroForEmptyLedgerAndMissingCache() {
        when(party.getPartyId()).thenReturn(3L);
        when(party.getPartyName()).thenReturn("Test Customer");
        when(partyRepository.findById(3L))
                .thenReturn(Optional.of(party));

        when(ledgerEventRepository
                .findByPartyPartyIdOrderByOccurredAtDesc(
                        eq(3L), any(Pageable.class)))
                .thenReturn(Page.empty());

        when(ledgerEventRepository.calculateBalanceByPartyId(3L))
                .thenReturn(BigDecimal.ZERO);

        when(partyBalanceRepository.findByPartyId(3L))
                .thenReturn(Optional.empty());

        LedgerHistoryResponse response =
                ledgerHistoryService.getEventHistory(3L, 0, 20);

        assertEquals(0, response.events().size());
        assertEquals(0, response.calculatedBalance().compareTo(
                BigDecimal.ZERO));
        assertEquals(0, response.cachedBalance().compareTo(
                BigDecimal.ZERO));
        assertTrue(response.balanceMatches());
        assertEquals(0, response.totalElements());
    }

    @Test
    void getEventHistory_rejectsUnknownParty() {
        when(partyRepository.findById(99999L))
                .thenReturn(Optional.empty());

        assertThrows(
                PartyNotFoundException.class,
                () -> ledgerHistoryService.getEventHistory(
                        99999L, 0, 20)
        );

        verifyNoInteractions(
                ledgerEventRepository,
                partyBalanceRepository
        );
    }

    @Test
    void getEventHistory_rejectsNegativePage() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ledgerHistoryService.getEventHistory(3L, -1, 20)
        );

        verifyNoInteractions(
                partyRepository,
                ledgerEventRepository,
                partyBalanceRepository
        );
    }

    @Test
    void getEventHistory_rejectsPageSizeAboveLimit() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ledgerHistoryService.getEventHistory(3L, 0, 101)
        );

        verifyNoInteractions(
                partyRepository,
                ledgerEventRepository,
                partyBalanceRepository
        );
    }
}
