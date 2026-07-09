package com.brayanpv.app.infrastructure.storage;

import com.brayanpv.app.domain.storage.IImageStoragePort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class S3ImageStorageAdapter implements IImageStoragePort {
    @Override
    public Mono<String> upload(byte[] imageBytes, String key) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> delete(String key) {
        return Mono.empty();
    }
}
