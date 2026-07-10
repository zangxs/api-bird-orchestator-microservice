package com.brayanpv.app.infrastructure.messaging.consumer;

import com.brayanpv.app.domain.exception.DeserializationException;
import com.brayanpv.app.domain.model.BirdDetectionResult;
import com.brayanpv.app.domain.model.ImageEvent;
import com.brayanpv.app.domain.repository.IImageEventRepository;
import com.brayanpv.app.infrastructure.mapper.ImageEventMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.Receiver;

import java.io.IOException;

import static org.springframework.util.SerializationUtils.deserialize;

@Component
@RequiredArgsConstructor
@Log4j2
public class BirdDetectionEventConsumer {

    private final IImageEventRepository imageEventRepository;
    private final Receiver receiver;
    private final ImageEventMapper imageEventMapper;
    private final ObjectMapper objectMapper;


    @Value("${rabbitmq.result-queue}")
    private String resultQueueName;


    @PostConstruct
    public void start() {
        receiver.consumeAutoAck(resultQueueName)
                .flatMap(delivery -> {
                    BirdDetectionResult message = deserialize(delivery.getBody());
                    log.info("BirdDetectionResult: {}", message);
                    return imageEventRepository.updateDetection(
                            message.getImageEventId(),
                            message.isBird(),
                            message.getConfidence()
                    );
                })
                .subscribe();
    }

    private BirdDetectionResult deserialize(byte[] body) {
        try {
            return objectMapper.readValue(body, BirdDetectionResult.class);
        } catch (IOException e) {
            log.error(e);
            throw new DeserializationException("Failed to deserialize detection result");
        }
    }



}
