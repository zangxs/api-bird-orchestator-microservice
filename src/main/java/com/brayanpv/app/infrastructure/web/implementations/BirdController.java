package com.brayanpv.app.infrastructure.web.implementations;

import com.brayanpv.app.application.dto.request.ImageUploadRequest;
import com.brayanpv.app.application.dto.response.MapSightingResponse;
import com.brayanpv.app.application.dto.response.ImageStatusResponse;
import com.brayanpv.app.application.dto.response.ImageUploadResponse;
import com.brayanpv.app.application.dto.response.GenericResponse;
import com.brayanpv.app.application.usecase.contracts.IBirdMapInformationUseCase;
import com.brayanpv.app.application.usecase.contracts.IGetImageStatusUseCase;
import com.brayanpv.app.application.usecase.contracts.IProcessBirdImageUseCase;
import com.brayanpv.app.infrastructure.web.contracts.IBirdController;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/bird")
@RequiredArgsConstructor
@Log4j2
public class BirdController implements IBirdController {

    private final IProcessBirdImageUseCase processBirdImageUseCase;
    private final IGetImageStatusUseCase getImageStatusUseCase;
    private final IBirdMapInformationUseCase birdMapInformationUseCase;

    @PostMapping(path = "/detect", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Override
    public Mono<ResponseEntity<GenericResponse<ImageUploadResponse>>> detectBird(@RequestPart("image") FilePart image,
                                                                                 @RequestPart("userId") String userIdStr,
                                                                                 @RequestPart(value = "longitude", required = false) String longitude,
                                                                                 @RequestPart(value = "latitude", required = false) String latitude) {
        log.info("Detecting Bird");

        UUID userId = UUID.fromString(userIdStr);
        BigDecimal longitudeDecimal =  longitude != null ? new BigDecimal(longitude) : null;
        BigDecimal latitudeDecimal =  latitude != null ? new BigDecimal(latitude) : null;
        ImageUploadRequest imageUploadRequest = new ImageUploadRequest(image, userId, longitudeDecimal, latitudeDecimal);

        return processBirdImageUseCase.execute(imageUploadRequest).flatMap(response -> {

            GenericResponse<ImageUploadResponse> generic = GenericResponse.<ImageUploadResponse>builder()
                    .dateTime(LocalDateTime.now(ZoneOffset.UTC))
                    .code(HttpStatus.OK.value())
                    .data(response)
                    .build();
            ResponseEntity<GenericResponse<ImageUploadResponse>> genericResponse = ResponseEntity.ok(generic);
            return Mono.just(genericResponse);
        });
    }

    @Override
    @GetMapping("/image-events/{imageEventId}/status")
    public Mono<ResponseEntity<GenericResponse<ImageStatusResponse>>> getImageStatus(String imageEventId) {
        log.info("Getting Image Status");
        UUID eventUudId = UUID.fromString(imageEventId);
        return getImageStatusUseCase.execute(eventUudId).flatMap(response -> {
            GenericResponse<ImageStatusResponse> generic = GenericResponse.<ImageStatusResponse>builder()
                    .dateTime(LocalDateTime.now(ZoneOffset.UTC))
                    .code(HttpStatus.OK.value())
                    .data(response)
                    .build();
            ResponseEntity<GenericResponse<ImageStatusResponse>> genericResponse = ResponseEntity.ok(generic);
            return Mono.just(genericResponse);
        });

    }

    @Override
    @GetMapping("/map")
    public Mono<ResponseEntity<GenericResponse<List<MapSightingResponse>>>> getMapSightings(
            @RequestParam BigDecimal minLat, @RequestParam BigDecimal maxLat,
            @RequestParam BigDecimal minLng, @RequestParam BigDecimal maxLng) {
        log.info("Getting Bird Map Information");
        return birdMapInformationUseCase.execute(minLat, maxLat, minLng, maxLng)
                .collectList()
                .log()
                .map(sightings -> {
                    GenericResponse<List<MapSightingResponse>> generic = GenericResponse.<List<MapSightingResponse>>builder()
                            .dateTime(LocalDateTime.now(ZoneOffset.UTC))
                            .code(HttpStatus.OK.value())
                            .data(sightings)
                            .build();
                    return ResponseEntity.ok(generic);
                });
    }


}