package com.chyrskiconsulting.accessment.fedex.aggregation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import com.chyrskiconsulting.accessment.fedex.aggregation.service.AggregationService;

@Configuration
public class AggregationConfiguration {

    @Value("${SERVICES_HOST:http://localhost:8080}")
    private String serviceHost;

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public AggregationService aggregationService() {
        return new AggregationService(webClientBuilder(), serviceHost);
    }
}
