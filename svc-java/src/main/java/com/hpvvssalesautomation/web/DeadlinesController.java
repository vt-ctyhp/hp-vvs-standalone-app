package com.hpvvssalesautomation.web;

import com.hpvvssalesautomation.domain.DeadlineRecordRequest;
import com.hpvvssalesautomation.domain.DeadlineRecordResponse;
import com.hpvvssalesautomation.domain.DeadlinesService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/deadlines")
public class DeadlinesController {

    private final DeadlinesService deadlinesService;

    public DeadlinesController(DeadlinesService deadlinesService) {
        this.deadlinesService = deadlinesService;
    }

    @PostMapping("/record")
    public ResponseEntity<DeadlineRecordResponse> record(@Valid @RequestBody DeadlineRecordRequest request) {
        return ResponseEntity.ok(deadlinesService.record(request));
    }
}
