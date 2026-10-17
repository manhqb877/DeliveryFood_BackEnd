package com.fooddelivery.apigateway.config;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitExceptionHandler implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpResponse originalResponse = exchange.getResponse();
        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> setComplete() {
                if (getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                    if (!getHeaders().containsKey("Content-Type")) {
                        getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    }
                    getHeaders().set("Retry-After", "5");

                    String errorJson = String.format(
                            "{\"status\":429,\"error\":\"TOO_MANY_REQUESTS\",\"message\":\"Bạn đã gửi quá nhiều yêu cầu (vượt quá giới hạn chống spam / brute-force). Vui lòng thử lại sau vài giây.\",\"timestamp\":\"%s\"}",
                            Instant.now()
                    );
                    DataBuffer buffer = bufferFactory().wrap(errorJson.getBytes(StandardCharsets.UTF_8));
                    return writeWith(Mono.just(buffer));
                }
                return super.setComplete();
            }
        };

        return chain.filter(exchange.mutate().response(decoratedResponse).build());
    }
}

