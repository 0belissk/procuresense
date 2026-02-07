package com.procuresense.backend.service;

import com.procuresense.backend.model.DemoLoadResponse;
import com.procuresense.backend.model.PurchaseSummary;
import com.procuresense.backend.repository.ProductRepository;
import com.procuresense.backend.repository.PurchaseLoadAuditRepository;
import com.procuresense.backend.repository.PurchaseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class DemoDataServiceTest {

    @Autowired
    private DemoDataService demoDataService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private PurchaseLoadAuditRepository purchaseLoadAuditRepository;

    @BeforeEach
    void cleanDb() {
        purchaseRepository.deleteAll();
        productRepository.deleteAll();
        purchaseLoadAuditRepository.deleteAll();
    }

    @Test
    void loadDemoDataPersistsRecordsAndReturnsSummary() {
        DemoLoadResponse response = demoDataService.loadDemoData("demo-org-a");
        PurchaseSummary summary = demoDataService.getSummary("demo-org-a");

        assertThat(response.orgId()).isEqualTo("demo-org-a");
        assertThat(response.importedRows()).isEqualTo(27);
        assertThat(productRepository.count()).isEqualTo(12);
        assertThat(purchaseRepository.count()).isEqualTo(27);
        assertThat(summary.orgId()).isEqualTo("demo-org-a");
        assertThat(summary.totalOrders()).isEqualTo(15);
        assertThat(summary.totalLineItems()).isEqualTo(27);
        assertThat(summary.totalQuantity()).isEqualTo(310);
        assertThat(summary.totalRevenue()).isEqualByComparingTo(new BigDecimal("7724.20"));
        assertThat(summary.totalSkus()).isEqualTo(12);
        assertThat(summary.dateRange()).isNotNull();
        assertThat(summary.dateRange().start()).isEqualTo(OffsetDateTime.parse("2024-04-01T10:15:00Z"));
        assertThat(summary.dateRange().end()).isEqualTo(OffsetDateTime.parse("2024-04-22T16:45:00Z"));
        assertThat(summary.lastLoadedAt()).isNotNull();
    }

    @Test
    void loadDemoDataIsIdempotentPerOrg() {
        demoDataService.loadDemoData("demo-org-a");
        PurchaseSummary first = demoDataService.getSummary("demo-org-a");

        demoDataService.loadDemoData("demo-org-a");
        PurchaseSummary second = demoDataService.getSummary("demo-org-a");

        assertThat(second.totalOrders()).isEqualTo(first.totalOrders());
        assertThat(second.totalLineItems()).isEqualTo(first.totalLineItems());
        assertThat(second.totalQuantity()).isEqualTo(first.totalQuantity());
        assertThat(second.totalSkus()).isEqualTo(first.totalSkus());
        assertThat(second.totalRevenue()).isEqualTo(first.totalRevenue());
        assertThat(second.dateRange()).isEqualTo(first.dateRange());
        assertThat(second.lastLoadedAt()).isAfterOrEqualTo(first.lastLoadedAt());
        assertThat(purchaseRepository.count()).isEqualTo(27);
    }
}
