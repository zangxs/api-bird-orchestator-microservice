package com.brayanpv.app.infrastructure.messaging.consumer;

import com.brayanpv.app.domain.exception.DeserializationException;
import com.brayanpv.app.domain.model.BirdClassificationResult;
import com.brayanpv.app.domain.model.BirdDetectionResult;
import com.brayanpv.app.domain.repository.IImageEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.rabbitmq.Receiver;

import java.io.IOException;
@Component
@RequiredArgsConstructor
@Log4j2
public class BirdClassificationEventConsumer {

    private final IImageEventRepository imageEventRepository;
    private final Receiver receiver;
    private final ObjectMapper objectMapper;

    @Value("${rabbitmq.result-queue}")
    private String resultQueueName;

    @PostConstruct
    public void start() {
        receiver.consumeAutoAck(resultQueueName)
                .flatMap(delivery -> {
                    BirdClassificationResult message = deserialize(delivery.getBody());
                    log.info("BirdClassificationResult: {}", message);
                    return imageEventRepository.updateClassification(
                            message.getImageEventId(),
                            message.getSpecieId(),
                            message.getSpecieConfidence(),
                            message.getFailureReason()
                    );
                })
                .subscribe();
    }

    private BirdClassificationResult deserialize(byte[] body) {
        try {
            return objectMapper.readValue(body, BirdClassificationResult.class);
        } catch (IOException e) {
            log.error(e);
            throw new DeserializationException("Failed to deserialize detection result");
        }
    }
}
