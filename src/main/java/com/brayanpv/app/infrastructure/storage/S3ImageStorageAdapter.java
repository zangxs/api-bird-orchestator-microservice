package com.brayanpv.app.infrastructure.storage;

import com.brayanpv.app.domain.storage.IImageStoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;


@Component
@RequiredArgsConstructor
@Log4j2
public class S3ImageStorageAdapter implements IImageStoragePort {

    private final S3AsyncClient s3AsyncClient;
    private final S3Presigner s3Presigner;
    @Value("${aws.s3.bucket}")
    private String bucketName;


    @Override
    public Mono<String> upload(byte[] imageBytes, String key) {
        log.info("Uploading image to S3 bucket {}", bucketName);
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

    @Override
    @Cacheable(cacheNames = "presignedImageUrls", key = "#key + '|' + #expiration")
    public Mono<String> generatePresignedUrl(String key, Duration expiration) {
        return Mono.fromCallable(() -> {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(expiration)
                    .getObjectRequest(getObjectRequest)
                    .build();

            return s3Presigner.presignGetObject(presignRequest).url().toString();
        }).subscribeOn(Schedulers.boundedElastic()); // S3Presigner es bloqueante, aísla el hilo
    }
}
