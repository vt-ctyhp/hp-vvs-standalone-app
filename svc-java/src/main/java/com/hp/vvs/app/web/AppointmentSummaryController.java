package com.hp.vvs.app.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hp.vvs.app.domain.AppointmentSummaryService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/appointment-summary", produces = MediaType.APPLICATION_JSON_VALUE)
public class AppointmentSummaryController {

    private final AppointmentSummaryService appointmentSummaryService;

    public AppointmentSummaryController(AppointmentSummaryService appointmentSummaryService) {
        this.appointmentSummaryService = appointmentSummaryService;
    }

    @PostMapping(path = "/run", consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<Map<String, String>> run(@RequestBody AppointmentSummaryPayload payload) {
        return appointmentSummaryService.run(
                Optional.ofNullable(payload.brand),
                Optional.ofNullable(payload.rep),
                parseDate(payload.dateFrom),
                parseDate(payload.dateTo));
    }

    private Optional<LocalDate> parseDate(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(LocalDate.parse(value));
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AppointmentSummaryPayload {
        @JsonProperty("brand")
        public String brand;

        @JsonProperty("rep")
        public String rep;

        @JsonProperty("dateFrom")
        public String dateFrom;

        @JsonProperty("dateTo")
        public String dateTo;
    }
}
