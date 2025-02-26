package com.example.exambuddy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import vn.payos.PayOS;

@Component
public class PayOSConfig {
    private final String clientId = "aae60675-76d3-4fdf-897e-c6d2c8f4207f";
    private final String apiKey = "fabf143e-0d7b-4712-93e2-be7014ee2f39";
    private final String checksumKey = "780d39bae9a6bb0a1937fcd4d3e26e6eb8b4e3e504cf942df450daeaba763576";

    @Bean
    public PayOS payOS() {
        return new PayOS(clientId, apiKey, checksumKey);
    }
}
