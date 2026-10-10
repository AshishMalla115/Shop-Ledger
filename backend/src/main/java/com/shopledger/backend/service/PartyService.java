
package com.shopledger.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shopledger.backend.dto.request.PartyCreateRequest;
import com.shopledger.backend.dto.response.PartyResponse;
import com.shopledger.backend.entity.Party;
import com.shopledger.backend.repository.PartyRepository;

import com.shopledger.backend.exception.PartyNotFoundException;

@Service
public class PartyService {

    private final PartyRepository partyRepository;

    public PartyService(PartyRepository partyRepository) {
        this.partyRepository = partyRepository;
    }

    @Transactional
    public PartyResponse createParty(PartyCreateRequest request) {
        Party party = new Party(
            request.getPartyName().trim(),
            request.getPhoneNumber().trim(),
            request.getPartyType()
        );
        party.setGstNumber(request.getGstNumber());
        party.setDefaultDiscountPercentage(
            request.getDefaultDiscountPercentage() != null
                ? request.getDefaultDiscountPercentage()
                : java.math.BigDecimal.ZERO
        );
        party.setAddress(request.getAddress());

        Party savedParty = partyRepository.save(party);

        return toResponse(savedParty);
    }

    @Transactional(readOnly = true)
    public List<PartyResponse> getAllParties() {
        return partyRepository.findAll()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public PartyResponse getPartyById(Long partyId) {
       Party party = partyRepository.findById(partyId)
            .orElseThrow(() -> new PartyNotFoundException(partyId));

        return toResponse(party);
    }

    private PartyResponse toResponse(Party party) {
        PartyResponse response = new PartyResponse();

        response.setPartyId(party.getPartyId());
        response.setPartyName(party.getPartyName());
        response.setPhoneNumber(party.getPhoneNumber());
        response.setGstNumber(party.getGstNumber());
        response.setPartyType(party.getPartyType());
        response.setDefaultDiscountPercentage(
            party.getDefaultDiscountPercentage()
        );
        response.setActive(party.isActive());
        response.setCreatedAt(party.getCreatedAt());
        response.setUpdatedAt(party.getUpdatedAt());
        response.setAddress(party.getAddress());

        return response;
    }
}
