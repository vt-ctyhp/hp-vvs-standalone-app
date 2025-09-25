package com.hpvvssalesautomation.web;

import com.hpvvssalesautomation.config.FeatureFlags;
import com.hpvvssalesautomation.domain.DashboardKpiResponse;
import com.hpvvssalesautomation.domain.DashboardService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@ConditionalOnProperty(prefix = "app.feature-flags", name = "reports", havingValue = "true")
public class DashboardController {

    private final DashboardService dashboardService;
    private final FeatureFlags featureFlags;

    public DashboardController(DashboardService dashboardService, FeatureFlags featureFlags) {
        this.dashboardService = dashboardService;
        this.featureFlags = featureFlags;
    }

    @GetMapping("/kpis")
    public DashboardKpiResponse getKpis(@RequestParam(value = "dateFrom", required = false) String dateFrom,
                                         @RequestParam(value = "dateTo", required = false) String dateTo) {
        featureFlags.requireReportsEnabled();
        return dashboardService.fetchKpis(dateFrom, dateTo);
    }
}
