package com.hpvvssalesautomation.seed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hpvvssalesautomation.alias.AliasRegistry;
import com.hpvvssalesautomation.util.HeaderMap;
import com.hpvvssalesautomation.util.TimeUtil;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class SeedRunner {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final AliasRegistry aliasRegistry;
    private final TimeUtil timeUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SeedRunner(NamedParameterJdbcTemplate jdbcTemplate, TimeUtil timeUtil, AliasRegistry aliasRegistry) {
        this.jdbcTemplate = jdbcTemplate;
        this.timeUtil = timeUtil;
        this.aliasRegistry = aliasRegistry;
    }

    public void run(Path fixturesDir) throws IOException {
        Path masterPath = fixturesDir.resolve("master.sample.csv");
        Path ledgerPath = fixturesDir.resolve("ledger.sample.csv");
        Path diamondsPath = fixturesDir.resolve("diamonds.sample.csv");

        truncateTables();
        seedMaster(masterPath);
        seedLedger(ledgerPath);
        seedDiamonds(diamondsPath);
        upsertMeta("last_seeded_at", timeUtil.nowInstant().toString());
    }

    private void truncateTables() {
        jdbcTemplate.getJdbcTemplate().execute("TRUNCATE TABLE diamonds_summary_100 RESTART IDENTITY CASCADE");
        jdbcTemplate.getJdbcTemplate().execute("TRUNCATE TABLE diamonds_orders_200 RESTART IDENTITY CASCADE");
        jdbcTemplate.getJdbcTemplate().execute("TRUNCATE TABLE per_client_entries RESTART IDENTITY CASCADE");
        jdbcTemplate.getJdbcTemplate().execute("TRUNCATE TABLE per_client_reports RESTART IDENTITY CASCADE");
        jdbcTemplate.getJdbcTemplate().execute("TRUNCATE TABLE client_status_log RESTART IDENTITY CASCADE");
        jdbcTemplate.getJdbcTemplate().execute("TRUNCATE TABLE payments RESTART IDENTITY CASCADE");
        jdbcTemplate.getJdbcTemplate().execute("TRUNCATE TABLE master RESTART IDENTITY CASCADE");
    }

    private void seedDiamonds(Path csvPath) throws IOException {
        if (!Files.exists(csvPath)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(csvPath);
             CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreSurroundingSpaces().parse(reader)) {

            List<String> shuffledHeaders = new ArrayList<>(parser.getHeaderNames());
            Collections.shuffle(shuffledHeaders);
            HeaderMap headerMap = new HeaderMap(shuffledHeaders, aliasRegistry.diamondsOrderAliases());

            for (CSVRecord record : parser) {
                MapSqlParameterSource params = new MapSqlParameterSource();
                params.addValue("root_appt_id", value(record, headerMap.getActual("RootApptID")));
                params.addValue("stone_reference", value(record, headerMap.getActual("Stone Reference")));
                params.addValue("stone_type", trim(value(record, headerMap.getActual("Stone Type"))));
                params.addValue("stone_status", trim(value(record, headerMap.getActual("Stone Status"))));
                params.addValue("order_status", trim(value(record, headerMap.getActual("Order Status"))));
                params.addValue("ordered_by", trim(value(record, headerMap.getActual("Ordered By"))));
                params.addValue("ordered_date", parseDate(record, headerMap.getActual("Ordered Date")));
                params.addValue("memo_invoice_date", parseDate(record, headerMap.getActual("Memo/Invoice Date")));
                params.addValue("return_due_date", parseDate(record, headerMap.getActual("Return Due Date")));
                params.addValue("decided_by", trim(value(record, headerMap.getActual("Decided By"))));
                params.addValue("decided_date", parseDate(record, headerMap.getActual("Decided Date")));

                jdbcTemplate.update(
                        "INSERT INTO diamonds_orders_200 (root_appt_id, stone_reference, stone_type, stone_status, order_status, ordered_by, ordered_date, memo_invoice_date, return_due_date, decided_by, decided_date) " +
                                "VALUES (:root_appt_id, :stone_reference, :stone_type, :stone_status, :order_status, :ordered_by, :ordered_date, :memo_invoice_date, :return_due_date, :decided_by, :decided_date)",
                        params
                );
            }
        }
    }

    private void seedMaster(Path csvPath) throws IOException {
        try (Reader reader = Files.newBufferedReader(csvPath);
             CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreSurroundingSpaces().parse(reader)) {

            List<String> shuffledHeaders = new ArrayList<>(parser.getHeaderNames());
            Collections.shuffle(shuffledHeaders);
            HeaderMap headerMap = new HeaderMap(shuffledHeaders, aliasRegistry.masterAppointmentAliases());

            for (CSVRecord record : parser) {
                MapSqlParameterSource params = new MapSqlParameterSource();
                params.addValue("visit_date", parseDate(record, headerMap.getActual("Visit Date")));
                params.addValue("root_appt_id", value(record, headerMap.getActual("RootApptID")));
                params.addValue("customer_name", value(record, headerMap.getActual("Customer")));
                params.addValue("phone", value(record, headerMap.getActual("Phone")));
                params.addValue("phone_normalized", value(record, headerMap.getActual("Phone (Normalized)")));
                params.addValue("email", value(record, headerMap.getActual("Email")));
                params.addValue("email_lower", value(record, headerMap.getActual("Email Lower")));
                params.addValue("visit_type", value(record, headerMap.getActual("Visit Type")));
                params.addValue("visit_number", parseInteger(record, headerMap.getActual("Visit #")));
                params.addValue("so_number", value(record, headerMap.getActual("SO#")));
                params.addValue("brand", value(record, headerMap.getActual("Brand")));
                params.addValue("sales_stage", value(record, headerMap.getActual("Sales Stage")));
                params.addValue("conversion_status", value(record, headerMap.getActual("Conversion Status")));
                params.addValue("custom_order_status", value(record, headerMap.getActual("Custom Order Status")));
                params.addValue("center_stone_order_status", value(record, headerMap.getActual("Center Stone Order Status")));
                params.addValue("assigned_rep", value(record, headerMap.getActual("Assigned Rep")));
                params.addValue("assisted_rep", value(record, headerMap.getActual("Assisted Rep")));
                params.addValue("headers_json", writeHeadersJson(headerMap.asMap()));

                jdbcTemplate.update(
                        "INSERT INTO master (visit_date, root_appt_id, customer_name, phone, phone_normalized, email, email_lower, visit_type, visit_number, so_number, brand, sales_stage, conversion_status, custom_order_status, center_stone_order_status, assigned_rep, assisted_rep, headers_json) " +
                                "VALUES (:visit_date, :root_appt_id, :customer_name, :phone, :phone_normalized, :email, :email_lower, :visit_type, :visit_number, :so_number, :brand, :sales_stage, :conversion_status, :custom_order_status, :center_stone_order_status, :assigned_rep, :assisted_rep, :headers_json)",
                        params
                );
            }
        }
    }

    private void seedLedger(Path csvPath) throws IOException {
        try (Reader reader = Files.newBufferedReader(csvPath);
             CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreSurroundingSpaces().parse(reader)) {

            List<String> shuffledHeaders = new ArrayList<>(parser.getHeaderNames());
            Collections.shuffle(shuffledHeaders);
            HeaderMap headerMap = new HeaderMap(shuffledHeaders, aliasRegistry.ledgerAliases());

            for (CSVRecord record : parser) {
                String status = value(record, headerMap.getActual("DocStatus"));
                String docType = value(record, headerMap.getActual("DocType"));
                String netValue = value(record, headerMap.getActual("AmountNet"));
                if (!shouldIncludeLedgerRow(docType, status, netValue)) {
                    continue;
                }
                MapSqlParameterSource params = new MapSqlParameterSource();
                params.addValue("root_appt_id", value(record, headerMap.getActual("RootApptID")));
                params.addValue("payment_datetime", parseDateTime(record, headerMap.getActual("PaymentDateTime")));
                params.addValue("doc_type", docType != null ? docType.trim().toUpperCase(Locale.US) : null);
                params.addValue("amount_net", parseDecimal(netValue));
                params.addValue("doc_status", status);

                jdbcTemplate.update(
                        "INSERT INTO payments (root_appt_id, payment_datetime, doc_type, amount_net, doc_status) VALUES (:root_appt_id, :payment_datetime, :doc_type, :amount_net, :doc_status)",
                        params
                );
            }
        }
    }

    private boolean shouldIncludeLedgerRow(String docType, String status, String netValue) {
        if (netValue == null) {
            return false;
        }
        try {
            if (Double.parseDouble(netValue.trim()) <= 0) {
                return false;
            }
        } catch (NumberFormatException ignored) {
            return false;
        }
        if (status != null) {
            switch (status.trim().toUpperCase(Locale.US)) {
                case "VOID", "VOIDED", "CANCELLED", "CANCELED", "REVERSED" -> {
                    return false;
                }
                default -> {
                }
            }
        }
        if (docType == null) {
            return false;
        }
        String normalized = docType.trim().toUpperCase(Locale.US);
        return normalized.contains("RECEIPT") || normalized.contains("DEPOSIT") || normalized.contains("PAYMENT") || normalized.contains("FINAL") || normalized.contains("PROGRESS") || normalized.contains("CREDIT");
    }

    private Object parseDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Double.valueOf(value.trim());
    }

    private LocalDate parseDate(CSVRecord record, String header) {
        if (header == null) {
            return null;
        }
        String raw = value(record, header);
        return timeUtil.parseDate(raw).orElse(null);
    }

    private Timestamp parseDateTime(CSVRecord record, String header) {
        if (header == null) {
            return null;
        }
        String raw = value(record, header);
        return timeUtil.parseDateTime(raw)
                .map(ZonedDateTime::toInstant)
                .map(Timestamp::from)
                .orElse(null);
    }

    private Integer parseInteger(CSVRecord record, String header) {
        if (header == null) {
            return null;
        }
        String value = value(record, header);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String value(CSVRecord record, String header) {
        if (header == null) {
            return null;
        }
        return record.isMapped(header) ? record.get(header) : null;
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String writeHeadersJson(Map<String, String> headers) {
        try {
            return objectMapper.writeValueAsString(headers);
        } catch (IOException e) {
            return "{}";
        }
    }

    private void upsertMeta(String key, String value) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("meta_key", key);
        params.addValue("meta_value", value);
        jdbcTemplate.update(
                "INSERT INTO meta (meta_key, meta_value) VALUES (:meta_key, :meta_value) " +
                        "ON CONFLICT (meta_key) DO UPDATE SET meta_value = EXCLUDED.meta_value",
                params
        );
    }
}
