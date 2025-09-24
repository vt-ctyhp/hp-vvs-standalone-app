package com.hp.vvs.app.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record AppointmentSummaryRow(Map<String, String> values) {

    public Map<String, String> ordered(List<String> columns) {
        Map<String, String> ordered = new LinkedHashMap<>();
        for (String column : columns) {
            ordered.put(column, values.getOrDefault(column, ""));
        }
        return ordered;
    }
}
