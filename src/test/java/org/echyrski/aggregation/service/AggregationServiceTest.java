package org.echyrski.aggregation.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings({"unchecked"})
public class AggregationServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Mock
    private WebClient.Builder webClientBuilder;
    @Mock
    private WebClient webClient;
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    private WebClient.ResponseSpec responseSpec;
    private AggregationService aggregationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.clientConnector(any())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        aggregationService = new AggregationService(webClientBuilder, "http://localhost:8080/");
        aggregationService.init();
    }

    @Test
    void submitTrackRequest_Success() throws IOException {
        // Load test input and expected output from JSON file
        InputStream inputStream = getClass().getResourceAsStream("/submitTrackRequest.json");
        Map<String, Object> jsonMap = objectMapper.readValue(inputStream, Map.class);
        List<String> input = (List<String>) jsonMap.get("input");
        Map<String, String> expectedOutput = (Map<String, String>) jsonMap.get("output");

        // Mock response from WebClient
        when(responseSpec.bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {
        }))
                .thenReturn(Mono.just(expectedOutput));


        Mono<Map<String, Optional<String>>> resultMono = aggregationService.submitTrackRequest(input);

        StepVerifier.create(resultMono)
                .expectNextMatches(response -> response.equals(convertToOptionalMap(expectedOutput)))
                .expectComplete()
                .verify();
    }

    @Test
    void submitTrackRequest_EmptyInput() {
        Mono<Map<String, Optional<String>>> resultMono = aggregationService.submitTrackRequest(List.of());

        StepVerifier.create(resultMono)
                .expectNext(Map.of())
                .expectComplete()
                .verify();
    }

    @Test
    void submitTrackRequest_NullInput() {
        Mono<Map<String, Optional<String>>> resultMono = aggregationService.submitTrackRequest(null);

        StepVerifier.create(resultMono)
                .expectNext(Map.of())
                .expectComplete()
                .verify();
    }

    @Test
    void submitShipmentRequest_Success() throws IOException {
        // Load test input and expected output from JSON file
        InputStream inputStream = getClass().getResourceAsStream("/submitShipmentRequest.json");
        Map<String, Object> jsonMap = objectMapper.readValue(inputStream, Map.class);
        List<String> input = (List<String>) jsonMap.get("input");
        Map<String, List<String>> expectedOutput = (Map<String, List<String>>) jsonMap.get("output");

        // Mock response from WebClient
        when(responseSpec.bodyToMono(new ParameterizedTypeReference<Map<String, List<String>>>() {
        }))
                .thenReturn(Mono.just(expectedOutput));

        aggregationService.init();
        Mono<Map<String, Optional<List<String>>>> resultMono = aggregationService.submitShipmentRequest(input);

        StepVerifier.create(resultMono)
                .expectNextMatches(response -> response.equals(convertToOptionalMap(expectedOutput)))
                .expectComplete()
                .verify();
    }

    @Test
    void submitShipmentRequest_EmptyInput() {
        Mono<Map<String, Optional<List<String>>>> resultMono = aggregationService.submitShipmentRequest(List.of());

        StepVerifier.create(resultMono)
                .expectNext(Map.of())
                .expectComplete()
                .verify();
    }

    @Test
    void submitShipmentRequest_NullInput() {
        Mono<Map<String, Optional<List<String>>>> resultMono = aggregationService.submitShipmentRequest(null);

        StepVerifier.create(resultMono)
                .expectNext(Map.of())
                .expectComplete()
                .verify();
    }

    @Test
    void submitPricingRequest_Success() throws IOException {
        // Load test input and expected output from JSON file
        InputStream inputStream = getClass().getResourceAsStream("/submitPricingRequest.json");
        Map<String, Object> jsonMap = objectMapper.readValue(inputStream, Map.class);
        List<String> input = (List<String>) jsonMap.get("input");
        Map<String, Double> expectedOutput = (Map<String, Double>) jsonMap.get("output");

        // Mock response from WebClient
        when(responseSpec.bodyToMono(new ParameterizedTypeReference<Map<String, Double>>() {
        }))
                .thenReturn(Mono.just(expectedOutput));

        aggregationService.init();
        Mono<Map<String, Optional<Double>>> resultMono = aggregationService.submitPricingRequest(input);

        StepVerifier.create(resultMono)
                .expectNextMatches(response -> response.equals(convertToOptionalMap(expectedOutput)))
                .expectComplete()
                .verify();
    }

    @Test
    void submitPricingRequest_Exception() throws IOException {
        // Load test input and expected output from JSON file
        InputStream inputStream = getClass().getResourceAsStream("/submitPricingRequestError.json");
        Map<String, Object> jsonMap = objectMapper.readValue(inputStream, Map.class);
        List<String> input = (List<String>) jsonMap.get("input");
        Map<String, Double> expectedOutput = (Map<String, Double>) jsonMap.get("output");

        // Mock response from WebClient
        when(responseSpec.bodyToMono(new ParameterizedTypeReference<Map<String, Double>>() {
        }))
                .thenReturn(Mono.error(new IOException()));

        aggregationService.init();
        Mono<Map<String, Optional<Double>>> resultMono = aggregationService.submitPricingRequest(input);

        StepVerifier.create(resultMono)
                .expectNextMatches(response -> response.equals(convertToOptionalMap(expectedOutput)))
                .expectComplete()
                .verify();
    }

    @Test
    void submitPricingRequest_EmptyInput() {
        Mono<Map<String, Optional<Double>>> resultMono = aggregationService.submitPricingRequest(List.of());

        StepVerifier.create(resultMono)
                .expectNext(Map.of())
                .expectComplete()
                .verify();
    }

    @Test
    void submitPricingRequest_NullInput() {
        Mono<Map<String, Optional<Double>>> resultMono = aggregationService.submitPricingRequest(null);

        StepVerifier.create(resultMono)
                .expectNext(Map.of())
                .expectComplete()
                .verify();
    }

    private <T> Map<String, Optional<T>> convertToOptionalMap(Map<String, T> map) {
        return map.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> Optional.ofNullable(e.getValue())));
    }


}
