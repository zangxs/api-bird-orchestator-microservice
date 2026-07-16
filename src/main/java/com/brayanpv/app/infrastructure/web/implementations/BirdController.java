package com.brayanpv.app.infrastructure.web.implementations;

import com.brayanpv.app.application.dto.request.ImageUploadRequest;
import com.brayanpv.app.application.dto.response.MapSightingResponse;
import com.brayanpv.app.application.dto.response.ImageStatusResponse;
import com.brayanpv.app.application.dto.response.ImageUploadResponse;
import com.brayanpv.app.application.dto.response.GenericResponse;
import com.brayanpv.app.domain.usecase.contracts.IBirdMapInformationUseCase;
import com.brayanpv.app.domain.usecase.contracts.IGetImageStatusUseCase;
import com.brayanpv.app.domain.usecase.contracts.IProcessBirdImageUseCase;
import com.brayanpv.app.infrastructure.web.contracts.IBirdController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
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
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/bird")
@RequiredArgsConstructor
@Log4j2
@Tag(name = "Bird", description = "Bird photo submission and sighting lookups")
public class BirdController implements IBirdController {

    private final IProcessBirdImageUseCase processBirdImageUseCase;
    private final IGetImageStatusUseCase getImageStatusUseCase;
    private final IBirdMapInformationUseCase birdMapInformationUseCase;
    private final Validator validator;

    @PostMapping(path = "/detect", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Override
    @Operation(
            summary = "Submit a bird photo for detection and classification",
            description = "Uploads the image to S3, records an ImageEvent, and publishes it to the "
                    + "detection pipeline. The response holds open (up to a 6s internal timeout) while "
                    + "detection and, if a bird is found, classification resolve over RabbitMQ - so "
                    + "`status` in the response may already be NOT_A_BIRD, DONE or FAILED, or still "
                    + "PROCESSING if the pipeline didn't finish in time."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pipeline outcome (possibly still PROCESSING on timeout)"),
            @ApiResponse(responseCode = "400", description = "Invalid userId/coordinates, oversized image (>10MB), or a downstream failure", content = @Content)
    })
    public Mono<ResponseEntity<GenericResponse<ImageUploadResponse>>> detectBird(
            @Parameter(description = "Photo to identify", required = true, content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE, schema = @Schema(type = "string", format = "binary")))
            @RequestPart("image") FilePart image,
            @Parameter(description = "UUID of the submitting user", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @RequestPart("userId") String userIdStr,
            @Parameter(description = "Sighting longitude, -180..180 (optional)", example = "-74.0817")
            @RequestPart(value = "longitude", required = false) String longitude,
            @Parameter(description = "Sighting latitude, -90..90 (optional)", example = "4.6097")
            @RequestPart(value = "latitude", required = false) String latitude) {
        log.info("Detecting Bird");

        UUID userId = UUID.fromString(userIdStr);
        BigDecimal longitudeDecimal =  longitude != null ? new BigDecimal(longitude) : null;
        BigDecimal latitudeDecimal =  latitude != null ? new BigDecimal(latitude) : null;
        ImageUploadRequest imageUploadRequest = new ImageUploadRequest(image, userId, longitudeDecimal, latitudeDecimal);

        Set<ConstraintViolation<ImageUploadRequest>> violations = validator.validate(imageUploadRequest);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

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
    @Operation(
            summary = "Poll an image event's current status",
            description = "Re-fetches an ImageEvent by id (joining its species once assigned), for "
                    + "clients that don't want to hold the /bird/detect request open."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current ImageEvent state"),
            @ApiResponse(responseCode = "400", description = "Malformed imageEventId or event not found", content = @Content)
    })
    public Mono<ResponseEntity<GenericResponse<ImageStatusResponse>>> getImageStatus(
            @Parameter(description = "ImageEvent id returned by POST /bird/detect", required = true)
            String imageEventId) {
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
    @Operation(
            summary = "List completed sightings within a lat/lng bounding box",
            description = "Returns DONE sightings inside the given box, each with a presigned S3 "
                    + "thumbnail URL - backs the map view."
    )
    @ApiResponse(responseCode = "200", description = "Sightings within the bounding box (possibly empty)")
    public Mono<ResponseEntity<GenericResponse<List<MapSightingResponse>>>> getMapSightings(
            @Parameter(description = "Minimum latitude", required = true, example = "4.4") @RequestParam BigDecimal minLat,
            @Parameter(description = "Maximum latitude", required = true, example = "4.9") @RequestParam BigDecimal maxLat,
            @Parameter(description = "Minimum longitude", required = true, example = "-74.3") @RequestParam BigDecimal minLng,
            @Parameter(description = "Maximum longitude", required = true, example = "-73.9") @RequestParam BigDecimal maxLng) {
        return birdMapInformationUseCase.execute(minLat, maxLat, minLng, maxLng)
                .collectList()
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