package com.brayanpv.app.infrastructure.web.contracts;

import com.brayanpv.app.application.dto.request.BirdRequest;
import com.brayanpv.app.application.dto.response.BirdResponse;
import com.brayanpv.app.application.dto.response.GenericResponse;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

public interface IBirdController {

    Mono<ResponseEntity<GenericResponse<BirdResponse>>> detectBird(BirdRequest birdRequest);
}
