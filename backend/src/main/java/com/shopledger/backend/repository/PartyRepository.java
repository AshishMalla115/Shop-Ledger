package com.shopledger.backend.repository;

import com.shopledger.backend.entity.Party;
import com.shopledger.backend.enums.PartyType;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PartyRepository extends JpaRepository<Party, Long> {
    boolean existsByPartyIdAndPartyType(
        Long partyId,
        PartyType partyType
    );
}
