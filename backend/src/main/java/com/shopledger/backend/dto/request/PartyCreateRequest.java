
package com.shopledger.backend.dto.request;

import java.math.BigDecimal;

import com.shopledger.backend.enums.PartyType;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class PartyCreateRequest {

    @NotBlank(message = "Party name is required")
    @Size(max = 150, message = "Party name cannot exceed 150 characters")
    private String partyName;

    @NotBlank(message = "Phone number is required")
    @Size(max = 15, message = "Phone number cannot exceed 15 characters")
    private String phoneNumber;

    @Size(max = 15, message = "GST number cannot exceed 15 characters")
    private String gstNumber;

    @NotNull(message = "Party type is required")
    private PartyType partyType;

    @NotNull(message = "Discount percentage is required")
    @DecimalMin(value = "0.00", message = "Discount cannot be negative")
    @DecimalMax(value = "100.00", message = "Discount cannot exceed 100")
    private BigDecimal defaultDiscountPercentage = BigDecimal.ZERO;

    private String address;

    public PartyCreateRequest() {
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
