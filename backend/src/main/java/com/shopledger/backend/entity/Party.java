
package com.shopledger.backend.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.shopledger.backend.enums.PartyType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "party")
public class Party {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "party_id")
    private Long partyId;

    @Column(name = "party_name", nullable = false, length = 150)
    private String partyName;

    @Column(name = "phone_number", nullable = false, length = 15)
    private String phoneNumber;

    @Column(name = "gst_number", length = 15)
    private String gstNumber;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(
        org.hibernate.type.SqlTypes.NAMED_ENUM
    )
    @Column(name = "party_type", nullable = false)
    private PartyType partyType;

    @Column(
        name = "default_discount_percentage",
        nullable = false,
        precision = 5,
        scale = 2
    )
    private BigDecimal defaultDiscountPercentage = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "address")
    private String address;

    protected Party() {
    }

    public Party(
        String partyName,
        String phoneNumber,
        PartyType partyType
    ) {
        this.partyName = partyName;
        this.phoneNumber = phoneNumber;
        this.partyType = partyType;
    }
    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public Long getPartyId() {
        return partyId;
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

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
