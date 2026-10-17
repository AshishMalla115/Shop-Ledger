
package com.shopledger.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import com.shopledger.backend.repository.ItemRepository;
import com.shopledger.backend.repository.ItemStockRepository;
import com.shopledger.backend.repository.LedgerEventRepository;
import com.shopledger.backend.repository.PartyBalanceRepository;
import com.shopledger.backend.repository.PartyRepository;

@ExtendWith(MockitoExtension.class)
class SaleServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private PartyRepository partyRepository;

    @Mock
    private ItemStockRepository itemStockRepository;

    @Mock
    private PartyBalanceRepository partyBalanceRepository;

    @Mock
    private LedgerEventRepository ledgerEventRepository;

    @InjectMocks
    private SaleService saleService;

    private Party customer;
    private Item item;
    private ItemStock stock;
    private PartyBalance balance;
    private SaleRequest request;

    private final UUID idempotencyKey =
            UUID.fromString("b5d1a77e-0a36-4ef0-90c9-8ab11e8a67d2");

    @BeforeEach
    void setUp() {
        customer = org.mockito.Mockito.mock(Party.class);
        item = org.mockito.Mockito.mock(Item.class);
        stock = org.mockito.Mockito.mock(ItemStock.class);
        balance = org.mockito.Mockito.mock(PartyBalance.class);

        when(customer.getPartyId()).thenReturn(3L);
        

        when(item.getItemId()).thenReturn(1L);

        request = new SaleRequest();
        request.setItemId(1L);
        request.setPartyId(3L);
        request.setQuantity(2);
        request.setUnitPrice(new BigDecimal("120.00"));
        request.setIdempotencyKey(idempotencyKey);
        request.setCreatedBy("Ashish");
        request.setNotes("Sale API test");
    }

   private void prepareNewSale(int availableStock) {
        when(customer.getPartyType()).thenReturn(PartyType.PURCHASER);
        when(customer.isActive()).thenReturn(true);
        when(item.isActive()).thenReturn(true);

        when(ledgerEventRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty());

        when(partyRepository.findById(3L))
                .thenReturn(Optional.of(customer));

        when(itemRepository.findById(1L))
                .thenReturn(Optional.of(item));

        when(itemStockRepository.findByItemIdForUpdate(1L))
                .thenReturn(Optional.of(stock));

        when(partyBalanceRepository.findByPartyIdForUpdate(3L))
                .thenReturn(Optional.of(balance));

        when(stock.getAvailableQuantity()).thenReturn(availableStock);
    }
    @Test
    void createSale_successfullyUpdatesStockAndBalance() {
        prepareNewSale(10);

        when(balance.getBalance()).thenReturn(new BigDecimal("0.00"));

        when(ledgerEventRepository.save(any(LedgerEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SaleResponse response = saleService.createSale(request);

        assertEquals("SALE", response.getEventType());
        assertEquals(0, response.getAmount().compareTo(
                new BigDecimal("240.00")));
        assertEquals(8, response.getNewStockQuantity());
        assertEquals(0, response.getNewPartyBalance().compareTo(
                new BigDecimal("240.00")));

        verify(stock).setAvailableQuantity(8);
        verify(balance).setBalance(new BigDecimal("240.00"));
        verify(ledgerEventRepository).save(any(LedgerEvent.class));
        verify(itemStockRepository).save(stock);
        verify(partyBalanceRepository).save(balance);
    }

    @Test
    void createSale_rejectsInsufficientStock() {
        prepareNewSale(1);

        assertThrows(
                IllegalArgumentException.class,
                () -> saleService.createSale(request));

        verify(ledgerEventRepository, never())
                .save(any(LedgerEvent.class));
        verify(itemStockRepository, never()).save(any(ItemStock.class));
        verify(partyBalanceRepository, never())
                .save(any(PartyBalance.class));
    }

    @Test
    void createSale_replaysExistingSaleWithoutCreatingAnotherEvent() {
        LedgerEvent existingEvent =
                org.mockito.Mockito.mock(LedgerEvent.class);

        when(ledgerEventRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(existingEvent));

        when(existingEvent.getEventType()).thenReturn(LedgerEventType.SALE);
        when(existingEvent.getParty()).thenReturn(customer);
        when(existingEvent.getItem()).thenReturn(item);
        when(existingEvent.getQuantity()).thenReturn(2);
        
        when(existingEvent.getId()).thenReturn(UUID.randomUUID());
        when(existingEvent.getAmount()).thenReturn(
                new BigDecimal("240.00"));

        when(itemStockRepository.findById(1L))
                .thenReturn(Optional.of(stock));
        when(partyBalanceRepository.findById(3L))
                .thenReturn(Optional.of(balance));

        when(stock.getAvailableQuantity()).thenReturn(8);
        when(balance.getBalance()).thenReturn(new BigDecimal("240.00"));

        when(existingEvent.getUnitPrice())
            .thenReturn(new BigDecimal("120.00"));

        SaleResponse response = saleService.createSale(request);

        assertEquals("SALE", response.getEventType());
        assertEquals(8, response.getNewStockQuantity());
        assertEquals(0, response.getNewPartyBalance().compareTo(
                new BigDecimal("240.00")));

        verify(ledgerEventRepository, never())
                .save(any(LedgerEvent.class));
        verify(itemStockRepository, never()).save(any(ItemStock.class));
        verify(partyBalanceRepository, never())
                .save(any(PartyBalance.class));
    }

    @Test
    void createSale_rejectsIdempotencyKeyUsedForDifferentRequest() {
        LedgerEvent existingEvent =
                org.mockito.Mockito.mock(LedgerEvent.class);

        when(ledgerEventRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(existingEvent));

        when(existingEvent.getEventType()).thenReturn(LedgerEventType.SALE);
        when(existingEvent.getParty()).thenReturn(customer);
        when(existingEvent.getItem()).thenReturn(item);
        when(existingEvent.getQuantity()).thenReturn(2);
       

        request.setQuantity(1);

        assertThrows(
                IdempotencyConflictException.class,
                () -> saleService.createSale(request));

        verify(ledgerEventRepository, never())
                .save(any(LedgerEvent.class));
    }
}
