package com.hpvvssalesautomation.domain.payments;

import java.math.BigDecimal;
import java.util.List;

public record PaymentRecordResult(
        PaymentRecordStatus status,
        String docNumber,
        String docRole,
        String anchorType,
        String rootApptId,
        String soNumber,
        String docType,
        String docStatus,
        String paymentDateTime,
        String method,
        String reference,
        String notes,
        BigDecimal amountGross,
        BigDecimal feePercent,
        BigDecimal feeAmount,
        BigDecimal subtotal,
        BigDecimal amountNet,
        List<PaymentLine> lines,
        String requestHash
) {
}
