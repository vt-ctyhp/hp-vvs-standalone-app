package com.hpvvssalesautomation.diamonds;

import com.hpvvssalesautomation.alias.AliasRegistry;
import com.hpvvssalesautomation.util.HeaderMap;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AliasRoundTripTests {

    @Test
    void diamondOrderAliasesResolveCanonicalColumns() {
        List<String> headers = Arrays.asList(
                "Root",
                "StoneRef",
                "OrderStatus",
                "StoneStatus",
                "StoneType",
                "OrderedBy",
                "OrderedOn",
                "MemoDate",
                "ReturnDue",
                "DecidedBy",
                "DecidedOn"
        );
        AliasRegistry registry = new AliasRegistry();
        HeaderMap map = new HeaderMap(headers, registry.diamondsOrderAliases());

        assertThat(map.getActual("RootApptID")).isEqualTo("Root");
        assertThat(map.getActual("Stone Reference")).isEqualTo("StoneRef");
        assertThat(map.getActual("Order Status")).isEqualTo("OrderStatus");
        assertThat(map.getActual("Stone Status")).isEqualTo("StoneStatus");
        assertThat(map.getActual("Stone Type")).isEqualTo("StoneType");
        assertThat(map.getActual("Ordered By")).isEqualTo("OrderedBy");
        assertThat(map.getActual("Ordered Date")).isEqualTo("OrderedOn");
        assertThat(map.getActual("Memo/Invoice Date")).isEqualTo("MemoDate");
        assertThat(map.getActual("Return Due Date")).isEqualTo("ReturnDue");
        assertThat(map.getActual("Decided By")).isEqualTo("DecidedBy");
        assertThat(map.getActual("Decided Date")).isEqualTo("DecidedOn");
    }
}
