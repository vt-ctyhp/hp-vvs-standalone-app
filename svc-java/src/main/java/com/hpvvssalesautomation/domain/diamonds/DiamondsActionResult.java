package com.hpvvssalesautomation.domain.diamonds;

import java.time.OffsetDateTime;
import java.util.Map;

public record DiamondsActionResult(
        String rootApptId,
        int affectedRows,
        String centerStoneOrderStatus,
        Map<String, Integer> counts,
        OffsetDateTime updatedAt
) {
}
