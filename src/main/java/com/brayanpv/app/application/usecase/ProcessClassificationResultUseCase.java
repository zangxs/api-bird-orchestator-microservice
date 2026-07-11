package com.brayanpv.app.application.usecase;

import com.brayanpv.app.domain.messaging.IEventPublisherPort;
import com.brayanpv.app.domain.messaging.IImageEventResultBroker;
import com.brayanpv.app.domain.model.BirdClassificationResult;
import com.brayanpv.app.domain.model.ImageEvent;
import com.brayanpv.app.domain.model.ManualClassificationRequested;
import com.brayanpv.app.domain.model.enums.ImageStatus;
import com.brayanpv.app.domain.repository.IImageEventRepository;
import com.brayanpv.app.domain.usecase.contracts.IProcessClassificationResultUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Log4j2
public class ProcessClassificationResultUseCase implements IProcessClassificationResultUseCase {

    private final IImageEventRepository imageEventRepository;
    private final IEventPublisherPort eventPublisherPort;
    private final IImageEventResultBroker resultBroker;

    @Value("${rabbitmq.manual-classification-routing-key}")
    private String manualClassificationRoutingKey;

    @Override
    public Mono<Void> execute(BirdClassificationResult result) {
        log.info("Process Classification Result: {}", result);
        return imageEventRepository.updateClassification(
                        result.getImageEventId(),
                        result.getScientificName(),
                        result.getSpecieConfidence(),
                        result.getFailureReason()
                )
                .flatMap(imageEvent -> {
                    if (imageEvent.getStatus() == ImageStatus.FAILED) {
                        return publishManualClassificationRequest(imageEvent, result.getFailureReason());
                    }
                    resultBroker.complete(imageEvent);
                    return Mono.empty();
                })
                .then();
    }

    private Mono<Void> publishManualClassificationRequest(ImageEvent imageEvent, String reason) {
        ManualClassificationRequested event = ManualClassificationRequested.builder()
                .imageEventId(imageEvent.getId())
                .s3Key(imageEvent.getS3Key())
                .reason(reason)
                .build();
        return eventPublisherPort.publish(manualClassificationRoutingKey, event);
    }

}
