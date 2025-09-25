package com.hpvvssalesautomation;

import com.hpvvssalesautomation.domain.ClientStatusResponse;
import com.hpvvssalesautomation.domain.ClientStatusSubmitRequest;
import com.hpvvssalesautomation.domain.DeadlineRecordRequest;
import com.hpvvssalesautomation.domain.DeadlineRecordResponse;
import com.hpvvssalesautomation.seed.SeedRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class IdempotencyTests extends AbstractIntegrationTest {

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
    void duplicateClientStatusPayloadReturnsUnchanged() {
        ClientStatusSubmitRequest payload = new ClientStatusSubmitRequest();
        payload.setRootApptId("HP-1001");
        payload.setSalesStage("Consult");
        payload.setConversionStatus("OPEN");
        payload.setCustomOrderStatus("In Queue");
        payload.setNextSteps("Book DV");
        payload.setAssistedRep("Alex");
        payload.setUpdatedBy("tester@local");

        ResponseEntity<ClientStatusResponse> first = restTemplate.postForEntity(
                "/client-status/submit",
                payload,
                ClientStatusResponse.class
        );
        ResponseEntity<ClientStatusResponse> second = restTemplate.postForEntity(
                "/client-status/submit",
                payload,
                ClientStatusResponse.class
        );

        assertThat(first.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(second.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(second.getBody()).isNotNull();
        assertThat(second.getBody().status()).isEqualTo("UNCHANGED");

        MapSqlParameterSource params = new MapSqlParameterSource("root", "HP-1001");
        Integer reportCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM per_client_reports WHERE root_appt_id = :root",
                params,
                Integer.class
        );
        assertThat(reportCount).isEqualTo(1);

        Integer logCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM client_status_log WHERE root_appt_id = :root",
                params,
                Integer.class
        );
        assertThat(logCount).isEqualTo(1);

        Integer statusEntryCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM per_client_entries WHERE root_appt_id = :root AND deadline_type IS NULL",
                params,
                Integer.class
        );
        assertThat(statusEntryCount).isEqualTo(1);

        OffsetDateTime snapshotUpdatedAt = jdbcTemplate.query(
                "SELECT updated_at FROM per_client_reports WHERE root_appt_id = :root",
                params,
                rs -> rs.next() ? rs.getObject("updated_at", OffsetDateTime.class) : null
        );
        assertThat(snapshotUpdatedAt).isEqualTo(first.getBody().updatedAt());
        assertThat(second.getBody().updatedAt()).isEqualTo(first.getBody().updatedAt());
    }

    @Test
    void deadlineMoveCountOnlyIncrementsWhenDateChanges() {
        DeadlineRecordRequest payload = new DeadlineRecordRequest();
        payload.setRootApptId("HP-1002");
        payload.setDeadlineType("3D");
        payload.setDeadlineDate("2025-10-01");
        payload.setMovedBy("planner@local");

        ResponseEntity<DeadlineRecordResponse> first = restTemplate.postForEntity(
                "/deadlines/record",
                payload,
                DeadlineRecordResponse.class
        );
        ResponseEntity<DeadlineRecordResponse> duplicate = restTemplate.postForEntity(
                "/deadlines/record",
                payload,
                DeadlineRecordResponse.class
        );

        DeadlineRecordRequest updated = new DeadlineRecordRequest();
        updated.setRootApptId("HP-1002");
        updated.setDeadlineType("3D");
        updated.setDeadlineDate("2025-10-05");
        updated.setMovedBy("planner@local");

        ResponseEntity<DeadlineRecordResponse> changed = restTemplate.postForEntity(
                "/deadlines/record",
                updated,
                DeadlineRecordResponse.class
        );

        assertThat(first.getBody()).isNotNull();
        assertThat(duplicate.getBody()).isNotNull();
        assertThat(changed.getBody()).isNotNull();

        assertThat(first.getBody().moveCount()).isEqualTo(1);
        assertThat(duplicate.getBody().moveCount()).isEqualTo(1);
        assertThat(changed.getBody().moveCount()).isEqualTo(2);

        MapSqlParameterSource params = new MapSqlParameterSource("root", "HP-1002");
        Integer moves = jdbcTemplate.queryForObject(
                "SELECT three_d_deadline_moves FROM master WHERE root_appt_id = :root",
                params,
                Integer.class
        );
        assertThat(moves).isEqualTo(2);
    }
}
