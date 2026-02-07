package com.procuresense.backend.controller;

import com.procuresense.backend.model.BundleRecommendation;
import com.procuresense.backend.model.DemoLoadResponse;
import com.procuresense.backend.model.PurchaseImportResponse;
import com.procuresense.backend.model.PurchaseSummary;
import com.procuresense.backend.model.ReorderPrediction;
import com.procuresense.backend.service.BundleRecommendationService;
import com.procuresense.backend.service.DemoDataService;
import com.procuresense.backend.service.PurchaseImportService;
import com.procuresense.backend.service.ReorderExplanationService;
import com.procuresense.backend.service.ReorderInsightService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {

    private final DemoDataService demoDataService;
    private final PurchaseImportService purchaseImportService;
    private final ReorderInsightService reorderInsightService;
    private final BundleRecommendationService bundleRecommendationService;
    private final ReorderExplanationService reorderExplanationService;

    public PurchaseController(DemoDataService demoDataService,
                              PurchaseImportService purchaseImportService,
                              ReorderInsightService reorderInsightService,
                              BundleRecommendationService bundleRecommendationService,
                              ReorderExplanationService reorderExplanationService) {
        this.demoDataService = demoDataService;
        this.purchaseImportService = purchaseImportService;
        this.reorderInsightService = reorderInsightService;
        this.bundleRecommendationService = bundleRecommendationService;
        this.reorderExplanationService = reorderExplanationService;
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

    @GetMapping("/insights/reorders")
    public ResponseEntity<List<ReorderPrediction>> reorders(
            @RequestHeader("X-Org-Id") @NotBlank String orgId,
            @RequestHeader("X-Role") @NotBlank String role,
            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        List<ReorderPrediction> predictions = reorderInsightService.computePredictions(orgId);
        if (limit > 0 && predictions.size() > limit) {
            predictions = predictions.subList(0, limit);
        }
        List<ReorderPrediction> enriched = reorderExplanationService.enrich(predictions);
        return ResponseEntity.ok(enriched);
    }

    @GetMapping("/insights/bundles/{sku}")
    public ResponseEntity<List<BundleRecommendation>> bundles(
            @RequestHeader("X-Org-Id") @NotBlank String orgId,
            @RequestHeader("X-Role") @NotBlank String role,
            @PathVariable("sku") String sku,
            @RequestParam(name = "limit", defaultValue = "5") int limit) {
        List<BundleRecommendation> bundles = bundleRecommendationService.getBundlesForSku(orgId, sku);
        if (limit > 0 && bundles.size() > limit) {
            bundles = bundles.subList(0, limit);
        }
        return ResponseEntity.ok(bundles);
    }
}
