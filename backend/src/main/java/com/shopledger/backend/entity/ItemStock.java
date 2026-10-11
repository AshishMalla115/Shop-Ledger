package com.shopledger.backend.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "item_stock")
public class ItemStock {

    @Id
    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity = 0;

    @Column(name = "last_stock_updated_at", nullable = false)
    private OffsetDateTime lastStockUpdatedAt;

    protected ItemStock() {
    }

    public ItemStock(Item item) {
        this.item = item;
        this.availableQuantity = 0;
        this.lastStockUpdatedAt = OffsetDateTime.now();
    }

    public Long getItemId() {
        return itemId;
    }

    public Item getItem() {
        return item;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public OffsetDateTime getLastStockUpdatedAt() {
        return lastStockUpdatedAt;
    }

    public void setLastStockUpdatedAt(OffsetDateTime lastStockUpdatedAt) {
        this.lastStockUpdatedAt = lastStockUpdatedAt;
    }
}
