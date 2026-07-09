package com.brayanpv.app.application.service.contracts;

import com.brayanpv.app.application.dto.request.BirdRequest;
import com.brayanpv.app.application.dto.response.BirdResponse;
import reactor.core.publisher.Mono;

public interface IBirdService {

    Mono<BirdResponse> detectBird(BirdRequest birdRequest);
}
