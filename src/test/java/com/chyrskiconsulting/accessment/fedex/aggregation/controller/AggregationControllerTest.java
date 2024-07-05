package com.chyrskiconsulting.accessment.fedex.aggregation.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.chyrskiconsulting.accessment.fedex.aggregation.model.AggregationResponse;
import com.chyrskiconsulting.accessment.fedex.aggregation.service.AggregationService;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class AggregationControllerTest {

    @Mock
    private AggregationService aggregationService;

    @InjectMocks
    private AggregationController aggregationController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void aggregate_Success() {
        // Mock the responses from the AggregationService
        when(aggregationService.submitPricingRequest(anyList()))
                .thenReturn(Mono.just(Map.of("109347263", Optional.of(100.0))));
        when(aggregationService.submitTrackRequest(anyList()))
                .thenReturn(Mono.just(Map.of("109347263", Optional.of("NEW"))));
        when(aggregationService.submitShipmentRequest(anyList()))
                .thenReturn(Mono.just(Map.of("109347263", Optional.of(List.of("box", "pallet")))));

        // Define input parameters
        List<String> pricing = List.of("109347263");
        List<String> track = List.of("109347263");
        List<String> shipments = List.of("109347263");

        // Call the aggregate method and verify the result
        Mono<AggregationResponse> result = aggregationController.aggregate(pricing, track, shipments);

        StepVerifier.create(result)
                .expectNextMatches(response ->
                        response.pricing().equals(Map.of("109347263", Optional.of(100.0))) &&
                                response.track().equals(Map.of("109347263", Optional.of("NEW"))) &&
                                response.shipments().equals(Map.of("109347263", Optional.of(List.of("box", "pallet"))))
                )
                .expectComplete()
                .verify();
    }

    @Test
    void aggregate_EmptyParameters() {
        // Mock the responses from the AggregationService for empty inputs
        when(aggregationService.submitPricingRequest(anyList()))
                .thenReturn(Mono.just(Map.of()));
        when(aggregationService.submitTrackRequest(anyList()))
                .thenReturn(Mono.just(Map.of()));
        when(aggregationService.submitShipmentRequest(anyList()))
                .thenReturn(Mono.just(Map.of()));

        // Define empty input parameters
        List<String> pricing = List.of();
        List<String> track = List.of();
        List<String> shipments = List.of();

        // Call the aggregate method and verify the result
        Mono<AggregationResponse> result = aggregationController.aggregate(pricing, track, shipments);

        StepVerifier.create(result)
                .expectNextMatches(response ->
                        response.pricing().isEmpty() &&
                                response.track().isEmpty() &&
                                response.shipments().isEmpty()
                )
                .expectComplete()
                .verify();
    }

    @Test
    void aggregate_NullParameters() {
        // Mock the responses from the AggregationService for null inputs
        when(aggregationService.submitPricingRequest(null))
                .thenReturn(Mono.just(Map.of()));
        when(aggregationService.submitTrackRequest(null))
                .thenReturn(Mono.just(Map.of()));
        when(aggregationService.submitShipmentRequest(null))
                .thenReturn(Mono.just(Map.of()));

        // Call the aggregate method with null parameters and verify the result
        Mono<AggregationResponse> result = aggregationController.aggregate(null, null, null);

        StepVerifier.create(result)
                .expectNextMatches(response ->
                        response.pricing().isEmpty() &&
                                response.track().isEmpty() &&
                                response.shipments().isEmpty()
                )
                .expectComplete()
                .verify();
    }


}
