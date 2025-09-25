package com.hpvvssalesautomation.payments;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hpvvssalesautomation.alias.AliasRegistry;
import com.hpvvssalesautomation.domain.payments.PaymentLine;
import com.hpvvssalesautomation.domain.payments.PaymentsMapper;
import com.hpvvssalesautomation.util.HeaderMap;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AliasRoundTripPaymentsTests {

    private final AliasRegistry registry = new AliasRegistry();
    private final PaymentsMapper mapper = new PaymentsMapper(registry, new ObjectMapper());

    @Test
    void ledgerAliasResolutionHandlesCommonSynonyms() {
        List<String> headers = List.of(
                "Doc #",
                "Payment Method",
                "Net Amount",
                "Payment Date",
                "Document Type",
                "Processing %",
                "Processing Fee",
                "Gross",
                "Ref #",
                "Lines"
        );
        HeaderMap headerMap = mapper.ledgerHeaderMap(headers);

        assertThat(headerMap.getActual("DocNumber")).isEqualTo("Doc #");
        assertThat(headerMap.getActual("Method")).isEqualTo("Payment Method");
        assertThat(headerMap.getActual("AmountNet")).isEqualTo("Net Amount");
        assertThat(headerMap.getActual("PaymentDateTime")).isEqualTo("Payment Date");
        assertThat(headerMap.getActual("DocType")).isEqualTo("Document Type");
        assertThat(headerMap.getActual("FeePercent")).isEqualTo("Processing %");
        assertThat(headerMap.getActual("FeeAmount")).isEqualTo("Processing Fee");
        assertThat(headerMap.getActual("AmountGross")).isEqualTo("Gross");
        assertThat(headerMap.getActual("Reference")).isEqualTo("Ref #");
        assertThat(headerMap.getActual("LinesJSON")).isEqualTo("Lines");
    }

    @Test
    void linesPayloadRoundTripsThroughJsonSerialization() {
        PaymentLine line = new PaymentLine("Deposit", BigDecimal.ONE, new BigDecimal("120.00"), new BigDecimal("120.00"));
        String json = mapper.linesToJson(List.of(line));
        assertThat(json).contains("\"desc\":\"Deposit\"");

        List<PaymentLine> restored = mapper.linesFromJson(json);
        assertThat(restored).hasSize(1);
        PaymentLine restoredLine = restored.get(0);
        assertThat(restoredLine.description()).isEqualTo("Deposit");
        assertThat(restoredLine.amount()).isEqualByComparingTo("120.00");
        assertThat(restoredLine.lineTotal()).isEqualByComparingTo("120.00");
    }
}
