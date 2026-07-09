package com.brayanpv.app.infrastructure.web.implementations;

import com.brayanpv.app.application.dto.request.BirdRequest;
import com.brayanpv.app.application.dto.response.BirdResponse;
import com.brayanpv.app.application.dto.response.GenericResponse;
import com.brayanpv.app.application.service.contracts.IBirdService;
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

@RestController
@RequestMapping("/bird")
@RequiredArgsConstructor
@Log4j2
public class BirdController implements IBirdController {

    private final IBirdService birdService;

    @PostMapping(path = "/detect", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Override
    public Mono<ResponseEntity<GenericResponse<BirdResponse>>> detectBird(@RequestPart("image") FilePart image,
                                                                          @RequestPart("userId") String userId) {
        log.info("Detecting Bird");

        BirdRequest birdRequest = new BirdRequest(image, userId);

        return birdService.detectBird(birdRequest).flatMap(response -> {

            GenericResponse<BirdResponse> generic = GenericResponse.<BirdResponse>builder()
                    .dateTime(LocalDateTime.now(ZoneOffset.UTC))
                    .code(HttpStatus.OK.value())
                    .data(response)
                    .build();
            ResponseEntity<GenericResponse<BirdResponse>> genericResponse = ResponseEntity.ok(generic);
            return Mono.just(genericResponse);
        });
    }
}
