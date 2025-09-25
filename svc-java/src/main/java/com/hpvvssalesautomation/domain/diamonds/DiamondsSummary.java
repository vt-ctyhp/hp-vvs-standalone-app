package com.hpvvssalesautomation.domain.diamonds;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public record DiamondsSummary(
        String rootApptId,
        int totalCount,
        int proposingCount,
        int notApprovedCount,
        int onTheWayCount,
        int deliveredCount,
        int inStockCount,
        int keepCount,
        int returnCount,
        int replaceCount,
        String centerStoneOrderStatus,
        OffsetDateTime updatedAt
) {

    public DiamondsSummary {
        if (centerStoneOrderStatus == null || centerStoneOrderStatus.isBlank()) {
            throw new IllegalArgumentException("centerStoneOrderStatus must be provided");
        }
    }

    public DiamondsCounts counts() {
        return new DiamondsCounts(
                rootApptId,
                totalCount,
                proposingCount,
                notApprovedCount,
                onTheWayCount,
                deliveredCount,
                inStockCount,
                keepCount,
                returnCount,
                replaceCount
        );
    }

    public Map<String, Integer> countsMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("total", totalCount);
        map.put("proposing", proposingCount);
        map.put("notApproved", notApprovedCount);
        map.put("onTheWay", onTheWayCount);
        map.put("delivered", deliveredCount);
        map.put("inStock", inStockCount);
        map.put("keep", keepCount);
        map.put("return", returnCount);
        map.put("replace", replaceCount);
        return Map.copyOf(map);
    }
}
