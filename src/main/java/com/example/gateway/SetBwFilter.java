package com.example.gateway;

import io.netty.handler.ssl.SslContextBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.X509TrustManager;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;

@Slf4j
@Component
public class SetBwFilter implements GatewayFilter {
    private final WebClient webClient;
    private final Config config;

    public SetBwFilter(Config config) {
        this.webClient = webClient();
        this.config = config;
    }
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return webClient.get()
                .uri("https://192.168.1.12/api/subscriber?id=1")
                .retrieve()
                .bodyToMono(String.class)
                .elapsed()
                .doOnNext(tuple -> log.info("First call took: " + tuple.getT1() + " ms"))
                .flatMap(tuple -> {
                    String response1 = tuple.getT2();
                    String secondUrl = "https://192.168.1.12/api/qci?_offset=0&_limit=20&_order=asc";
                    return webClient.get()
                            .uri(secondUrl)
                            .retrieve()
                            .bodyToMono(String.class)
                            .elapsed()
                            .doOnNext(tuple2 -> log.info("Second call took: " + tuple2.getT1() + " ms"))
                            .map(tuple2 -> combineResponses(response1, tuple2.getT2()));
                })
                .flatMap(response2 -> {
                    String thirdUrl = "https://192.168.1.12/api/pcrf_pcc_rule";
                    return webClient.post()
                            .uri(thirdUrl)
                            .retrieve()
                            .bodyToMono(String.class)
                            .elapsed()
                            .doOnNext(tuple2 -> log.info("Third call took: " + tuple2.getT1() + " ms"))
                            .map(tuple2 -> combineResponses(response2, tuple2.getT2()));
                })
                .flatMap(response3 -> {
                    String thirdUrl = "https://192.168.1.12/api/pcrf_pcc_rule?id=3271";
                    return webClient.get()
                            .uri(thirdUrl)
                            .retrieve()
                            .bodyToMono(String.class)
                            .elapsed()
                            .doOnNext(tuple2 -> log.info("Forth call took: " + tuple2.getT1() + " ms"))
                            .map(tuple2 -> combineResponses(response3, tuple2.getT2()));
                })
                .flatMap(response3 -> {
                    String thirdUrl = "https://192.168.1.12/api/pcrf_pcc_rule?id=3271";
                    return webClient.patch()
                            .uri(thirdUrl)
                            .retrieve()
                            .bodyToMono(String.class)
                            .elapsed()
                            .doOnNext(tuple2 -> log.info("Fifth call took: " + tuple2.getT1() + " ms"))
                            .map(tuple2 -> combineResponses(response3, tuple2.getT2()));
                })
                .flatMap(combinedResponse -> writeResponse(exchange, combinedResponse))
                .then(chain.filter(exchange));
    }

    private String combineResponses(String response1, String response2) {
        return response1 + " and " + response2;
    }

    private Mono<Void> writeResponse(ServerWebExchange exchange, String body) {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    public WebClient webClient() {
        SslContextBuilder sslContextBuilder = SslContextBuilder
                .forClient()
                .trustManager(new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                });
        HttpClient httpClient = HttpClient.create()
                .secure(sslContextSpec -> sslContextSpec.sslContext(sslContextBuilder));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
