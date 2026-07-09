package com.brayanpv.app.application.service.implementations;

import com.brayanpv.app.application.dto.request.BirdRequest;
import com.brayanpv.app.application.dto.response.BirdResponse;
import com.brayanpv.app.application.service.contracts.IBirdService;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Log4j2
public class BirdService implements IBirdService {
    @Override
    public Mono<BirdResponse> detectBird(BirdRequest birdRequest) {
        log.info("Bird Detect service");
        return null;
    }
}
