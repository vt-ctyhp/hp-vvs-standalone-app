package com.hpvvssalesautomation;

import com.hpvvssalesautomation.domain.ClientStatusResponse;
import com.hpvvssalesautomation.domain.ClientStatusSubmitRequest;
import com.hpvvssalesautomation.seed.SeedRunner;
import com.hpvvssalesautomation.util.TimeUtil;
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
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClientStatusTests extends AbstractIntegrationTest {

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
    void submitClientStatusAppendsLogAndSnapshot() {
        ClientStatusSubmitRequest payload = new ClientStatusSubmitRequest();
        payload.setRootApptId("HP-1001");
        payload.setSalesStage("Consult");
        payload.setConversionStatus("OPEN");
        payload.setCustomOrderStatus("In Queue");
        payload.setInProductionStatus("Not Started");
        payload.setCenterStoneOrderStatus("Pending");
        payload.setNextSteps("Call customer back");
        payload.setAssistedRep("Alex Support");
        payload.setUpdatedBy("tester@local");

        ResponseEntity<ClientStatusResponse> response = restTemplate.postForEntity(
                "/client-status/submit",
                payload,
                ClientStatusResponse.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().rootApptId()).isEqualTo("HP-1001");
        assertThat(response.getBody().status()).isEqualTo("OK");

        MapSqlParameterSource params = new MapSqlParameterSource("root", "HP-1001");
        Map<String, Object> logRow = jdbcTemplate.queryForMap(
                "SELECT log_date, sales_stage, conversion_status, custom_order_status, in_production_status, center_stone_order_status, next_steps, assisted_rep, updated_by, updated_at FROM client_status_log WHERE root_appt_id = :root",
                params
        );
        LocalDate expectedDate = timeUtil.today();
        assertThat(((java.sql.Date) logRow.get("log_date")).toLocalDate()).isEqualTo(expectedDate);
        assertThat(logRow.get("sales_stage")).isEqualTo("Consult");
        assertThat(logRow.get("conversion_status")).isEqualTo("OPEN");
        assertThat(logRow.get("custom_order_status")).isEqualTo("In Queue");
        assertThat(logRow.get("in_production_status")).isEqualTo("Not Started");
        assertThat(logRow.get("center_stone_order_status")).isEqualTo("Pending");
        assertThat(logRow.get("next_steps")).isEqualTo("Call customer back");
        assertThat(logRow.get("assisted_rep")).isEqualTo("Alex Support");
        assertThat(logRow.get("updated_by")).isEqualTo("tester@local");

        Map<String, Object> statusRow = jdbcTemplate.queryForObject(
                "SELECT log_date, sales_stage, conversion_status, custom_order_status, center_stone_order_status, next_steps, deadline_type, deadline_date, move_count, assisted_rep, updated_by, updated_at " +
                        "FROM per_client_entries WHERE root_appt_id = :root AND deadline_type IS NULL ORDER BY id DESC LIMIT 1",
                params,
                (rs, rowNum) -> {
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("log_date", rs.getDate("log_date"));
                    data.put("sales_stage", rs.getString("sales_stage"));
                    data.put("conversion_status", rs.getString("conversion_status"));
                    data.put("custom_order_status", rs.getString("custom_order_status"));
                    data.put("center_stone_order_status", rs.getString("center_stone_order_status"));
                    data.put("next_steps", rs.getString("next_steps"));
                    data.put("deadline_type", rs.getString("deadline_type"));
                    data.put("deadline_date", rs.getDate("deadline_date"));
                    data.put("move_count", rs.getObject("move_count"));
                    data.put("assisted_rep", rs.getString("assisted_rep"));
                    data.put("updated_by", rs.getString("updated_by"));
                    data.put("updated_at", rs.getObject("updated_at", OffsetDateTime.class));
                    return data;
                }
        );
        assertThat(((java.sql.Date) statusRow.get("log_date")).toLocalDate()).isEqualTo(expectedDate);
        assertThat(statusRow.get("sales_stage")).isEqualTo("Consult");
        assertThat(statusRow.get("conversion_status")).isEqualTo("OPEN");
        assertThat(statusRow.get("custom_order_status")).isEqualTo("In Queue");
        assertThat(statusRow.get("center_stone_order_status")).isEqualTo("Pending");
        assertThat(statusRow.get("next_steps")).isEqualTo("Call customer back");
        assertThat(statusRow.get("deadline_type")).isNull();
        assertThat(statusRow.get("deadline_date")).isNull();
        assertThat(statusRow.get("move_count")).isNull();
        assertThat(statusRow.get("assisted_rep")).isEqualTo("Alex Support");
        assertThat(statusRow.get("updated_by")).isEqualTo("tester@local");
        assertThat(statusRow.get("updated_at")).isInstanceOf(OffsetDateTime.class);

        Map<String, Object> snapshot = jdbcTemplate.query(
                "SELECT updated_by, updated_at, sales_stage, conversion_status, custom_order_status, in_production_status, center_stone_order_status, next_steps, assisted_rep FROM per_client_reports WHERE root_appt_id = :root",
                params,
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("updated_by", rs.getString("updated_by"));
                    data.put("updated_at", rs.getObject("updated_at", OffsetDateTime.class));
                    data.put("sales_stage", rs.getString("sales_stage"));
                    data.put("conversion_status", rs.getString("conversion_status"));
                    data.put("custom_order_status", rs.getString("custom_order_status"));
                    data.put("in_production_status", rs.getString("in_production_status"));
                    data.put("center_stone_order_status", rs.getString("center_stone_order_status"));
                    data.put("next_steps", rs.getString("next_steps"));
                    data.put("assisted_rep", rs.getString("assisted_rep"));
                    return data;
                }
        );
        assertThat(snapshot).isNotNull();
        assertThat(snapshot.get("updated_by")).isEqualTo("tester@local");
        assertThat(snapshot.get("updated_at")).isInstanceOf(OffsetDateTime.class);
        assertThat(snapshot.get("sales_stage")).isEqualTo("Consult");
        assertThat(snapshot.get("conversion_status")).isEqualTo("OPEN");
        assertThat(snapshot.get("custom_order_status")).isEqualTo("In Queue");
        assertThat(snapshot.get("in_production_status")).isEqualTo("Not Started");
        assertThat(snapshot.get("center_stone_order_status")).isEqualTo("Pending");
        assertThat(snapshot.get("next_steps")).isEqualTo("Call customer back");
        assertThat(snapshot.get("assisted_rep")).isEqualTo("Alex Support");
    }

    @Test
    void timestampsArePacificTime() {
        ClientStatusSubmitRequest payload = new ClientStatusSubmitRequest();
        payload.setRootApptId("HP-1002");
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

        MapSqlParameterSource params = new MapSqlParameterSource("root", "HP-1002");
        LocalDate logDate = jdbcTemplate.query(
                "SELECT log_date FROM client_status_log WHERE root_appt_id = :root",
                params,
                rs -> rs.next() ? rs.getDate("log_date").toLocalDate() : null
        );
        assertThat(logDate).isEqualTo(timeUtil.today());

        OffsetDateTime dbUpdatedAt = jdbcTemplate.query(
                "SELECT updated_at FROM client_status_log WHERE root_appt_id = :root",
                params,
                rs -> rs.next() ? rs.getObject("updated_at", OffsetDateTime.class) : null
        );
        assertThat(dbUpdatedAt).isNotNull();
        ZoneId pacific = ZoneId.of("America/Los_Angeles");
        OffsetDateTime normalizedDb = dbUpdatedAt.atZoneSameInstant(pacific).toOffsetDateTime();
        assertThat(normalizedDb.getOffset()).isEqualTo(response.getBody().updatedAt().getOffset());
        assertThat(normalizedDb.toLocalDateTime()).isEqualTo(response.getBody().updatedAt().toLocalDateTime());
    }
}
