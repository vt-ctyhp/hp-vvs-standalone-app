package com.hpvvssalesautomation.diamonds;

import com.hpvvssalesautomation.domain.diamonds.DiamondsActionResponse;
import com.hpvvssalesautomation.domain.diamonds.DiamondsConfirmDeliveryItem;
import com.hpvvssalesautomation.domain.diamonds.DiamondsConfirmDeliveryRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConfirmDeliveryTests extends AbstractDiamondsIntegrationTest {

    @BeforeEach
    void loadData() {
        loadDiamondsFixture();
    }

    @Test
    void confirmDeliveryUpdatesRowsAndSummaries() {
        DiamondsConfirmDeliveryItem item = new DiamondsConfirmDeliveryItem();
        item.setRootApptId("HP-1001");
        item.setMemoDate("2025-02-05");
        item.setSelected(true);

        DiamondsConfirmDeliveryRequest request = new DiamondsConfirmDeliveryRequest();
        request.setItems(List.of(item));
        request.setDefaultMemoDate("2025-02-10");
        request.setApplyDefaultToAll(false);

        ResponseEntity<DiamondsActionResponse> response = restTemplate.postForEntity(
                "/diamonds/confirm-delivery",
                request,
                DiamondsActionResponse.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().results()).hasSize(1);
        assertThat(response.getBody().results().get(0).centerStoneOrderStatus()).isEqualTo("Proposing");

        Map<String, Object> delivered = fetchOrderByStone("DV-001");
        assertThat(delivered.get("order_status")).isEqualTo("Delivered");
        assertThat(delivered.get("stone_status")).isEqualTo("In Stock");
        assertThat(((Date) delivered.get("memo_invoice_date")).toLocalDate()).isEqualTo(LocalDate.parse("2025-02-05"));
        assertThat(((Date) delivered.get("return_due_date")).toLocalDate()).isEqualTo(LocalDate.parse("2025-02-25"));

        Map<String, Object> summary = fetchSummary("HP-1001");
        assertThat(summary.get("center_stone_order_status")).isEqualTo("Proposing");
        assertThat((Integer) summary.get("delivered_count")).isEqualTo(1);
        assertThat((Integer) summary.get("in_stock_count")).isEqualTo(1);
    }
}
