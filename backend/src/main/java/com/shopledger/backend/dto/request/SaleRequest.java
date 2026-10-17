
package com.shopledger.backend.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class SaleRequest {

    @NotNull(message = "Item ID is required.")
    @Positive(message = "Item ID must be positive.")
    private Long itemId;

    @NotNull(message = "Party ID is required.")
    @Positive(message = "Party ID must be positive.")
    private Long partyId;

    @NotNull(message = "Quantity is required.")
    @Positive(message = "Quantity must be positive.")
    private Integer quantity;

    @NotNull(message = "Unit selling price is required.")
    @DecimalMin(value = "0.00", message = "Unit selling price cannot be negative.")
    private BigDecimal unitPrice;

    @NotNull(message = "Idempotency key is required.")
    private UUID idempotencyKey;

    @NotNull(message = "Created by is required.")
    @Size(min = 1, max = 100, message = "Created by must contain 1–100 characters.")
    private String createdBy;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters.")
    private String notes;

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Long getPartyId() {
        return partyId;
    }

    public void setPartyId(Long partyId) {
        this.partyId = partyId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public UUID getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(UUID idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
