package com.brayanpv.app.infrastructure.messaging.consumer;

import com.brayanpv.app.domain.model.BirdDetectionResult;
import com.brayanpv.app.domain.repository.IImageEventRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.rabbitmq.Receiver;

import static org.springframework.util.SerializationUtils.deserialize;

@Component
@RequiredArgsConstructor
@Log4j2
public class BirdDetectionEventConsumer {

    private final IImageEventRepository imageEventRepository;
    private final Receiver receiver;

    @Value("${rabbitmq.result-queue}")
    private String resultQueueName;

    /*
    @PostConstruct
    public void start() {
        receiver.consumeAutoAck(resultQueueName)
                .flatMap(delivery -> {
                    BirdDetectionResult message = deserialize(delivery.getBody());
                    //mapear BirdDetectionResult a ImageEvent
                    return imageEventRepository.updateDetection(message);
                })
                .subscribe();
    }

     */

}
