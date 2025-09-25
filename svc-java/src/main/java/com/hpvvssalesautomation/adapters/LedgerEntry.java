package com.hpvvssalesautomation.adapters;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public record LedgerEntry(
        String rootApptId,
        ZonedDateTime paymentDate,
        String docType,
        BigDecimal amountNet,
        String status
) {}
