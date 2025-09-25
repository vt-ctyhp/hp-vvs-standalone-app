package com.hpvvssalesautomation.config;

import com.hpvvssalesautomation.alias.AliasRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;
import java.util.TimeZone;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class AppConfig {

    private final AppProperties appProperties;

    public AppConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @PostConstruct
    public void configureTimezone() {
        TimeZone.setDefault(TimeZone.getTimeZone(appProperties.getTimezone()));
    }

    @Bean
    public ZoneId appZoneId() {
        return ZoneId.of(appProperties.getTimezone());
    }

    @Bean
    public AliasRegistry aliasRegistry() {
        return new AliasRegistry();
    }
}
