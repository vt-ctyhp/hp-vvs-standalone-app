package com.hpvvssalesautomation.payments;

import com.hpvvssalesautomation.AbstractIntegrationTest;
import com.hpvvssalesautomation.domain.payments.PaymentRecordRequest;
import com.hpvvssalesautomation.domain.payments.PaymentRecordResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = "FEATURE_PAYMENTS=true")
class PaymentsRecordTests extends AbstractIntegrationTest {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTable() {
        jdbcTemplate.getJdbcTemplate().update("DELETE FROM payments_ledger");
    }

    @Test
    void recordPaymentPersistsLedgerRow() {
        PaymentRecordRequest request = buildRequest("SO-1001", "HP-ROOT-1");

        ResponseEntity<PaymentRecordResult> response = restTemplate.postForEntity(
                "/payments/record",
                request,
                PaymentRecordResult.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        PaymentRecordResult body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.docRole()).isEqualTo("RECEIPT");
        assertThat(body.amountNet()).isEqualByComparingTo("194.50");
        assertThat(body.subtotal()).isEqualByComparingTo("200.00");
        assertThat(body.paymentDateTime()).isEqualTo("2024-07-04T10:15:00-07:00");
        assertThat(body.requestHash()).isNotBlank();

        MapSqlParameterSource params = new MapSqlParameterSource("doc_number", body.docNumber());
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT doc_role, amount_net, subtotal FROM payments_ledger WHERE doc_number = :doc_number",
                params
        );
        assertThat(row.get("doc_role")).isEqualTo("RECEIPT");
        assertThat((BigDecimal) row.get("amount_net")).isEqualByComparingTo("194.50");
        assertThat((BigDecimal) row.get("subtotal")).isEqualByComparingTo("200.00");
        OffsetDateTime storedDate = jdbcTemplate.query(
                "SELECT payment_datetime FROM payments_ledger WHERE doc_number = :doc_number",
                params,
                rs -> rs.next() ? rs.getObject("payment_datetime", OffsetDateTime.class) : null
        );
        assertThat(storedDate).isNotNull();
        OffsetDateTime expected = OffsetDateTime.parse("2024-07-04T10:15:00-07:00");
        assertThat(storedDate.toInstant()).isEqualTo(expected.toInstant());
    }

    @Test
    void invoiceDocTypeInfersInvoiceDocRole() {
        PaymentRecordRequest request = buildRequest("SO-3003", "HP-ROOT-3");
        request.setDocType("Sales Invoice");
        request.setMethod("Wire");

        ResponseEntity<PaymentRecordResult> response = restTemplate.postForEntity(
                "/payments/record",
                request,
                PaymentRecordResult.class
        );

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().docRole()).isEqualTo("INVOICE");
    }

    private PaymentRecordRequest buildRequest(String soNumber, String rootApptId) {
        PaymentRecordRequest request = new PaymentRecordRequest();
        request.setAnchorType("SO");
        request.setSoNumber(soNumber);
        request.setRootApptId(rootApptId);
        request.setDocType("Deposit Receipt");
        request.setPaymentDateTime("2024-07-04T17:15:00Z");
        request.setAmountGross(new BigDecimal("200.00"));
        request.setFeePercent(new BigDecimal("2.75"));
        request.setMethod("Card");
        request.setReference("AUTH-001");
        request.setNotes("Initial deposit");

        PaymentRecordRequest.PaymentRecordRequestLine line = new PaymentRecordRequest.PaymentRecordRequestLine();
        line.setDesc("Deposit");
        line.setQty(BigDecimal.ONE);
        line.setAmt(new BigDecimal("200.00"));
        request.setLines(List.of(line));
        return request;
    }
}
