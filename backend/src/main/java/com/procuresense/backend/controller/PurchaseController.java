package com.procuresense.backend.controller;

import com.procuresense.backend.model.PurchaseSummary;
import com.procuresense.backend.service.DemoDataService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {

    private final DemoDataService demoDataService;

    public PurchaseController(DemoDataService demoDataService) {
        this.demoDataService = demoDataService;
    }

    @PostMapping("/demo/load")
    public ResponseEntity<PurchaseSummary> loadDemoData(
            @RequestHeader("X-Org-Id") @NotBlank String orgId,
            @RequestHeader("X-Role") @NotBlank String role) {
        return ResponseEntity.ok(demoDataService.loadDemoData());
    }

    @GetMapping("/summary")
    public ResponseEntity<PurchaseSummary> summary() {
        return ResponseEntity.ok(demoDataService.getSummary());
    }
}
