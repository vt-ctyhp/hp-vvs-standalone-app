package com.hpvvssalesautomation;

import com.hpvvssalesautomation.alias.AliasRegistry;
import com.hpvvssalesautomation.util.HeaderMap;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AliasRegistryTests {

    private final AliasRegistry aliasRegistry = new AliasRegistry();

    @Test
    void masterAliasesRoundTrip() {
        assertAliasRoundTrip(aliasRegistry.masterAppointmentAliases());
    }

    @Test
    void ledgerAliasesRoundTrip() {
        assertAliasRoundTrip(aliasRegistry.ledgerAliases());
    }

    @Test
    void perClientAliasesRoundTrip() {
        assertAliasRoundTrip(aliasRegistry.perClientAliases());
    }

    @Test
    void dashboardAliasesRoundTrip() {
        assertAliasRoundTrip(aliasRegistry.dashboardAliases());
    }

    private void assertAliasRoundTrip(Map<String, List<String>> aliasMap) {
        List<String> canonicals = new ArrayList<>(aliasMap.keySet());
        for (Map.Entry<String, List<String>> entry : aliasMap.entrySet()) {
            String canonical = entry.getKey();
            List<String> aliases = entry.getValue();
            for (String alias : aliases) {
                List<String> headers = new ArrayList<>(canonicals);
                int index = canonicals.indexOf(canonical);
                headers.set(index, alias);
                HeaderMap map = new HeaderMap(headers, aliasMap);
                assertThat(map.getActual(canonical))
                        .as("Alias '%s' should resolve to canonical '%s'", alias, canonical)
                        .isEqualTo(alias);
            }
        }
    }
}
