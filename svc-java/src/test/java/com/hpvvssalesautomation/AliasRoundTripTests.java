package com.hpvvssalesautomation;

import com.hpvvssalesautomation.alias.AliasRegistry;
import com.hpvvssalesautomation.util.HeaderMap;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AliasRoundTripTests {

    private final AliasRegistry registry = new AliasRegistry();

    @Test
    void clientStatusAliasesResolveToCanonicalHeaders() {
        List<String> headers = Arrays.asList(
                "Status Date",
                "Stage",
                "Conversion",
                "Custom Order",
                "CSOS",
                "Action Items",
                "Deadline",
                "Due Date",
                "Moves",
                "Assistant Rep",
                "Updater",
                "UpdatedAt"
        );
        HeaderMap map = new HeaderMap(headers, registry.perClientAliases());
        assertThat(map.getActual("Log Date")).isEqualTo("Status Date");
        assertThat(map.getActual("Sales Stage")).isEqualTo("Stage");
        assertThat(map.getActual("Conversion Status")).isEqualTo("Conversion");
        assertThat(map.getActual("Custom Order Status")).isEqualTo("Custom Order");
        assertThat(map.getActual("Center Stone Order Status")).isEqualTo("CSOS");
        assertThat(map.getActual("Next Steps")).isEqualTo("Action Items");
        assertThat(map.getActual("Deadline Type")).isEqualTo("Deadline");
        assertThat(map.getActual("Deadline Date")).isEqualTo("Due Date");
        assertThat(map.getActual("Move Count")).isEqualTo("Moves");
        assertThat(map.getActual("Assisted Rep")).isEqualTo("Assistant Rep");
        assertThat(map.getActual("Updated By")).isEqualTo("Updater");
        assertThat(map.getActual("Updated At")).isEqualTo("UpdatedAt");
    }
}
