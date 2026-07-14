package com.brayanpv.app.infrastructure.messaging.consumer;

import com.brayanpv.app.domain.exception.DeserializationException;
import com.brayanpv.app.domain.model.BirdClassificationResult;
import com.brayanpv.app.domain.usecase.contracts.IProcessClassificationResultUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.Receiver;

import java.io.IOException;
@Component
@RequiredArgsConstructor
@Log4j2
public class BirdClassificationEventConsumer {

    private final IProcessClassificationResultUseCase processClassificationResultUseCase;
    private final Receiver receiver;
    private final ObjectMapper objectMapper;

    @Value("${rabbitmq.classification-result-queue}")
    private String classificationResultQueueName;

    @PostConstruct
    public void start() {
        receiver.consumeAutoAck(classificationResultQueueName)
                .flatMap(delivery -> deserialize(delivery.getBody())
                        .flatMap(processClassificationResultUseCase::execute)
                        .onErrorResume(e -> {
                            log.error("Failed to process classification result", e);
                            return Mono.empty();
                        }))
                .subscribe();
    }

    private Mono<BirdClassificationResult> deserialize(byte[] body) {
        return Mono.fromCallable(() -> {
            try {
                return objectMapper.readValue(body, BirdClassificationResult.class);
            } catch (IOException e) {
                throw new DeserializationException("Failed to deserialize classification result");
            }
        });
    }
}
