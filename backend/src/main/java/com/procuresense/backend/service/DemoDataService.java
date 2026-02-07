package com.procuresense.backend.service;

import com.procuresense.backend.config.DemoDataProperties;
import com.procuresense.backend.model.DemoLoadResponse;
import com.procuresense.backend.model.Product;
import com.procuresense.backend.model.Purchase;
import com.procuresense.backend.model.PurchaseSummary;
import com.procuresense.backend.repository.ProductRepository;
import com.procuresense.backend.repository.PurchaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import java.util.stream.Collectors;

@Service
public class DemoDataService {

    private static final Logger log = LoggerFactory.getLogger(DemoDataService.class);

    private final DemoDataProperties properties;
    private final ProductRepository productRepository;
    private final PurchaseRepository purchaseRepository;
    private final ResourceLoader resourceLoader;
    private final PurchaseLoadAuditService purchaseLoadAuditService;

    public DemoDataService(DemoDataProperties properties,
                           ProductRepository productRepository,
                           PurchaseRepository purchaseRepository,
                           ResourceLoader resourceLoader,
                           PurchaseLoadAuditService purchaseLoadAuditService) {
        this.properties = properties;
        this.productRepository = productRepository;
        this.purchaseRepository = purchaseRepository;
        this.resourceLoader = resourceLoader;
        this.purchaseLoadAuditService = purchaseLoadAuditService;
    }

    @Transactional
    public DemoLoadResponse loadDemoData(String orgId) {
        String targetOrg = requireOrgId(orgId);
        log.info("Loading demo data for {} from {} and {}", targetOrg, properties.productsFile(), properties.purchasesFile());
        purchaseRepository.deleteByOrgId(targetOrg);

        Map<String, Product> products = loadProducts();
        productRepository.saveAll(products.values());
        List<Purchase> purchases = loadPurchases(products, targetOrg);
        purchaseRepository.saveAll(purchases);
        if (!purchases.isEmpty()) {
            purchaseLoadAuditService.markLoaded(targetOrg);
        }
        return new DemoLoadResponse(targetOrg, purchases.size());
    }

    public PurchaseSummary getSummary(String orgId) {
        return buildSummary(requireOrgId(orgId));
    }

    private Map<String, Product> loadProducts() {
        List<String[]> rows = readCsv(properties.productsFile());
        Map<String, Product> products = new HashMap<>();
        for (String[] row : rows) {
            String sku = row[0];
            Product product = productRepository.findBySku(sku).orElseGet(Product::new);
            product.setSku(sku);
            product.setName(row[1]);
            product.setCategory(row[2]);
            product.setUnitPrice(new BigDecimal(row[3]));
            products.put(product.getSku(), product);
        }
        return products;
    }

    private List<Purchase> loadPurchases(Map<String, Product> products, String orgId) {
        List<String[]> rows = readCsv(properties.purchasesFile());
        return rows.stream().map(row -> {
            String sku = row[1];
            Product product = products.get(sku);
            if (product == null) {
                throw new IllegalStateException("Missing product for sku " + sku);
            }
            Purchase purchase = new Purchase();
            purchase.setOrgId(orgId);
            purchase.setOrderId(row[0]);
            purchase.setProduct(product);
            purchase.setQuantity(Integer.parseInt(row[2]));
            purchase.setUnitPrice(new BigDecimal(row[3]));
            purchase.setPurchasedAt(OffsetDateTime.parse(row[4]));
            return purchase;
        }).collect(Collectors.toList());
    }

    private List<String[]> readCsv(String location) {
        Resource resource = resourceLoader.getResource(location);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines()
                    .skip(1) // header
                    .filter(line -> !line.isBlank())
                    .map(line -> line.split(","))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read demo data from " + location, e);
        }
    }

    private PurchaseSummary buildSummary(String orgId) {
        long orders = purchaseRepository.countDistinctOrdersByOrg(orgId);
        long lineItems = purchaseRepository.countByOrgId(orgId);
        long quantity = purchaseRepository.sumQuantitiesByOrg(orgId);
        long totalSkus = purchaseRepository.countDistinctSkusByOrg(orgId);
        BigDecimal revenue = java.util.Optional.ofNullable(purchaseRepository.sumRevenueByOrg(orgId)).orElse(BigDecimal.ZERO);
        var minDate = purchaseRepository.findFirstPurchaseDate(orgId);
        var maxDate = purchaseRepository.findLastPurchaseDate(orgId);
        PurchaseSummary.DateRange range = (minDate != null && maxDate != null)
                ? new PurchaseSummary.DateRange(minDate, maxDate)
                : null;
        return new PurchaseSummary(orgId, orders, lineItems, quantity, revenue, totalSkus, range,
                purchaseLoadAuditService.getLastLoadedAt(orgId));
    }

    private String requireOrgId(String orgId) {
        if (!StringUtils.hasText(orgId)) {
            throw new IllegalArgumentException("X-Org-Id header is required");
        }
        return orgId.trim();
    }
}
