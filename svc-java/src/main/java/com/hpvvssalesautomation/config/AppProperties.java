package com.hpvvssalesautomation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String timezone = "America/Los_Angeles";
    private Map<String, String> spreadsheetIds = Collections.emptyMap();
    private Map<String, String> templateIds = Collections.emptyMap();
    private List<String> configAliases = Collections.emptyList();
    private Map<String, Boolean> featureFlags = Collections.emptyMap();

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Map<String, String> getSpreadsheetIds() {
        return spreadsheetIds;
    }

    public void setSpreadsheetIds(Map<String, String> spreadsheetIds) {
        this.spreadsheetIds = spreadsheetIds;
    }

    public Map<String, String> getTemplateIds() {
        return templateIds;
    }

    public void setTemplateIds(Map<String, String> templateIds) {
        this.templateIds = templateIds;
    }

    public List<String> getConfigAliases() {
        return configAliases;
    }

    public void setConfigAliases(List<String> configAliases) {
        this.configAliases = configAliases;
    }

    public Map<String, Boolean> getFeatureFlags() {
        return featureFlags;
    }

    public void setFeatureFlags(Map<String, Boolean> featureFlags) {
        this.featureFlags = featureFlags == null ? Collections.emptyMap() : featureFlags;
    }
}
