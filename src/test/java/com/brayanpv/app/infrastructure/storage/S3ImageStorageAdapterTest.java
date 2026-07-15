package com.brayanpv.app.infrastructure.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3ImageStorageAdapterTest {

    private static final String BUCKET = "bird-dex-bucket";

    @Mock
    private S3AsyncClient s3AsyncClient;

    @Mock
    private S3Presigner s3Presigner;

    private S3ImageStorageAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new S3ImageStorageAdapter(s3AsyncClient, s3Presigner);
        ReflectionTestUtils.setField(adapter, "bucketName", BUCKET);
    }

    @Test
    void upload_putsObjectInConfiguredBucketAndReturnsKey() {
        when(s3AsyncClient.putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.async.AsyncRequestBody.class)))
                .thenReturn(CompletableFuture.completedFuture(PutObjectResponse.builder().build()));

        StepVerifier.create(adapter.upload("bytes".getBytes(), "images/key.jpg"))
                .expectNext("images/key.jpg")
                .verifyComplete();
    }

    @Test
    void delete_deletesObjectFromConfiguredBucket() {
        when(s3AsyncClient.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(DeleteObjectResponse.builder().build()));

        StepVerifier.create(adapter.delete("images/key.jpg")).verifyComplete();

        verify(s3AsyncClient).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void generatePresignedUrl_returnsUrlFromPresigner() throws java.net.MalformedURLException {
        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(URI.create("https://s3.example.com/images/key.jpg?signature=abc").toURL());
        when(s3Presigner.presignGetObject(any(software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest.class)))
                .thenReturn(presigned);

        StepVerifier.create(adapter.generatePresignedUrl("images/key.jpg", Duration.ofMinutes(30)))
                .assertNext(url -> assertThat(url).isEqualTo("https://s3.example.com/images/key.jpg?signature=abc"))
                .verifyComplete();
    }
}
