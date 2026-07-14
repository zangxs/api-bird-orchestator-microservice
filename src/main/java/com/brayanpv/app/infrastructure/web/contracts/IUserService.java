package com.brayanpv.app.infrastructure.web.contracts;

import com.brayanpv.app.domain.model.ImageEvent;
import org.springframework.web.bind.annotation.PathVariable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface IUserService {

    Flux<ImageEvent> getImageEvents(@PathVariable UUID userId);
}
