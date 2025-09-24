package com.hp.vvs.app.adapters;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

@Component
public class LedgerAdapter {

    private static final List<String> RECEIPT_TYPES = List.of("Receipt", "Deposit", "Payment");
    private static final List<String> EXCLUDED_STATUSES = List.of("VOID", "CANCELLED", "REVERSED");

    private final JdbcTemplate jdbcTemplate;

    public LedgerAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<LedgerEntry> fetchDeposits(LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder("SELECT root_appt_id, so_number, payment_datetime, amount_net, doc_type, doc_status FROM payments WHERE amount_net > 0 AND LOWER(doc_type) IN (");
        List<Object> params = new ArrayList<>();
        for (int i = 0; i < RECEIPT_TYPES.size(); i++) {
            sql.append("?");
            if (i < RECEIPT_TYPES.size() - 1) {
                sql.append(",");
            }
            params.add(RECEIPT_TYPES.get(i).toLowerCase());
        }
        sql.append(")");

        sql.append(" AND (doc_status IS NULL OR UPPER(doc_status) NOT IN (");
        for (int i = 0; i < EXCLUDED_STATUSES.size(); i++) {
            sql.append("?");
            if (i < EXCLUDED_STATUSES.size() - 1) {
                sql.append(",");
            }
            params.add(EXCLUDED_STATUSES.get(i));
        }
        sql.append("))");

        if (from != null) {
            sql.append(" AND payment_datetime >= ?");
            params.add(from.atStartOfDay());
        }
        if (to != null) {
            sql.append(" AND payment_datetime <= ?");
            params.add(to.plusDays(1).atStartOfDay());
        }

        sql.append(" ORDER BY payment_datetime ASC");
        return jdbcTemplate.query(sql.toString(), params.toArray(), new LedgerEntryMapper());
    }

    public Optional<LedgerEntry> firstDepositForRoot(String rootApptId) {
        if (rootApptId == null || rootApptId.isBlank()) {
            return Optional.empty();
        }
        String sql = "SELECT root_appt_id, so_number, payment_datetime, amount_net, doc_type, doc_status FROM payments "
                + "WHERE amount_net > 0 AND LOWER(doc_type) IN (?, ?, ?) "
                + "AND (doc_status IS NULL OR UPPER(doc_status) NOT IN (?, ?, ?)) "
                + "AND root_appt_id = ? ORDER BY payment_datetime ASC LIMIT 1";
        List<Object> params = new ArrayList<>();
        for (String type : RECEIPT_TYPES) {
            params.add(type.toLowerCase());
        }
        for (String status : EXCLUDED_STATUSES) {
            params.add(status);
        }
        params.add(rootApptId);
        return jdbcTemplate.query(sql, params.toArray(), new LedgerEntryMapper()).stream().findFirst();
    }

    public record LedgerEntry(String rootApptId,
                              String soNumber,
                              java.time.LocalDateTime paymentDateTime,
                              double amountNet,
                              String docType,
                              String docStatus) {
    }

    private static class LedgerEntryMapper implements RowMapper<LedgerEntry> {
        @Override
        public LedgerEntry mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new LedgerEntry(
                    rs.getString("root_appt_id"),
                    rs.getString("so_number"),
                    rs.getTimestamp("payment_datetime").toLocalDateTime(),
                    rs.getDouble("amount_net"),
                    rs.getString("doc_type"),
                    rs.getString("doc_status"));
        }
    }
}
