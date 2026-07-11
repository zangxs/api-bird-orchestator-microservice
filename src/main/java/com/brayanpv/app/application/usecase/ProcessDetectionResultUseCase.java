package com.brayanpv.app.application.usecase;

import com.brayanpv.app.domain.messaging.IEventPublisherPort;
import com.brayanpv.app.domain.messaging.IImageEventResultBroker;
import com.brayanpv.app.domain.model.BirdDetectionResult;
import com.brayanpv.app.domain.model.ClassificationRequested;
import com.brayanpv.app.domain.repository.IImageEventRepository;
import com.brayanpv.app.domain.usecase.contracts.IProcessDetectionResultUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Log4j2
public class ProcessDetectionResultUseCase implements IProcessDetectionResultUseCase {

    private final IImageEventRepository imageEventRepository;
    private final IEventPublisherPort eventPublisherPort;
    private final IImageEventResultBroker resultBroker;

    @Value("${rabbitmq.classification-routing-key}")
    private String classificationRoutingKey;

    @Override
    public Mono<Void> execute(BirdDetectionResult result) {
        log.info("Process Detection Result");
        return imageEventRepository.updateDetection(
                        result.getImageEventId(),
                        result.isBird(),
                        result.getConfidence()
                )
                .flatMap(imageEvent -> {
                    if (result.isBird()) {
                        ClassificationRequested event = ClassificationRequested.builder()
                                .imageEventId(imageEvent.getId())
                                .s3Key(imageEvent.getS3Key())
                                .build();
                        return eventPublisherPort.publish(classificationRoutingKey, event);
                    }
                    resultBroker.complete(imageEvent);
                    return Mono.empty();
                });
    }
}
