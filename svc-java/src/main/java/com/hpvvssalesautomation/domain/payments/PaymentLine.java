package com.hpvvssalesautomation.domain.payments;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentLine(
        @JsonProperty("desc") String description,
        @JsonProperty("qty") BigDecimal quantity,
        @JsonProperty("amt") BigDecimal amount,
        @JsonProperty("lineTotal") BigDecimal lineTotal
) {
}
