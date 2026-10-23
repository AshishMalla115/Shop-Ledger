
package com.shopledger.backend.service;

import com.shopledger.backend.dto.response.PaymentResponse;

public record PaymentResult(
        PaymentResponse response,
        boolean created
) {
}
