package com.procuresense.backend.controller;

import com.procuresense.backend.model.DemoLoadResponse;
import com.procuresense.backend.model.PurchaseImportResponse;
import com.procuresense.backend.model.PurchaseSummary;
import com.procuresense.backend.service.DemoDataService;
import com.procuresense.backend.service.PurchaseImportService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {

    private final DemoDataService demoDataService;
    private final PurchaseImportService purchaseImportService;

    public PurchaseController(DemoDataService demoDataService,
                              PurchaseImportService purchaseImportService) {
        this.demoDataService = demoDataService;
        this.purchaseImportService = purchaseImportService;
    }

    @PostMapping("/upload")
    public ResponseEntity<PurchaseImportResponse> uploadPurchases(
            @RequestPart("file") MultipartFile file,
            @RequestHeader("X-Org-Id") @NotBlank String orgId,
            @RequestHeader("X-Role") @NotBlank String role) {
        return ResponseEntity.ok(purchaseImportService.importCsv(file, orgId));
    }

    @PostMapping("/demo/load")
    public ResponseEntity<DemoLoadResponse> loadDemoData(
            @RequestHeader("X-Org-Id") @NotBlank String orgId,
            @RequestHeader("X-Role") @NotBlank String role) {
        return ResponseEntity.ok(demoDataService.loadDemoData(orgId));
    }

    @GetMapping("/summary")
    public ResponseEntity<PurchaseSummary> summary(
            @RequestHeader("X-Org-Id") @NotBlank String orgId,
            @RequestHeader("X-Role") @NotBlank String role) {
        return ResponseEntity.ok(demoDataService.getSummary(orgId));
    }
}
