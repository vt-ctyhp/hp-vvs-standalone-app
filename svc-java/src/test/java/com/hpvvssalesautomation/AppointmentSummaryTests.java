package com.hpvvssalesautomation;

import com.hpvvssalesautomation.adapters.LedgerAdapter;
import com.hpvvssalesautomation.adapters.LedgerEntry;
import com.hpvvssalesautomation.alias.AliasRegistry;
import com.hpvvssalesautomation.domain.AppointmentSummaryRequest;
import com.hpvvssalesautomation.domain.AppointmentSummaryRow;
import com.hpvvssalesautomation.domain.AppointmentSummaryService;
import com.hpvvssalesautomation.seed.SeedRunner;
import com.hpvvssalesautomation.util.HeaderMap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AppointmentSummaryTests extends AbstractIntegrationTest {
    @Autowired
    private SeedRunner seedRunner;

    @Autowired
    private AppointmentSummaryService summaryService;

    @Autowired
    private LedgerAdapter ledgerAdapter;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    private final AliasRegistry aliasRegistry = new AliasRegistry();

    @BeforeAll
    void seedDatabase() throws Exception {
        Path fixturesDir = Paths.get("..", "fixtures").toAbsolutePath().normalize();
        seedRunner.run(fixturesDir);
    }

    @Test
    void appointmentSummaryReturnsCanonicalColumns() {
        List<Map<String, Object>> rows = summaryService.run(new AppointmentSummaryRequest());
        assertThat(rows).isNotEmpty();

        Map<String, Object> first = rows.get(0);
        assertThat(new ArrayList<>(first.keySet()))
                .containsExactly(AppointmentSummaryRow.COLUMN_ORDER);
    }

    @Test
    void appointmentSummaryHonorsFilters() {
        AppointmentSummaryRequest request = new AppointmentSummaryRequest();
        request.setBrand("HPUSA");
        request.setDateFrom(LocalDate.of(2024, 4, 1).toString());
        List<Map<String, Object>> rows = summaryService.run(request);
        assertThat(rows).isNotEmpty();
        assertThat(rows).allMatch(row -> "HPUSA".equals(row.get("Brand")));
    }

    @Test
    void ledgerAdapterFiltersVoidDocs() {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("root_appt_id", "HP-VOID");
        params.addValue("payment_datetime", null);
        params.addValue("doc_type", "SALES_RECEIPT");
        params.addValue("amount_net", 25.00);
        params.addValue("doc_status", "VOID");
        jdbcTemplate.update("INSERT INTO payments (root_appt_id, payment_datetime, doc_type, amount_net, doc_status) VALUES (:root_appt_id, :payment_datetime, :doc_type, :amount_net, :doc_status)", params);

        List<LedgerEntry> entries = ledgerAdapter.fetchReceiptEntries();
        assertThat(entries).noneMatch(entry -> "VOID".equalsIgnoreCase(entry.status()));
    }

    @Test
    void headerMapResolvesAliasesRegardlessOfOrder() {
        List<String> headers = Arrays.asList("Root Appt ID", "Customer Name", "Visit Date");
        List<String> shuffled = new ArrayList<>(headers);
        java.util.Collections.reverse(shuffled);
        HeaderMap map = new HeaderMap(shuffled, aliasRegistry.masterAppointmentAliases());
        LinkedHashMap<String, String> resolved = new LinkedHashMap<>(map.asMap());
        assertThat(resolved.get("RootApptID")).isEqualTo("Root Appt ID");
        assertThat(resolved.get("Customer")).isEqualTo("Customer Name");
    }
}
