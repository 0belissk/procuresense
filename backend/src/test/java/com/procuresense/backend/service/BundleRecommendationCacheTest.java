package com.procuresense.backend.service;

import com.procuresense.backend.model.BundleRecommendation;
import com.procuresense.backend.model.Product;
import com.procuresense.backend.repository.BundleInsightRepository;
import com.procuresense.backend.repository.ProductRepository;
import com.procuresense.backend.repository.PurchaseLoadAuditRepository;
import com.procuresense.backend.repository.PurchaseRepository;
import com.procuresense.backend.service.ai.OpenAiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;

@SpringBootTest
@ActiveProfiles("test")
class BundleRecommendationCacheTest {

    @Autowired
    private BundleRecommendationService bundleRecommendationService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private PurchaseLoadAuditRepository purchaseLoadAuditRepository;

    @Autowired
    private BundleInsightRepository bundleInsightRepository;

    @MockBean
    private OpenAiClient openAiClient;

    @BeforeEach
    void setup() {
        purchaseRepository.deleteAll();
        productRepository.deleteAll();
        purchaseLoadAuditRepository.deleteAll();
        bundleInsightRepository.deleteAll();
    }

    @Test
    void reusesCachedRationaleWithoutCallingOpenAiAgain() {
        Product cups = saveProduct("SKU-5001", "Biodegradable Cups");
        Product cutlery = saveProduct("SKU-9001", "Compostable Cutlery");

        insertPurchase("demo-org-a", "ORD-900", cups, "2024-05-01T10:00:00Z");
        insertPurchase("demo-org-a", "ORD-900", cutlery, "2024-05-01T10:01:00Z");
        insertPurchase("demo-org-a", "ORD-901", cups, "2024-05-03T09:00:00Z");
        insertPurchase("demo-org-a", "ORD-901", cutlery, "2024-05-03T09:02:00Z");

        Mockito.when(openAiClient.generateBundleRationale(anyString(), anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(Optional.of("AI says they pair nicely."));

        List<BundleRecommendation> first = bundleRecommendationService.getBundlesForSku("demo-org-a", "SKU-5001");
        assertThat(first).isNotEmpty();
        BundleRecommendation firstPair = first.get(0);
        assertThat(firstPair.rationale()).isEqualTo("AI says they pair nicely.");
        assertThat(bundleInsightRepository.count()).isEqualTo(1);

        List<BundleRecommendation> second = bundleRecommendationService.getBundlesForSku("demo-org-a", "SKU-5001");
        assertThat(second.get(0).rationale()).isEqualTo("AI says they pair nicely.");

        Mockito.verify(openAiClient, Mockito.times(1))
                .generateBundleRationale(anyString(), anyString(), anyString(), anyString(), anyLong());
    }

    private Product saveProduct(String sku, String name) {
        Product product = new Product();
        product.setSku(sku);
        product.setName(name);
        product.setCategory("Test");
        product.setUnitPrice(new BigDecimal("10.00"));
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
