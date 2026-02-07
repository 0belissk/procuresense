package com.procuresense.backend.service;

import com.procuresense.backend.model.Product;
import com.procuresense.backend.model.ReorderPrediction;
import com.procuresense.backend.repository.ProductRepository;
import com.procuresense.backend.repository.PurchaseLoadAuditRepository;
import com.procuresense.backend.repository.PurchaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ReorderInsightServiceTest {

    @Autowired
    private ReorderInsightService reorderInsightService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private PurchaseLoadAuditRepository purchaseLoadAuditRepository;

    @BeforeEach
    void setup() {
        purchaseRepository.deleteAll();
        productRepository.deleteAll();
        purchaseLoadAuditRepository.deleteAll();
    }

    @Test
    void computePredictionsCalculatesMedianAndPrediction() {
        Product towels = productRepository.save(buildProduct("SKU-1001", "Eco Towels"));
        Product gloves = productRepository.save(buildProduct("SKU-2001", "Safety Gloves"));
        Product single = productRepository.save(buildProduct("SKU-9999", "Single Item"));

        insertPurchase("demo-org-a", towels, 5, "2024-01-01T08:00:00Z");
        insertPurchase("demo-org-a", towels, 5, "2024-01-11T08:00:00Z");
        insertPurchase("demo-org-a", towels, 5, "2024-01-21T08:00:00Z");

        insertPurchase("demo-org-a", gloves, 10, "2024-02-01T10:00:00Z");
        insertPurchase("demo-org-a", gloves, 10, "2024-02-07T10:00:00Z");
        insertPurchase("demo-org-a", gloves, 10, "2024-02-15T10:00:00Z");
        insertPurchase("demo-org-a", gloves, 10, "2024-02-24T10:00:00Z");

        insertPurchase("demo-org-a", single, 1, "2024-03-01T09:00:00Z");

        List<ReorderPrediction> predictions = reorderInsightService.computePredictions("demo-org-a");

        assertThat(predictions).hasSize(2);
        ReorderPrediction towelsPrediction = predictions.stream()
                .filter(p -> p.sku().equals("SKU-1001"))
                .findFirst()
                .orElseThrow();
        assertThat(towelsPrediction.medianDaysBetween()).isEqualTo(10);
        assertThat(towelsPrediction.predictedReorderAt()).isEqualTo(OffsetDateTime.parse("2024-01-31T08:00:00Z"));
        assertThat(towelsPrediction.confidence()).isEqualTo(0.8);

        ReorderPrediction glovesPrediction = predictions.stream()
                .filter(p -> p.sku().equals("SKU-2001"))
                .findFirst()
                .orElseThrow();
        assertThat(glovesPrediction.medianDaysBetween()).isEqualTo(8);
        assertThat(glovesPrediction.predictedReorderAt()).isEqualTo(OffsetDateTime.parse("2024-03-03T10:00:00Z"));
        assertThat(glovesPrediction.confidence()).isBetween(0.5, 1.0);

        assertThat(predictions).noneMatch(p -> p.sku().equals("SKU-9999"));
    }

    private Product buildProduct(String sku, String name) {
        Product product = new Product();
        product.setSku(sku);
        product.setName(name);
        product.setCategory("Test");
        product.setUnitPrice(new BigDecimal("10.00"));
        return product;
    }

    private void insertPurchase(String orgId, Product product, int quantity, String purchasedAt) {
        com.procuresense.backend.model.Purchase purchase = new com.procuresense.backend.model.Purchase();
        purchase.setOrgId(orgId);
        purchase.setProduct(product);
        purchase.setOrderId(orgId + "-" + product.getSku() + purchasedAt);
        purchase.setQuantity(quantity);
        purchase.setUnitPrice(product.getUnitPrice());
        purchase.setPurchasedAt(OffsetDateTime.parse(purchasedAt));
        purchaseRepository.save(purchase);
    }
}
