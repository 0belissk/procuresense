package com.procuresense.backend.service;

import com.procuresense.backend.model.PurchaseSummary;
import com.procuresense.backend.repository.ProductRepository;
import com.procuresense.backend.repository.PurchaseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

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

    @Test
    void loadDemoDataPersistsRecordsAndReturnsSummary() {
        PurchaseSummary summary = demoDataService.loadDemoData();

        assertThat(productRepository.count()).isEqualTo(12);
        assertThat(purchaseRepository.count()).isEqualTo(15);
        assertThat(summary.totalOrders()).isEqualTo(9);
        assertThat(summary.totalLineItems()).isEqualTo(15);
        assertThat(summary.totalQuantity()).isEqualTo(162);
        assertThat(summary.totalRevenue()).isEqualByComparingTo(new BigDecimal("3918.53"));
    }
}
