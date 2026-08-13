package com.shopledger.backend.repository;

import com.shopledger.backend.entity.Party;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartyRepository extends JpaRepository<Party, Long> {
    
}
