package com.hpvvssalesautomation.util;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

@Component
public class TimeUtil {

    private final ZoneId zoneId;
    private final DateTimeFormatter dateFormatter;
    private final DateTimeFormatter dateTimeFormatter;

    public TimeUtil(ZoneId zoneId) {
        this.zoneId = zoneId;
        this.dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(zoneId);
        this.dateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(zoneId);
    }

    public Optional<LocalDate> parseDate(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String trimmed = value.trim();
        try {
            return Optional.of(LocalDate.parse(trimmed));
        } catch (DateTimeParseException ignored) {
            try {
                LocalDateTime ldt = LocalDateTime.parse(trimmed);
                return Optional.of(ldt.atZone(zoneId).toLocalDate());
            } catch (DateTimeParseException ignoredAgain) {
                try {
                    OffsetDateTime odt = OffsetDateTime.parse(trimmed);
                    return Optional.of(odt.atZoneSameInstant(zoneId).toLocalDate());
                } catch (DateTimeParseException ignoredOffset) {
                    try {
                        ZonedDateTime zdt = ZonedDateTime.parse(trimmed);
                        return Optional.of(zdt.withZoneSameInstant(zoneId).toLocalDate());
                    } catch (DateTimeParseException ignoredZoned) {
                        return Optional.empty();
                    }
                }
            }
        }
    }

    public Optional<ZonedDateTime> parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(ZonedDateTime.parse(value.trim()).withZoneSameInstant(zoneId));
        } catch (DateTimeParseException ignored) {
            try {
                LocalDateTime ldt = LocalDateTime.parse(value.trim());
                return Optional.of(ldt.atZone(zoneId));
            } catch (DateTimeParseException ignoredAgain) {
                return Optional.empty();
            }
        }
    }

    public String formatDate(LocalDate date) {
        return date == null ? null : dateFormatter.format(date);
    }

    public String formatDateTime(ZonedDateTime dateTime) {
        return dateTime == null ? null : dateTimeFormatter.format(dateTime);
    }

    public LocalDate today() {
        return LocalDate.now(zoneId);
    }

    public Instant nowInstant() {
        return ZonedDateTime.now(zoneId).toInstant();
    }

    public ZonedDateTime nowZoned() {
        return ZonedDateTime.now(zoneId);
    }
}
