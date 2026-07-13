package com.brayanpv.app.application.service.implementations;

import com.brayanpv.app.application.dto.request.ImageUploadRequest;
import com.brayanpv.app.application.dto.response.ImageStatusResponse;
import com.brayanpv.app.application.dto.response.ImageUploadResponse;
import com.brayanpv.app.application.dto.response.GenericResponse;
import com.brayanpv.app.application.service.contracts.IBirdService;
import com.brayanpv.app.domain.messaging.IEventPublisherPort;
import com.brayanpv.app.domain.messaging.IImageEventResultBroker;
import com.brayanpv.app.domain.model.BirdObserved;
import com.brayanpv.app.domain.model.ImageEvent;
import com.brayanpv.app.domain.model.enums.ImageStatus;
import com.brayanpv.app.domain.repository.IImageEventRepository;
import com.brayanpv.app.domain.storage.IImageStoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Log4j2
@RequiredArgsConstructor
public class BirdService implements IBirdService {

    private final IImageStoragePort imageStoragePort;
    private final IEventPublisherPort eventPublisherPort;
    private final IImageEventRepository imageEventRepository;
    private final IImageEventResultBroker resultBroker;

    @Value("${rabbitmq.routing-key}")
    private String routingKey;

    private static final Duration RESULT_TIMEOUT = Duration.ofSeconds(6);


    @Override
    public Mono<ImageUploadResponse> processImage(ImageUploadRequest imageUploadRequest) {
        log.info("Bird Detect service");

        String key = buildS3Key(imageUploadRequest.userId());

        return extractBytes(imageUploadRequest.image())
                .flatMap(bytes -> imageStoragePort.upload(bytes, key))
                .then(saveImageEvent(imageUploadRequest, key))
                .flatMap(imageEvent ->
                        eventPublisherPort.publish(routingKey, toBirdObserved(imageEvent)).thenReturn(imageEvent)
                )
                .flatMap(imageEvent ->
                        resultBroker.awaitResult(imageEvent.getId(), RESULT_TIMEOUT)
                                .defaultIfEmpty(imageEvent) // timeout: se queda con el ImageEvent original (PROCESSING)
                )
                .map(imageEvent -> ImageUploadResponse.builder()
                        .imageEventId(imageEvent.getId())
                        .status(imageEvent.getStatus().name())
                        .specieId(imageEvent.getSpecieId())
                        .speciesConfidence(imageEvent.getSpecieConfidence())
                        .build());

    }

    @Override
    public Mono<ImageStatusResponse> getImageStatus(UUID imageEventId) {
        log.info("Bird Detect service checking status");
        return imageEventRepository.findById(imageEventId).map(imageEvent -> {
            return ImageStatusResponse.builder()
                    .imageEventId(imageEvent.getId())
                    .specieConfidence(imageEvent.getSpecieConfidence())
                    .imageStatus(imageEvent.getStatus())
                    .failureReason(imageEvent.getFailureReason())
                    .build();
        });

    }

    private String buildS3Key(UUID userId) {
        return "images/%s/%s.jpg".formatted(userId, UUID.randomUUID());
    }

    private Mono<byte[]> extractBytes(FilePart filePart) {
        return DataBufferUtils.join(filePart.content())
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return bytes;
                });
    }

    private Mono<ImageEvent> saveImageEvent(ImageUploadRequest request, String key) {
        ImageEvent event = ImageEvent.builder()
                .userId(request.userId())
                .s3Key(key)
                .status(ImageStatus.PROCESSING)
                .build();

        return imageEventRepository.save(event);
    }

    private BirdObserved toBirdObserved(ImageEvent imageEvent) {
        return BirdObserved.builder()
                .status(imageEvent.getStatus().name())
                .s3Key(imageEvent.getS3Key())
                .userId(imageEvent.getUserId())
                .imageEventId(imageEvent.getId())
                .build();
    }
}