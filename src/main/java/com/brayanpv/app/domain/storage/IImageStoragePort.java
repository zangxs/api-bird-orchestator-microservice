package com.brayanpv.app.domain.storage;

import reactor.core.publisher.Mono;

public interface IImageStoragePort {

    Mono<String> upload(byte[] imageBytes, String key);
    Mono<Void> delete(String key);
}
