package com.hpvvssalesautomation.config;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.Map;

@Component
public class FeatureFlags {

    private final Map<String, Boolean> flags;

    public FeatureFlags(AppProperties appProperties) {
        Map<String, Boolean> configured = appProperties.getFeatureFlags();
        this.flags = configured == null ? Collections.emptyMap() : Map.copyOf(configured);
    }

    public boolean isDiamondsEnabled() {
        return flags.getOrDefault("diamonds", Boolean.FALSE);
    }

    public boolean isPaymentsEnabled() {
        return flags.getOrDefault("payments", Boolean.FALSE);
    }

    public boolean isReportsEnabled() {
        return flags.getOrDefault("reports", Boolean.FALSE);
    }

    public Map<String, Boolean> snapshot() {
        return flags;
    }

    public void requireDiamondsEnabled() {
        if (!isDiamondsEnabled()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Diamonds feature disabled");
        }
    }

    public void requirePaymentsEnabled() {
        if (!isPaymentsEnabled()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payments feature disabled");
        }
    }

    public void requireReportsEnabled() {
        if (!isReportsEnabled()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reports feature disabled");
        }
    }
}
