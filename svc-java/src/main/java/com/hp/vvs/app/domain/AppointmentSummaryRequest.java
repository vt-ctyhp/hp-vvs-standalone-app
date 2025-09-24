package com.hp.vvs.app.domain;

import java.time.LocalDate;
import java.util.Optional;

public record AppointmentSummaryRequest(
        Optional<String> brand,
        Optional<String> rep,
        Optional<LocalDate> dateFrom,
        Optional<LocalDate> dateTo) {
}
