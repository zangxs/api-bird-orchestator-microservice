package com.brayanpv.app.domain.repository;

import com.brayanpv.app.domain.model.ImageEvent;
import reactor.core.publisher.Mono;

public interface IImageEventRepository {
    Mono<ImageEvent> save(ImageEvent imageEvent);
}
