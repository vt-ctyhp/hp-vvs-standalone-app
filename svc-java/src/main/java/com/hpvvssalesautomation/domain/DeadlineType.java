package com.hpvvssalesautomation.domain;

public enum DeadlineType {
    THREE_D("3D"),
    PRODUCTION("PROD");

    private final String value;

    DeadlineType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static DeadlineType fromString(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("deadlineType is required");
        }
        String normalized = raw.trim().toUpperCase();
        return switch (normalized) {
            case "3D" -> THREE_D;
            case "PROD", "PRODUCTION" -> PRODUCTION;
            default -> throw new IllegalArgumentException("Unsupported deadlineType: " + raw);
        };
    }
}
