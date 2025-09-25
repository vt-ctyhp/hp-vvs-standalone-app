package com.hpvvssalesautomation.reports;

import com.hpvvssalesautomation.AbstractIntegrationTest;
import com.hpvvssalesautomation.domain.DashboardKpiResponse;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = "FEATURE_REPORTS=true")
class DashboardTests extends AbstractIntegrationTest {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @BeforeEach
    void setupData() {
        jdbcTemplate.getJdbcTemplate().update("DELETE FROM payments_ledger");
        jdbcTemplate.getJdbcTemplate().update("DELETE FROM master");

        insertMasterRow("2024-07-02", "HP-601", "SO-601", "Deposit", "Won", "Production", "2024-06-28", "2024-06-25");
        insertMasterRow("2024-07-03", "HP-602", "SO-602", "Lead", "Open", "New", null, "2024-06-20");

        insertStageWeight("DEPOSIT", new BigDecimal("0.90"));
        insertStageWeight("LEAD", new BigDecimal("0.10"));

        insertLedger("SO", "HP-601", "SO-601", "INVOICE", "2024-07-01T18:00:00Z", new BigDecimal("6000.00"));
        insertLedgerReceipt("SO", "HP-601", "SO-601", "2024-07-02T18:05:00Z", new BigDecimal("2000.00"));
        insertLedgerReceipt("SO", "HP-601", "SO-601", "2024-07-10T18:05:00Z", new BigDecimal("1000.00"));

        insertLedger("SO", "HP-602", "SO-602", "INVOICE", "2024-07-05T16:00:00Z", new BigDecimal("3000.00"));
    }

    @Test
    void dashboardKpisReturnWeightedPipelineAndDeposits() {
        ResponseEntity<DashboardKpiResponse> response = restTemplate.getForEntity(
                "/dashboard/kpis?dateFrom=2024-07-01&dateTo=2024-07-31",
                DashboardKpiResponse.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        DashboardKpiResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.weightedPipeline()).isEqualByComparingTo(new BigDecimal("5700.00"));
        assertThat(body.totalDeposits()).isEqualByComparingTo(new BigDecimal("3000.00"));
        assertThat(body.firstTimeDepositCount()).isEqualTo(1);
        assertThat(body.overdueProductionCount()).isEqualTo(1);
        assertThat(body.overdueThreeDCount()).isEqualTo(1);
    }

    private void insertMasterRow(String visitDate,
                                 String rootApptId,
                                 String soNumber,
                                 String salesStage,
                                 String conversionStatus,
                                 String customOrderStatus,
                                 String productionDeadline,
                                 String threeDDeadline) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("visit_date", LocalDate.parse(visitDate))
                .addValue("root_appt_id", rootApptId)
                .addValue("customer_name", rootApptId + " Customer")
                .addValue("assigned_rep", rootApptId.endsWith("1") ? "Jamie Rep" : "Kelly Rep")
                .addValue("assisted_rep", null)
                .addValue("brand", "HPUSA")
                .addValue("so_number", soNumber)
                .addValue("sales_stage", salesStage)
                .addValue("conversion_status", conversionStatus)
                .addValue("custom_order_status", customOrderStatus)
                .addValue("center_stone_order_status", "Ordered")
                .addValue("in_production_status", "Casting")
                .addValue("production_deadline", productionDeadline != null ? LocalDate.parse(productionDeadline) : null)
                .addValue("three_d_deadline", threeDDeadline != null ? LocalDate.parse(threeDDeadline) : null)
                .addValue("next_steps", "Follow up");

        String sql = "INSERT INTO master (visit_date, root_appt_id, customer_name, assigned_rep, assisted_rep, brand, so_number, " +
                "sales_stage, conversion_status, custom_order_status, center_stone_order_status, in_production_status, production_deadline, three_d_deadline, next_steps) " +
                "VALUES (:visit_date, :root_appt_id, :customer_name, :assigned_rep, :assisted_rep, :brand, :so_number, :sales_stage, :conversion_status, :custom_order_status, :center_stone_order_status, :in_production_status, :production_deadline, :three_d_deadline, :next_steps)";
        jdbcTemplate.update(sql, params);
    }

    private void insertLedger(String anchorType,
                              String rootApptId,
                              String soNumber,
                              String docRole,
                              String paymentDateTime,
                              BigDecimal amount) {
        insertLedgerInternal(anchorType, rootApptId, soNumber, docRole, paymentDateTime, amount, amount);
    }

    private void insertLedgerReceipt(String anchorType,
                                     String rootApptId,
                                     String soNumber,
                                     String paymentDateTime,
                                     BigDecimal amount) {
        insertLedgerInternal(anchorType, rootApptId, soNumber, "RECEIPT", paymentDateTime, amount, null);
    }

    private void insertLedgerInternal(String anchorType,
                                      String rootApptId,
                                      String soNumber,
                                      String docRole,
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
                .addValue("doc_status", "ISSUED")
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

    private void insertStageWeight(String stage, BigDecimal weight) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("stage", stage)
                .addValue("weight", weight);
        jdbcTemplate.update("INSERT INTO dashboard_stage_weights(stage, weight) VALUES (:stage, :weight) ON CONFLICT (stage) DO UPDATE SET weight = EXCLUDED.weight", params);
    }
}
