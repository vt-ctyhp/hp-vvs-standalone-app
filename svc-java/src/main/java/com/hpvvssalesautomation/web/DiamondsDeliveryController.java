package com.hpvvssalesautomation.web;

import com.hpvvssalesautomation.domain.diamonds.DiamondsActionResponse;
import com.hpvvssalesautomation.domain.diamonds.DiamondsConfirmDeliveryRequest;
import com.hpvvssalesautomation.domain.diamonds.DiamondsDeliveryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/diamonds")
public class DiamondsDeliveryController {

    private final DiamondsDeliveryService diamondsDeliveryService;

    public DiamondsDeliveryController(DiamondsDeliveryService diamondsDeliveryService) {
        this.diamondsDeliveryService = diamondsDeliveryService;
    }

    @PostMapping("/confirm-delivery")
    public ResponseEntity<DiamondsActionResponse> confirm(@Valid @RequestBody DiamondsConfirmDeliveryRequest request) {
        return ResponseEntity.ok(diamondsDeliveryService.confirm(request));
    }
}
