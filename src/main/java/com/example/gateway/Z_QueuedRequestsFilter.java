package com.example.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Component
public class Z_QueuedRequestsFilter implements GlobalFilter, Ordered {
    private final ConcurrentLinkedQueue<ServerWebExchange> queuedRequests = new ConcurrentLinkedQueue<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.warn("==================== Z_QueuedRequestsFilter");
        queuedRequests.offer(exchange);

        return chain.filter(exchange)
                .doOnSuccess(empty -> queuedRequests.remove(exchange))
                .doOnError(error -> queuedRequests.remove(exchange))
                .contextWrite(Context.of("queuedRequests", queuedRequests));
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
