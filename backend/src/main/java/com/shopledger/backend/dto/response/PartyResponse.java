
package com.shopledger.backend.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.shopledger.backend.enums.PartyType;

public class PartyResponse {

    private Long partyId;
    private String partyName;
    private String phoneNumber;
    private String gstNumber;
    private PartyType partyType;
    private BigDecimal defaultDiscountPercentage;
    private boolean active;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String address;

    public PartyResponse() {
    }

    public Long getPartyId() {
        return partyId;
    }

    public void setPartyId(Long partyId) {
        this.partyId = partyId;
    }

    public String getPartyName() {
        return partyName;
    }

    public void setPartyName(String partyName) {
        this.partyName = partyName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getGstNumber() {
        return gstNumber;
    }

    public void setGstNumber(String gstNumber) {
        this.gstNumber = gstNumber;
    }

    public PartyType getPartyType() {
        return partyType;
    }

    public void setPartyType(PartyType partyType) {
        this.partyType = partyType;
    }

    public BigDecimal getDefaultDiscountPercentage() {
        return defaultDiscountPercentage;
    }

    public void setDefaultDiscountPercentage(BigDecimal defaultDiscountPercentage) {
        this.defaultDiscountPercentage = defaultDiscountPercentage;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
