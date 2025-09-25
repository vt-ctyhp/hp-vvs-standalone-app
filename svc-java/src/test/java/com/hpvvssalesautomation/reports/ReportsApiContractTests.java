package com.hpvvssalesautomation.reports;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ReportsApiContractTests {

    private static final Path SPEC_PATH = Path.of("..", "docs", "api.yaml").toAbsolutePath().normalize();

    @Test
    void openApiIncludesReportsAndDashboardEndpoints() {
        OpenAPI openAPI = new OpenAPIV3Parser().read(SPEC_PATH.toString());
        assertThat(openAPI).isNotNull();
        assertThat(openAPI.getPaths()).containsKeys(
                "/reports/by-status",
                "/reports/by-rep",
                "/dashboard/kpis"
        );
        assertThat(openAPI.getComponents().getSchemas()).containsKeys(
                "ReportsByStatusResponse",
                "ReportsByStatusRow",
                "ReportsByRepResponse",
                "DashboardKpiResponse"
        );
    }
}
