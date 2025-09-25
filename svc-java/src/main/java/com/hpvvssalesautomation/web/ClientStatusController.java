package com.hpvvssalesautomation.web;

import com.hpvvssalesautomation.domain.ClientStatusResponse;
import com.hpvvssalesautomation.domain.ClientStatusService;
import com.hpvvssalesautomation.domain.ClientStatusSubmitRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/client-status")
public class ClientStatusController {

    private final ClientStatusService clientStatusService;

    public ClientStatusController(ClientStatusService clientStatusService) {
        this.clientStatusService = clientStatusService;
    }

    @PostMapping("/submit")
    public ResponseEntity<ClientStatusResponse> submit(@Valid @RequestBody ClientStatusSubmitRequest request) {
        return ResponseEntity.ok(clientStatusService.submit(request));
    }
}
