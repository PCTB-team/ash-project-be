package com.pctb.webapp.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.payos.PayOS;

@Configuration
@Slf4j
public class PayOSConfig {

    @Value("${payos.client-id}")
    private String clientId;

    @Value("${payos.api-key}")
    private String apiKey;

    @Value("${payos.checksum-key}")
    private String checksumKey;

    @Bean
    public PayOS payOS() {

        log.info("Initializing PayOS...");

        if (clientId == null || apiKey == null || checksumKey == null) {
            throw new IllegalStateException("PayOS config is missing in application.properties");
        }

        log.info("PayOS initialized successfully");

        return new PayOS(clientId, apiKey, checksumKey);
    }
}