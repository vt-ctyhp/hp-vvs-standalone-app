package com.hp.vvs.app.adapters;

import com.hp.vvs.app.domain.AppointmentSummaryRequest;
import com.hp.vvs.app.domain.AppointmentSummaryRow;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

@Component
public class SheetsAdapter {

    private final JdbcTemplate jdbcTemplate;
    private final DateTimeFormatter visitDateFormatter;

    public SheetsAdapter(JdbcTemplate jdbcTemplate, ZoneId zoneId) {
        this.jdbcTemplate = jdbcTemplate;
        this.visitDateFormatter = DateTimeFormatter.ISO_DATE;
    }

    public List<AppointmentSummaryRow> fetchAppointmentSummary(AppointmentSummaryRequest request) {
        StringBuilder sql = new StringBuilder("SELECT visit_date, root_appt_id, customer, phone, email, visit_type, visit_number, so_number, brand, sales_stage, conversion_status, custom_order_status, center_stone_order_status, assigned_rep, assisted_rep FROM master WHERE 1=1");
        List<Object> params = new ArrayList<>();

        request.brand().ifPresent(value -> {
            sql.append(" AND LOWER(brand) = LOWER(?)");
            params.add(value);
        });

        request.rep().ifPresent(value -> {
            sql.append(" AND (LOWER(assigned_rep) = LOWER(?) OR LOWER(assisted_rep) = LOWER(?))");
            params.add(value);
            params.add(value);
        });

        request.dateFrom().ifPresent(value -> {
            sql.append(" AND visit_date >= ?");
            params.add(value);
        });

        request.dateTo().ifPresent(value -> {
            sql.append(" AND visit_date <= ?");
            params.add(value);
        });

        sql.append(" ORDER BY visit_date DESC, root_appt_id");

        return jdbcTemplate.query(sql.toString(), params.toArray(), new AppointmentSummaryMapper());
    }

    private class AppointmentSummaryMapper implements RowMapper<AppointmentSummaryRow> {
        @Override
        public AppointmentSummaryRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            Map<String, String> values = new LinkedHashMap<>();
            values.put("Visit Date", Optional.ofNullable(rs.getDate("visit_date"))
                    .map(date -> visitDateFormatter.format(date.toLocalDate()))
                    .orElse(""));
            values.put("RootApptID", rs.getString("root_appt_id"));
            values.put("Customer", rs.getString("customer"));
            values.put("Phone", rs.getString("phone"));
            values.put("Email", rs.getString("email"));
            values.put("Visit Type", rs.getString("visit_type"));
            values.put("Visit #", rs.getString("visit_number"));
            values.put("SO#", rs.getString("so_number"));
            values.put("Brand", rs.getString("brand"));
            values.put("Sales Stage", rs.getString("sales_stage"));
            values.put("Conversion Status", rs.getString("conversion_status"));
            values.put("Custom Order Status", rs.getString("custom_order_status"));
            values.put("Center Stone Order Status", rs.getString("center_stone_order_status"));
            values.put("Assigned Rep", rs.getString("assigned_rep"));
            values.put("Assisted Rep", rs.getString("assisted_rep"));
            return new AppointmentSummaryRow(values);
        }
    }
}
