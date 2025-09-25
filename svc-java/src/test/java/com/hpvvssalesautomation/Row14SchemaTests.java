package com.hpvvssalesautomation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class Row14SchemaTests extends AbstractIntegrationTest {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    void perClientEntriesColumnsMatchRow14Schema() {
        List<String> columnNames = jdbcTemplate.query(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'per_client_entries' ORDER BY ordinal_position",
                new MapSqlParameterSource(),
                (rs, rowNum) -> rs.getString(1)
        );

        assertThat(columnNames).containsSubsequence(
                "log_date",
                "sales_stage",
                "conversion_status",
                "custom_order_status",
                "center_stone_order_status",
                "next_steps",
                "deadline_type",
                "deadline_date",
                "move_count",
                "assisted_rep",
                "updated_by",
                "updated_at"
        );
    }
}
