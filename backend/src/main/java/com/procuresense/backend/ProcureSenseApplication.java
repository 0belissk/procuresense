package com.procuresense.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ProcureSenseApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProcureSenseApplication.class, args);
    }
}
