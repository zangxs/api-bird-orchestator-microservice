package com.brayanpv.app.infrastructure.web.filter;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Baseline OWASP-recommended response headers for a JSON-only API with no server-rendered views
 * and (currently) no auth layer - applies regardless of that gap being closed later. The
 * Swagger UI / OpenAPI doc pages are the one exception: they're actual HTML pages that load
 * their own inline styles/scripts, so a blanket "default-src 'none'" would leave the UI blank.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersWebFilter implements WebFilter {

    private static final List<String> DOCS_PATH_PREFIXES =
            List.of("/swagger-ui", "/v3/api-docs", "/webjars/swagger-ui");
    private static final String DOCS_CSP =
            "default-src 'self'; style-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-inline'; img-src 'self' data:";
    private static final String API_CSP = "default-src 'none'";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpResponse response = exchange.getResponse();
        var headers = response.getHeaders();
        headers.add("X-Content-Type-Options", "nosniff");
        headers.add("X-Frame-Options", "DENY");
        headers.add("Content-Security-Policy", isDocsPath(exchange) ? DOCS_CSP : API_CSP);
        headers.add("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        return chain.filter(exchange);
    }

    private boolean isDocsPath(ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getPath();
        return DOCS_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }
}
