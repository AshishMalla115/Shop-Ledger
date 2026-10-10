
package com.shopledger.backend.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shopledger.backend.dto.request.ItemCreateRequest;
import com.shopledger.backend.dto.response.ItemResponse;
import com.shopledger.backend.entity.Item;
import com.shopledger.backend.exception.DuplicateItemException;
import com.shopledger.backend.exception.ItemNotFoundException;
import com.shopledger.backend.repository.ItemRepository;

@Service
public class ItemService {

    private final ItemRepository itemRepository;

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Transactional
    public ItemResponse createItem(ItemCreateRequest request) {
        Item item = new Item(
                request.getItemName().trim(),
                request.getItemCategory(),
                request.getItemBrand().trim()
        );

        String partNumber =
                trimToNull(request.getManufacturerPartNumber());

        item.setManufacturerPartNumber(partNumber);
        item.setSearchReference(trimToNull(request.getSearchReference()));
        item.setShelfLocation(trimToNull(request.getShelfLocation()));

        item.setDefaultSellingPrice(
                request.getDefaultSellingPrice() != null
                        ? request.getDefaultSellingPrice()
                        : BigDecimal.ZERO
        );

        item.setMinimumStock(
                request.getMinimumStock() != null
                        ? request.getMinimumStock()
                        : 0
        );

        if (partNumber != null
                && itemRepository.existsByManufacturerPartNumber(partNumber)) {
            throw new DuplicateItemException(partNumber);
        }

        try {
            Item savedItem = itemRepository.saveAndFlush(item);
            return toResponse(savedItem);
        } catch (DataIntegrityViolationException exception) {
            if (partNumber != null
                    && itemRepository.existsByManufacturerPartNumber(partNumber)) {
                throw new DuplicateItemException(partNumber);
            }

            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> getAllItems() {
        return itemRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ItemResponse getItemById(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ItemNotFoundException(itemId));

        return toResponse(item);
    }

    private ItemResponse toResponse(Item item) {
        ItemResponse response = new ItemResponse();

        response.setItemId(item.getItemId());
        response.setManufacturerPartNumber(item.getManufacturerPartNumber());
        response.setItemName(item.getItemName());
        response.setSearchReference(item.getSearchReference());
        response.setItemCategory(item.getItemCategory());
        response.setItemBrand(item.getItemBrand());
        response.setShelfLocation(item.getShelfLocation());
        response.setDefaultSellingPrice(item.getDefaultSellingPrice());
        response.setMinimumStock(item.getMinimumStock());
        response.setActive(item.isActive());
        response.setCreatedAt(item.getCreatedAt());
        response.setUpdatedAt(item.getUpdatedAt());

        return response;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    
}
