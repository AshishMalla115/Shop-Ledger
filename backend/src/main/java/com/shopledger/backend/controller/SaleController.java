
package com.shopledger.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shopledger.backend.dto.request.SaleRequest;
import com.shopledger.backend.dto.response.SaleResponse;
import com.shopledger.backend.service.SaleService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/events")
@Validated
public class SaleController {

    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @PostMapping("/sale")
    public ResponseEntity<SaleResponse> createSale(
            @Valid @RequestBody SaleRequest request) {

        SaleResponse response = saleService.createSale(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
