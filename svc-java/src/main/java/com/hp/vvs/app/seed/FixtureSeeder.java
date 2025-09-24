package com.hp.vvs.app.seed;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.vvs.app.alias.AliasRegistry;
import com.hp.vvs.app.util.HeaderMap;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class FixtureSeeder {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random(42);

    public FixtureSeeder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void seed(Path masterCsv, Path ledgerCsv) throws IOException {
        jdbcTemplate.execute("TRUNCATE master RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE payments RESTART IDENTITY CASCADE");
        loadMaster(masterCsv);
        loadLedger(ledgerCsv);
    }

    private void loadMaster(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path);
             CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {
            List<String> originalHeaders = parser.getHeaderNames();
            List<String> shuffledHeaders = new ArrayList<>(originalHeaders);
            Collections.shuffle(shuffledHeaders, random);
            HeaderMap headerMap = new HeaderMap(shuffledHeaders);
            String headersJson = toJson(shuffledHeaders);

            for (CSVRecord record : parser) {
                jdbcTemplate.update(
                        "INSERT INTO master (visit_date, root_appt_id, customer, phone, email, visit_type, visit_number, so_number, brand, sales_stage, conversion_status, custom_order_status, center_stone_order_status, assigned_rep, assisted_rep, headers_json) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?::jsonb)",
                        LocalDate.parse(resolveValue(record, headerMap, AliasRegistry.masterAliases("Visit Date"))),
                        resolveValue(record, headerMap, AliasRegistry.masterAliases("RootApptID")),
                        resolveValue(record, headerMap, AliasRegistry.masterAliases("Customer")),
                        resolveValue(record, headerMap, AliasRegistry.masterAliases("Phone")),
                        resolveValue(record, headerMap, AliasRegistry.masterAliases("Email")),
                        resolveValue(record, headerMap, AliasRegistry.masterAliases("Visit Type")),
                        resolveValue(record, headerMap, AliasRegistry.masterAliases("Visit #")),
                        resolveValue(record, headerMap, AliasRegistry.masterAliases("SO#")),
                        resolveValue(record, headerMap, AliasRegistry.masterAliases("Brand")),
                        resolveValue(record, headerMap, AliasRegistry.masterAliases("Sales Stage")),
                        resolveValue(record, headerMap, AliasRegistry.masterAliases("Conversion Status")),
                        resolveValue(record, headerMap, AliasRegistry.masterAliases("Custom Order Status")),
                        resolveValue(record, headerMap, AliasRegistry.masterAliases("Center Stone Order Status")),
                        resolveValue(record, headerMap, AliasRegistry.masterAliases("Assigned Rep")),
                        resolveValue(record, headerMap, AliasRegistry.masterAliases("Assisted Rep")),
                        headersJson);
            }
        }
    }

    private void loadLedger(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path);
             CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {
            List<String> originalHeaders = parser.getHeaderNames();
            List<String> shuffledHeaders = new ArrayList<>(originalHeaders);
            Collections.shuffle(shuffledHeaders, random);
            HeaderMap headerMap = new HeaderMap(shuffledHeaders);
            String headersJson = toJson(shuffledHeaders);

            for (CSVRecord record : parser) {
                String amount = resolveValue(record, headerMap, AliasRegistry.ledgerAliases("AmountNet"));
                jdbcTemplate.update(
                        "INSERT INTO payments (root_appt_id, so_number, payment_datetime, amount_net, doc_type, doc_status, headers_json) VALUES (?,?,?,?,?,?,?::jsonb)",
                        resolveValue(record, headerMap, AliasRegistry.ledgerAliases("RootApptID")),
                        resolveValue(record, headerMap, AliasRegistry.ledgerAliases("SO#")),
                        OffsetDateTime.parse(resolveValue(record, headerMap, AliasRegistry.ledgerAliases("PaymentDateTime"))),
                        Double.parseDouble(amount),
                        resolveValue(record, headerMap, AliasRegistry.ledgerAliases("DocType")),
                        resolveValue(record, headerMap, AliasRegistry.ledgerAliases("DocStatus")),
                        headersJson);
            }
        }
    }

    private String resolveValue(CSVRecord record, HeaderMap headerMap, List<String> aliases) {
        Optional<String> header = AliasRegistry.resolve(headerMap, aliases);
        if (header.isEmpty()) {
            throw new IllegalStateException("Missing header for aliases " + aliases);
        }
        return record.get(header.get()).trim();
    }

    private String toJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize headers", e);
        }
    }
}
