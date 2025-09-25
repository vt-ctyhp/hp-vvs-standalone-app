package com.hpvvssalesautomation.domain.payments;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

public record ValidatedPaymentRecord(
        String docNumber,
        String docRole,
        String anchorType,
        String rootApptId,
        String soNumber,
        String docType,
        String docStatus,
        ZonedDateTime paymentDateTime,
        String method,
        String reference,
        String notes,
        BigDecimal amountGross,
        BigDecimal feePercent,
        BigDecimal feeAmount,
        BigDecimal subtotal,
        BigDecimal amountNet,
        List<PaymentLine> lines
) {
}
