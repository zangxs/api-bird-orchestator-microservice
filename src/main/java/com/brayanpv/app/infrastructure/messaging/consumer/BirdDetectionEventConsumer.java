package com.brayanpv.app.infrastructure.messaging.consumer;

import com.brayanpv.app.domain.exception.DeserializationException;
import com.brayanpv.app.domain.model.BirdDetectionResult;
import com.brayanpv.app.domain.repository.IImageEventRepository;
import com.brayanpv.app.domain.usecase.contracts.IProcessDetectionResultUseCase;
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
public class BirdDetectionEventConsumer {

    private final IProcessDetectionResultUseCase processDetectionResultUseCase;
    private final Receiver receiver;
    private final ObjectMapper objectMapper;


    @Value("${rabbitmq.result-queue}")
    private String resultQueueName;


    @PostConstruct
    public void start() {
        receiver.consumeAutoAck(resultQueueName)
                .flatMap(delivery -> deserialize(delivery.getBody())
                        .flatMap(processDetectionResultUseCase::execute)
                        .onErrorResume(e -> {
                            log.error("Failed to process detection result", e);
                            return Mono.empty();
                        }))
                .subscribe();
    }

    private Mono<BirdDetectionResult> deserialize(byte[] body) {
        return Mono.fromCallable(() -> {
            try {
                return objectMapper.readValue(body, BirdDetectionResult.class);
            } catch (IOException e) {
                throw new DeserializationException("Failed to deserialize detection result");
            }
        });
    }


}
