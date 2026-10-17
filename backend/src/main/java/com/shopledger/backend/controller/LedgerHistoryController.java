
package com.shopledger.backend.controller;

import com.shopledger.backend.dto.response.LedgerHistoryResponse;
import com.shopledger.backend.service.LedgerHistoryService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class LedgerHistoryController {

    private final LedgerHistoryService ledgerHistoryService;

    public LedgerHistoryController(
            LedgerHistoryService ledgerHistoryService) {
        this.ledgerHistoryService = ledgerHistoryService;
    }

    @GetMapping
    public LedgerHistoryResponse getEventHistory(
            @RequestParam Long partyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ledgerHistoryService.getEventHistory(
                partyId,
                page,
                size
        );
    }
}
