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
public class Z_AccessQueuedRequestsFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.warn("==================== Z_AccessQueuedRequestsFilter");
//        return chain.filter(exchange)
//                .contextWrite(Context.of("queuedRequests", exchange.getAttributes().get("queuedRequests")))
//                .doOnSuccess(empty -> {
//                    Object as = Mono.deferContextual();
//                    // Access the queued requests
//                    ConcurrentLinkedQueue<ServerWebExchange> queuedRequests = exchange.getAttribute("queuedRequests");
//                    if (queuedRequests != null) {
//                        queuedRequests.forEach(request -> {
//                            // Do something with the queued requests
//                            System.out.println("Queued request: " + request.getRequest().getURI());
//                        });
//                    }
//                });
        return Mono.deferContextual(contextView -> {
            ConcurrentLinkedQueue<ServerWebExchange> queue = contextView.getOrDefault("queuedRequests", new ConcurrentLinkedQueue<>());

            return chain.filter(exchange);
        });
    }

    @Override
    public int getOrder() {
        return Integer.MAX_VALUE;
    }
}
