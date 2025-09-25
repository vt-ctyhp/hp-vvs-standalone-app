package com.hpvvssalesautomation.domain;

import java.math.BigDecimal;

public record DashboardKpiResponse(
        BigDecimal weightedPipeline,
        BigDecimal totalDeposits,
        long firstTimeDepositCount,
        long overdueProductionCount,
        long overdueThreeDCount
) {
}
