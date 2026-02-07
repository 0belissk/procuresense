package com.procuresense.backend.service;

import com.procuresense.backend.model.Purchase;
import com.procuresense.backend.model.PurchaseImportResponse;
import com.procuresense.backend.repository.ProductRepository;
import com.procuresense.backend.repository.PurchaseLoadAuditRepository;
import com.procuresense.backend.repository.PurchaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PurchaseImportServiceTest {

    @Autowired
    private PurchaseImportService purchaseImportService;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PurchaseLoadAuditService purchaseLoadAuditService;

    @Autowired
    private PurchaseLoadAuditRepository purchaseLoadAuditRepository;

    @BeforeEach
    void cleanDb() {
        purchaseRepository.deleteAll();
        productRepository.deleteAll();
        purchaseLoadAuditRepository.deleteAll();
    }

    @Test
    void importCsvPersistsPurchasesWithOrgScope() {
        String csv = """
                order_id,sku,product_name,category,quantity,unit_price,purchased_at
                ORD-100,SKU-2001,Bio Cleaner,Cleaning,4,19.99,2024-02-01T09:00:00Z
                ORD-100,SKU-2002,Reusable Gloves,Supplies,2,7.50,2024-02-01T09:00:00Z
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "purchases.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        PurchaseImportResponse response = purchaseImportService.importCsv(file, "demo-org-a");

        assertThat(response.importedRows()).isEqualTo(2);
        assertThat(response.rejectedRows()).isZero();

        List<Purchase> purchases = purchaseRepository.findAll();
        assertThat(purchases).hasSize(2);
        assertThat(purchases)
                .extracting(Purchase::getOrgId)
                .containsOnly("demo-org-a");

        assertThat(purchaseLoadAuditService.getLastLoadedAt("demo-org-a")).isNotNull();
    }
}
