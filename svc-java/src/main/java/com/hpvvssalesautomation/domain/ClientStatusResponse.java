package com.hpvvssalesautomation.domain;

import java.time.OffsetDateTime;

public record ClientStatusResponse(String rootApptId, OffsetDateTime updatedAt, String status) {
}
