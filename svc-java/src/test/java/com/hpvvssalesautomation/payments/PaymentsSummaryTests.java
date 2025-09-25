package com.hpvvssalesautomation.payments;

import com.hpvvssalesautomation.AbstractIntegrationTest;
import com.hpvvssalesautomation.domain.payments.PaymentRecordRequest;
import com.hpvvssalesautomation.domain.payments.PaymentRecordResult;
import com.hpvvssalesautomation.domain.payments.PaymentsSummaryResponse;
import com.hpvvssalesautomation.domain.payments.PaymentSummaryEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = "FEATURE_PAYMENTS=true")
class PaymentsSummaryTests extends AbstractIntegrationTest {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTable() {
        jdbcTemplate.getJdbcTemplate().update("DELETE FROM payments_ledger");
    }

    @Test
    void summaryCalculatesTotalsAndFiltersVoidReceipts() {
        String root = "HP-SUM-ROOT";
        String so = "SO-SUM-1";
        PaymentRecordRequest receipt = buildReceipt(root, so, "2024-07-01T08:00:00-07:00", "Card", new BigDecimal("150.00"));
        PaymentRecordResult receiptResult = restTemplate.postForEntity("/payments/record", receipt, PaymentRecordResult.class).getBody();
        assertThat(receiptResult).isNotNull();

        PaymentRecordRequest secondReceipt = buildReceipt(root, so, "2024-07-02T09:30:00-07:00", "Wire", new BigDecimal("200.00"));
        PaymentRecordResult secondResult = restTemplate.postForEntity("/payments/record", secondReceipt, PaymentRecordResult.class).getBody();
        assertThat(secondResult).isNotNull();

        jdbcTemplate.update(
                "UPDATE payments_ledger SET doc_status = 'VOID' WHERE doc_number = :doc",
                new MapSqlParameterSource("doc", secondResult.docNumber())
        );

        PaymentRecordRequest invoice = buildInvoice(root, so, "2024-06-30T15:00:00-07:00", new BigDecimal("500.00"));
        restTemplate.postForEntity("/payments/record", invoice, PaymentRecordResult.class);

        ResponseEntity<PaymentsSummaryResponse> response = restTemplate.getForEntity(
                URI.create("/payments/summary?soNumber=" + so),
                PaymentsSummaryResponse.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        PaymentsSummaryResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.totalPayments()).isEqualByComparingTo("147.50");
        assertThat(body.invoicesLinesSubtotal()).isEqualByComparingTo("500.00");
        assertThat(body.netLinesMinusPayments()).isEqualByComparingTo("352.50");

        Map<String, BigDecimal> byMethod = body.byMethod();
        assertThat(byMethod).containsEntry("Card", new BigDecimal("147.50"));
        assertThat(byMethod).doesNotContainKey("Wire");

        List<PaymentSummaryEntry> entries = body.entries();
        assertThat(entries).hasSize(3);
        assertThat(entries)
                .extracting(PaymentSummaryEntry::docStatus)
                .contains("VOID");
        assertThat(entries)
                .isSortedAccordingTo((a, b) -> {
                    String first = a.paymentDateTime();
                    String second = b.paymentDateTime();
                    return first.compareTo(second);
                });

        ResponseEntity<PaymentsSummaryResponse> byRoot = restTemplate.getForEntity(
                URI.create("/payments/summary?rootApptId=" + root),
                PaymentsSummaryResponse.class
        );
        assertThat(byRoot.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(byRoot.getBody()).usingRecursiveComparison().isEqualTo(body);
    }

    @Test
    void summaryClampsNetToZeroWhenPaymentsExceedInvoices() {
        String root = "HP-SUM-CLAMP";
        String so = "SO-SUM-CLAMP";

        PaymentRecordRequest invoice = buildInvoice(root, so, "2024-07-01T10:00:00-07:00", new BigDecimal("300.00"));
        restTemplate.postForEntity("/payments/record", invoice, PaymentRecordResult.class);

        PaymentRecordRequest receiptOne = buildReceipt(root, so, "2024-07-02T09:00:00-07:00", "Wire", new BigDecimal("200.00"));
        restTemplate.postForEntity("/payments/record", receiptOne, PaymentRecordResult.class);

        PaymentRecordRequest receiptTwo = buildReceipt(root, so, "2024-07-03T09:00:00-07:00", "Card", new BigDecimal("200.00"));
        restTemplate.postForEntity("/payments/record", receiptTwo, PaymentRecordResult.class);

        PaymentsSummaryResponse summary = restTemplate.getForObject(
                "/payments/summary?soNumber=" + so,
                PaymentsSummaryResponse.class
        );

        assertThat(summary).isNotNull();
        assertThat(summary.invoicesLinesSubtotal()).isEqualByComparingTo("300.00");
        assertThat(summary.totalPayments()).isEqualByComparingTo("395.00");
        assertThat(summary.netLinesMinusPayments()).isEqualByComparingTo("0.00");
    }

    private PaymentRecordRequest buildReceipt(String root, String so, String paymentDateTime, String method, BigDecimal amount) {
        PaymentRecordRequest request = new PaymentRecordRequest();
        request.setAnchorType("SO");
        request.setRootApptId(root);
        request.setSoNumber(so);
        request.setDocType("Sales Receipt");
        request.setPaymentDateTime(paymentDateTime);
        request.setAmountGross(amount);
        request.setFeeAmount(new BigDecimal("2.50"));
        request.setMethod(method);
        request.setReference("RCPT-" + method);

        PaymentRecordRequest.PaymentRecordRequestLine line = new PaymentRecordRequest.PaymentRecordRequestLine();
        line.setDesc("Line");
        line.setQty(BigDecimal.ONE);
        line.setAmt(amount);
        request.setLines(List.of(line));
        return request;
    }

    private PaymentRecordRequest buildInvoice(String root, String so, String paymentDateTime, BigDecimal subtotal) {
        PaymentRecordRequest request = new PaymentRecordRequest();
        request.setAnchorType("SO");
        request.setRootApptId(root);
        request.setSoNumber(so);
        request.setDocType("Sales Invoice");
        request.setPaymentDateTime(paymentDateTime);
        request.setAmountGross(subtotal);
        request.setMethod("Check");

        PaymentRecordRequest.PaymentRecordRequestLine line = new PaymentRecordRequest.PaymentRecordRequestLine();
        line.setDesc("Invoice Line");
        line.setQty(BigDecimal.ONE);
        line.setAmt(subtotal);
        request.setLines(List.of(line));
        return request;
    }
}
