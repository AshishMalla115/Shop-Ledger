
package com.shopledger.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.shopledger.backend.dto.request.PaymentRequest;
import com.shopledger.backend.entity.LedgerEvent;
import com.shopledger.backend.entity.Party;
import com.shopledger.backend.entity.PartyBalance;
import com.shopledger.backend.enums.LedgerEventType;
import com.shopledger.backend.enums.PartyType;
import com.shopledger.backend.enums.PaymentDirection;
import com.shopledger.backend.exception.IdempotencyConflictException;
import com.shopledger.backend.repository.LedgerEventRepository;
import com.shopledger.backend.repository.PartyBalanceRepository;
import com.shopledger.backend.repository.PartyRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PartyRepository partyRepository;

    @Mock
    private LedgerEventRepository ledgerEventRepository;

    @Mock
    private PartyBalanceRepository partyBalanceRepository;

    @InjectMocks
    private PaymentService paymentService;

    private Party supplier;
    private Party customer;
    private PartyBalance balance;

    private final UUID idempotencyKey =
            UUID.fromString("a1b2c3d4-e5f6-4789-8123-456789abcdef");


    @BeforeEach
    void setUp() {
        supplier = org.mockito.Mockito.mock(Party.class);
        customer = org.mockito.Mockito.mock(Party.class);
        balance = org.mockito.Mockito.mock(PartyBalance.class);
    }


    private PaymentRequest createRequest(
            Long partyId,
            BigDecimal amount,
            PaymentDirection direction) {

        return new PaymentRequest(
                partyId,
                amount,
                direction,
                idempotencyKey,
                "Ashish",
                "Payment service test");
    }


    private void prepareNewPayment(
            PaymentRequest request,
            Party party,
            BigDecimal startingBalance) {


        when(party.getPartyType()).thenReturn(
                request.direction() == PaymentDirection.MADE
                        ? PartyType.SUPPLIER
                        : PartyType.PURCHASER);
        when(party.isActive()).thenReturn(true);

        when(ledgerEventRepository.findByIdempotencyKey(
                request.idempotencyKey()))
                .thenReturn(Optional.empty());

        when(partyRepository.findById(request.partyId()))
                .thenReturn(Optional.of(party));

        when(partyBalanceRepository.findByPartyIdForUpdate(
                request.partyId()))
                .thenReturn(Optional.of(balance));

        when(balance.getBalance()).thenReturn(startingBalance);
    }


    @Test
    void makePayment_supplierPaymentUpdatesBalance() {
        PaymentRequest request = createRequest(
                1L,
                new BigDecimal("450.00"),
                PaymentDirection.MADE);

        prepareNewPayment(
                request, supplier, new BigDecimal("0.00"));

        when(ledgerEventRepository.save(any(LedgerEvent.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResult result = paymentService.makePayment(request);

        assertTrue(result.created());
        assertEquals("PAYMENT_MADE", result.response().eventType());
        assertEquals(0, result.response().amount().compareTo(
                new BigDecimal("-450.00")));
        assertEquals(0, result.response().newPartyBalance().compareTo(
                new BigDecimal("-450.00")));

        verify(balance).setBalance(new BigDecimal("-450.00"));
        verify(ledgerEventRepository).save(any(LedgerEvent.class));
        verify(partyBalanceRepository).save(balance);
    }

    @Test
    void makePayment_customerPaymentReceivedUpdatesBalance() {
        PaymentRequest request = createRequest(
                3L,
                new BigDecimal("100.00"),
                PaymentDirection.RECEIVED);

        prepareNewPayment(
                request, customer, new BigDecimal("240.00"));

        when(ledgerEventRepository.save(any(LedgerEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResult result = paymentService.makePayment(request);

        assertTrue(result.created());
        assertEquals("PAYMENT_RECEIVED", result.response().eventType());
        assertEquals(0, result.response().amount().compareTo(
                new BigDecimal("-100.00")));
        assertEquals(0, result.response().newPartyBalance().compareTo(
                new BigDecimal("140.00")));

        verify(balance).setBalance(new BigDecimal("140.00"));
        verify(ledgerEventRepository).save(any(LedgerEvent.class));
        verify(partyBalanceRepository).save(balance);
    }

    @Test
    void makePayment_replaysExistingPaymentWithoutSavingDuplicate() {
        PaymentRequest request = createRequest(
                1L,
                new BigDecimal("450.00"),
                PaymentDirection.MADE);

        LedgerEvent existingEvent =
                org.mockito.Mockito.mock(LedgerEvent.class);

        when(ledgerEventRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(existingEvent));

        when(existingEvent.getEventType())
                .thenReturn(LedgerEventType.PAYMENT_MADE);
        when(existingEvent.getParty()).thenReturn(supplier);
        when(existingEvent.getAmount())
                .thenReturn(new BigDecimal("-450.00"));
        when(existingEvent.getCreatedBy()).thenReturn("Ashish");
        when(existingEvent.getNotes()).thenReturn("Payment service test");
        when(existingEvent.getId()).thenReturn(UUID.randomUUID());

        when(partyBalanceRepository.findByPartyId(1L))
                .thenReturn(Optional.of(balance));
        when(balance.getBalance()).thenReturn(new BigDecimal("-450.00"));

        when(supplier.getPartyId()).thenReturn(1L);

        PaymentResult result = paymentService.makePayment(request);

        assertFalse(result.created());
        assertEquals("PAYMENT_MADE", result.response().eventType());
        assertEquals(0, result.response().newPartyBalance().compareTo(
                new BigDecimal("-450.00")));

        verify(ledgerEventRepository, never())
                .save(any(LedgerEvent.class));
        verify(partyBalanceRepository, never()).save(any(PartyBalance.class));
    }

    @Test
    void makePayment_rejectsIdempotencyKeyUsedForDifferentAmount() {
        PaymentRequest request = createRequest(
                1L,
                new BigDecimal("500.00"),
                PaymentDirection.MADE);

        LedgerEvent existingEvent =
                org.mockito.Mockito.mock(LedgerEvent.class);

        when(ledgerEventRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(existingEvent));

        when(existingEvent.getEventType())
                .thenReturn(LedgerEventType.PAYMENT_MADE);
        when(existingEvent.getParty()).thenReturn(supplier);
        when(existingEvent.getAmount())
                .thenReturn(new BigDecimal("-450.00"));

        when(supplier.getPartyId()).thenReturn(1L);

        assertThrows(
                IdempotencyConflictException.class,
                () -> paymentService.makePayment(request));

        verify(ledgerEventRepository, never())
                .save(any(LedgerEvent.class));
        verify(partyBalanceRepository, never()).save(any(PartyBalance.class));
    }
}
