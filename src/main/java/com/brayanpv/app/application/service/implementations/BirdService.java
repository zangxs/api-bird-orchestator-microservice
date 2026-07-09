package com.brayanpv.app.application.service.implementations;

import com.brayanpv.app.application.dto.request.BirdRequest;
import com.brayanpv.app.application.dto.response.BirdResponse;
import com.brayanpv.app.application.service.contracts.IBirdService;
import com.brayanpv.app.domain.messaging.IEventPublisherPort;
import com.brayanpv.app.domain.storage.IImageStoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Log4j2
@RequiredArgsConstructor
public class BirdService implements IBirdService {

    private final IImageStoragePort imageStoragePort;
    private final IEventPublisherPort eventPublisherPort;

    @Override
    public Mono<BirdResponse> detectBird(BirdRequest birdRequest) {
        log.info("Bird Detect service");

        return Mono.just(birdRequest).flatMap(request -> {
            //do something

            return Mono.empty();
        });

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
    }
}
