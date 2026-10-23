
package com.shopledger.backend.repository;

import com.shopledger.backend.entity.LedgerEvent;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;

import java.util.Optional;
import java.util.UUID;

public interface LedgerEventRepository
        extends JpaRepository<LedgerEvent, UUID> {

    Optional<LedgerEvent> findByIdempotencyKey(UUID idempotencyKey);

    Page<LedgerEvent> findByPartyPartyIdOrderByOccurredAtDesc(
            Long partyId,
            Pageable pageable
    );

    @Query("""
        SELECT COALESCE(SUM(event.amount), 0)
        FROM LedgerEvent event
        WHERE event.party.partyId = :partyId
        """)
    BigDecimal calculateBalanceByPartyId(
            @Param("partyId") Long partyId
    );
    long countByIdempotencyKey(UUID idempotencyKey);
}
