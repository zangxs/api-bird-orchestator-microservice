package com.brayanpv.app.infrastructure.web.implementations;

import com.brayanpv.app.application.dto.request.ImageUploadRequest;
import com.brayanpv.app.application.dto.response.ImageStatusResponse;
import com.brayanpv.app.application.dto.response.ImageUploadResponse;
import com.brayanpv.app.application.dto.response.GenericResponse;
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

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@RestController
@RequestMapping("/bird")
@RequiredArgsConstructor
@Log4j2
public class BirdController implements IBirdController {

    private final IProcessBirdImageUseCase processBirdImageUseCase;
    private final IGetImageStatusUseCase getImageStatusUseCase;

    @PostMapping(path = "/detect", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Override
    public Mono<ResponseEntity<GenericResponse<ImageUploadResponse>>> detectBird(@RequestPart("image") FilePart image,
                                                                                 @RequestPart("userId") String userIdStr) {
        log.info("Detecting Bird");

        UUID userId = UUID.fromString(userIdStr);
        ImageUploadRequest imageUploadRequest = new ImageUploadRequest(image, userId);

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


}