package com.hpvvssalesautomation.domain;

import java.util.List;
import java.util.Map;

public record ReportsResponse(List<Map<String, Object>> rows) {
}
