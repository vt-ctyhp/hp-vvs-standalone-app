package com.hpvvssalesautomation.payments;

import com.hpvvssalesautomation.AbstractIntegrationTest;
import com.hpvvssalesautomation.domain.payments.PaymentRecordRequest;
import com.hpvvssalesautomation.domain.payments.PaymentRecordResult;
import com.hpvvssalesautomation.domain.payments.PaymentRecordStatus;
import com.hpvvssalesautomation.domain.payments.PaymentsSummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = "FEATURE_PAYMENTS=true")
class IdempotencyTests extends AbstractIntegrationTest {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanLedger() {
        jdbcTemplate.getJdbcTemplate().update("DELETE FROM payments_ledger");
    }

    @Test
    void repostingSamePayloadDoesNotCreateDuplicates() {
        PaymentRecordRequest payload = buildRequest();

        ResponseEntity<PaymentRecordResult> first = restTemplate.postForEntity(
                "/payments/record",
                payload,
                PaymentRecordResult.class
        );
        ResponseEntity<PaymentRecordResult> second = restTemplate.postForEntity(
                "/payments/record",
                payload,
                PaymentRecordResult.class
        );

        assertThat(first.getBody()).isNotNull();
        assertThat(second.getBody()).isNotNull();
        assertThat(second.getBody().status()).isEqualTo(PaymentRecordStatus.UPDATED);
        assertThat(second.getBody().docNumber()).isEqualTo(first.getBody().docNumber());
        assertThat(second.getBody().requestHash()).isEqualTo(first.getBody().requestHash());

        MapSqlParameterSource params = new MapSqlParameterSource("doc_number", first.getBody().docNumber());
        Integer rows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payments_ledger WHERE doc_number = :doc_number",
                params,
                Integer.class
        );
        assertThat(rows).isEqualTo(1);

        PaymentsSummaryResponse summary = restTemplate.getForObject(
                "/payments/summary?soNumber=SO-IDEMP-1",
                PaymentsSummaryResponse.class
        );
        assertThat(summary).isNotNull();
        assertThat(summary.totalPayments()).isEqualByComparingTo("245.00");
        assertThat(summary.entries()).hasSize(1);
    }

    private PaymentRecordRequest buildRequest() {
        PaymentRecordRequest request = new PaymentRecordRequest();
        request.setAnchorType("SO");
        request.setSoNumber("SO-IDEMP-1");
        request.setDocType("Sales Receipt");
        request.setPaymentDateTime("2024-07-05T18:45:00Z");
        request.setAmountGross(new BigDecimal("250.00"));
        request.setFeeAmount(new BigDecimal("5.00"));
        request.setMethod("Card");
        request.setReference("IDEMP-001");

        PaymentRecordRequest.PaymentRecordRequestLine line = new PaymentRecordRequest.PaymentRecordRequestLine();
        line.setDesc("Receipt");
        line.setQty(BigDecimal.ONE);
        line.setAmt(new BigDecimal("250.00"));
        request.setLines(java.util.List.of(line));
        return request;
    }
}
