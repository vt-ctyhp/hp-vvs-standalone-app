package com.hpvvssalesautomation.web;

import com.hpvvssalesautomation.domain.payments.PaymentRecordRequest;
import com.hpvvssalesautomation.domain.payments.PaymentRecordResult;
import com.hpvvssalesautomation.domain.payments.PaymentsService;
import com.hpvvssalesautomation.domain.payments.PaymentsSummaryResponse;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/payments")
@ConditionalOnProperty(prefix = "app.feature-flags", name = "payments", havingValue = "true")
public class PaymentsController {

    private final PaymentsService paymentsService;

    public PaymentsController(PaymentsService paymentsService) {
        this.paymentsService = paymentsService;
    }

    @PostMapping("/record")
    public ResponseEntity<PaymentRecordResult> record(@Valid @RequestBody PaymentRecordRequest request) {
        try {
            return ResponseEntity.ok(paymentsService.record(request));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @GetMapping("/summary")
    public ResponseEntity<PaymentsSummaryResponse> summary(@RequestParam(value = "rootApptId", required = false) String rootApptId,
                                                            @RequestParam(value = "soNumber", required = false) String soNumber) {
        try {
            return ResponseEntity.ok(paymentsService.summarize(rootApptId, soNumber));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}
