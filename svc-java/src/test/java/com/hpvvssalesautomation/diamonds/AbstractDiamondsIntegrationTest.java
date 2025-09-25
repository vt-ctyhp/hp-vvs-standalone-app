package com.hpvvssalesautomation.diamonds;

import com.hpvvssalesautomation.AbstractIntegrationTest;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

abstract class AbstractDiamondsIntegrationTest extends AbstractIntegrationTest {

    private static final Path FIXTURES_DIR = Paths.get("..", "fixtures").toAbsolutePath().normalize();
    private static final Path DIAMONDS_FIXTURE = FIXTURES_DIR.resolve("diamonds.sample.csv");

    @Autowired
    protected NamedParameterJdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void enableDiamonds(DynamicPropertyRegistry registry) {
        registry.add("FEATURE_DIAMONDS", () -> "true");
    }

    @BeforeEach
    void truncatePhaseThreeTables() {
        jdbcTemplate.getJdbcTemplate().execute("TRUNCATE TABLE diamonds_summary_100 RESTART IDENTITY");
        jdbcTemplate.getJdbcTemplate().execute("TRUNCATE TABLE diamonds_orders_200 RESTART IDENTITY");
    }

    protected void loadDiamondsFixture() {
        try (Reader reader = Files.newBufferedReader(DIAMONDS_FIXTURE);
             CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreSurroundingSpaces().parse(reader)) {

            for (CSVRecord record : parser) {
                MapSqlParameterSource params = new MapSqlParameterSource();
                params.addValue("stone_reference", value(record, "Stone ID"));
                params.addValue("root_appt_id", value(record, "RootApptID"));
                params.addValue("order_status", value(record, "Order Status"));
                params.addValue("stone_status", value(record, "Stone Status"));
                params.addValue("stone_type", value(record, "Stone Type"));
                params.addValue("ordered_by", value(record, "Ordered By"));
                params.addValue("ordered_date", parseDate(value(record, "Ordered Date")));
                params.addValue("memo_invoice_date", parseDate(value(record, "Memo Date")));
                params.addValue("return_due_date", parseDate(value(record, "Return Due Date")));
                params.addValue("decided_by", value(record, "Decided By"));
                params.addValue("decided_date", parseDate(value(record, "Decided Date")));

                jdbcTemplate.update(
                        "INSERT INTO diamonds_orders_200 (stone_reference, root_appt_id, order_status, stone_status, stone_type, ordered_by, ordered_date, memo_invoice_date, return_due_date, decided_by, decided_date) " +
                                "VALUES (:stone_reference, :root_appt_id, :order_status, :stone_status, :stone_type, :ordered_by, :ordered_date, :memo_invoice_date, :return_due_date, :decided_by, :decided_date)",
                        params
                );
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load diamonds fixture", e);
        }
    }

    protected Map<String, Object> fetchOrderByStone(String stoneReference) {
        MapSqlParameterSource params = new MapSqlParameterSource("stone_reference", stoneReference);
        return jdbcTemplate.queryForMap(
                "SELECT stone_reference, root_appt_id, order_status, stone_status, stone_type, ordered_by, ordered_date, memo_invoice_date, return_due_date, decided_by, decided_date " +
                        "FROM diamonds_orders_200 WHERE stone_reference = :stone_reference",
                params
        );
    }

    protected Map<String, Object> fetchSummary(String rootApptId) {
        MapSqlParameterSource params = new MapSqlParameterSource("root_appt_id", rootApptId);
        return jdbcTemplate.queryForMap(
                "SELECT center_stone_order_status, total_count, proposing_count, not_approved_count, on_the_way_count, delivered_count, in_stock_count, keep_count, return_count, replace_count, updated_at " +
                        "FROM diamonds_summary_100 WHERE root_appt_id = :root_appt_id",
                params
        );
    }

    protected Map<String, Object> fetchCounts(String rootApptId) {
        Map<String, Object> map = new HashMap<>();
        MapSqlParameterSource params = new MapSqlParameterSource("root_appt_id", rootApptId);
        jdbcTemplate.query(
                "SELECT COUNT(*) AS total_count, " +
                        "COUNT(*) FILTER (WHERE lower(order_status) = 'proposing') AS proposing_count, " +
                        "COUNT(*) FILTER (WHERE lower(order_status) = 'not approved') AS not_approved_count, " +
                        "COUNT(*) FILTER (WHERE lower(order_status) = 'on the way') AS on_the_way_count, " +
                        "COUNT(*) FILTER (WHERE lower(order_status) = 'delivered') AS delivered_count, " +
                        "COUNT(*) FILTER (WHERE lower(stone_status) = 'in stock') AS in_stock_count, " +
                        "COUNT(*) FILTER (WHERE lower(stone_status) = 'keep') AS keep_count, " +
                        "COUNT(*) FILTER (WHERE lower(stone_status) = 'return') AS return_count, " +
                        "COUNT(*) FILTER (WHERE lower(stone_status) = 'replace') AS replace_count " +
                        "FROM diamonds_orders_200 WHERE root_appt_id = :root_appt_id",
                params,
                rs -> {
                    if (rs.next()) {
                        map.put("total_count", rs.getInt("total_count"));
                        map.put("proposing_count", rs.getInt("proposing_count"));
                        map.put("not_approved_count", rs.getInt("not_approved_count"));
                        map.put("on_the_way_count", rs.getInt("on_the_way_count"));
                        map.put("delivered_count", rs.getInt("delivered_count"));
                        map.put("in_stock_count", rs.getInt("in_stock_count"));
                        map.put("keep_count", rs.getInt("keep_count"));
                        map.put("return_count", rs.getInt("return_count"));
                        map.put("replace_count", rs.getInt("replace_count"));
                    }
                    return null;
                }
        );
        return map;
    }

    private String value(CSVRecord record, String header) {
        return record.isMapped(header) ? record.get(header) : null;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value.trim());
    }
}
