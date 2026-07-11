package com.brayanpv.app.infrastructure.configuration;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Log4j2
public class RabbitTopologyInitializer {
    private final Mono<Void> topology;

    @PostConstruct
    public void init() {
        topology.subscribe(
                unused -> {},
                error -> log.error("Failed to declare RabbitMQ topology", error),
                () -> log.info("RabbitMQ topology declared successfully")
        );
    }
}
