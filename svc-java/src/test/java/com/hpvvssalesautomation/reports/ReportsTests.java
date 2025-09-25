package com.hpvvssalesautomation.reports;

import com.hpvvssalesautomation.AbstractIntegrationTest;
import com.hpvvssalesautomation.domain.ReportsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = "FEATURE_REPORTS=true")
class ReportsTests extends AbstractIntegrationTest {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @BeforeEach
    void setupData() {
        jdbcTemplate.getJdbcTemplate().update("DELETE FROM payments_ledger");
        jdbcTemplate.getJdbcTemplate().update("DELETE FROM master");

        insertMasterRow("2024-07-10", "HP-501", "SO-501", "Alice Example", "Jamie Rep", "Sky Helper",
                "HPUSA", "Deposit", "Won", "Production", "Ordered", "Casting", "2024-07-01", "Refine sketches");
        insertMasterRow("2024-07-05", "HP-502", "SO-502", "Bob Sample", "Kelly Rep", "",
                "VVS", "Consult", "Lead", "Planning", "Not Started", null, null, "Follow up call");

        insertLedger("SO", "HP-501", "SO-501", "INVOICE", "ISSUED", "2024-07-01T18:00:00Z", new BigDecimal("8200.00"), null);
        insertLedger("SO", "HP-501", "SO-501", "RECEIPT", "ISSUED", "2024-07-02T18:05:00Z", new BigDecimal("2400.00"), new BigDecimal("2500.00"));
        insertLedger("SO", "HP-502", "SO-502", "INVOICE", "ISSUED", "2024-07-05T17:00:00Z", new BigDecimal("3500.00"), null);

    }

    @Test
    void byStatusReturnsShapedRowsWithProductionColumns() {
        ResponseEntity<ReportsResponse> response = restTemplate.getForEntity(
                "/reports/by-status?filters=status:Deposit,includeProductionCols:true",
                ReportsResponse.class
        );
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        ReportsResponse body = response.getBody();
        assertThat(body).isNotNull();

        List<Map<String, Object>> rows = body.rows();
        assertThat(rows)
                .withFailMessage("rows => %s", rows)
                .hasSize(1);
        Map<String, Object> row = rows.get(0);
        assertThat(row.get("RootApptID")).isEqualTo("HP-501");
        assertThat(new BigDecimal(row.get("Order Total").toString())).isEqualByComparingTo("8200.00");
        assertThat(new BigDecimal(row.get("Total Pay To Date").toString())).isEqualByComparingTo("2400.00");
        assertThat(row.get("Assisted Rep")).isEqualTo("Assisted (Sky Helper)");
        assertThat(row).containsKeys("In Production Status", "Production Deadline");
        assertThat(row.get("Production Deadline")).isEqualTo("2024-07-01");
    }

    @Test
    void byRepHonorsAliasFiltersAndSortsByAssignedRep() {
        ResponseEntity<ReportsResponse> response = restTemplate.getForEntity(
                "/reports/by-rep?filters=brand:hpusa",
                ReportsResponse.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        ReportsResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.rows()).hasSize(1);

        Map<String, Object> row = body.rows().get(0);
        assertThat(row.get("Assigned Rep")).isEqualTo("Jamie Rep");
        assertThat(new BigDecimal(row.get("Order Total").toString())).isEqualByComparingTo("8200.00");
        assertThat(row).doesNotContainKey("In Production Status");

        List<String> keyOrder = body.rows().get(0).keySet().stream().toList();
        assertThat(keyOrder.subList(0, 3)).containsExactly("Visit Date", "Order Total", "Total Pay To Date");
    }

    private void insertMasterRow(String visitDate,
                                 String rootApptId,
                                 String soNumber,
                                 String customer,
                                 String assignedRep,
                                 String assistedRep,
                                 String brand,
                                 String salesStage,
                                 String conversionStatus,
                                 String customOrderStatus,
                                 String centerStoneStatus,
                                 String inProductionStatus,
                                 String productionDeadline,
                                 String nextSteps) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("visit_date", visitDate != null ? LocalDate.parse(visitDate) : null)
                .addValue("root_appt_id", rootApptId)
                .addValue("customer_name", customer)
                .addValue("assigned_rep", assignedRep)
                .addValue("assisted_rep", assistedRep)
                .addValue("brand", brand)
                .addValue("so_number", soNumber)
                .addValue("sales_stage", salesStage)
                .addValue("conversion_status", conversionStatus)
                .addValue("custom_order_status", customOrderStatus)
                .addValue("center_stone_order_status", centerStoneStatus)
                .addValue("in_production_status", inProductionStatus)
                .addValue("production_deadline", productionDeadline != null ? LocalDate.parse(productionDeadline) : null)
                .addValue("next_steps", nextSteps);

        String sql = "INSERT INTO master (visit_date, root_appt_id, customer_name, assigned_rep, assisted_rep, brand, so_number, " +
                "sales_stage, conversion_status, custom_order_status, center_stone_order_status, in_production_status, production_deadline, next_steps) " +
                "VALUES (:visit_date, :root_appt_id, :customer_name, :assigned_rep, :assisted_rep, :brand, :so_number, :sales_stage, :conversion_status, :custom_order_status, :center_stone_order_status, :in_production_status, :production_deadline, :next_steps)";
        jdbcTemplate.update(sql, params);
    }

    private void insertLedger(String anchorType,
                              String rootApptId,
                              String soNumber,
                              String docRole,
                              String docStatus,
                              String paymentDateTime,
                              BigDecimal amount,
                              BigDecimal subtotal) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("doc_number", anchorType + "-" + rootApptId + "-" + docRole + "-" + paymentDateTime)
                .addValue("doc_role", docRole)
                .addValue("anchor_type", anchorType)
                .addValue("root_appt_id", rootApptId)
                .addValue("so_number", soNumber)
                .addValue("doc_type", docRole.equals("INVOICE") ? "Sales Invoice" : "Sales Receipt")
                .addValue("doc_status", docStatus)
                .addValue("payment_datetime", OffsetDateTime.parse(paymentDateTime))
                .addValue("amount_gross", amount)
                .addValue("subtotal", subtotal)
                .addValue("amount_net", amount)
                .addValue("lines_json", null)
                .addValue("request_hash", paymentDateTime + docRole);

        String sql = "INSERT INTO payments_ledger (doc_number, doc_role, anchor_type, root_appt_id, so_number, doc_type, doc_status, payment_datetime, amount_gross, subtotal, amount_net, lines_json, request_hash) " +
                "VALUES (:doc_number, :doc_role, :anchor_type, :root_appt_id, :so_number, :doc_type, :doc_status, :payment_datetime, :amount_gross, :subtotal, :amount_net, :lines_json, :request_hash)";
        jdbcTemplate.update(sql, params);
    }
}
