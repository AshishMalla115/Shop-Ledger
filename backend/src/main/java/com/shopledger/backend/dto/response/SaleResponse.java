
package com.shopledger.backend.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public class SaleResponse {

    private UUID eventId;
    private String eventType;
    private BigDecimal amount;
    private Integer newStockQuantity;
    private BigDecimal newPartyBalance;

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Integer getNewStockQuantity() {
        return newStockQuantity;
    }

    public void setNewStockQuantity(Integer newStockQuantity) {
        this.newStockQuantity = newStockQuantity;
    }

    public BigDecimal getNewPartyBalance() {
        return newPartyBalance;
    }

    public void setNewPartyBalance(BigDecimal newPartyBalance) {
        this.newPartyBalance = newPartyBalance;
    }
}
