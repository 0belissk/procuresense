package com.procuresense.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ProcureSenseApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProcureSenseApplication.class, args);
    }

    @Bean
    public Clock utcClock() {
        return Clock.systemUTC();
    }
}
