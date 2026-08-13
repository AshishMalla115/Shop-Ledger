package com.shopledger.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import com.shopledger.backend.enums.PartyType;
import jakarta.persistence.EnumType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "party")
public class Party
{
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
    private boolean active=true;

    @Column(
        name = "created_at",
        nullable = false,
        updatable = false
    )
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "address")
    private String address;


}