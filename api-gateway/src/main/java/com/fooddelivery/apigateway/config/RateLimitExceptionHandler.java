package com.fooddelivery.apigateway.config;

import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
@Order(-1)
public class RateLimitExceptionHandler implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return chain.filter(exchange).then(Mono.defer(() -> {
            if (exchange.getResponse().getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                exchange.getResponse().getHeaders().set("Retry-After", "5");

                String errorJson = String.format(
                        "{\"status\":429,\"error\":\"TOO_MANY_REQUESTS\",\"message\":\"Bạn đã gửi quá nhiều yêu cầu (vượt quá giới hạn chống spam / brute-force). Vui lòng thử lại sau vài giây.\",\"timestamp\":\"%s\"}",
                        Instant.now()
                );
                DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(errorJson.getBytes(StandardCharsets.UTF_8));
                return exchange.getResponse().writeWith(Mono.just(buffer));
            }
            return Mono.empty();
        }));
    }
}
