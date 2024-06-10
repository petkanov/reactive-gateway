package com.example.gateway;

import io.netty.handler.ssl.SslContextBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

@RestController
@SpringBootApplication
public class GatewayApplication {
	@Autowired
	private CachingFilter cachingFilter;
	@Autowired
	private DuplicateRequestFilter duplicateRequestFilter;
	@Autowired
	private SetBwFilter setBwFilter;

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}

	@Bean
	public RouteLocator myRoutes(RouteLocatorBuilder builder, DuplicateRequestFilter duplicateRequestFilter) {
		return builder.routes()
				.route(p -> p
						.path("/gateway-splitter/**")
						.filters(f -> f.filter(duplicateRequestFilter))
						.uri("http://dummyuri"))
				.route("core-route", r -> r.path("/gateway-core/**")
						.filters(f -> f.filter(cachingFilter))
						.uri("http://dummyuri"))
				.route("setBw-route", r -> r.path("/gateway-setbw/**")
						.filters(f -> f.filter(setBwFilter))
						.uri("http://dummyuri"))
				.build();
	}
}
