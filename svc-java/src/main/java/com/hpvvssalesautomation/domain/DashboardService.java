package com.hpvvssalesautomation.domain;

import com.hpvvssalesautomation.util.TimeUtil;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DashboardService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TimeUtil timeUtil;

    public DashboardService(NamedParameterJdbcTemplate jdbcTemplate, TimeUtil timeUtil) {
        this.jdbcTemplate = jdbcTemplate;
        this.timeUtil = timeUtil;
    }

    public DashboardKpiResponse fetchKpis(String dateFrom, String dateTo) {
        Optional<LocalDate> from = timeUtil.parseDate(dateFrom);
        Optional<LocalDate> to = timeUtil.parseDate(dateTo);

        BigDecimal weightedPipeline = calculateWeightedPipeline(from, to);
        BigDecimal totalDeposits = calculateTotalDeposits(from, to);
        long firstTimeDeposits = calculateFirstTimeDeposits(from, to);
        long overdueProduction = countOverdue("production_deadline", from, to);
        long overdueThreeD = countOverdue("three_d_deadline", from, to);

        return new DashboardKpiResponse(weightedPipeline, totalDeposits, firstTimeDeposits, overdueProduction, overdueThreeD);
    }

    private BigDecimal calculateWeightedPipeline(Optional<LocalDate> from, Optional<LocalDate> to) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder sql = new StringBuilder("WITH payment_agg AS (" +
                " SELECT COALESCE(NULLIF(so_number, ''), root_appt_id) AS anchor_key," +
                " SUM(CASE WHEN doc_role = 'INVOICE' THEN COALESCE(subtotal, amount_gross) ELSE 0 END) AS order_total" +
                " FROM payments_ledger GROUP BY COALESCE(NULLIF(so_number, ''), root_appt_id)" +
                "), weights AS (SELECT stage, weight FROM dashboard_stage_weights)" +
                " SELECT m.sales_stage, COALESCE(agg.order_total, 0) AS order_total, COALESCE(w.weight, 0) AS weight" +
                " FROM master m" +
                " LEFT JOIN payment_agg agg ON agg.anchor_key = COALESCE(NULLIF(m.so_number, ''), m.root_appt_id)" +
                " LEFT JOIN weights w ON UPPER(w.stage) = UPPER(COALESCE(m.sales_stage, ''))" +
                " WHERE 1=1");

        from.ifPresent(date -> {
            sql.append(" AND m.visit_date >= :kpi_date_from");
            params.addValue("kpi_date_from", date);
        });
        to.ifPresent(date -> {
            sql.append(" AND m.visit_date <= :kpi_date_to");
            params.addValue("kpi_date_to", date);
        });

        List<Map<String, Object>> rows = jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> {
            Map<String, Object> map = new HashMap<>();
            map.put("order_total", rs.getBigDecimal("order_total"));
            map.put("weight", rs.getBigDecimal("weight"));
            map.put("stage", rs.getString("sales_stage"));
            return map;
        });

        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> row : rows) {
            BigDecimal orderTotal = (BigDecimal) row.get("order_total");
            BigDecimal weight = (BigDecimal) row.get("weight");
            if (orderTotal == null) {
                orderTotal = BigDecimal.ZERO;
            }
            if (weight == null) {
                weight = BigDecimal.ZERO;
            }
            total = total.add(orderTotal.multiply(weight));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTotalDeposits(Optional<LocalDate> from, Optional<LocalDate> to) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder sql = new StringBuilder("SELECT COALESCE(SUM(amount_net), 0) AS deposits" +
                " FROM payments_ledger" +
                " WHERE doc_role = 'RECEIPT' AND amount_net > 0" +
                " AND (doc_status IS NULL OR NOT (UPPER(doc_status) = ANY(:blocked)))");
        params.addValue("blocked", new String[]{"VOID", "VOIDED", "CANCELLED", "CANCELED", "REVERSED"});

        from.ifPresent(date -> {
            sql.append(" AND payment_datetime >= :dep_from");
            params.addValue("dep_from", date.atStartOfDay(timeUtil.nowZoned().getZone()).toOffsetDateTime());
        });
        to.ifPresent(date -> {
            sql.append(" AND payment_datetime < :dep_to");
            params.addValue("dep_to", date.plusDays(1).atStartOfDay(timeUtil.nowZoned().getZone()).toOffsetDateTime());
        });

        BigDecimal total = jdbcTemplate.query(sql.toString(), params,
                rs -> rs.next() ? rs.getBigDecimal("deposits") : BigDecimal.ZERO);
        return total == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : total.setScale(2, RoundingMode.HALF_UP);
    }

    private long calculateFirstTimeDeposits(Optional<LocalDate> from, Optional<LocalDate> to) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder sql = new StringBuilder("WITH receipts AS (" +
                " SELECT COALESCE(NULLIF(so_number, ''), root_appt_id) AS anchor_key," +
                " MIN(payment_datetime) AS first_payment" +
                " FROM payments_ledger" +
                " WHERE doc_role = 'RECEIPT' AND amount_net > 0" +
                " AND (doc_status IS NULL OR NOT (UPPER(doc_status) = ANY(:blocked)))" +
                " GROUP BY COALESCE(NULLIF(so_number, ''), root_appt_id)" +
                ") SELECT COUNT(*) FROM receipts WHERE 1=1");
        params.addValue("blocked", new String[]{"VOID", "VOIDED", "CANCELLED", "CANCELED", "REVERSED"});

        from.ifPresent(date -> {
            sql.append(" AND first_payment >= :first_dep_from");
            params.addValue("first_dep_from", date.atStartOfDay(timeUtil.nowZoned().getZone()).toOffsetDateTime());
        });
        to.ifPresent(date -> {
            sql.append(" AND first_payment < :first_dep_to");
            params.addValue("first_dep_to", date.plusDays(1).atStartOfDay(timeUtil.nowZoned().getZone()).toOffsetDateTime());
        });

        Integer count = jdbcTemplate.query(sql.toString(), params,
                rs -> rs.next() ? rs.getInt(1) : 0);
        return count == null ? 0 : count;
    }

    private long countOverdue(String deadlineColumn, Optional<LocalDate> from, Optional<LocalDate> to) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM master WHERE " + deadlineColumn + " IS NOT NULL");

        LocalDate upperBound = to.orElse(timeUtil.today());
        sql.append(" AND " + deadlineColumn + " < :upper_bound");
        params.addValue("upper_bound", upperBound);

        from.ifPresent(date -> {
            sql.append(" AND " + deadlineColumn + " >= :lower_bound");
            params.addValue("lower_bound", date);
        });

        sql.append(" AND (sales_stage IS NULL OR UPPER(sales_stage) <> 'ORDER COMPLETED')");

        Integer count = jdbcTemplate.query(sql.toString(), params,
                rs -> rs.next() ? rs.getInt(1) : 0);
        return count == null ? 0 : count;
    }
}
