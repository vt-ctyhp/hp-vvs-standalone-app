package com.hpvvssalesautomation.payments;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ApiContractTests {

    private static final Path SPEC_PATH = Path.of("..", "docs", "api.yaml").toAbsolutePath().normalize();

    @Test
    void openApiDescribesPaymentsEndpointsAndSchemas() {
        OpenAPI openAPI = new OpenAPIV3Parser().read(SPEC_PATH.toString());
        assertThat(openAPI).as("openapi parsed").isNotNull();
        assertThat(openAPI.getPaths()).containsKeys("/payments/record", "/payments/summary");

        MediaType recordMediaType = openAPI.getPaths()
                .get("/payments/record")
                .getPost()
                .getRequestBody()
                .getContent()
                .get("application/json");
        assertThat(recordMediaType).isNotNull();
        assertThat(recordMediaType.getExample()).isNotNull();

        assertThat(openAPI.getPaths()
                .get("/payments/summary")
                .getGet()
                .getResponses()
                .get("200")
                .getContent()
                .get("application/json")
                .getSchema()).isNotNull();

        assertThat(openAPI.getComponents().getSchemas()).containsKeys(
                "PaymentRecordRequest",
                "PaymentRecordResponse",
                "PaymentsSummaryResponse",
                "PaymentSummaryEntry",
                "PaymentLineItem"
        );

        assertThat(openAPI.getComponents().getSchemas()
                .get("PaymentRecordRequest")
                .getRequired()).containsExactlyInAnyOrder(
                "anchorType", "docType", "paymentDateTime", "method", "amountGross"
        );

        assertThat(openAPI.getComponents().getSchemas()
                .get("PaymentRecordResponse")
                .getRequired()).contains(
                "status",
                "docNumber",
                "docRole",
                "docType",
                "docStatus",
                "paymentDateTime",
                "method",
                "amountGross",
                "amountNet",
                "lines",
                "requestHash"
        );

        assertThat(openAPI.getComponents().getSchemas()
                .get("PaymentSummaryEntry")
                .getRequired()).contains(
                "docNumber",
                "docRole",
                "docType",
                "docStatus",
                "amountGross",
                "amountNet"
        );

        assertThat(openAPI.getComponents().getSchemas()
                .get("PaymentsSummaryResponse")
                .getRequired()).containsExactlyInAnyOrder(
                "invoicesLinesSubtotal",
                "totalPayments",
                "netLinesMinusPayments",
                "byMethod",
                "entries"
        );
    }
}
