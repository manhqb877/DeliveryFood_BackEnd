package com.fooddelivery.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Configuration
public class RateLimiterConfig {

    /**
     * KeyResolver theo IP của client (hỗ trợ đọc qua proxy/reverse proxy X-Forwarded-For).
     * Dùng cho chống brute-force đăng nhập, đăng ký, quên mật khẩu (khi chưa có token).
     */
    @Bean(name = "ipKeyResolver")
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                // Lấy IP đầu tiên trong danh sách proxy
                String clientIp = xForwardedFor.split(",")[0].trim();
                return Mono.just(clientIp);
            }

            String xRealIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
            if (xRealIp != null && !xRealIp.isBlank()) {
                return Mono.just(xRealIp.trim());
            }

            InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
            if (remoteAddress != null && remoteAddress.getAddress() != null) {
                return Mono.just(remoteAddress.getAddress().getHostAddress());
            }

            return Mono.just("anonymous_client");
        };
    }

    /**
     * KeyResolver thông minh: Ưu tiên theo Bearer Token (nếu đã đăng nhập),
     * nếu không có Token sẽ fallback về IP client.
     * Chống spam cực kỳ hiệu quả kể cả khi kẻ tấn công dùng rotating proxy/botnet đổi IP.
     */
    @Bean(name = "userOrIpKeyResolver")
    public KeyResolver userOrIpKeyResolver() {
        return exchange -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ") && authHeader.length() > 15) {
                String token = authHeader.substring(7).trim();
                int lastDot = token.lastIndexOf('.');
                String tokenKey = (lastDot != -1 && lastDot < token.length() - 1)
                        ? "token_" + token.substring(lastDot + 1)
                        : "token_" + Math.abs(token.hashCode());
                return Mono.just(tokenKey);
            }

            // Fallback về IP
            String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                return Mono.just("ip_" + xForwardedFor.split(",")[0].trim());
            }

            String xRealIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
            if (xRealIp != null && !xRealIp.isBlank()) {
                return Mono.just("ip_" + xRealIp.trim());
            }

            InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
            if (remoteAddress != null && remoteAddress.getAddress() != null) {
                return Mono.just("ip_" + remoteAddress.getAddress().getHostAddress());
            }

            return Mono.just("anonymous_client");
        };
    }

    /**
     * Rate Limiter chống Brute-Force cho các API xác thực (Auth: Login, Register, Refresh).
     * Giới hạn: 5 request/giây, burst tối đa 10 request.
     */
    @Bean(name = "authRateLimiter")
    public RedisRateLimiter authRateLimiter() {
        return new RedisRateLimiter(5, 10, 1);
    }

    /**
     * Rate Limiter chống Spam cho các API Đặt hàng & Thanh toán & Chat (Orders, Payments, Notifications).
     * Giới hạn: 15 request/giây, burst tối đa 30 request.
     */
    @Bean(name = "orderRateLimiter")
    public RedisRateLimiter orderRateLimiter() {
        return new RedisRateLimiter(15, 30, 1);
    }

    /**
     * Rate Limiter chống Spam cho AI Agent Service (tránh cạn kiệt API token / chi phí gọi LLM).
     * Giới hạn: 5 request/giây, burst tối đa 10 request.
     */
    @Bean(name = "aiAgentRateLimiter")
    public RedisRateLimiter aiAgentRateLimiter() {
        return new RedisRateLimiter(5, 10, 1);
    }

    /**
     * Rate Limiter mặc định cho toàn bộ các endpoint khác (Core, Tracking, Analytics v.v.).
     * Giới hạn: 50 request/giây, burst tối đa 100 request.
     */
    @Bean(name = "defaultRateLimiter")
    @Primary
    public RedisRateLimiter defaultRateLimiter() {
        return new RedisRateLimiter(50, 100, 1);
    }
}
