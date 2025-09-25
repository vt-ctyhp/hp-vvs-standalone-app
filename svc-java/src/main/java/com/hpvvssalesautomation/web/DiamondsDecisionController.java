package com.hpvvssalesautomation.web;

import com.hpvvssalesautomation.domain.diamonds.DiamondsActionResponse;
import com.hpvvssalesautomation.domain.diamonds.DiamondsDecisionService;
import com.hpvvssalesautomation.domain.diamonds.DiamondsStoneDecisionsRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/diamonds")
public class DiamondsDecisionController {

    private final DiamondsDecisionService diamondsDecisionService;

    public DiamondsDecisionController(DiamondsDecisionService diamondsDecisionService) {
        this.diamondsDecisionService = diamondsDecisionService;
    }

    @PostMapping("/stone-decisions")
    public ResponseEntity<DiamondsActionResponse> decide(@Valid @RequestBody DiamondsStoneDecisionsRequest request) {
        return ResponseEntity.ok(diamondsDecisionService.decide(request));
    }
}
