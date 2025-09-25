package com.hpvvssalesautomation.diamonds;

import com.hpvvssalesautomation.domain.diamonds.DiamondsActionResponse;
import com.hpvvssalesautomation.domain.diamonds.DiamondsStoneDecisionItem;
import com.hpvvssalesautomation.domain.diamonds.DiamondsStoneDecisionsRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StoneDecisionTests extends AbstractDiamondsIntegrationTest {

    @BeforeEach
    void loadData() {
        loadDiamondsFixture();
    }

    @Test
    void stoneDecisionsUpdateStatusAndSummary() {
        DiamondsStoneDecisionItem keepItem = new DiamondsStoneDecisionItem();
        keepItem.setRootApptId("HP-1002");
        keepItem.setDecision("Keep");
        keepItem.setDecidedBy("Alex Harper");
        keepItem.setDecidedDate("2025-02-06");

        DiamondsStoneDecisionItem replaceItem = new DiamondsStoneDecisionItem();
        replaceItem.setRootApptId("HP-1004");
        replaceItem.setDecision("Replace");

        DiamondsStoneDecisionsRequest request = new DiamondsStoneDecisionsRequest();
        request.setItems(List.of(keepItem, replaceItem));
        request.setDefaultDecidedBy("Morgan Lee");
        request.setDefaultDecidedDate("2025-02-08");
        request.setApplyDefaultsToAll(false);

        ResponseEntity<DiamondsActionResponse> response = restTemplate.postForEntity(
                "/diamonds/stone-decisions",
                request,
                DiamondsActionResponse.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().results()).hasSize(2);

        Map<String, Object> keepRow = fetchOrderByStone("CST-002");
        assertThat(keepRow.get("stone_status")).isEqualTo("Keep");
        assertThat(keepRow.get("decided_by")).isEqualTo("Alex Harper");
        assertThat(((Date) keepRow.get("decided_date")).toLocalDate()).isEqualTo(LocalDate.parse("2025-02-06"));

        Map<String, Object> replaceRow = fetchOrderByStone("CST-004");
        assertThat(replaceRow.get("stone_status")).isEqualTo("Replace");
        assertThat(replaceRow.get("decided_by")).isEqualTo("Morgan Lee");
        assertThat(((Date) replaceRow.get("decided_date")).toLocalDate()).isEqualTo(LocalDate.parse("2025-02-08"));

        Map<String, Object> keepSummary = fetchSummary("HP-1002");
        assertThat(keepSummary.get("center_stone_order_status")).isEqualTo("Keep");
        assertThat((Integer) keepSummary.get("keep_count")).isEqualTo(1);

        Map<String, Object> replaceSummary = fetchSummary("HP-1004");
        assertThat((Integer) replaceSummary.get("replace_count")).isEqualTo(1);
    }
}
