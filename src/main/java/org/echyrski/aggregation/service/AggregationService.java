package org.echyrski.aggregation.service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

public class AggregationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AggregationService.class);
    private static final int AGGREGATION_TIMEOUT_SEC = 5;
    private static final int AGGREGATE_REQUESTS = 5;
    private static final int RETRY_COUNT = 5;
    private static final int BATCH_PROCESSING_PARALLELISM = 500;
    private static final Duration RESPONSE_SLA = Duration.ofSeconds(10);

    private final WebClient webClient;
    private final String baseUrl;

    private final Sinks.Many<Tuple2<String, ResponseCollector<Double>>> pricingSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<Tuple2<String, ResponseCollector<String>>> trackSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<Tuple2<String, ResponseCollector<List<String>>>> shipmentsSink = Sinks.many().multicast().onBackpressureBuffer();

    private final ReentrantLock pricingSinkLock = new ReentrantLock();
    private final ReentrantLock trackSinkLock = new ReentrantLock();
    private final ReentrantLock shipmentsSinkLock = new ReentrantLock();

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
        return submitRequest(data, shipmentsSink, shipmentsSinkLock);
    }

    public Mono<Map<String, Optional<String>>> submitTrackRequest(List<String> tracks) {
        return submitRequest(tracks, trackSink, trackSinkLock);
    }

    public Mono<Map<String, Optional<Double>>> submitPricingRequest(List<String> pricing) {
        return submitRequest(pricing, pricingSink, pricingSinkLock);
    }

    private <T> Mono<Map<String, Optional<T>>> submitRequest(List<String> data, Sinks.Many<Tuple2<String, ResponseCollector<T>>> sink, ReentrantLock lock) {
        if (CollectionUtils.isEmpty(data)) {
            return Mono.just(Map.of());
        }
        ResponseCollector<T> request = new ResponseCollector<>(data.size());
        lock.lock();
        try {
            for (String elem : data) {
                Sinks.EmitResult res = sink.tryEmitNext(Tuples.of(elem, request));
                if (res != Sinks.EmitResult.OK) {
                    LOGGER.error("Enable to emit into requesting sink: {} {}", elem, res);
                    request.submit(Tuples.of(elem, Optional.empty()));
                }
            }
        } finally {
            lock.unlock();
        }
        return request.asFlux()
                .collectMap(Tuple2::getT1, Tuple2::getT2)
                .onErrorResume(throwable -> Mono.just(data.stream().collect(Collectors.toMap(e -> e, e -> Optional.empty()))));
    }

    private <T> void startSinkProcessing(Sinks.Many<Tuple2<String, ResponseCollector<T>>> sink, String uriTemplate, ParameterizedTypeReference<Map<String, T>> responseType) {
        sink.asFlux()
                .bufferTimeout(AGGREGATE_REQUESTS, Duration.ofSeconds(AGGREGATION_TIMEOUT_SEC))
                .filter(batch -> !batch.isEmpty())
                .parallel(BATCH_PROCESSING_PARALLELISM).runOn(Schedulers.newParallel(uriTemplate))

                .flatMap(batch -> {
                    List<Tuple2<String, ResponseCollector<T>>> requests = List.copyOf(batch);
                    String queries = requests.stream().map(Tuple2::getT1).distinct().collect(Collectors.joining(","));
                    LOGGER.debug("Starting aggregation processing for {}", queries);
                    return webClient.get()
                            .uri(uriTemplate, queries)
                            .retrieve()
                            .bodyToMono(responseType)
                            .retry(RETRY_COUNT)
                            .timeout(RESPONSE_SLA)
                            .onErrorResume(TimeoutException.class, this::processTimeout)
                            .onErrorResume(this::processThrowable)
                            .doOnNext(response -> requests.forEach(request -> processResponse(request.getT1(), response, request.getT2())));
                })
                .subscribe();
    }


    private <T> void processResponse(String request, Map<String, T> response, ResponseCollector<T> responseCollector) {
        responseCollector.submit(Tuples.of(request, Optional.ofNullable(response.get(request))));
    }

    private <T> Mono<Map<String, T>> processTimeout(Throwable error) {
        LOGGER.warn("Timed out while awaiting from the response from downstream service.");
        return Mono.just(Map.of());
    }

    private <T> Mono<Map<String, T>> processThrowable(Throwable error) {
        LOGGER.error(error.getMessage(), error);
        return Mono.just(Map.of());
    }
}
