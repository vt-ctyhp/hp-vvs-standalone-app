package com.hpvvssalesautomation.domain.payments;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record PaymentsSummaryResponse(
        BigDecimal invoicesLinesSubtotal,
        BigDecimal totalPayments,
        BigDecimal netLinesMinusPayments,
        Map<String, BigDecimal> byMethod,
        List<PaymentSummaryEntry> entries
) {
}
