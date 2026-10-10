package com.shopledger.backend.dto.request;

import java.math.BigDecimal;

import com.shopledger.backend.enums.ItemCategory;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public class ItemCreateRequest {

    @NotBlank(message = "Item name is required")
    @Size(max = 150, message = "Item name cannot exceed 150 characters")
    private String itemName;

    @Size(max = 100, message = "Manufacturer part number cannot exceed 100 characters")
    private String manufacturerPartNumber;

    @Size(max = 100, message = "Search reference cannot exceed 100 characters")
    private String searchReference;

    @NotNull(message = "Item category is required")
    private ItemCategory itemCategory;

    @NotBlank(message = "Item brand is required")
    @Size(max = 100, message = "Item brand cannot exceed 100 characters")
    private String itemBrand;

    @Size(max = 100, message = "Shelf location cannot exceed 100 characters")
    private String shelfLocation;

    @NotNull(message = "Default selling price is required")
    @DecimalMin(value = "0.00", message = "Selling price cannot be negative")
    private BigDecimal defaultSellingPrice = BigDecimal.ZERO;

    @NotNull(message = "Minimum stock is required")
    @PositiveOrZero(message = "Minimum stock cannot be negative")
    private Integer minimumStock = 0;

    public ItemCreateRequest() {
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getManufacturerPartNumber() {
        return manufacturerPartNumber;
    }

    public void setManufacturerPartNumber(String manufacturerPartNumber) {
        this.manufacturerPartNumber = manufacturerPartNumber;
    }

    public String getSearchReference() {
        return searchReference;
    }

    public void setSearchReference(String searchReference) {
        this.searchReference = searchReference;
    }

    public ItemCategory getItemCategory() {
        return itemCategory;
    }

    public void setItemCategory(ItemCategory itemCategory) {
        this.itemCategory = itemCategory;
    }

    public String getItemBrand() {
        return itemBrand;
    }

    public void setItemBrand(String itemBrand) {
        this.itemBrand = itemBrand;
    }

    public String getShelfLocation() {
        return shelfLocation;
    }

    public void setShelfLocation(String shelfLocation) {
        this.shelfLocation = shelfLocation;
    }

    public BigDecimal getDefaultSellingPrice() {
        return defaultSellingPrice;
    }

    public void setDefaultSellingPrice(BigDecimal defaultSellingPrice) {
        this.defaultSellingPrice = defaultSellingPrice;
    }

    public Integer getMinimumStock() {
        return minimumStock;
    }

    public void setMinimumStock(Integer minimumStock) {
        this.minimumStock = minimumStock;
    }
}
