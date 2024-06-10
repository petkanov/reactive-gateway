package com.example.gateway;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class CachingFilter implements GatewayFilter {
    private final ResponsesCache responsesCache;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String transactionId = exchange.getRequest().getHeaders().getFirst("TransactionId");
        if (transactionId == null) {
            exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
            return exchange.getResponse().setComplete();
        }

        return responsesCache.getResponse(transactionId)
                .doOnNext((a) -> log.info("1) .doOnNext((a) -> " + a))
                .flatMap(response -> writeResponse(exchange, response, chain))
                .doOnNext((a) -> log.info("2) .doOnNext((a) -> " + a))
                .switchIfEmpty(Mono.defer(() -> {
                    exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
                    return exchange.getResponse().setComplete();
                }));
    }

    private Mono<Void> writeResponse(ServerWebExchange exchange, String body, GatewayFilterChain chain) {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer))
                .then(chain.filter(exchange));
    }
}
