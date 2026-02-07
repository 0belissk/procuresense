package com.procuresense.backend.service;

import com.procuresense.backend.model.Product;
import com.procuresense.backend.model.Purchase;
import com.procuresense.backend.model.PurchaseImportResponse;
import com.procuresense.backend.repository.ProductRepository;
import com.procuresense.backend.repository.PurchaseRepository;
import com.procuresense.backend.service.exception.CsvValidationException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class PurchaseImportService {

    private static final Set<String> REQUIRED_HEADERS = Set.of(
            "order_id",
            "sku",
            "product_name",
            "category",
            "quantity",
            "unit_price",
            "purchased_at"
    );
    private static final int MAX_SAMPLE_ERRORS = 5;

    private final ProductRepository productRepository;
    private final PurchaseRepository purchaseRepository;
    private final PurchaseLoadAuditService purchaseLoadAuditService;

    public PurchaseImportService(ProductRepository productRepository,
                                 PurchaseRepository purchaseRepository,
                                 PurchaseLoadAuditService purchaseLoadAuditService) {
        this.productRepository = productRepository;
        this.purchaseRepository = purchaseRepository;
        this.purchaseLoadAuditService = purchaseLoadAuditService;
    }

    @Transactional
    public PurchaseImportResponse importCsv(MultipartFile file, String orgId) {
        if (!StringUtils.hasText(orgId)) {
            throw new IllegalArgumentException("X-Org-Id header is required");
        }
        if (file == null || file.isEmpty()) {
            throw new CsvValidationException("Upload file is required");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {

            Map<String, String> headerLookup = normalizeHeaders(parser.getHeaderMap());
            validateHeaders(headerLookup.keySet());

            Map<String, Product> productCache = new HashMap<>();
            Map<String, Product> newProducts = new LinkedHashMap<>();
            List<Purchase> purchases = new ArrayList<>();
            List<String> sampleErrors = new ArrayList<>();
            int imported = 0;
            int rejected = 0;

            for (CSVRecord record : parser) {
                try {
                    Row row = toRow(record, headerLookup);
                    Product product = resolveProduct(row, productCache, newProducts);
                    purchases.add(toPurchase(row, product, orgId));
                    imported++;
                } catch (IllegalArgumentException ex) {
                    rejected++;
                    if (sampleErrors.size() < MAX_SAMPLE_ERRORS) {
                        sampleErrors.add("Row " + record.getRecordNumber() + ": " + ex.getMessage());
                    }
                }
            }

            if (!newProducts.isEmpty()) {
                productRepository.saveAll(newProducts.values());
            }
            if (!purchases.isEmpty()) {
                purchaseRepository.saveAll(purchases);
            }
            if (imported > 0) {
                purchaseLoadAuditService.markLoaded(orgId);
            }
            return new PurchaseImportResponse(imported, rejected, sampleErrors);
        } catch (IOException e) {
            throw new CsvValidationException("Unable to read uploaded CSV: " + e.getMessage());
        }
    }

    private Map<String, String> normalizeHeaders(Map<String, Integer> headerMap) {
        Map<String, String> normalized = new HashMap<>();
        headerMap.keySet().forEach(original -> {
            String key = original == null ? "" : original.trim().toLowerCase(Locale.US);
            if (StringUtils.hasText(key)) {
                normalized.put(key, original);
            }
        });
        return normalized;
    }

    private void validateHeaders(Set<String> actualHeaders) {
        Set<String> missing = new HashSet<>(REQUIRED_HEADERS);
        missing.removeAll(actualHeaders);
        if (!missing.isEmpty()) {
            throw new CsvValidationException("Missing required columns: " + String.join(", ", missing));
        }
    }

    private Row toRow(CSVRecord record, Map<String, String> headers) {
        String orderId = getValue(record, headers.get("order_id"));
        String sku = getValue(record, headers.get("sku"));
        String productName = getValue(record, headers.get("product_name"));
        String category = getValue(record, headers.get("category"));
        String quantityValue = getValue(record, headers.get("quantity"));
        String unitPriceValue = getValue(record, headers.get("unit_price"));
        String purchasedAtValue = getValue(record, headers.get("purchased_at"));

        if (!StringUtils.hasText(orderId)) {
            throw new IllegalArgumentException("order_id is required");
        }
        if (!StringUtils.hasText(sku)) {
            throw new IllegalArgumentException("sku is required");
        }
        if (!StringUtils.hasText(productName)) {
            throw new IllegalArgumentException("product_name is required");
        }
        if (!StringUtils.hasText(category)) {
            throw new IllegalArgumentException("category is required");
        }

        int quantity;
        try {
            quantity = Integer.parseInt(quantityValue);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("quantity must be an integer");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }

        BigDecimal unitPrice;
        try {
            unitPrice = new BigDecimal(unitPriceValue);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("unit_price must be numeric");
        }
        if (unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("unit_price must be greater than zero");
        }

        OffsetDateTime purchasedAt;
        try {
            purchasedAt = OffsetDateTime.parse(purchasedAtValue);
        } catch (Exception e) {
            throw new IllegalArgumentException("purchased_at must be ISO-8601");
        }

        return new Row(orderId, sku, productName, category, quantity, unitPrice, purchasedAt);
    }

    private Product resolveProduct(Row row,
                                   Map<String, Product> productCache,
                                   Map<String, Product> pendingNewProducts) {
        return productCache.computeIfAbsent(row.sku(), sku -> {
            Product existing = productRepository.findBySku(sku).orElse(null);
            if (existing != null) {
                existing.setName(row.productName());
                existing.setCategory(row.category());
                existing.setUnitPrice(row.unitPrice());
                return existing;
            }
            Product product = new Product();
            product.setSku(row.sku());
            product.setName(row.productName());
            product.setCategory(row.category());
            product.setUnitPrice(row.unitPrice());
            pendingNewProducts.putIfAbsent(row.sku(), product);
            return product;
        });
    }

    private Purchase toPurchase(Row row, Product product, String orgId) {
        Purchase purchase = new Purchase();
        purchase.setOrgId(orgId);
        purchase.setOrderId(row.orderId());
        purchase.setProduct(product);
        purchase.setQuantity(row.quantity());
        purchase.setUnitPrice(row.unitPrice());
        purchase.setPurchasedAt(row.purchasedAt());
        return purchase;
    }

    private String getValue(CSVRecord record, String columnName) {
        if (!StringUtils.hasText(columnName)) {
            return null;
        }
        return record.get(columnName);
    }

    private record Row(String orderId,
                       String sku,
                       String productName,
                       String category,
                       int quantity,
                       BigDecimal unitPrice,
                       OffsetDateTime purchasedAt) {
    }
}
