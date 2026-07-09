package com.brayanpv.app.application.service.implementations;

import com.brayanpv.app.application.dto.request.ImageUploadRequest;
import com.brayanpv.app.application.dto.response.ImageUploadResponse;
import com.brayanpv.app.application.dto.response.GenericResponse;
import com.brayanpv.app.application.service.contracts.IBirdService;
import com.brayanpv.app.domain.messaging.IEventPublisherPort;
import com.brayanpv.app.domain.model.BirdObserved;
import com.brayanpv.app.domain.model.ImageEvent;
import com.brayanpv.app.domain.model.enums.ImageStatus;
import com.brayanpv.app.domain.repository.IImageEventRepository;
import com.brayanpv.app.domain.storage.IImageStoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Log4j2
@RequiredArgsConstructor
public class BirdService implements IBirdService {

    private final IImageStoragePort imageStoragePort;
    private final IEventPublisherPort eventPublisherPort;
    private final IImageEventRepository imageEventRepository;


    //que debo hacer
        /*
        1. POST /images (Java)
        2. Sube a S3
        3. Guarda imagen + relación usuario en DB, estado = PROCESSING
        4. Publica a cola "deteccion"

        5. Detector (Python) consume "deteccion"
        6. Descarga de S3
        7. Corre modelo
        8. Publica resultado a cola "deteccion-resultado" (bird: true/false, confidence, imageId)

        9. Orquestador (Java) consume "deteccion-resultado"
        10a. Si NOT bird → estado = NOT_A_BIRD (el job de limpieza se encarga de S3 después)
        10b. Si bird → estado = BIRD_DETECTED → publica a cola "clasificacion"
         */
    @Override
    public Mono<ImageUploadResponse> processImage(ImageUploadRequest imageUploadRequest) {
        log.info("Bird Detect service");

        String key = buildS3Key(imageUploadRequest.userId());

        return extractBytes(imageUploadRequest.image())
                .flatMap(bytes -> imageStoragePort.upload(bytes, key))
                .then(saveImageEvent(imageUploadRequest, key))
                .flatMap(imageEvent ->
                        eventPublisherPort.publish(toBirdObserved(imageEvent)).thenReturn(imageEvent)
                )
                .map(imageEvent -> ImageUploadResponse.builder()
                                .imageEventId(imageEvent.getId())
                                .status(imageEvent.getStatus().name())
                                .build());

    }

    private String buildS3Key(String userId) {
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
                .build();
    }
}
