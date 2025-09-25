package com.hpvvssalesautomation.adapters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hpvvssalesautomation.domain.AppointmentSummaryRequest;
import com.hpvvssalesautomation.domain.DeadlineType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class SheetsAdapter {

    public record StatusLogView(String salesStage, String conversionStatus, String customOrderStatus, String inProductionStatus, String centerStoneOrderStatus, String nextSteps, String assistedRep) { }

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    private static final String META_CLIENT_STATUS_HEADERS_KEY = "client_status_log_headers";
    private static final List<String> CLIENT_STATUS_LOG_HEADERS = List.of(
            "Log Date",
            "Sales Stage",
            "Conversion Status",
            "Custom Order Status",
            "In Production Status",
            "Center Stone Order Status",
            "Next Steps",
            "Deadline Type",
            "Deadline Date",
            "Move Count",
            "Assisted Rep",
            "Updated By",
            "Updated At"
    );

    public SheetsAdapter(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public List<MasterRecord> fetchMasterRows(AppointmentSummaryRequest request, Optional<LocalDate> dateFrom, Optional<LocalDate> dateTo) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder sql = new StringBuilder("SELECT visit_date, root_appt_id, customer_name, phone, email, visit_type, visit_number, so_number, brand, sales_stage, conversion_status, custom_order_status, center_stone_order_status, assigned_rep, assisted_rep FROM master WHERE 1=1");

        if (request.getBrand() != null && !request.getBrand().isBlank()) {
            sql.append(" AND LOWER(brand) = :brand");
            params.addValue("brand", request.getBrand().trim().toLowerCase(Locale.US));
        }
        if (request.getRep() != null && !request.getRep().isBlank()) {
            sql.append(" AND (LOWER(assigned_rep) = :rep OR LOWER(assisted_rep) = :rep)");
            params.addValue("rep", request.getRep().trim().toLowerCase(Locale.US));
        }
        dateFrom.ifPresent(date -> {
            sql.append(" AND visit_date >= :dateFrom");
            params.addValue("dateFrom", date);
        });
        dateTo.ifPresent(date -> {
            sql.append(" AND visit_date <= :dateTo");
            params.addValue("dateTo", date);
        });

        sql.append(" ORDER BY visit_date DESC NULLS LAST, root_appt_id ASC");

        return jdbcTemplate.query(sql.toString(), params, this::mapRow);
    }

    private MasterRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        LocalDate visitDate = rs.getDate("visit_date") != null ? rs.getDate("visit_date").toLocalDate() : null;
        Integer visitNumber = rs.getObject("visit_number") != null ? rs.getInt("visit_number") : null;
        return new MasterRecord(
                visitDate,
                rs.getString("root_appt_id"),
                rs.getString("customer_name"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("visit_type"),
                visitNumber,
                rs.getString("so_number"),
                rs.getString("brand"),
                rs.getString("sales_stage"),
                rs.getString("conversion_status"),
                rs.getString("custom_order_status"),
                rs.getString("center_stone_order_status"),
                rs.getString("assigned_rep"),
                rs.getString("assisted_rep")
        );
    }

    public Optional<MasterDetail> findMasterDetail(String rootApptId) {
        String sql = "SELECT root_appt_id, customer_name, brand, sales_stage, conversion_status, custom_order_status, " +
                "in_production_status, center_stone_order_status, assigned_rep, assisted_rep, so_number, next_steps, " +
                "three_d_deadline, three_d_deadline_moves, production_deadline, production_deadline_moves " +
                "FROM master WHERE root_appt_id = :root";

        List<MasterDetail> results = jdbcTemplate.query(sql,
                new MapSqlParameterSource("root", rootApptId),
                (rs, rowNum) -> new MasterDetail(
                        rs.getString("root_appt_id"),
                        rs.getString("customer_name"),
                        rs.getString("brand"),
                        rs.getString("sales_stage"),
                        rs.getString("conversion_status"),
                        rs.getString("custom_order_status"),
                        rs.getString("in_production_status"),
                        rs.getString("center_stone_order_status"),
                        rs.getString("assigned_rep"),
                        rs.getString("assisted_rep"),
                        rs.getString("so_number"),
                        rs.getString("next_steps"),
                        rs.getDate("three_d_deadline") != null ? rs.getDate("three_d_deadline").toLocalDate() : null,
                        rs.getObject("three_d_deadline_moves") != null ? rs.getInt("three_d_deadline_moves") : null,
                        rs.getDate("production_deadline") != null ? rs.getDate("production_deadline").toLocalDate() : null,
                        rs.getObject("production_deadline_moves") != null ? rs.getInt("production_deadline_moves") : null
                ));
        return results.stream().findFirst();
    }

    public void updateMasterFromStatus(String rootApptId,
                                       String salesStage,
                                       String conversionStatus,
                                       String customOrderStatus,
                                       String inProductionStatus,
                                       String centerStoneOrderStatus,
                                       String nextSteps,
                                       String assistedRep,
                                       ZonedDateTime updatedAt) {

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("root_appt_id", rootApptId)
                .addValue("sales_stage", salesStage)
                .addValue("conversion_status", conversionStatus)
                .addValue("custom_order_status", customOrderStatus)
                .addValue("in_production_status", inProductionStatus)
                .addValue("center_stone_order_status", centerStoneOrderStatus)
                .addValue("next_steps", nextSteps)
                .addValue("assisted_rep", assistedRep)
                .addValue("updated_at", updatedAt != null ? updatedAt.toOffsetDateTime() : null);

        jdbcTemplate.update("UPDATE master SET sales_stage = :sales_stage, conversion_status = :conversion_status, " +
                        "custom_order_status = :custom_order_status, in_production_status = :in_production_status, " +
                        "center_stone_order_status = :center_stone_order_status, next_steps = :next_steps, " +
                        "assisted_rep = COALESCE(:assisted_rep, assisted_rep), updated_at = COALESCE(:updated_at, updated_at) " +
                        "WHERE root_appt_id = :root_appt_id",
                params);
    }

    public void appendClientStatusLog(String rootApptId,
                                      LocalDate logDate,
                                      String salesStage,
                                      String conversionStatus,
                                      String customOrderStatus,
                                      String inProductionStatus,
                                      String centerStoneOrderStatus,
                                      String nextSteps,
                                      String assistedRep,
                                      String updatedBy,
                                      ZonedDateTime updatedAt,
                                      Map<String, Object> rawPayload) {

        ensureClientStatusLogHeaders();

        MapSqlParameterSource params = baseLogParams(rootApptId, logDate, updatedBy, updatedAt)
                .addValue("log_type", "STATUS")
                .addValue("sales_stage", salesStage)
                .addValue("conversion_status", conversionStatus)
                .addValue("custom_order_status", customOrderStatus)
                .addValue("in_production_status", inProductionStatus)
                .addValue("center_stone_order_status", centerStoneOrderStatus)
                .addValue("next_steps", nextSteps)
                .addValue("deadline_type", null)
                .addValue("deadline_date", null)
                .addValue("move_count", null)
                .addValue("assisted_rep", assistedRep)
                .addValue("raw_payload", toJson(rawPayload));

        jdbcTemplate.update(
                "INSERT INTO client_status_log (root_appt_id, log_date, log_type, sales_stage, conversion_status, custom_order_status, " +
                        "in_production_status, center_stone_order_status, next_steps, deadline_type, deadline_date, move_count, assisted_rep, updated_by, updated_at, raw_payload) " +
                        "VALUES (:root_appt_id, :log_date, :log_type, :sales_stage, :conversion_status, :custom_order_status, :in_production_status, :center_stone_order_status, :next_steps, :deadline_type, :deadline_date, :move_count, :assisted_rep, :updated_by, :updated_at, :raw_payload)",
                params
        );
    }

    public void appendDeadlineLog(String rootApptId,
                                  LocalDate logDate,
                                  DeadlineType deadlineType,
                                  LocalDate deadlineDate,
                                  Integer moveCount,
                                  String assistedRep,
                                  String movedBy,
                                  ZonedDateTime updatedAt,
                                  Map<String, Object> rawPayload) {

        ensureClientStatusLogHeaders();

        MapSqlParameterSource params = baseLogParams(rootApptId, logDate, movedBy, updatedAt)
                .addValue("log_type", "DEADLINE")
                .addValue("sales_stage", null)
                .addValue("conversion_status", null)
                .addValue("custom_order_status", null)
                .addValue("in_production_status", null)
                .addValue("center_stone_order_status", null)
                .addValue("next_steps", null)
                .addValue("deadline_type", deadlineType.value())
                .addValue("deadline_date", deadlineDate)
                .addValue("move_count", moveCount)
                .addValue("assisted_rep", assistedRep)
                .addValue("raw_payload", toJson(rawPayload));

        jdbcTemplate.update(
                "INSERT INTO client_status_log (root_appt_id, log_date, log_type, sales_stage, conversion_status, custom_order_status, " +
                        "in_production_status, center_stone_order_status, next_steps, deadline_type, deadline_date, move_count, assisted_rep, updated_by, updated_at, raw_payload) " +
                        "VALUES (:root_appt_id, :log_date, :log_type, :sales_stage, :conversion_status, :custom_order_status, :in_production_status, :center_stone_order_status, :next_steps, :deadline_type, :deadline_date, :move_count, :assisted_rep, :updated_by, :updated_at, :raw_payload)",
                params
        );
    }

    public int applyDeadline(String rootApptId, DeadlineType deadlineType, LocalDate deadlineDate, ZonedDateTime updatedAt) {
        String column = deadlineType == DeadlineType.THREE_D ? "three_d_deadline" : "production_deadline";
        String counterColumn = deadlineType == DeadlineType.THREE_D ? "three_d_deadline_moves" : "production_deadline_moves";

        MapSqlParameterSource lookupParams = new MapSqlParameterSource("root", rootApptId);
        DeadlineState current = jdbcTemplate.query(
                "SELECT " + column + " AS deadline_value, " + counterColumn + " AS move_count FROM master WHERE root_appt_id = :root",
                lookupParams,
                rs -> {
                    if (!rs.next()) {
                        return new DeadlineState(null, 0);
                    }
                    LocalDate existingDeadline = rs.getObject("deadline_value", LocalDate.class);
                    int moves = rs.getObject("move_count") != null ? rs.getInt("move_count") : 0;
                    return new DeadlineState(existingDeadline, moves);
                }
        );

        boolean dateChanged = current.deadline() == null || !current.deadline().isEqual(deadlineDate);
        int newCount = dateChanged ? current.moves() + 1 : current.moves();

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("root", rootApptId)
                .addValue("deadline", deadlineDate)
                .addValue("moves", newCount)
                .addValue("updated_at", Timestamp.from(updatedAt.toInstant()));

        jdbcTemplate.update("UPDATE master SET " + column + " = :deadline, " + counterColumn + " = :moves, updated_at = :updated_at " +
                "WHERE root_appt_id = :root", params);

        return newCount;
    }

    private record DeadlineState(LocalDate deadline, int moves) { }

    public Optional<StatusLogView> findLatestStatusLog(String rootApptId) {
        List<StatusLogView> results = jdbcTemplate.query(
                "SELECT sales_stage, conversion_status, custom_order_status, in_production_status, center_stone_order_status, next_steps, assisted_rep " +
                        "FROM client_status_log WHERE root_appt_id = :root ORDER BY updated_at DESC NULLS LAST, id DESC LIMIT 1",
                new MapSqlParameterSource("root", rootApptId),
                (rs, rowNum) -> new StatusLogView(
                        rs.getString("sales_stage"),
                        rs.getString("conversion_status"),
                        rs.getString("custom_order_status"),
                        rs.getString("in_production_status"),
                        rs.getString("center_stone_order_status"),
                        rs.getString("next_steps"),
                        rs.getString("assisted_rep"))
        );
        return results.stream().findFirst();
    }

    public Optional<OffsetDateTime> findSnapshotUpdatedAt(String rootApptId) {
        List<OffsetDateTime> results = jdbcTemplate.query(
                "SELECT updated_at FROM per_client_reports WHERE root_appt_id = :root",
                new MapSqlParameterSource("root", rootApptId),
                (rs, rowNum) -> rs.getObject("updated_at", OffsetDateTime.class)
        );
        return results.stream().findFirst();
    }

    public void ensureClientStatusLogHeaders() {
        List<String> headers = readClientStatusHeaders();
        if (!headers.contains("In Production Status")) {
            int referenceIndex = headers.indexOf("Custom Order Status");
            if (referenceIndex >= 0 && referenceIndex + 1 <= headers.size()) {
                headers.add(referenceIndex + 1, "In Production Status");
            } else {
                headers.add("In Production Status");
            }
        }
        for (String header : CLIENT_STATUS_LOG_HEADERS) {
            if (!headers.contains(header)) {
                headers.add(header);
            }
        }
        storeClientStatusHeaders(headers);
    }

    private List<String> readClientStatusHeaders() {
        String existing = jdbcTemplate.query(
                "SELECT meta_value FROM meta WHERE meta_key = :key",
                new MapSqlParameterSource("key", META_CLIENT_STATUS_HEADERS_KEY),
                rs -> rs.next() ? rs.getString(1) : null
        );
        if (existing == null || existing.isBlank()) {
            return new ArrayList<>(CLIENT_STATUS_LOG_HEADERS);
        }
        try {
            List<String> values = objectMapper.readValue(existing, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            return new ArrayList<>(values);
        } catch (JsonProcessingException e) {
            return new ArrayList<>(CLIENT_STATUS_LOG_HEADERS);
        }
    }

    private void storeClientStatusHeaders(List<String> headers) {
        try {
            String serialized = objectMapper.writeValueAsString(headers);
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("key", META_CLIENT_STATUS_HEADERS_KEY)
                    .addValue("value", serialized);
            jdbcTemplate.update("INSERT INTO meta (meta_key, meta_value) VALUES (:key, :value) " +
                            "ON CONFLICT (meta_key) DO UPDATE SET meta_value = :value",
                    params);
        } catch (JsonProcessingException e) {
            // best-effort; if serialization fails we ignore healing rather than blocking the flow
        }
    }

    private MapSqlParameterSource baseLogParams(String rootApptId, LocalDate logDate, String updatedBy, ZonedDateTime updatedAt) {
        return new MapSqlParameterSource()
                .addValue("root_appt_id", rootApptId)
                .addValue("log_date", logDate)
                .addValue("updated_by", updatedBy)
                .addValue("updated_at", updatedAt != null ? updatedAt.toOffsetDateTime() : null);
    }

    private String toJson(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
