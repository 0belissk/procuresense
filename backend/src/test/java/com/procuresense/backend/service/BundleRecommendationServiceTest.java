package com.procuresense.backend.service;

import com.procuresense.backend.model.BundleRecommendation;
import com.procuresense.backend.model.Product;
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
class BundleRecommendationServiceTest {

    @Autowired
    private BundleRecommendationService bundleRecommendationService;

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
    void computesCoPurchaseCountsPerSkuPair() {
        Product wipes = saveProduct("SKU-1001", "Eco Wipes");
        Product bins = saveProduct("SKU-2001", "Storage Bins");
        Product gloves = saveProduct("SKU-3001", "Safety Gloves");
        Product tape = saveProduct("SKU-4001", "Packing Tape");

        insertPurchase("demo-org-a", "ORD-001", wipes, "2024-04-01T10:00:00Z");
        insertPurchase("demo-org-a", "ORD-001", bins, "2024-04-01T10:05:00Z");
        insertPurchase("demo-org-a", "ORD-001", gloves, "2024-04-01T10:06:00Z");

        insertPurchase("demo-org-a", "ORD-002", wipes, "2024-04-05T09:00:00Z");
        insertPurchase("demo-org-a", "ORD-002", bins, "2024-04-05T09:02:00Z");

        insertPurchase("demo-org-a", "", tape, "2024-04-07T12:00:00Z");
        insertPurchase("demo-org-a", "", wipes, "2024-04-07T12:30:00Z"); // fallback same-day window

        List<BundleRecommendation> wipesBundles = bundleRecommendationService.getBundlesForSku("demo-org-a", "SKU-1001");
        assertThat(wipesBundles).isNotEmpty();
        BundleRecommendation wipesToBins = wipesBundles.stream()
                .filter(r -> r.relatedSku().equals("SKU-2001"))
                .findFirst().orElseThrow();
        assertThat(wipesToBins.coPurchaseCount()).isEqualTo(2);

        BundleRecommendation wipesToTape = wipesBundles.stream()
                .filter(r -> r.relatedSku().equals("SKU-4001"))
                .findFirst().orElseThrow();
        assertThat(wipesToTape.coPurchaseCount()).isEqualTo(1); // fallback same-day grouping

        List<BundleRecommendation> binsBundles = bundleRecommendationService.getBundlesForSku("demo-org-a", "SKU-2001");
        BundleRecommendation binsToGloves = binsBundles.stream()
                .filter(r -> r.relatedSku().equals("SKU-3001"))
                .findFirst().orElseThrow();
        assertThat(binsToGloves.coPurchaseCount()).isEqualTo(1);
    }

    private Product saveProduct(String sku, String name) {
        Product product = new Product();
        product.setSku(sku);
        product.setName(name);
        product.setCategory("Test");
        product.setUnitPrice(new BigDecimal("5.00"));
        return productRepository.save(product);
    }

    private void insertPurchase(String orgId, String orderId, Product product, String purchasedAt) {
        com.procuresense.backend.model.Purchase purchase = new com.procuresense.backend.model.Purchase();
        purchase.setOrgId(orgId);
        purchase.setOrderId(orderId);
        purchase.setProduct(product);
        purchase.setQuantity(1);
        purchase.setUnitPrice(product.getUnitPrice());
        purchase.setPurchasedAt(OffsetDateTime.parse(purchasedAt));
        purchaseRepository.save(purchase);
    }
}
