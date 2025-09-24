package com.hp.vvs.app.adapters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class PerClientReportAdapter {

    private static final String META_KEY_HEADERS = "client_status_headers";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PerClientReportAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void ensureHeaderSchema(List<String> headers) {
        try {
            String payload = objectMapper.writeValueAsString(headers);
            jdbcTemplate.update("INSERT INTO meta (key, value) VALUES (?, ?) "
                            + "ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value",
                    META_KEY_HEADERS, payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize headers", e);
        }
    }

    public void appendEntries(String rootApptId, List<Map<String, String>> rows) {
        for (Map<String, String> row : rows) {
            jdbcTemplate.update(
                    "INSERT INTO client_status_entries (root_appt_id, payload_json) VALUES (?, ?)",
                    rootApptId,
                    toJson(row));
        }
    }

    private String toJson(Map<String, String> row) {
        try {
            return objectMapper.writeValueAsString(row);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize row", e);
        }
    }
}
