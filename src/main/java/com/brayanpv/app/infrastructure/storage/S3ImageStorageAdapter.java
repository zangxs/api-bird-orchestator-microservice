package com.brayanpv.app.infrastructure.storage;

import com.brayanpv.app.domain.storage.IImageStoragePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;


@Component
@RequiredArgsConstructor
public class S3ImageStorageAdapter implements IImageStoragePort {

    private final S3AsyncClient s3AsyncClient;
    @Value("${aws.s3.bucket}")
    private String bucketName;


    @Override
    public Mono<String> upload(byte[] imageBytes, String key) {

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        return Mono.fromFuture(
                s3AsyncClient.putObject(request, AsyncRequestBody.fromBytes(imageBytes))
        ).thenReturn(key);
    }

    @Override
    public Mono<Void> delete(String key) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        return Mono.fromFuture(s3AsyncClient.deleteObject(request)).then();
    }
}
