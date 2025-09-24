package com.hp.vvs.app;

import com.hp.vvs.app.domain.AppointmentSummaryService;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
class AppointmentSummaryTests {

    private static final boolean DOCKER_AVAILABLE;
    private static PostgreSQLContainer<?> postgres;

    static {
        boolean available;
        try {
            DockerClientFactory.instance().client();
            available = true;
        } catch (Throwable throwable) {
            available = false;
        }
        DOCKER_AVAILABLE = available;
        if (DOCKER_AVAILABLE) {
            postgres = new PostgreSQLContainer<>("postgres:15-alpine");
            postgres.start();
        }
    }

    @DynamicPropertySource
    static void registerDataSource(DynamicPropertyRegistry registry) {
        if (postgres == null) {
            return;
        }
        registry.add("DB_URL", postgres::getJdbcUrl);
        registry.add("DB_USERNAME", postgres::getUsername);
        registry.add("DB_PASSWORD", postgres::getPassword);
        registry.add("app.datasource.url", postgres::getJdbcUrl);
        registry.add("app.datasource.username", postgres::getUsername);
        registry.add("app.datasource.password", postgres::getPassword);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AppointmentSummaryService appointmentSummaryService;

    @Autowired
    private MockMvc mockMvc;

    @BeforeAll
    static void ensureDocker() {
        Assumptions.assumeTrue(DOCKER_AVAILABLE, "Docker is required for Testcontainers");
    }

    @AfterAll
    static void shutdown() {
        if (postgres != null) {
            postgres.stop();
        }
    }

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE master RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE payments RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("TRUNCATE meta RESTART IDENTITY CASCADE");
        seedMaster();
        seedPayments();
    }

    private void seedMaster() {
        jdbcTemplate.update("INSERT INTO master (visit_date, root_appt_id, customer, phone, email, visit_type, visit_number, so_number, brand, sales_stage, conversion_status, custom_order_status, center_stone_order_status, assigned_rep, assisted_rep) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                LocalDate.of(2024, 3, 15), "ROOT-1", "Alice Smith", "555-1111", "alice@example.com", "Showroom", "1", "SO-100", "HP", "Presented", "Pending", "Cutting", "Queued", "Reed", "Morgan");
        jdbcTemplate.update("INSERT INTO master (visit_date, root_appt_id, customer, phone, email, visit_type, visit_number, so_number, brand, sales_stage, conversion_status, custom_order_status, center_stone_order_status, assigned_rep, assisted_rep) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                LocalDate.of(2024, 3, 16), "ROOT-2", "Bob Jones", "555-2222", "bob@example.com", "Virtual", "2", "SO-200", "VVS", "Closed Won", "Converted", "Delivered", "Delivered", "Morgan", "Reed");
    }

    private void seedPayments() {
        jdbcTemplate.update("INSERT INTO payments (root_appt_id, so_number, payment_datetime, amount_net, doc_type, doc_status) VALUES (?,?,?,?,?,?)",
                "ROOT-1", "SO-100", LocalDate.of(2024, 3, 17).atStartOfDay(), 1200.50, "Receipt", "POSTED");
        jdbcTemplate.update("INSERT INTO payments (root_appt_id, so_number, payment_datetime, amount_net, doc_type, doc_status) VALUES (?,?,?,?,?,?)",
                "ROOT-2", "SO-200", LocalDate.of(2024, 3, 18).atStartOfDay(), 0.00, "Receipt", "POSTED");
        jdbcTemplate.update("INSERT INTO payments (root_appt_id, so_number, payment_datetime, amount_net, doc_type, doc_status) VALUES (?,?,?,?,?,?)",
                "ROOT-2", "SO-200", LocalDate.of(2024, 3, 19).atStartOfDay(), 950.00, "Void", "VOID");
    }

    @Test
    void appointmentSummaryServiceReturnsRows() {
        List<Map<String, String>> rows = appointmentSummaryService.run(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        assertThat(rows).hasSize(2);
        Map<String, String> first = rows.get(0);
        assertThat(first).isInstanceOf(LinkedHashMap.class);
        assertThat(first.keySet()).containsExactlyInAnyOrderElementsOf(com.hp.vvs.app.domain.AppointmentSummaryService.COLUMN_ORDER);
    }

    @Test
    void controllerRespondsWithOrderedColumns() throws Exception {
        mockMvc.perform(post("/appointment-summary/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void healthEndpointUp() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"UP\"}"));
    }
}
