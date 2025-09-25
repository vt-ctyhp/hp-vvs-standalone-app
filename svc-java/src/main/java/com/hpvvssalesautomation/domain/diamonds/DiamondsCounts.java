package com.hpvvssalesautomation.domain.diamonds;

public record DiamondsCounts(
        String rootApptId,
        int totalCount,
        int proposingCount,
        int notApprovedCount,
        int onTheWayCount,
        int deliveredCount,
        int inStockCount,
        int keepCount,
        int returnCount,
        int replaceCount
) {
}
