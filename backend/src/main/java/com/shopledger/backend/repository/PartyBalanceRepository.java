package com.shopledger.backend.repository;

import java.util.Optional;

import com.shopledger.backend.entity.PartyBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface PartyBalanceRepository
        extends JpaRepository<PartyBalance, Long> {
        @Modifying
        @Query(value = """
            INSERT INTO party_balance
                (party_id, balance, version, updated_at)
            VALUES (:partyId, 0.00, 0, NOW())
            ON CONFLICT (party_id) DO NOTHING
            """, nativeQuery = true)
        int initializeBalanceIfMissing(@Param("partyId") Long partyId);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("""
            SELECT balance
            FROM PartyBalance balance
            WHERE balance.partyId = :partyId
            """)
        Optional<PartyBalance> findByPartyIdForUpdate(
                @Param("partyId") Long partyId
        );
        Optional<PartyBalance> findByPartyId(Long partyId);
}
