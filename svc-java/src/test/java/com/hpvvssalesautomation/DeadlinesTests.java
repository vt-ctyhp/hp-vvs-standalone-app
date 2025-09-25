package com.hpvvssalesautomation;

import com.hpvvssalesautomation.domain.DeadlineRecordRequest;
import com.hpvvssalesautomation.domain.DeadlineRecordResponse;
import com.hpvvssalesautomation.seed.SeedRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DeadlinesTests extends AbstractIntegrationTest {

    private static final Path FIXTURES_DIR = Paths.get("..", "fixtures").toAbsolutePath().normalize();

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private SeedRunner seedRunner;

    @BeforeEach
    void setUp() throws Exception {
        seedRunner.run(FIXTURES_DIR);
    }

    @Test
    void threeDDeadlineUpdatesCountersAndLogs() {
        DeadlineRecordRequest payload = new DeadlineRecordRequest();
        payload.setRootApptId("HP-1001");
        payload.setDeadlineType("3D");
        payload.setDeadlineDate("2025-10-01");
        payload.setMovedBy("planner@local");
        payload.setAssistedRep("Alex Support");

        ResponseEntity<DeadlineRecordResponse> response = restTemplate.postForEntity(
                "/deadlines/record",
                payload,
                DeadlineRecordResponse.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().moveCount()).isEqualTo(1);
        assertThat(response.getBody().deadlineType()).isEqualTo("3D");

        MapSqlParameterSource params = new MapSqlParameterSource("root", "HP-1001");
        Map<String, Object> master = jdbcTemplate.queryForMap(
                "SELECT three_d_deadline, three_d_deadline_moves FROM master WHERE root_appt_id = :root",
                params
        );
        assertThat(master.get("three_d_deadline")).isInstanceOf(java.sql.Date.class);
        assertThat(((java.sql.Date) master.get("three_d_deadline")).toLocalDate()).isEqualTo(LocalDate.parse("2025-10-01"));
        assertThat(master.get("three_d_deadline_moves")).isEqualTo(1);

        Map<String, Object> entry = jdbcTemplate.queryForMap(
                "SELECT deadline_type, deadline_date, move_count, assisted_rep FROM per_client_entries WHERE root_appt_id = :root AND deadline_type = '3D' ORDER BY id DESC LIMIT 1",
                params
        );
        assertThat(entry.get("deadline_type")).isEqualTo("3D");
        assertThat(((java.sql.Date) entry.get("deadline_date")).toLocalDate()).isEqualTo(LocalDate.parse("2025-10-01"));
        assertThat(entry.get("move_count")).isEqualTo(1);
        assertThat(entry.get("assisted_rep")).isEqualTo("Alex Support");

        Map<String, Object> snapshot = jdbcTemplate.queryForMap(
                "SELECT three_d_deadline, three_d_deadline_moves, assisted_rep FROM per_client_reports WHERE root_appt_id = :root",
                params
        );
        assertThat(((java.sql.Date) snapshot.get("three_d_deadline")).toLocalDate()).isEqualTo(LocalDate.parse("2025-10-01"));
        assertThat(snapshot.get("three_d_deadline_moves")).isEqualTo(1);
        assertThat(snapshot.get("assisted_rep")).isEqualTo("Alex Support");
    }

    @Test
    void productionDeadlineUpdatesCountersAndLogs() {
        DeadlineRecordRequest payload = new DeadlineRecordRequest();
        payload.setRootApptId("HP-1002");
        payload.setDeadlineType("PROD");
        payload.setDeadlineDate("2025-11-15");
        payload.setMovedBy("planner@local");
        payload.setAssistedRep("Jamie Support");

        ResponseEntity<DeadlineRecordResponse> response = restTemplate.postForEntity(
                "/deadlines/record",
                payload,
                DeadlineRecordResponse.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().moveCount()).isEqualTo(1);
        assertThat(response.getBody().deadlineType()).isEqualTo("PROD");

        MapSqlParameterSource params = new MapSqlParameterSource("root", "HP-1002");
        Map<String, Object> master = jdbcTemplate.queryForMap(
                "SELECT production_deadline, production_deadline_moves FROM master WHERE root_appt_id = :root",
                params
        );
        assertThat(((java.sql.Date) master.get("production_deadline")).toLocalDate()).isEqualTo(LocalDate.parse("2025-11-15"));
        assertThat(master.get("production_deadline_moves")).isEqualTo(1);

        Map<String, Object> entry = jdbcTemplate.queryForMap(
                "SELECT deadline_type, deadline_date, move_count, assisted_rep FROM per_client_entries WHERE root_appt_id = :root AND deadline_type = 'PROD' ORDER BY id DESC LIMIT 1",
                params
        );
        assertThat(entry.get("deadline_type")).isEqualTo("PROD");
        assertThat(((java.sql.Date) entry.get("deadline_date")).toLocalDate()).isEqualTo(LocalDate.parse("2025-11-15"));
        assertThat(entry.get("move_count")).isEqualTo(1);
        assertThat(entry.get("assisted_rep")).isEqualTo("Jamie Support");

        Map<String, Object> snapshot = jdbcTemplate.queryForMap(
                "SELECT production_deadline, production_deadline_moves FROM per_client_reports WHERE root_appt_id = :root",
                params
        );
        assertThat(((java.sql.Date) snapshot.get("production_deadline")).toLocalDate()).isEqualTo(LocalDate.parse("2025-11-15"));
        assertThat(snapshot.get("production_deadline_moves")).isEqualTo(1);
    }
}
