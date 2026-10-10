package com.shopledger.backend.exception;

public class PartyNotFoundException extends RuntimeException {

    public PartyNotFoundException(Long partyId) {
        super("Party not found with ID: " + partyId);
    }
}