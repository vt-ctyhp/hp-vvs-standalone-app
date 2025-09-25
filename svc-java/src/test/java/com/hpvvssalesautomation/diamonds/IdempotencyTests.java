package com.hpvvssalesautomation.diamonds;

import com.hpvvssalesautomation.domain.diamonds.DiamondsActionResponse;
import com.hpvvssalesautomation.domain.diamonds.DiamondsConfirmDeliveryItem;
import com.hpvvssalesautomation.domain.diamonds.DiamondsConfirmDeliveryRequest;
import com.hpvvssalesautomation.domain.diamonds.DiamondsOrderApprovalItem;
import com.hpvvssalesautomation.domain.diamonds.DiamondsOrderApprovalsRequest;
import com.hpvvssalesautomation.domain.diamonds.DiamondsStoneDecisionItem;
import com.hpvvssalesautomation.domain.diamonds.DiamondsStoneDecisionsRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class IdempotencyTests extends AbstractDiamondsIntegrationTest {

    @BeforeEach
    void loadData() {
        loadDiamondsFixture();
    }

    @Test
    void orderApprovalsAreIdempotent() {
        DiamondsOrderApprovalsRequest request = new DiamondsOrderApprovalsRequest();
        request.setDefaultOrderedBy("Jamie Lee");
        request.setDefaultOrderedDate("2025-02-01");
        request.setApplyDefaultsToAll(false);

        List<DiamondsOrderApprovalItem> items = new ArrayList<>();
        DiamondsOrderApprovalItem item = new DiamondsOrderApprovalItem();
        item.setRootApptId("HP-1001");
        item.setDecision("On the Way");
        item.setOrderedBy("Casey Stone");
        item.setOrderedDate("2025-02-03");
        items.add(item);

        DiamondsOrderApprovalItem second = new DiamondsOrderApprovalItem();
        second.setRootApptId("HP-1003");
        second.setDecision("Not Approved");
        items.add(second);
        request.setItems(items);

        ResponseEntity<DiamondsActionResponse> first = restTemplate.postForEntity(
                "/diamonds/order-approvals",
                request,
                DiamondsActionResponse.class
        );
        assertThat(first.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(first.getBody()).isNotNull();
        assertThat(first.getBody().results()).allSatisfy(result -> assertThat(result.affectedRows()).isGreaterThan(0));

        Map<String, Object> summaryBefore = fetchSummary("HP-1001");
        int onTheWayBefore = (Integer) summaryBefore.get("on_the_way_count");

        ResponseEntity<DiamondsActionResponse> secondCall = restTemplate.postForEntity(
                "/diamonds/order-approvals",
                request,
                DiamondsActionResponse.class
        );
        assertThat(secondCall.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(secondCall.getBody()).isNotNull();
        assertThat(secondCall.getBody().results()).allSatisfy(result -> assertThat(result.affectedRows()).isZero());

        Map<String, Object> summaryAfter = fetchSummary("HP-1001");
        assertThat((Integer) summaryAfter.get("on_the_way_count")).isEqualTo(onTheWayBefore);

        Map<String, Object> updated = fetchOrderByStone("CST-001");
        assertThat(((Date) updated.get("ordered_date")).toLocalDate()).isEqualTo(LocalDate.parse("2025-02-03"));
    }

    @Test
    void confirmDeliveryIsIdempotent() {
        DiamondsConfirmDeliveryItem item = new DiamondsConfirmDeliveryItem();
        item.setRootApptId("HP-1001");
        item.setMemoDate("2025-02-05");
        item.setSelected(true);

        DiamondsConfirmDeliveryRequest request = new DiamondsConfirmDeliveryRequest();
        request.setItems(List.of(item));
        request.setApplyDefaultToAll(false);

        ResponseEntity<DiamondsActionResponse> first = restTemplate.postForEntity(
                "/diamonds/confirm-delivery",
                request,
                DiamondsActionResponse.class
        );
        assertThat(first.getStatusCode().is2xxSuccessful()).isTrue();

        Map<String, Object> delivered = fetchOrderByStone("DV-001");
        LocalDate memoDate = ((Date) delivered.get("memo_invoice_date")).toLocalDate();
        LocalDate dueDate = ((Date) delivered.get("return_due_date")).toLocalDate();
        assertThat(dueDate).isEqualTo(memoDate.plusDays(20));

        ResponseEntity<DiamondsActionResponse> second = restTemplate.postForEntity(
                "/diamonds/confirm-delivery",
                request,
                DiamondsActionResponse.class
        );
        assertThat(second.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(second.getBody()).isNotNull();
        assertThat(second.getBody().results()).allSatisfy(result -> assertThat(result.affectedRows()).isZero());

        Map<String, Object> deliveredAfter = fetchOrderByStone("DV-001");
        LocalDate memoDateAfter = ((Date) deliveredAfter.get("memo_invoice_date")).toLocalDate();
        LocalDate dueDateAfter = ((Date) deliveredAfter.get("return_due_date")).toLocalDate();
        assertThat(memoDateAfter).isEqualTo(memoDate);
        assertThat(dueDateAfter).isEqualTo(dueDate);

    }

    @Test
    void stoneDecisionsAreIdempotent() {
        DiamondsStoneDecisionItem item = new DiamondsStoneDecisionItem();
        item.setRootApptId("HP-1002");
        item.setDecision("Keep");

        DiamondsStoneDecisionsRequest request = new DiamondsStoneDecisionsRequest();
        request.setItems(List.of(item));
        request.setDefaultDecidedBy("Morgan Lee");
        request.setDefaultDecidedDate("2025-02-08");
        request.setApplyDefaultsToAll(true);

        ResponseEntity<DiamondsActionResponse> first = restTemplate.postForEntity(
                "/diamonds/stone-decisions",
                request,
                DiamondsActionResponse.class
        );
        assertThat(first.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(first.getBody()).isNotNull();
        assertThat(first.getBody().results()).allSatisfy(result -> assertThat(result.affectedRows()).isGreaterThan(0));

        Map<String, Object> keepRow = fetchOrderByStone("CST-002");
        LocalDate decidedDate = ((Date) keepRow.get("decided_date")).toLocalDate();
        assertThat(decidedDate).isEqualTo(LocalDate.parse("2025-02-08"));

        ResponseEntity<DiamondsActionResponse> second = restTemplate.postForEntity(
                "/diamonds/stone-decisions",
                request,
                DiamondsActionResponse.class
        );
        assertThat(second.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(second.getBody()).isNotNull();

        Map<String, Object> keepRowAfter = fetchOrderByStone("CST-002");
        assertThat(((Date) keepRowAfter.get("decided_date")).toLocalDate()).isEqualTo(decidedDate);
    }
}
