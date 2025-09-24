package com.hp.vvs.app.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class AppConfig {

    public static final ZoneId PT_ZONE_ID = ZoneId.of("America/Los_Angeles");

    private static final Map<String, String> ENV_ALIASES = Map.of(
            "REPORT_REANLYZE_TOKEN", "REPORT_REANALYZE_TOKEN",
            "PAYMENTS_400_FILEID", "PAYMENTS_400_FILE_ID",
            "SPREADSHEETID", "SPREADSHEET_ID",
            "SERVICE_BASEURL", "SERVICE_BASE_URL");

    @Bean
    public DataSource dataSource(
            @Value("${app.datasource.url}") String url,
            @Value("${app.datasource.username}") String username,
            @Value("${app.datasource.password}") String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(5);
        config.setPoolName("hp-vvs-pool");
        return new HikariDataSource(config);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public AppSettings appSettings(Environment environment) {
        return new AppSettings(environment);
    }

    @Bean
    public ZoneId zoneId() {
        return PT_ZONE_ID;
    }

    public static class AppSettings {
        private final Environment environment;
        private final Map<String, String> normalizedCache = new HashMap<>();

        public AppSettings(Environment environment) {
            this.environment = environment;
        }

        public Optional<String> find(String key) {
            String normalized = normalize(key);
            return Optional.ofNullable(normalizedCache.computeIfAbsent(normalized, this::lookup));
        }

        public String require(String key) {
            return find(key).orElseThrow(() ->
                    new IllegalStateException("Missing configuration for key: " + key));
        }

        private String lookup(String normalizedKey) {
            String direct = environment.getProperty(normalizedKey);
            if (direct != null) {
                return direct;
            }
            String alias = ENV_ALIASES.get(normalizedKey);
            if (alias != null) {
                return environment.getProperty(alias);
            }
            return null;
        }

        private String normalize(String key) {
            return key.replaceAll("[^A-Za-z0-9]+", "_")
                    .toUpperCase(Locale.ROOT);
        }
    }
}
