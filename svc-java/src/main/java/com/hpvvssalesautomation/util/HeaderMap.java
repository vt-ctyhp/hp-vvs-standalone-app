package com.hpvvssalesautomation.util;

import com.hpvvssalesautomation.alias.AliasRegistry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class HeaderMap {

    private final Map<String, String> resolved;
    private final List<String> canonicalOrder;

    public HeaderMap(List<String> headers, Map<String, List<String>> aliasSet) {
        Map<String, String> normalized = headers.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(h -> AliasRegistry.normalize(h), h -> h, (first, second) -> first));

        this.canonicalOrder = List.copyOf(aliasSet.keySet());
        Map<String, String> map = new LinkedHashMap<>();
        aliasSet.forEach((canonical, aliases) -> {
            String match = aliases.stream()
                    .map(AliasRegistry::normalize)
                    .map(normalized::get)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
            if (match == null) {
                map.put(canonical, canonical);
            } else {
                map.put(canonical, match);
            }
        });
        this.resolved = Collections.unmodifiableMap(map);
    }

    public boolean has(String canonical) {
        return resolved.containsKey(canonical) && !Objects.equals(resolved.get(canonical), canonical);
    }

    public String getActual(String canonical) {
        return resolved.get(canonical);
    }

    public String require(String canonical) {
        String actual = resolved.get(canonical);
        if (actual == null) {
            throw new IllegalArgumentException("Missing canonical header: " + canonical);
        }
        return actual;
    }

    public List<String> missingCanonicals() {
        return resolved.entrySet().stream()
                .filter(entry -> Objects.equals(entry.getKey(), entry.getValue()))
                .map(Map.Entry::getKey)
                .toList();
    }

    public Map<String, String> asMap() {
        return resolved;
    }

    public List<String> healedHeaders(List<String> targetOrder) {
        if (targetOrder == null || targetOrder.isEmpty()) {
            targetOrder = canonicalOrder;
        }
        List<String> healed = targetOrder.stream()
                .map(resolved::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        missingCanonicals().forEach(healed::add);
        return healed;
    }

    public Optional<String> findActualInsensitive(String header) {
        String normalized = AliasRegistry.normalize(header);
        return resolved.values().stream()
                .filter(Objects::nonNull)
                .filter(actual -> AliasRegistry.normalize(actual).equals(normalized))
                .findFirst();
    }
}
