
package com.shopledger.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shopledger.backend.dto.request.PartyCreateRequest;
import com.shopledger.backend.dto.response.PartyResponse;
import com.shopledger.backend.service.PartyService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/parties")
public class PartyController {

    private final PartyService partyService;

    public PartyController(PartyService partyService) {
        this.partyService = partyService;
    }

    @PostMapping
    public ResponseEntity<PartyResponse> createParty(
            @Valid @RequestBody PartyCreateRequest request) {

        PartyResponse response = partyService.createParty(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<PartyResponse>> getAllParties() {
        return ResponseEntity.ok(partyService.getAllParties());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PartyResponse> getPartyById(
            @PathVariable Long id) {

        return ResponseEntity.ok(partyService.getPartyById(id));
    }
}
