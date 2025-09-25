package com.hpvvssalesautomation.diamonds;

import com.hpvvssalesautomation.domain.diamonds.DiamondsActionResponse;
import com.hpvvssalesautomation.domain.diamonds.DiamondsOrderApprovalItem;
import com.hpvvssalesautomation.domain.diamonds.DiamondsOrderApprovalsRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OrderApprovalsTests extends AbstractDiamondsIntegrationTest {

    @BeforeEach
    void loadData() {
        loadDiamondsFixture();
    }

    @Test
    void approvingProposalsUpdatesOrdersAndSummaries() {
        DiamondsOrderApprovalsRequest request = new DiamondsOrderApprovalsRequest();
        request.setDefaultOrderedBy("Jamie Lee");
        request.setDefaultOrderedDate("2025-02-01");
        request.setApplyDefaultsToAll(false);

        List<DiamondsOrderApprovalItem> items = new ArrayList<>();

        DiamondsOrderApprovalItem first = new DiamondsOrderApprovalItem();
        first.setRootApptId("HP-1001");
        first.setDecision("On the Way");
        first.setOrderedBy("Casey Stone");
        first.setOrderedDate("2025-02-03");
        items.add(first);

        DiamondsOrderApprovalItem second = new DiamondsOrderApprovalItem();
        second.setRootApptId("HP-1003");
        second.setDecision("Not Approved");
        items.add(second);

        request.setItems(items);

        ResponseEntity<DiamondsActionResponse> response = restTemplate.postForEntity(
                "/diamonds/order-approvals",
                request,
                DiamondsActionResponse.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().results()).hasSize(2);

        Map<String, Object> updatedHp1001 = fetchOrderByStone("CST-001");
        assertThat(updatedHp1001.get("order_status")).isEqualTo("On the Way");
        assertThat(updatedHp1001.get("ordered_by")).isEqualTo("Casey Stone");
        assertThat(((Date) updatedHp1001.get("ordered_date")).toLocalDate()).isEqualTo(LocalDate.parse("2025-02-03"));

        Map<String, Object> updatedHp1003 = fetchOrderByStone("DV-003");
        assertThat(updatedHp1003.get("order_status")).isEqualTo("Not Approved");
        assertThat(updatedHp1003.get("ordered_by")).isEqualTo("Jamie Lee");
        assertThat(((Date) updatedHp1003.get("ordered_date")).toLocalDate()).isEqualTo(LocalDate.parse("2025-02-01"));

        Map<String, Object> summary1001 = fetchSummary("HP-1001");
        assertThat(summary1001.get("center_stone_order_status")).isEqualTo("On the Way");
        assertThat((Integer) summary1001.get("on_the_way_count")).isEqualTo(2);
        assertThat((Integer) summary1001.get("total_count")).isEqualTo(2);

        Map<String, Object> summary1003 = fetchSummary("HP-1003");
        assertThat(summary1003.get("center_stone_order_status")).isEqualTo("Not Approved");
        assertThat((Integer) summary1003.get("not_approved_count")).isEqualTo(1);
        assertThat((Integer) summary1003.get("total_count")).isEqualTo(1);
    }
}
