package com.hpvvssalesautomation.domain;

import com.hpvvssalesautomation.alias.AliasRegistry;
import com.hpvvssalesautomation.util.TimeUtil;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class ReportsService {

    private static final Set<String> RECEIPT_BLOCKED_STATUSES = Set.of("VOID", "VOIDED", "CANCELLED", "CANCELED", "REVERSED");

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TimeUtil timeUtil;
    private final AliasRegistry aliasRegistry;

    public ReportsService(NamedParameterJdbcTemplate jdbcTemplate, TimeUtil timeUtil, AliasRegistry aliasRegistry) {
        this.jdbcTemplate = jdbcTemplate;
        this.timeUtil = timeUtil;
        this.aliasRegistry = aliasRegistry;
    }

    public ReportsResponse byStatus(String filtersQuery) {
        FilterContext context = parseFilters(filtersQuery);
        List<Map<String, Object>> rows = fetchRows(context, "sales_stage", true);
        return new ReportsResponse(rows);
    }

    public ReportsResponse byRep(String filtersQuery) {
        FilterContext context = parseFilters(filtersQuery);
        List<Map<String, Object>> rows = fetchRows(context, "assigned_rep", false);
        return new ReportsResponse(rows);
    }

    private List<Map<String, Object>> fetchRows(FilterContext context, String orderColumn, boolean includeStatusGrouping) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder sql = new StringBuilder("WITH payment_agg AS (" +
                " SELECT COALESCE(NULLIF(so_number, ''), root_appt_id) AS anchor_key, " +
                " SUM(CASE WHEN doc_role = 'RECEIPT' AND amount_net > 0 " +
                "          AND (doc_status IS NULL OR NOT (UPPER(doc_status) = ANY(:blocked_statuses))) " +
                "     THEN amount_net ELSE 0 END) AS total_pay_to_date, " +
                " SUM(CASE WHEN doc_role = 'INVOICE' THEN COALESCE(subtotal, amount_gross) ELSE 0 END) AS order_total " +
                " FROM payments_ledger GROUP BY COALESCE(NULLIF(so_number, ''), root_appt_id)" +
                ") SELECT m.visit_date, m.root_appt_id, m.customer_name, m.assigned_rep, m.assisted_rep, m.brand, " +
                " m.so_number, m.sales_stage, m.conversion_status, m.custom_order_status, m.center_stone_order_status, " +
                " m.next_steps, m.in_production_status, m.production_deadline, agg.order_total, agg.total_pay_to_date " +
                " FROM master m " +
                " LEFT JOIN payment_agg agg ON agg.anchor_key = COALESCE(NULLIF(m.so_number, ''), m.root_appt_id) " +
                " WHERE 1=1");
        params.addValue("blocked_statuses", RECEIPT_BLOCKED_STATUSES.toArray(new String[0]));

        context.sqlFilters().forEach((column, value) -> {
            String paramName = column.replace('.', '_');
            sql.append(" AND LOWER(" + column + ") = :" + paramName);
            params.addValue(paramName, value.toLowerCase(Locale.US));
        });

        if (context.dateFrom().isPresent()) {
            sql.append(" AND m.visit_date >= :date_from");
            params.addValue("date_from", context.dateFrom().get());
        }
        if (context.dateTo().isPresent()) {
            sql.append(" AND m.visit_date <= :date_to");
            params.addValue("date_to", context.dateTo().get());
        }

        sql.append(" ORDER BY COALESCE(" + orderColumn + ", ''), m.visit_date DESC NULLS LAST, m.root_appt_id");

        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> mapRow(rs, context.includeProduction()));
    }

    private Map<String, Object> mapRow(ResultSet rs, boolean includeProduction) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        LocalDate visitDate = rs.getDate("visit_date") != null ? rs.getDate("visit_date").toLocalDate() : null;
        row.put("Visit Date", visitDate != null ? timeUtil.formatDate(visitDate) : null);

        BigDecimal orderTotal = Optional.ofNullable(rs.getBigDecimal("order_total"))
                .orElse(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal payToDate = Optional.ofNullable(rs.getBigDecimal("total_pay_to_date"))
                .orElse(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        row.put("Order Total", orderTotal);
        row.put("Total Pay To Date", payToDate);

        row.put("RootApptID", rs.getString("root_appt_id"));
        row.put("Customer Name", rs.getString("customer_name"));
        row.put("Assigned Rep", rs.getString("assigned_rep"));

        String assisted = rs.getString("assisted_rep");
        row.put("Assisted Rep", assisted == null || assisted.isBlank() ? "" : "Assisted (" + assisted + ")");

        row.put("Brand", rs.getString("brand"));
        row.put("SO#", rs.getString("so_number"));
        row.put("Sales Stage", rs.getString("sales_stage"));
        row.put("Conversion Status", rs.getString("conversion_status"));
        row.put("Custom Order Status", rs.getString("custom_order_status"));

        if (includeProduction) {
            row.put("In Production Status", rs.getString("in_production_status"));
            LocalDate prodDeadline = rs.getDate("production_deadline") != null ? rs.getDate("production_deadline").toLocalDate() : null;
            row.put("Production Deadline", prodDeadline != null ? timeUtil.formatDate(prodDeadline) : null);
        }

        row.put("Center Stone Order Status", rs.getString("center_stone_order_status"));
        row.put("Next Steps", rs.getString("next_steps"));
        row.put("Client Status Report URL", null);
        return row;
    }

    private FilterContext parseFilters(String filtersQuery) {
        if (filtersQuery == null || filtersQuery.isBlank()) {
            return new FilterContext(Map.of(), Optional.empty(), Optional.empty(), false);
        }

        Map<String, String> raw = new LinkedHashMap<>();
        for (String part : filtersQuery.split(",")) {
            if (part == null || part.isBlank()) {
                continue;
            }
            String[] kv = part.split(":", 2);
            if (kv.length == 2) {
                raw.put(kv[0].trim(), kv[1].trim());
            }
        }

        boolean includeProduction = raw.entrySet().stream()
                .anyMatch(entry -> entry.getKey().equalsIgnoreCase("includeProductionCols") && isTrue(entry.getValue()));

        Map<String, String> sqlFilters = new LinkedHashMap<>();
        Optional<LocalDate> dateFrom = Optional.empty();
        Optional<LocalDate> dateTo = Optional.empty();

        for (Map.Entry<String, String> entry : raw.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key.equalsIgnoreCase("includeProductionCols")) {
                continue;
            }
            if (key.equalsIgnoreCase("dateFrom")) {
                dateFrom = timeUtil.parseDate(value);
                continue;
            }
            if (key.equalsIgnoreCase("dateTo")) {
                dateTo = timeUtil.parseDate(value);
                continue;
            }

            String column = resolveColumn(key);
            if (column != null && value != null && !value.isBlank()) {
                sqlFilters.put(column, value.trim());
            }
        }

        return new FilterContext(sqlFilters, dateFrom, dateTo, includeProduction);
    }

    private boolean isTrue(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.US);
        return normalized.equals("true") || normalized.equals("1") || normalized.equals("yes");
    }

    private String resolveColumn(String key) {
        String normalized = key.trim().toLowerCase(Locale.US);
        Map<String, List<String>> aliases = aliasRegistry.masterAppointmentAliases();
        for (Map.Entry<String, List<String>> entry : aliases.entrySet()) {
            for (String alias : entry.getValue()) {
                if (alias != null && normalized.equals(alias.toLowerCase(Locale.US))) {
                    return switch (entry.getKey()) {
                        case "Visit Date" -> "m.visit_date";
                        case "RootApptID" -> "m.root_appt_id";
                        case "Customer" -> "m.customer_name";
                        case "Assigned Rep" -> "m.assigned_rep";
                        case "Assisted Rep" -> "m.assisted_rep";
                        case "Brand" -> "m.brand";
                        case "SO#" -> "m.so_number";
                        case "Sales Stage" -> "m.sales_stage";
                        case "Conversion Status" -> "m.conversion_status";
                        case "Custom Order Status" -> "m.custom_order_status";
                        case "Center Stone Order Status" -> "m.center_stone_order_status";
                        default -> null;
                    };
                }
            }
        }

        return switch (normalized) {
            case "rep", "assignedrep" -> "m.assigned_rep";
            case "status" -> "m.sales_stage";
            default -> null;
        };
    }

    private record FilterContext(Map<String, String> sqlFilters,
                                 Optional<LocalDate> dateFrom,
                                 Optional<LocalDate> dateTo,
                                 boolean includeProduction) {
    }
}
