
package com.shopledger.backend.dto.request;

import com.shopledger.backend.enums.PaymentDirection;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequest(

        @NotNull(message = "Party ID is required")
        @Positive(message = "Party ID must be positive")
        Long partyId,

        @NotNull(message = "Payment amount is required")
        @DecimalMin(value = "0.01", message = "Payment amount must be greater than zero")
        @Digits(integer = 10, fraction = 2,
                message = "Payment amount must have at most 10 integer digits and 2 decimal places")
        BigDecimal amount,

        @NotNull(message = "Payment direction is required")
        PaymentDirection direction,

        @NotNull(message = "Idempotency key is required")
        UUID idempotencyKey,

        @NotBlank(message = "Created by is required")
        @Size(max = 100, message = "Created by cannot exceed 100 characters")
        String createdBy,

        String notes
) {
}
