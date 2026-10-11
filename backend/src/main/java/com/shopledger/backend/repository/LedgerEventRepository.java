package com.shopledger.backend.repository;

import com.shopledger.backend.entity.LedgerEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LedgerEventRepository
        extends JpaRepository<LedgerEvent, UUID> {

    Optional<LedgerEvent> findByIdempotencyKey(UUID idempotencyKey);
}
