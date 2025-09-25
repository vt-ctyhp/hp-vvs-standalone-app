package com.hpvvssalesautomation.web;

import com.hpvvssalesautomation.domain.AppointmentSummaryRequest;
import com.hpvvssalesautomation.domain.AppointmentSummaryService;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/appointment-summary")
@Validated
public class AppointmentSummaryController {

    private final AppointmentSummaryService service;

    public AppointmentSummaryController(AppointmentSummaryService service) {
        this.service = service;
    }

    @PostMapping("/run")
    @ResponseStatus(HttpStatus.OK)
    public List<Map<String, Object>> run(@RequestBody AppointmentSummaryRequest request) {
        return service.run(request);
    }
}
