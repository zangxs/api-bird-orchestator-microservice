package com.brayanpv.app.infrastructure.web.filter;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Baseline OWASP-recommended response headers for a JSON-only API with no server-rendered views
 * and (currently) no auth layer - applies regardless of that gap being closed later.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersWebFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpResponse response = exchange.getResponse();
        var headers = response.getHeaders();
        headers.add("X-Content-Type-Options", "nosniff");
        headers.add("X-Frame-Options", "DENY");
        headers.add("Content-Security-Policy", "default-src 'none'");
        headers.add("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        return chain.filter(exchange);
    }
}
