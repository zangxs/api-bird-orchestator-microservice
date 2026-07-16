package com.brayanpv.app.infrastructure.web.implementations;

import com.brayanpv.app.application.dto.response.BirdSightingResponse;
import com.brayanpv.app.application.dto.response.GenericResponse;
import com.brayanpv.app.domain.usecase.contracts.IGetImageEventUseCase;
import com.brayanpv.app.domain.model.ImageEvent;
import com.brayanpv.app.domain.storage.IImageStoragePort;
import com.brayanpv.app.infrastructure.web.contracts.IUserController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Log4j2
@Tag(name = "User", description = "Per-user bird sighting history")
public class UserController implements IUserController {

    private final IGetImageEventUseCase getImageEventUseCase;
    private final IImageStoragePort imageStoragePort;

    @Override
    @GetMapping("/{userId}/birds")
    @Operation(
            summary = "List a user's bird sightings",
            description = "Returns every ImageEvent for the user, each with a presigned S3 thumbnail URL."
    )
    @ApiResponse(responseCode = "200", description = "The user's sightings (possibly empty)")
    public Mono<ResponseEntity<GenericResponse<List<BirdSightingResponse>>>> getImageEvents(
            @Parameter(description = "User id", required = true) @PathVariable UUID userId) {
        log.info("getImageEvents called for user {}", userId);

        return getImageEventUseCase.getImageEvents(userId)
                .flatMap(imageEvent ->
                        imageStoragePort.generatePresignedUrl(imageEvent.getS3Key(), Duration.ofMinutes(30))
                                .map(url -> toBirdSightingResponse(imageEvent, url))
                )
                .collectList()
                .map(sightings -> {
                    GenericResponse<List<BirdSightingResponse>> generic = GenericResponse.<List<BirdSightingResponse>>builder()
                            .dateTime(LocalDateTime.now(ZoneOffset.UTC))
                            .code(HttpStatus.OK.value())
                            .data(sightings)
                            .build();
                    return ResponseEntity.ok(generic);
                });
    }

    private BirdSightingResponse toBirdSightingResponse(ImageEvent imageEvent, String thumbnailUrl) {
        return BirdSightingResponse.builder()
                .imageEventId(imageEvent.getId())
                .specieId(imageEvent.getSpecieId())
                .scientificName(imageEvent.getScientificName())
                .thumbnailUrl(thumbnailUrl)
                .status(imageEvent.getStatus())
                .build();
    }
}
