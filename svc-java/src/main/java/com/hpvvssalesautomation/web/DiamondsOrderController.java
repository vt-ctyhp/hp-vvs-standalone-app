package com.hpvvssalesautomation.web;

import com.hpvvssalesautomation.domain.diamonds.DiamondsActionResponse;
import com.hpvvssalesautomation.domain.diamonds.DiamondsOrderApprovalsRequest;
import com.hpvvssalesautomation.domain.diamonds.DiamondsOrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/diamonds")
public class DiamondsOrderController {

    private final DiamondsOrderService diamondsOrderService;

    public DiamondsOrderController(DiamondsOrderService diamondsOrderService) {
        this.diamondsOrderService = diamondsOrderService;
    }

    @PostMapping("/order-approvals")
    public ResponseEntity<DiamondsActionResponse> approve(@Valid @RequestBody DiamondsOrderApprovalsRequest request) {
        return ResponseEntity.ok(diamondsOrderService.approve(request));
    }
}
