package com.example.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class DuplicateRequestFilter implements GatewayFilter {
    private final WebClient webClient;
    private final Config config;
    public DuplicateRequestFilter(Config config) {
        this.webClient = WebClient.builder().build();
        this.config = config;
    }

    private Mono<String> forwardRequest(String uri, String transactionId) {
        return webClient.get()
                .uri(URI.create(uri))
                .header("TransactionId", transactionId)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(e -> {
                    log.error("Failed to retrieve from " + uri + ": " + e.getMessage());
                    return Mono.empty();
                });
    }

    private Mono<Void> writeResponse(ServerWebExchange exchange, String body) {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        final String transactionId = UUID.randomUUID().toString();

        List<Mono<String>> requestResponses = new ArrayList<>();
        for (String apiRouter : config.getConfiguredApiRouters()) {
            Mono<String> response = forwardRequest(apiRouter, transactionId);
            requestResponses.add(response);
        }

        return Mono.firstWithSignal(requestResponses)
                .flatMap(response -> writeResponse(exchange, response))
                .switchIfEmpty(Mono.defer(() -> {
                    exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
                    return exchange.getResponse().setComplete();
                }))
                .then(chain.filter(exchange));
    }
}
