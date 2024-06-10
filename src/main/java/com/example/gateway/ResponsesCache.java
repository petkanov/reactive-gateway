package com.example.gateway;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResponsesCache {
    private final Map<String, Sinks.One<String>> cache = new ConcurrentHashMap<>();

    private final Config config;

    private final WebClient webClient = WebClient.builder().build();
    private final Scheduler scheduler = Schedulers.boundedElastic();

    public Mono<String> getResponse(String transactionId) {
        return cache.computeIfAbsent(transactionId, tid -> {
            Sinks.One<String> sink = Sinks.one();
            callBackendService().subscribe(
                    sink::tryEmitValue,
                    sink::tryEmitError,
                    () -> {}
            );
            return sink;
        }).asMono();
    }

    public Mono<String> callBackendService() {
        final String uri = config.getCoreUri();

        return webClient.get()
                .uri(URI.create(uri))
                .header("X-My-Header", "MyValue")
                .retrieve()
                .bodyToMono(String.class)
                .subscribeOn(scheduler)
                .doOnNext(a -> log.info("callBackendService() CORE CALLED"))
                .onErrorResume(e -> {
                    log.error("Failed to retrieve from " + uri + ": " + e.getMessage());
                    return Mono.empty();
                });
    }

    public void saveResponse(String transactionId, String response) {
        Sinks.One<String> sink = cache.get(transactionId);
        if (sink != null) {
            sink.tryEmitValue(response).orThrow();
        }
    }

    public void removeResponse(String transactionId) {
        cache.remove(transactionId);
    }










//    private final Map<String, MonoProcessor<String>> cache = new ConcurrentHashMap<>();
//
//    public Mono<String> getResponse(String transactionId) {
//        return cache.computeIfAbsent(transactionId, tid -> {
//            MonoProcessor<String> processor = MonoProcessor.create();
//            // This will hold the place in the cache until the actual call is completed
//            return processor;
//        });
//    }
//
//    public void saveResponse(String transactionId, String response) {
//        cache.get(transactionId).onNext(response);
//        cache.get(transactionId).onComplete();
//    }
//
//    public void removeResponse(String transactionId) {
//        cache.remove(transactionId);
//    }




//    private final Map<String, Sinks.One<String>> cache = new ConcurrentHashMap<>();
//
//    public Mono<String> getResponse(String transactionId) {
//        return cache.computeIfAbsent(transactionId, tid -> {
//            Sinks.One<String> sink = Sinks.one();
//            // This will hold the place in the cache until the actual call is completed
//            return sink;
//        }).asMono();
//    }
//
//    public void saveResponse(String transactionId, String response) {
//        Sinks.One<String> sink = cache.get(transactionId);
//        if (sink != null) {
//            sink.tryEmitValue(response).orThrow();
//            sink.asMono().tryEmitComplete().orThrow();
//        }
//    }
//
//    public void removeResponse(String transactionId) {
//        cache.remove(transactionId);
//    }
}
