package com.hpvvssalesautomation.diamonds;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApiContractTests {

    private static final Path SPEC_PATH = Path.of("..", "docs", "api.yaml").toAbsolutePath().normalize();

    @Test
    void openApiIncludesDiamondsPhaseThreeContract() {
        OpenAPI openAPI = new OpenAPIV3Parser().read(SPEC_PATH.toString());
        assertThat(openAPI).as("openapi parsed").isNotNull();
        assertThat(openAPI.getPaths()).containsKeys(
                "/diamonds/order-approvals",
                "/diamonds/confirm-delivery",
                "/diamonds/stone-decisions"
        );

        assertThat(openAPI.getComponents().getSchemas()).containsKeys(
                "DiamondsOrderApprovalsRequest",
                "DiamondsOrderApprovalItem",
                "DiamondsConfirmDeliveryRequest",
                "DiamondsConfirmDeliveryItem",
                "DiamondsStoneDecisionsRequest",
                "DiamondsStoneDecisionItem",
                "DiamondsActionResponse",
                "DiamondsActionResult"
        );

        List<String> approvalsRequired = openAPI.getComponents().getSchemas()
                .get("DiamondsOrderApprovalsRequest").getRequired();
        assertThat(approvalsRequired).containsExactly("items");

        List<String> approvalsItemRequired = openAPI.getComponents().getSchemas()
                .get("DiamondsOrderApprovalItem").getRequired();
        assertThat(approvalsItemRequired).containsExactlyInAnyOrder("rootApptId", "decision");

        List<String> deliveryRequired = openAPI.getComponents().getSchemas()
                .get("DiamondsConfirmDeliveryRequest").getRequired();
        assertThat(deliveryRequired).containsExactly("items");

        List<String> deliveryItemRequired = openAPI.getComponents().getSchemas()
                .get("DiamondsConfirmDeliveryItem").getRequired();
        assertThat(deliveryItemRequired).containsExactly("rootApptId");

        List<String> decisionsRequired = openAPI.getComponents().getSchemas()
                .get("DiamondsStoneDecisionsRequest").getRequired();
        assertThat(decisionsRequired).containsExactly("items");

        List<String> decisionsItemRequired = openAPI.getComponents().getSchemas()
                .get("DiamondsStoneDecisionItem").getRequired();
        assertThat(decisionsItemRequired).containsExactlyInAnyOrder("rootApptId", "decision");

        List<String> actionResultRequired = openAPI.getComponents().getSchemas()
                .get("DiamondsActionResult").getRequired();
        assertThat(actionResultRequired).containsExactlyInAnyOrder("rootApptId", "affectedRows", "centerStoneOrderStatus", "counts", "updatedAt");
    }
}
