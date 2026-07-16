package com.brayanpv.app.infrastructure.web.filter;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityHeadersWebFilterTest {

    private final SecurityHeadersWebFilter filter = new SecurityHeadersWebFilter();

    @Test
    void filter_addsBaselineSecurityHeadersToEveryResponse() {
        HttpHeaders headers = exerciseFilter("/bird/detect");

        assertThat(headers.getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(headers.getFirst("X-Frame-Options")).isEqualTo("DENY");
        assertThat(headers.getFirst("Content-Security-Policy")).isEqualTo("default-src 'none'");
        assertThat(headers.getFirst("Strict-Transport-Security")).isEqualTo("max-age=31536000; includeSubDomains");
    }

    @Test
    void filter_relaxesCspForSwaggerUiAndApiDocsPaths() {
        assertThat(exerciseFilter("/swagger-ui/index.html").getFirst("Content-Security-Policy"))
                .isEqualTo("default-src 'self'; style-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-inline'; img-src 'self' data:");
        assertThat(exerciseFilter("/v3/api-docs").getFirst("Content-Security-Policy"))
                .isEqualTo("default-src 'self'; style-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-inline'; img-src 'self' data:");
        assertThat(exerciseFilter("/webjars/swagger-ui/index.html").getFirst("Content-Security-Policy"))
                .isEqualTo("default-src 'self'; style-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-inline'; img-src 'self' data:");
    }

    private HttpHeaders exerciseFilter(String path) {
        ServerWebExchange exchange = mock(ServerWebExchange.class);
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        HttpHeaders headers = new HttpHeaders();
        WebFilterChain chain = mock(WebFilterChain.class);

        when(exchange.getRequest()).thenReturn(request);
        when(request.getURI()).thenReturn(URI.create("http://localhost" + path));
        when(exchange.getResponse()).thenReturn(response);
        when(response.getHeaders()).thenReturn(headers);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        return headers;
    }
}
