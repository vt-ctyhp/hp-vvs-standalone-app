package com.hpvvssalesautomation.adapters;

import com.hpvvssalesautomation.alias.AliasRegistry;
import com.hpvvssalesautomation.util.HeaderMap;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class PerClientReportAdapter {

    private static final List<String> ROW_HEADERS = List.of(
            "Log Date",
            "Sales Stage",
            "Conversion Status",
            "Custom Order Status",
            "Center Stone Order Status",
            "Next Steps",
            "Deadline Type",
            "Deadline Date",
            "Move Count",
            "Assisted Rep",
            "Updated By",
            "Updated At"
    );

    private final AliasRegistry aliasRegistry;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PerClientReportAdapter(AliasRegistry aliasRegistry, NamedParameterJdbcTemplate jdbcTemplate) {
        this.aliasRegistry = aliasRegistry;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<String> expectedColumns() {
        return aliasRegistry.canonicalPerClientColumns();
    }

    public void ensureSchema(List<String> headers) {
        HeaderMap map = new HeaderMap(headers, aliasRegistry.perClientAliases());
        if (!map.missingCanonicals().isEmpty()) {
            throw new IllegalStateException("Client status log headers missing: " + map.missingCanonicals());
        }
    }

    public void ensureReportExists(MasterDetail masterDetail) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("root_appt_id", masterDetail.rootApptId())
                .addValue("client_name", masterDetail.customerName())
                .addValue("brand", masterDetail.brand())
                .addValue("assigned_rep", masterDetail.assignedRep())
                .addValue("assisted_rep", masterDetail.assistedRep())
                .addValue("so_number", masterDetail.soNumber());

        jdbcTemplate.update(
                "INSERT INTO per_client_reports (root_appt_id, client_name, brand, assigned_rep, assisted_rep, so_number) " +
                        "VALUES (:root_appt_id, :client_name, :brand, :assigned_rep, :assisted_rep, :so_number) " +
                        "ON CONFLICT (root_appt_id) DO NOTHING",
                params
        );
    }

    public void appendStatusEntry(String rootApptId,
                                  LocalDate logDate,
                                  String salesStage,
                                  String conversionStatus,
                                  String customOrderStatus,
                                  String centerStoneOrderStatus,
                                  String nextSteps,
                                  String assistedRep,
                                  String updatedBy,
                                  ZonedDateTime updatedAt) {

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("root_appt_id", rootApptId)
                .addValue("log_date", logDate)
                .addValue("sales_stage", salesStage)
                .addValue("conversion_status", conversionStatus)
                .addValue("custom_order_status", customOrderStatus)
                .addValue("center_stone_order_status", centerStoneOrderStatus)
                .addValue("next_steps", nextSteps)
                .addValue("deadline_type", null)
                .addValue("deadline_date", null)
                .addValue("move_count", null)
                .addValue("assisted_rep", assistedRep)
                .addValue("updated_by", updatedBy)
                .addValue("updated_at", updatedAt != null ? Timestamp.from(updatedAt.toInstant()) : null);

        jdbcTemplate.update(
                "INSERT INTO per_client_entries (root_appt_id, log_date, sales_stage, conversion_status, custom_order_status, " +
                        "center_stone_order_status, next_steps, deadline_type, deadline_date, move_count, assisted_rep, updated_by, updated_at) " +
                        "VALUES (:root_appt_id, :log_date, :sales_stage, :conversion_status, :custom_order_status, :center_stone_order_status, :next_steps, :deadline_type, :deadline_date, :move_count, :assisted_rep, :updated_by, :updated_at)",
                params
        );
    }

    public void appendDeadlineEntry(String rootApptId,
                                    LocalDate logDate,
                                    String deadlineType,
                                    LocalDate deadlineDate,
                                    Integer moveCount,
                                    String assistedRep,
                                    String updatedBy,
                                    ZonedDateTime updatedAt) {

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("root_appt_id", rootApptId)
                .addValue("log_date", logDate)
                .addValue("sales_stage", null)
                .addValue("conversion_status", null)
                .addValue("custom_order_status", null)
                .addValue("center_stone_order_status", null)
                .addValue("next_steps", null)
                .addValue("deadline_type", deadlineType)
                .addValue("deadline_date", deadlineDate)
                .addValue("move_count", moveCount)
                .addValue("assisted_rep", assistedRep)
                .addValue("updated_by", updatedBy)
                .addValue("updated_at", updatedAt != null ? Timestamp.from(updatedAt.toInstant()) : null);

        jdbcTemplate.update(
                "INSERT INTO per_client_entries (root_appt_id, log_date, sales_stage, conversion_status, custom_order_status, " +
                        "center_stone_order_status, next_steps, deadline_type, deadline_date, move_count, assisted_rep, updated_by, updated_at) " +
                        "VALUES (:root_appt_id, :log_date, :sales_stage, :conversion_status, :custom_order_status, :center_stone_order_status, :next_steps, :deadline_type, :deadline_date, :move_count, :assisted_rep, :updated_by, :updated_at)",
                params
        );
    }

    public void updateStatusSnapshot(MasterDetail masterDetail,
                                     String salesStage,
                                     String conversionStatus,
                                     String customOrderStatus,
                                     String inProductionStatus,
                                     String centerStoneOrderStatus,
                                     String nextSteps,
                                     String assistedRep,
                                     String updatedBy,
                                     ZonedDateTime updatedAt) {

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("root_appt_id", masterDetail.rootApptId())
                .addValue("client_name", masterDetail.customerName())
                .addValue("brand", masterDetail.brand())
                .addValue("assigned_rep", masterDetail.assignedRep())
                .addValue("assisted_rep", Optional.ofNullable(assistedRep).orElse(masterDetail.assistedRep()))
                .addValue("so_number", masterDetail.soNumber())
                .addValue("sales_stage", salesStage)
                .addValue("conversion_status", conversionStatus)
                .addValue("custom_order_status", customOrderStatus)
                .addValue("in_production_status", inProductionStatus)
                .addValue("center_stone_order_status", centerStoneOrderStatus)
                .addValue("next_steps", nextSteps)
                .addValue("updated_by", updatedBy)
                .addValue("updated_at", updatedAt != null ? Timestamp.from(updatedAt.toInstant()) : null);

        jdbcTemplate.update(
                "UPDATE per_client_reports SET client_name = :client_name, brand = :brand, assigned_rep = :assigned_rep, assisted_rep = :assisted_rep, so_number = :so_number, " +
                        "sales_stage = :sales_stage, conversion_status = :conversion_status, custom_order_status = :custom_order_status, " +
                        "in_production_status = :in_production_status, center_stone_order_status = :center_stone_order_status, next_steps = :next_steps, " +
                        "updated_by = :updated_by, updated_at = :updated_at, last_modified = COALESCE(:updated_at, NOW()) " +
                        "WHERE root_appt_id = :root_appt_id",
                params
        );
    }

    public void updateDeadlineSnapshot(String rootApptId,
                                       String deadlineType,
                                       LocalDate deadlineDate,
                                       Integer moveCount,
                                       String movedBy,
                                       String assistedRep,
                                       ZonedDateTime updatedAt) {

        boolean isThreeD = "3D".equalsIgnoreCase(deadlineType);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("root_appt_id", rootApptId)
                .addValue("updated_by", movedBy)
                .addValue("updated_at", updatedAt != null ? Timestamp.from(updatedAt.toInstant()) : null)
                .addValue("deadline_date", deadlineDate)
                .addValue("move_count", moveCount)
                .addValue("assisted_rep", assistedRep);

        String setClause = isThreeD
                ? "three_d_deadline = :deadline_date, three_d_deadline_moves = :move_count"
                : "production_deadline = :deadline_date, production_deadline_moves = :move_count";

        jdbcTemplate.update(
                "UPDATE per_client_reports SET " + setClause + ", assisted_rep = COALESCE(:assisted_rep, assisted_rep), updated_by = :updated_by, updated_at = :updated_at, last_modified = COALESCE(:updated_at, NOW()) " +
                        "WHERE root_appt_id = :root_appt_id",
                params
        );
    }

    public HeaderMap canonicalRowHeaderMap() {
        return new HeaderMap(ROW_HEADERS, aliasRegistry.perClientAliases());
    }

    public Optional<OffsetDateTime> findSnapshotUpdatedAt(String rootApptId) {
        List<OffsetDateTime> results = jdbcTemplate.query(
                "SELECT updated_at FROM per_client_reports WHERE root_appt_id = :root",
                new MapSqlParameterSource("root", rootApptId),
                (rs, rowNum) -> rs.getObject("updated_at", OffsetDateTime.class)
        );
        return results.stream().findFirst();
    }
}
