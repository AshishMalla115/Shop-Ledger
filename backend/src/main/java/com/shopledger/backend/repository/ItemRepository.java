package com.shopledger.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shopledger.backend.entity.Item;

public interface ItemRepository extends JpaRepository<Item, Long> {
    boolean existsByManufacturerPartNumber(String manufacturerPartNumber);
}