package com.shopledger.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import jakarta.persistence.LockModeType;

import com.shopledger.backend.entity.ItemStock;


public interface ItemStockRepository
        extends JpaRepository<ItemStock, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT stock
        FROM ItemStock stock
        WHERE stock.itemId = :itemId
        """)
    Optional<ItemStock> findByItemIdForUpdate(
            @Param("itemId") Long itemId
    );
    @Modifying
    @Query(value = """
        INSERT INTO item_stock
            (item_id, available_quantity, last_stock_updated_at)
        VALUES (:itemId, 0, NOW())
        ON CONFLICT (item_id) DO NOTHING
        """, nativeQuery = true)
    int initializeStockIfMissing(@Param("itemId") Long itemId);
}
