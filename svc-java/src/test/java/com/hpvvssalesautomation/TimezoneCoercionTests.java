package com.hpvvssalesautomation;

import com.hpvvssalesautomation.domain.ClientStatusResponse;
import com.hpvvssalesautomation.domain.ClientStatusSubmitRequest;
import com.hpvvssalesautomation.domain.DeadlineRecordRequest;
import com.hpvvssalesautomation.domain.DeadlineRecordResponse;
import com.hpvvssalesautomation.seed.SeedRunner;
import com.hpvvssalesautomation.util.TimeUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class TimezoneCoercionTests extends AbstractIntegrationTest {

    private static final Path FIXTURES_DIR = Paths.get("..", "fixtures").toAbsolutePath().normalize();

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private SeedRunner seedRunner;

    @Autowired
    private TimeUtil timeUtil;

    @BeforeEach
    void setUp() throws Exception {
        seedRunner.run(FIXTURES_DIR);
    }

    @Test
    void clientStatusTimestampsArePacific() {
        ClientStatusSubmitRequest payload = new ClientStatusSubmitRequest();
        payload.setRootApptId("HP-1003");
        payload.setSalesStage("Consult");
        payload.setConversionStatus("OPEN");
        payload.setUpdatedBy("tester@local");

        ResponseEntity<ClientStatusResponse> response = restTemplate.postForEntity(
                "/client-status/submit",
                payload,
                ClientStatusResponse.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();

        MapSqlParameterSource params = new MapSqlParameterSource("root", "HP-1003");
        LocalDate logDate = jdbcTemplate.query(
                "SELECT log_date FROM client_status_log WHERE root_appt_id = :root",
                params,
                rs -> rs.next() ? rs.getDate("log_date").toLocalDate() : null
        );
        assertThat(logDate).isEqualTo(timeUtil.today());

        OffsetDateTime updatedAt = jdbcTemplate.query(
                "SELECT updated_at FROM client_status_log WHERE root_appt_id = :root",
                params,
                rs -> rs.next() ? rs.getObject("updated_at", OffsetDateTime.class) : null
        );
        assertThat(updatedAt).isNotNull();

        ZoneId pacific = ZoneId.of("America/Los_Angeles");
        OffsetDateTime normalized = updatedAt.atZoneSameInstant(pacific).toOffsetDateTime();
        assertThat(normalized.getOffset()).isEqualTo(response.getBody().updatedAt().getOffset());
        assertThat(normalized.toLocalDateTime()).isEqualTo(response.getBody().updatedAt().toLocalDateTime());
    }

    @Test
    void deadlineDatesHandleIsoVariants() {
        DeadlineRecordRequest plain = new DeadlineRecordRequest();
        plain.setRootApptId("HP-1001");
        plain.setDeadlineType("PROD");
        plain.setDeadlineDate("2025-12-01");
        plain.setMovedBy("scheduler@local");

        ResponseEntity<DeadlineRecordResponse> first = restTemplate.postForEntity(
                "/deadlines/record",
                plain,
                DeadlineRecordResponse.class
        );
        assertThat(first.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(first.getBody()).isNotNull();
        assertThat(first.getBody().deadlineDate()).isEqualTo(LocalDate.parse("2025-12-01"));

        DeadlineRecordRequest offset = new DeadlineRecordRequest();
        offset.setRootApptId("HP-1001");
        offset.setDeadlineType("PROD");
        offset.setDeadlineDate("2025-12-15T12:30:00Z");
        offset.setMovedBy("scheduler@local");

        ResponseEntity<DeadlineRecordResponse> second = restTemplate.postForEntity(
                "/deadlines/record",
                offset,
                DeadlineRecordResponse.class
        );
        assertThat(second.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(second.getBody()).isNotNull();
        assertThat(second.getBody().deadlineDate()).isEqualTo(LocalDate.parse("2025-12-15"));

        DeadlineRecordRequest zoned = new DeadlineRecordRequest();
        zoned.setRootApptId("HP-1001");
        zoned.setDeadlineType("PROD");
        zoned.setDeadlineDate("2026-01-05T09:00:00-04:00");
        zoned.setMovedBy("scheduler@local");

        ResponseEntity<DeadlineRecordResponse> third = restTemplate.postForEntity(
                "/deadlines/record",
                zoned,
                DeadlineRecordResponse.class
        );
        assertThat(third.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(third.getBody()).isNotNull();
        assertThat(third.getBody().deadlineDate()).isEqualTo(LocalDate.parse("2026-01-05"));
    }
}
