package com.chyrskiconsulting.accessment.fedex.aggregation.service;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.util.function.Tuple2;

public class ResponseCollector<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseCollector.class);
    private final AtomicInteger processed = new AtomicInteger();
    private final Sinks.Many<Tuple2<String, Optional<T>>> sink = Sinks.many().unicast().onBackpressureBuffer();
    private final int expectedCount;

    public ResponseCollector(int expectedCount) {
        LOGGER.debug("Collecter started with expectedCount={}", expectedCount);
        this.expectedCount = expectedCount;
    }

    public void submit(Tuple2<String, Optional<T>> rec) {
        Sinks.EmitResult emitresult = sink.tryEmitNext(rec);
        if (emitresult != Sinks.EmitResult.OK) {
            LOGGER.debug("Request processing error for record {}:{}. Unable to emit  next:{}", rec.getT1(), rec.getT2(), emitresult);
            LOGGER.debug("Collector complete abnormally expectedCount={}", expectedCount);

            sink.tryEmitComplete();
            return;
        }
        if (processed.addAndGet(1) == expectedCount) {
            LOGGER.debug("Collector complete normally expectedCount={}", expectedCount);
            sink.tryEmitComplete();
        }
    }

    public Flux<Tuple2<String, Optional<T>>> asFlux() {
        return sink.asFlux();
    }
}
