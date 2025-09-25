package com.hpvvssalesautomation.adapters;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class LedgerAdapter {

    private static final Set<String> RECEIPT_TYPES = Set.of(
            "SALES_RECEIPT",
            "PAYMENT_RECEIPT",
            "PAYMENT",
            "DEPOSIT",
            "PROGRESS",
            "FINAL",
            "CREDIT"
    );

    private static final Set<String> BLOCKED_STATUSES = Set.of(
            "VOID",
            "VOIDED",
            "CANCELLED",
            "CANCELED",
            "REVERSED"
    );

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ZoneId zoneId;

    public LedgerAdapter(NamedParameterJdbcTemplate jdbcTemplate, ZoneId zoneId) {
        this.jdbcTemplate = jdbcTemplate;
        this.zoneId = zoneId;
    }

    public List<LedgerEntry> fetchReceiptEntries() {
        String sql = "SELECT root_appt_id, payment_datetime, doc_type, amount_net, doc_status FROM payments WHERE amount_net > 0";
        return jdbcTemplate.query(sql, this::mapRow).stream()
                .filter(entry -> entry.docType() != null && RECEIPT_TYPES.contains(entry.docType().toUpperCase(Locale.US)))
                .filter(entry -> entry.status() == null || !BLOCKED_STATUSES.contains(entry.status().toUpperCase(Locale.US)))
                .toList();
    }

    private LedgerEntry mapRow(ResultSet rs, int rowNum) throws SQLException {
        ZonedDateTime paymentDate = null;
        if (rs.getTimestamp("payment_datetime") != null) {
            paymentDate = rs.getTimestamp("payment_datetime").toInstant().atZone(zoneId);
        }
        return new LedgerEntry(
                rs.getString("root_appt_id"),
                paymentDate,
                rs.getString("doc_type"),
                rs.getBigDecimal("amount_net"),
                rs.getString("doc_status")
        );
    }
}
