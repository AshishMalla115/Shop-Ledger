
package com.shopledger.backend.dto.response;

import com.shopledger.backend.enums.LedgerEventType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record LedgerEventResponse(
        UUID id,
        Long partyId,
        String partyName,
        Long itemId,
        String itemName,
        LedgerEventType eventType,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal amount,
        OffsetDateTime occurredAt,
        String createdBy,
        String notes
) {
}
