package org.echyrski.aggregation.controller;

import java.util.List;

import org.echyrski.aggregation.model.AggregationResponse;
import org.echyrski.aggregation.service.AggregationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
public class AggregationController {
    private final AggregationService aggregationService;

    public AggregationController(AggregationService aggregationService) {
        this.aggregationService = aggregationService;

    }

    @GetMapping("/aggregation")
    public Mono<AggregationResponse> aggregate(
            @RequestParam(name = "pricing", required = false) List<String> pricing,
            @RequestParam(name = "track", required = false) List<String> track,
            @RequestParam(name = "shipments", required = false) List<String> shipments) {
        return Mono.zip(
                aggregationService.submitPricingRequest(pricing),
                aggregationService.submitTrackRequest(track),
                aggregationService.submitShipmentRequest(shipments)
        ).map(tuple -> new AggregationResponse(tuple.getT1(), tuple.getT2(), tuple.getT3()));
    }


}
