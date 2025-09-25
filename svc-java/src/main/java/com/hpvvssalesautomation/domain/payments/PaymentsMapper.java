package com.hpvvssalesautomation.domain.payments;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hpvvssalesautomation.alias.AliasRegistry;
import com.hpvvssalesautomation.util.HeaderMap;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class PaymentsMapper {

    private final AliasRegistry aliasRegistry;
    private final ObjectMapper objectMapper;

    public PaymentsMapper(AliasRegistry aliasRegistry, ObjectMapper objectMapper) {
        this.aliasRegistry = aliasRegistry;
        this.objectMapper = objectMapper;
    }

    public HeaderMap ledgerHeaderMap(List<String> headers) {
        List<String> effective = headers == null || headers.isEmpty()
                ? aliasRegistry.canonicalPaymentsLedgerColumns()
                : headers;
        return new HeaderMap(effective, aliasRegistry.paymentsLedgerAliases());
    }

    public String linesToJson(List<PaymentLine> lines) {
        try {
            if (lines == null || lines.isEmpty()) {
                return "[]";
            }
            return objectMapper.writeValueAsString(lines);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize payment lines", e);
        }
    }

    public List<PaymentLine> linesFromJson(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            PaymentLine[] items = objectMapper.readValue(json, PaymentLine[].class);
            return List.of(items);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize payment lines", e);
        }
    }
}
