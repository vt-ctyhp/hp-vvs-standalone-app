package com.hpvvssalesautomation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hpvvssalesautomation.domain.ClientStatusSubmitRequest;
import com.hpvvssalesautomation.seed.SeedRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HeaderHealingTests extends AbstractIntegrationTest {

    private static final Path FIXTURES_DIR = Paths.get("..", "fixtures").toAbsolutePath().normalize();

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private SeedRunner seedRunner;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        seedRunner.run(FIXTURES_DIR);
    }

    @Test
    void missingHeaderIsAppendedAndRowPersists() throws Exception {
        List<String> legacyHeaders = List.of(
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

        jdbcTemplate.update(
                "INSERT INTO meta (meta_key, meta_value) VALUES (:key, :value) ON CONFLICT (meta_key) DO UPDATE SET meta_value = :value",
                new MapSqlParameterSource()
                        .addValue("key", "client_status_log_headers")
                        .addValue("value", objectMapper.writeValueAsString(legacyHeaders))
        );

        ClientStatusSubmitRequest payload = new ClientStatusSubmitRequest();
        payload.setRootApptId("HP-1001");
        payload.setSalesStage("Consult");
        payload.setConversionStatus("OPEN");
        payload.setInProductionStatus("Started");
        payload.setUpdatedBy("tester@local");

        ResponseEntity<Void> response = restTemplate.postForEntity(
                "/client-status/submit",
                payload,
                Void.class
        );
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        String stored = jdbcTemplate.query(
                "SELECT meta_value FROM meta WHERE meta_key = :key",
                new MapSqlParameterSource("key", "client_status_log_headers"),
                rs -> rs.next() ? rs.getString(1) : null
        );
        List<String> healed = objectMapper.readValue(stored, new TypeReference<>() {});
        assertThat(healed).contains("In Production Status");

        Integer logCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM client_status_log WHERE root_appt_id = :root AND in_production_status = 'Started'",
                new MapSqlParameterSource("root", "HP-1001"),
                Integer.class
        );
        assertThat(logCount).isEqualTo(1);
    }
}
