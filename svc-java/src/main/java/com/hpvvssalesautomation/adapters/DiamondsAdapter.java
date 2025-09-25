package com.hpvvssalesautomation.adapters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hpvvssalesautomation.domain.diamonds.DiamondsCounts;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class DiamondsAdapter {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public DiamondsAdapter(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public int applyOrderDecision(String rootApptId,
                                  String newOrderStatus,
                                  String orderedBy,
                                  LocalDate orderedDate,
                                  ZonedDateTime updatedAt) {

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("root_appt_id", rootApptId)
                .addValue("new_order_status", newOrderStatus)
                .addValue("ordered_by", orderedBy)
                .addValue("ordered_date", orderedDate)
                .addValue("updated_at", toOffset(updatedAt));

        return jdbcTemplate.update(
                "UPDATE diamonds_orders_200 SET order_status = :new_order_status, " +
                        "ordered_by = COALESCE(:ordered_by, ordered_by), " +
                        "ordered_date = COALESCE(:ordered_date, ordered_date), " +
                        "updated_at = :updated_at " +
                        "WHERE root_appt_id = :root_appt_id AND lower(order_status) = 'proposing'",
                params
        );
    }

    public int confirmDelivery(String rootApptId,
                               LocalDate memoDate,
                               LocalDate returnDueDate,
                               ZonedDateTime updatedAt) {

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("root_appt_id", rootApptId)
                .addValue("memo_invoice_date", memoDate)
                .addValue("return_due_date", returnDueDate)
                .addValue("updated_at", toOffset(updatedAt));

        return jdbcTemplate.update(
                "UPDATE diamonds_orders_200 SET order_status = 'Delivered', stone_status = 'In Stock', " +
                        "memo_invoice_date = :memo_invoice_date, return_due_date = :return_due_date, updated_at = :updated_at " +
                        "WHERE root_appt_id = :root_appt_id AND lower(order_status) = 'on the way' " +
                        "AND (stone_status IS NULL OR lower(stone_status) <> 'in stock')",
                params
        );
    }

    public int applyStoneDecision(String rootApptId,
                                  String stoneStatus,
                                  String decidedBy,
                                  LocalDate decidedDate,
                                  ZonedDateTime updatedAt) {

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("root_appt_id", rootApptId)
                .addValue("stone_status", stoneStatus)
                .addValue("decided_by", decidedBy)
                .addValue("decided_date", decidedDate)
                .addValue("updated_at", toOffset(updatedAt));

        return jdbcTemplate.update(
                "UPDATE diamonds_orders_200 SET stone_status = :stone_status, decided_by = COALESCE(:decided_by, decided_by), " +
                        "decided_date = COALESCE(:decided_date, decided_date), updated_at = :updated_at " +
                        "WHERE root_appt_id = :root_appt_id",
                params
        );
    }

    public DiamondsCounts readCounts(String rootApptId) {
        MapSqlParameterSource params = new MapSqlParameterSource("root_appt_id", rootApptId);
        return jdbcTemplate.query(
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
                    rs.next();
                    return new DiamondsCounts(
                            rootApptId,
                            rs.getInt("total_count"),
                            rs.getInt("proposing_count"),
                            rs.getInt("not_approved_count"),
                            rs.getInt("on_the_way_count"),
                            rs.getInt("delivered_count"),
                            rs.getInt("in_stock_count"),
                            rs.getInt("keep_count"),
                            rs.getInt("return_count"),
                            rs.getInt("replace_count")
                    );
                }
        );
    }

    public void persistSummary(DiamondsCounts counts,
                               String centerStoneOrderStatus,
                               ZonedDateTime updatedAt) {

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("root_appt_id", counts.rootApptId())
                .addValue("center_stone_order_status", centerStoneOrderStatus)
                .addValue("total_count", counts.totalCount())
                .addValue("proposing_count", counts.proposingCount())
                .addValue("not_approved_count", counts.notApprovedCount())
                .addValue("on_the_way_count", counts.onTheWayCount())
                .addValue("delivered_count", counts.deliveredCount())
                .addValue("in_stock_count", counts.inStockCount())
                .addValue("keep_count", counts.keepCount())
                .addValue("return_count", counts.returnCount())
                .addValue("replace_count", counts.replaceCount())
                .addValue("summary_json", toJson(counts))
                .addValue("updated_at", toOffset(updatedAt));

        jdbcTemplate.update(
                "INSERT INTO diamonds_summary_100 (root_appt_id, center_stone_order_status, total_count, proposing_count, not_approved_count, on_the_way_count, delivered_count, in_stock_count, keep_count, return_count, replace_count, summary_json, updated_at) " +
                        "VALUES (:root_appt_id, :center_stone_order_status, :total_count, :proposing_count, :not_approved_count, :on_the_way_count, :delivered_count, :in_stock_count, :keep_count, :return_count, :replace_count, :summary_json, :updated_at) " +
                        "ON CONFLICT (root_appt_id) DO UPDATE SET center_stone_order_status = EXCLUDED.center_stone_order_status, " +
                        "total_count = EXCLUDED.total_count, proposing_count = EXCLUDED.proposing_count, not_approved_count = EXCLUDED.not_approved_count, " +
                        "on_the_way_count = EXCLUDED.on_the_way_count, delivered_count = EXCLUDED.delivered_count, in_stock_count = EXCLUDED.in_stock_count, " +
                        "keep_count = EXCLUDED.keep_count, return_count = EXCLUDED.return_count, replace_count = EXCLUDED.replace_count, summary_json = EXCLUDED.summary_json, " +
                        "updated_at = EXCLUDED.updated_at",
                params
        );

        jdbcTemplate.update(
                "UPDATE master SET center_stone_order_status = :center_stone_order_status, updated_at = COALESCE(:updated_at, updated_at) " +
                        "WHERE root_appt_id = :root_appt_id",
                params
        );
    }

    private OffsetDateTime toOffset(ZonedDateTime dateTime) {
        return dateTime == null ? null : dateTime.toOffsetDateTime();
    }

    private String toJson(DiamondsCounts counts) {
        Map<String, Integer> payload = new HashMap<>();
        payload.put("totalCount", counts.totalCount());
        payload.put("proposingCount", counts.proposingCount());
        payload.put("notApprovedCount", counts.notApprovedCount());
        payload.put("onTheWayCount", counts.onTheWayCount());
        payload.put("deliveredCount", counts.deliveredCount());
        payload.put("inStockCount", counts.inStockCount());
        payload.put("keepCount", counts.keepCount());
        payload.put("returnCount", counts.returnCount());
        payload.put("replaceCount", counts.replaceCount());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
