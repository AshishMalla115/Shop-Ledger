
package com.shopledger.backend.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentResponse(
        UUID eventId,
        String eventType,
        BigDecimal amount,
        BigDecimal newPartyBalance
) {
}
