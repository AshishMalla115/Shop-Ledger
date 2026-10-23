
package com.shopledger.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.TimeZone;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.shopledger.backend.dto.request.PaymentRequest;
import com.shopledger.backend.enums.LedgerEventType;
import com.shopledger.backend.enums.PartyType;
import com.shopledger.backend.enums.PaymentDirection;
import com.shopledger.backend.repository.LedgerEventRepository;
import com.shopledger.backend.repository.PartyBalanceRepository;
import com.shopledger.backend.service.PartyService;
import com.shopledger.backend.service.PaymentService;
import com.shopledger.backend.dto.request.PartyCreateRequest;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.CountDownLatch;

@Testcontainers
@SpringBootTest
class PaymentIntegrationTest {

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
    }

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18")
                    .withDatabaseName("shop_ledger_test")
                    .withUsername("test_user")
                    .withPassword("test_password");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add(
            "spring.datasource.driver-class-name",
            postgres::getDriverClassName
    );
    }

    @Autowired
    private PartyService partyService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private LedgerEventRepository ledgerEventRepository;

    @Autowired
    private PartyBalanceRepository partyBalanceRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;
    @Test
    void paymentPersistsLedgerEventAndUpdatesPartyBalance() {
        // Arrange: create a supplier in the temporary PostgreSQL database.
        PartyCreateRequest partyRequest = new PartyCreateRequest();
        partyRequest.setPartyName("Integration Test Supplier");
        partyRequest.setPhoneNumber("9876543210");
        partyRequest.setPartyType(PartyType.SUPPLIER);

        Long partyId = partyService.createParty(partyRequest).getPartyId();

        UUID idempotencyKey = UUID.randomUUID();

        PaymentRequest paymentRequest = new PaymentRequest(
                partyId,
                new BigDecimal("450.00"),
                PaymentDirection.MADE,
                idempotencyKey,
                "integration-test",
                "Test supplier payment"
        );

        // Act: execute the actual payment service.
        var result = paymentService.makePayment(paymentRequest);

        // Assert: verify the ledger event was persisted.
        assertThat(result.created()).isTrue();

        var savedEvent = ledgerEventRepository
                .findByIdempotencyKey(idempotencyKey);

        assertThat(savedEvent).isPresent();
        assertThat(savedEvent.get().getEventType())
                .isEqualTo(LedgerEventType.PAYMENT_MADE);
        assertThat(savedEvent.get().getAmount())
                .isEqualByComparingTo("-450.00");
        assertThat(savedEvent.get().getParty().getPartyId())
                .isEqualTo(partyId);

        // Assert: verify the materialized party balance was updated.
        var savedBalance = partyBalanceRepository.findByPartyId(partyId);

        assertThat(savedBalance).isPresent();
        assertThat(savedBalance.get().getBalance())
                .isEqualByComparingTo("-450.00");
    }

    @Test
    void paymentRollsBackWhenDatabaseOperationFails() {
        // Arrange: create a supplier before starting the transaction under test.
        PartyCreateRequest partyRequest = new PartyCreateRequest();
        partyRequest.setPartyName("Rollback Test Supplier");
        partyRequest.setPhoneNumber("9876543211");
        partyRequest.setPartyType(PartyType.SUPPLIER);

        Long partyId = partyService.createParty(partyRequest).getPartyId();
        UUID idempotencyKey = UUID.randomUUID();

        PaymentRequest paymentRequest = new PaymentRequest(
                partyId,
                new BigDecimal("300.00"),
                PaymentDirection.MADE,
                idempotencyKey,
                "integration-test",
                "Rollback test"
        );

        // Act: execute payment and deliberately violate the unique key constraint
        // before the surrounding transaction can commit.
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                new TransactionTemplate(transactionManager)
                        .executeWithoutResult(status -> {
                            paymentService.makePayment(paymentRequest);

                            // Force pending JPA changes to reach PostgreSQL.
                            ledgerEventRepository.flush();

                            // Reuse the same idempotency key to trigger a
                            // PostgreSQL unique-constraint violation.
                            jdbcTemplate.update("""
                                    INSERT INTO ledger_event
                                        (party_id, event_type, amount,
                                        idempotency_key, created_by)
                                    VALUES (?, 'PAYMENT_MADE', 0.00, ?, ?)
                                    """,
                                    partyId,
                                    idempotencyKey,
                                    "rollback-test"
                            );
                        })
        ).isInstanceOf(DataIntegrityViolationException.class);

        // Assert: the complete transaction was rolled back.
        assertThat(ledgerEventRepository.findByIdempotencyKey(idempotencyKey))
                .isEmpty();

        var balance = partyBalanceRepository.findByPartyId(partyId);

        assertThat(balance).isPresent();
        assertThat(balance.get().getBalance())
                .isEqualByComparingTo("0.00");
    }

    @Test
    void repeatedPaymentRequestDoesNotCreateDuplicateEvent() {
        // Arrange: create a supplier in the test database.
        PartyCreateRequest partyRequest = new PartyCreateRequest();
        partyRequest.setPartyName("Idempotency Test Supplier");
        partyRequest.setPhoneNumber("9876543212");
        partyRequest.setPartyType(PartyType.SUPPLIER);

        Long partyId = partyService.createParty(partyRequest).getPartyId();

        UUID idempotencyKey = UUID.randomUUID();

        PaymentRequest request = new PaymentRequest(
                partyId,
                new BigDecimal("450.00"),
                PaymentDirection.MADE,
                idempotencyKey,
                "integration-test",
                "Idempotency retry test"
        );

        // Act: submit the same request twice.
        var firstResult = paymentService.makePayment(request);
        var secondResult = paymentService.makePayment(request);

        // Assert: the first request creates an event; the retry does not.
        assertThat(firstResult.created()).isTrue();
        assertThat(secondResult.created()).isFalse();

        assertThat(secondResult.response().eventId())
                .isEqualTo(firstResult.response().eventId());

        // Exactly one ledger event should exist for this idempotency key.
        assertThat(ledgerEventRepository.findByIdempotencyKey(idempotencyKey))
                .isPresent();

        assertThat(ledgerEventRepository.countByIdempotencyKey(idempotencyKey))
                .isEqualTo(1);

        // The payment must be applied only once.
        var balance = partyBalanceRepository.findByPartyId(partyId);

        assertThat(balance).isPresent();
        assertThat(balance.get().getBalance())
                .isEqualByComparingTo("-450.00");
    }

    @Test
    void concurrentPaymentsDoNotLoseBalanceUpdates() throws Exception {
        // Arrange: create one supplier.
        PartyCreateRequest partyRequest = new PartyCreateRequest();
        partyRequest.setPartyName("Concurrent Payment Supplier");
        partyRequest.setPhoneNumber("9876543213");
        partyRequest.setPartyType(PartyType.SUPPLIER);

        Long partyId = partyService.createParty(partyRequest).getPartyId();

        PaymentRequest firstRequest = new PaymentRequest(
                partyId,
                new BigDecimal("100.00"),
                PaymentDirection.MADE,
                UUID.randomUUID(),
                "integration-test",
                "Concurrent payment A"
        );

        PaymentRequest secondRequest = new PaymentRequest(
                partyId,
                new BigDecimal("200.00"),
                PaymentDirection.MADE,
                UUID.randomUUID(),
                "integration-test",
                "Concurrent payment B"
        );

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            // Ensure both tasks are submitted independently.
            Callable<?> firstTask = () -> paymentService.makePayment(firstRequest);
            Callable<?> secondTask = () -> paymentService.makePayment(secondRequest);

            Future<?> firstFuture = executor.submit(firstTask);
            Future<?> secondFuture = executor.submit(secondTask);

            // get() propagates any exception raised by either payment.
            firstFuture.get(20, TimeUnit.SECONDS);
            secondFuture.get(20, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        // Assert: both independent events were saved.
        assertThat(ledgerEventRepository.findByIdempotencyKey(
                firstRequest.idempotencyKey())).isPresent();

        assertThat(ledgerEventRepository.findByIdempotencyKey(
                secondRequest.idempotencyKey())).isPresent();

        // Both payments reduce the supplier balance.
        var balance = partyBalanceRepository.findByPartyId(partyId);

        assertThat(balance).isPresent();
        assertThat(balance.get().getBalance())
                .isEqualByComparingTo("-300.00");
    }

    @Test
    void concurrentRequestsWithSameIdempotencyKeyDoNotDuplicatePayment()
            throws Exception {

        PartyCreateRequest partyRequest = new PartyCreateRequest();
        partyRequest.setPartyName("Same Key Concurrency Supplier");
        partyRequest.setPhoneNumber("9876543214");
        partyRequest.setPartyType(PartyType.SUPPLIER);

        Long partyId = partyService.createParty(partyRequest).getPartyId();

        UUID idempotencyKey = UUID.randomUUID();

        PaymentRequest request = new PaymentRequest(
                partyId,
                new BigDecimal("450.00"),
                PaymentDirection.MADE,
                idempotencyKey,
                "integration-test",
                "Same-key concurrency test"
        );

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Callable<?> task = () -> {
                ready.countDown();

                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException(
                            "Timed out waiting for concurrent start");
                }

                return paymentService.makePayment(request);
            };

            Future<?> first = executor.submit(task);
            Future<?> second = executor.submit(task);

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();

            // Release both workers as close together as possible.
            start.countDown();

            first.get(20, TimeUnit.SECONDS);
            second.get(20, TimeUnit.SECONDS);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }

        assertThat(ledgerEventRepository.countByIdempotencyKey(idempotencyKey))
                .isEqualTo(1);

        var balance = partyBalanceRepository.findByPartyId(partyId);

        assertThat(balance).isPresent();
        assertThat(balance.get().getBalance())
                .isEqualByComparingTo("-450.00");
    }

}
