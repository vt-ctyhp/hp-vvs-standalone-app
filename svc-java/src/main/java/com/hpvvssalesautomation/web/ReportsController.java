package com.hpvvssalesautomation.web;

import com.hpvvssalesautomation.config.FeatureFlags;
import com.hpvvssalesautomation.domain.ReportsResponse;
import com.hpvvssalesautomation.domain.ReportsService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reports")
@ConditionalOnProperty(prefix = "app.feature-flags", name = "reports", havingValue = "true")
public class ReportsController {

    private final ReportsService reportsService;
    private final FeatureFlags featureFlags;

    public ReportsController(ReportsService reportsService, FeatureFlags featureFlags) {
        this.reportsService = reportsService;
        this.featureFlags = featureFlags;
    }

    @GetMapping("/by-status")
    public ReportsResponse byStatus(@RequestParam(value = "filters", required = false) String filters) {
        featureFlags.requireReportsEnabled();
        return reportsService.byStatus(filters);
    }

    @GetMapping("/by-rep")
    public ReportsResponse byRep(@RequestParam(value = "filters", required = false) String filters) {
        featureFlags.requireReportsEnabled();
        return reportsService.byRep(filters);
    }
}
