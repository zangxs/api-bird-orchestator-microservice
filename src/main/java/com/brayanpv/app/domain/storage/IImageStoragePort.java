package com.brayanpv.app.domain.storage;

import reactor.core.publisher.Mono;

import java.time.Duration;

public interface IImageStoragePort {

    Mono<String> upload(byte[] imageBytes, String key);
    Mono<Void> delete(String key);
    Mono<String> generatePresignedUrl(String key, Duration expiration);

}
