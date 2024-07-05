package com.chyrskiconsulting.accessment.fedex.aggregation.service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

public class AggregationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AggregationService.class);
    private static final int AGGREGATION_TIMEOUT_SEC = 5;
    private static final int AGGREGATE_REQUESTS = 5;
    private final WebClient webClient;
    private final String baseUrl;

    private final Sinks.Many<Tuple2<String, ResponseCollector<Double>>> pricingSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<Tuple2<String, ResponseCollector<String>>> trackSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<Tuple2<String, ResponseCollector<List<String>>>> shipmentsSink = Sinks.many().multicast().onBackpressureBuffer();

    public AggregationService(WebClient.Builder webClientBuilder, String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.baseUrl = baseUrl;
    }

    @PostConstruct
    public void init() {
        LOGGER.info("Initializing aggregation service to consume from {}", baseUrl);
        startSinkProcessing(pricingSink, "/pricing?q={queries}", new ParameterizedTypeReference<>() {
        });
        startSinkProcessing(trackSink, "/track?q={queries}", new ParameterizedTypeReference<>() {
        });
        startSinkProcessing(shipmentsSink, "/shipments?q={queries}", new ParameterizedTypeReference<>() {
        });
    }


    public Mono<Map<String, Optional<List<String>>>> submitShipmentRequest(List<String> data) {
        return submitRequest(data, shipmentsSink);
    }

    public Mono<Map<String, Optional<String>>> submitTrackRequest(List<String> tracks) {
        return submitRequest(tracks, trackSink);
    }

    public Mono<Map<String, Optional<Double>>> submitPricingRequest(List<String> pricing) {
        return submitRequest(pricing, pricingSink);
    }

    private <T> Mono<Map<String, Optional<T>>> submitRequest(List<String> data, Sinks.Many<Tuple2<String, ResponseCollector<T>>> sink) {
        if (CollectionUtils.isEmpty(data)) {
            return Mono.just(Map.of());
        }
        ResponseCollector<T> request = new ResponseCollector<>(data.size());
        data.forEach(s -> sink.tryEmitNext(Tuples.of(s, request)));
        return request.asFlux()
                .collectMap(Tuple2::getT1, Tuple2::getT2)
                .onErrorResume(throwable -> Mono.just(data.stream().collect(Collectors.toMap(e -> e, e -> Optional.empty()))));
    }

    private <T> void startSinkProcessing(Sinks.Many<Tuple2<String, ResponseCollector<T>>> sink, String uriTemplate, ParameterizedTypeReference<Map<String, T>> responseType) {
        sink.asFlux()
                .bufferTimeout(AGGREGATE_REQUESTS, Duration.ofSeconds(AGGREGATION_TIMEOUT_SEC))
                .filter(batch -> !batch.isEmpty())
                .flatMap(batch -> {
                    List<Tuple2<String, ResponseCollector<T>>> requests = List.copyOf(batch);
                    String queries = requests.stream().map(Tuple2::getT1).collect(Collectors.joining(","));
                    return webClient.get()
                            .uri(uriTemplate, queries)
                            .retrieve()
                            .bodyToMono(responseType)
                            .onErrorResume(this::processOnError)
                            .doOnNext(response -> requests.forEach(request -> processResponse(request.getT1(), response, request.getT2())));
                })
                .subscribe();
    }


    private <T> void processResponse(String request, Map<String, T> response, ResponseCollector<T> responseCollector) {
        responseCollector.submit(Tuples.of(request, Optional.ofNullable(response.get(request))));
    }

    private <T> Mono<Map<String, T>> processOnError(Throwable error) {
        LOGGER.error(error.getMessage(), error);
        return Mono.just(Map.of());
    }
}
