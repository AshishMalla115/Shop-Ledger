
package com.shopledger.backend.dto.response;

import java.math.BigDecimal;
import java.util.List;


public record LedgerHistoryResponse(
        Long partyId,
        String partyName,
        BigDecimal calculatedBalance,
        BigDecimal cachedBalance,
        boolean balanceMatches,
        List<LedgerEventResponse> events,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
