package com.shopledger.backend.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.shopledger.backend.dto.request.PurchaseRequest;
import com.shopledger.backend.dto.response.PurchaseResponse;
import com.shopledger.backend.service.PurchaseService;

@RestController
@RequestMapping("/api/events")
public class PurchaseController {

    private final PurchaseService purchaseService;

    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @PostMapping("/purchase")
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseResponse receivePurchase(
            @Valid @RequestBody PurchaseRequest request) {
        return purchaseService.receivePurchase(request);
    }
}
