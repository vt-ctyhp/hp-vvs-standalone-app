package com.hp.vvs.app.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class TimeUtil {

    private final ZoneId zoneId;
    private final DateTimeFormatter dateFormatter;
    private final DateTimeFormatter dateTimeFormatter;

    public TimeUtil(ZoneId zoneId) {
        this.zoneId = zoneId;
        this.dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(zoneId);
        this.dateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(zoneId);
    }

    public LocalDate parseDate(String value) {
        return LocalDate.parse(value, dateFormatter);
    }

    public LocalDateTime parseDateTime(String value) {
        return LocalDateTime.ofInstant(Instant.from(dateTimeFormatter.parse(value)), zoneId);
    }

    public String formatDate(LocalDate date) {
        return dateFormatter.format(date);
    }

    public ZonedDateTime now() {
        return ZonedDateTime.now(zoneId);
    }
}
