package com.procuresense.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "demo.data")
public record DemoDataProperties(String productsFile, String purchasesFile) {
}
