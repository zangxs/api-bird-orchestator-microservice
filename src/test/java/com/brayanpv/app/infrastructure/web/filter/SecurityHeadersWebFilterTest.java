package com.brayanpv.app.infrastructure.web.filter;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityHeadersWebFilterTest {

    private final SecurityHeadersWebFilter filter = new SecurityHeadersWebFilter();

    @Test
    void filter_addsBaselineSecurityHeadersToEveryResponse() {
        ServerWebExchange exchange = mock(ServerWebExchange.class);
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        HttpHeaders headers = new HttpHeaders();
        WebFilterChain chain = mock(WebFilterChain.class);

        when(exchange.getResponse()).thenReturn(response);
        when(response.getHeaders()).thenReturn(headers);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(headers.getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(headers.getFirst("X-Frame-Options")).isEqualTo("DENY");
        assertThat(headers.getFirst("Content-Security-Policy")).isEqualTo("default-src 'none'");
        assertThat(headers.getFirst("Strict-Transport-Security")).isEqualTo("max-age=31536000; includeSubDomains");
    }
}
