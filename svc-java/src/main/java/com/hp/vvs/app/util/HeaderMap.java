package com.hp.vvs.app.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class HeaderMap {

    private final Map<String, Integer> normalizedIndex = new HashMap<>();
    private final List<String> headers = new ArrayList<>();

    public HeaderMap(List<String> headerRow) {
        for (int i = 0; i < headerRow.size(); i++) {
            String header = headerRow.get(i);
            headers.add(header);
            normalizedIndex.putIfAbsent(normalize(header), i);
        }
    }

    public Optional<String> findHeader(String name) {
        Integer idx = normalizedIndex.get(normalize(name));
        if (idx == null) {
            return Optional.empty();
        }
        return Optional.of(headers.get(idx));
    }

    public int getOrInsert(String headerName) {
        String key = normalize(headerName);
        Integer idx = normalizedIndex.get(key);
        if (idx != null) {
            return idx;
        }
        int next = headers.size();
        headers.add(headerName);
        normalizedIndex.put(key, next);
        return next;
    }

    public List<String> headers() {
        return List.copyOf(headers);
    }

    public static String normalize(String value) {
        return value.replaceAll("[^A-Za-z0-9]+", "").toLowerCase(Locale.ROOT);
    }
}
