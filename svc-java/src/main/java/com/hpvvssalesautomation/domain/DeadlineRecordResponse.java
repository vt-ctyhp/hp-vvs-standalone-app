package com.hpvvssalesautomation.domain;

import java.time.OffsetDateTime;
import java.time.LocalDate;

public record DeadlineRecordResponse(String rootApptId, String deadlineType, LocalDate deadlineDate, int moveCount, OffsetDateTime updatedAt) {
}
