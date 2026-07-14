package com.brayanpv.app.infrastructure.web.contracts;

import com.brayanpv.app.application.dto.response.BirdSightingResponse;
import com.brayanpv.app.application.dto.response.GenericResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface IUserController {

    Mono<ResponseEntity<GenericResponse<List<BirdSightingResponse>>>> getImageEvents(@PathVariable UUID userId);
}
