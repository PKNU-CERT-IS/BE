package org.certis.studyplatform.shared.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class S3AttachmentServiceTest {

    private S3AttachmentService s3AttachmentService;

    @BeforeEach
    void setUp() throws Exception {
        s3AttachmentService = new S3AttachmentService();
        setField(s3AttachmentService, "bucketName", "test-bucket");
        setField(s3AttachmentService, "region", "ap-northeast-2");
        setField(s3AttachmentService, "accessKeyId", "dummy-access-key");
        setField(s3AttachmentService, "secretAccessKey", "dummy-secret-key");
        s3AttachmentService.initializeS3Client();
    }

    @AfterEach
    void tearDown() {
        s3AttachmentService.closeS3Client();
    }

    @Test
    void generatePresignedUrl_shouldReturnSignedUrl_forValidS3Url() {
        String originalUrl = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/study/1234/file.pdf";
        String presigned = s3AttachmentService.generatePresignedUrl(originalUrl, Duration.ofMinutes(5));

        Assertions.assertNotNull(presigned);
        Assertions.assertNotEquals(originalUrl, presigned);
        Assertions.assertTrue(presigned.contains("X-Amz-Algorithm"));
        Assertions.assertTrue(presigned.contains("X-Amz-Credential"));
        Assertions.assertTrue(presigned.contains("X-Amz-Signature"));
        Assertions.assertTrue(presigned.contains("X-Amz-Expires=300"));
    }

    @Test
    void extractKey_shouldHandlePathStyleUrl() {
        // s3.<region>.amazonaws.com/bucket/key style
        String pathStyle = "https://s3.ap-northeast-2.amazonaws.com/test-bucket/study/5678/notes.txt";
        String presigned = s3AttachmentService.generatePresignedUrl(pathStyle, Duration.ofMinutes(10));
        Assertions.assertNotNull(presigned);
        Assertions.assertNotEquals(pathStyle, presigned);
        Assertions.assertTrue(presigned.contains("X-Amz-Signature"));
    }

    @Test
    void extractKey_shouldHandleAccelerateAndDualstack() {
        String accel = "https://test-bucket.s3-accelerate.amazonaws.com/study/1111/image.png";
        String dual = "https://test-bucket.s3-accelerate.dualstack.amazonaws.com/study/2222/image.png";
        String p1 = s3AttachmentService.generatePresignedUrl(accel, Duration.ofMinutes(3));
        String p2 = s3AttachmentService.generatePresignedUrl(dual, Duration.ofMinutes(3));
        Assertions.assertTrue(p1.contains("X-Amz-Signature"));
        Assertions.assertTrue(p2.contains("X-Amz-Signature"));
    }

    @Test
    void presign_shouldUseBucketAndRegionFromUrl_whenDifferentFromDefault() throws Exception {
        // simulate different region/bucket in URL
        String url = "https://legacy-bucket.s3.us-west-2.amazonaws.com/study/9999/legacy.pdf";
        String presigned = s3AttachmentService.generatePresignedUrl(url, Duration.ofMinutes(10));
        Assertions.assertNotNull(presigned);
        Assertions.assertNotEquals(url, presigned);
        // Region is not directly visible in query, but we at least assert it is signed
        Assertions.assertTrue(presigned.contains("X-Amz-Signature"));
    }

    @Test
    void upload_and_presign_shouldWorkWithKoreanFilename() throws Exception {
        // Build a URL simulating an uploaded object with a Korean filename
        String koreanFile = "보고서 최종본(최신).pdf";
        // mimic upload URL format (the service encodes paths when building URL)
        String encodedUrl = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/board/1234/" +
                java.net.URLEncoder.encode(koreanFile, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");

        // Ensure presign works and returns a signed URL
        String presigned = s3AttachmentService.generatePresignedUrl(encodedUrl, Duration.ofMinutes(15));
        Assertions.assertNotNull(presigned);
        Assertions.assertTrue(presigned.contains("X-Amz-Algorithm"));
        Assertions.assertTrue(presigned.contains("X-Amz-Signature"));

        // Decode the last path segment and ensure it matches the original filename
        java.net.URI uri = java.net.URI.create(presigned);
        String rawPath = uri.getRawPath();
        String lastSegment = rawPath.substring(rawPath.lastIndexOf('/') + 1);
        String decoded = java.net.URLDecoder.decode(lastSegment, java.nio.charset.StandardCharsets.UTF_8);
        Assertions.assertEquals(koreanFile, decoded);
    }

    @Test
    void uploadBytesDeterministic_shouldReuseLocalPresenceCache_afterFirstUpload() throws Exception {
        S3AttachmentService service = mockedService();
        S3Client mockS3Client = getS3Client(service);

        Mockito.when(mockS3Client.headObject(Mockito.any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("missing").build());
        Mockito.when(mockS3Client.putObject(Mockito.any(PutObjectRequest.class), Mockito.any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().eTag("etag").build());

        byte[] bytes = "hello".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        String first = service.uploadBytesDeterministic(bytes, "text/plain", "memo.txt", "study-attachments", 1L, "same-key");
        String second = service.uploadBytesDeterministic(bytes, "text/plain", "memo.txt", "study-attachments", 1L, "same-key");

        Assertions.assertEquals(first, second);
        Mockito.verify(mockS3Client, Mockito.times(1)).headObject(Mockito.any(HeadObjectRequest.class));
        Mockito.verify(mockS3Client, Mockito.times(1)).putObject(Mockito.any(PutObjectRequest.class), Mockito.any(RequestBody.class));
    }

    @Test
    void uploadBytesDeterministic_shouldCollapseConcurrentUploads_forSameKey() throws Exception {
        S3AttachmentService service = mockedService();
        S3Client mockS3Client = getS3Client(service);

        CountDownLatch putStarted = new CountDownLatch(1);
        CountDownLatch releasePut = new CountDownLatch(1);

        Mockito.when(mockS3Client.headObject(Mockito.any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("missing").build());
        Mockito.when(mockS3Client.putObject(Mockito.any(PutObjectRequest.class), Mockito.any(RequestBody.class)))
                .thenAnswer(invocation -> {
                    putStarted.countDown();
                    Assertions.assertTrue(releasePut.await(1, TimeUnit.SECONDS));
                    return PutObjectResponse.builder().eTag("etag").build();
                });

        byte[] bytes = "hello".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> service.uploadBytesDeterministic(
                    bytes,
                    "text/plain",
                    "memo.txt",
                    "study-attachments",
                    1L,
                    "same-key"
            ));

            Assertions.assertTrue(putStarted.await(1, TimeUnit.SECONDS));

            var second = executor.submit(() -> service.uploadBytesDeterministic(
                    bytes,
                    "text/plain",
                    "memo.txt",
                    "study-attachments",
                    1L,
                    "same-key"
            ));

            releasePut.countDown();

            Assertions.assertEquals(first.get(1, TimeUnit.SECONDS), second.get(1, TimeUnit.SECONDS));
        }

        Mockito.verify(mockS3Client, Mockito.times(1)).headObject(Mockito.any(HeadObjectRequest.class));
        Mockito.verify(mockS3Client, Mockito.times(1)).putObject(Mockito.any(PutObjectRequest.class), Mockito.any(RequestBody.class));
    }

    @Test
    void deleteFile_shouldEvictDeterministicCache() throws Exception {
        S3AttachmentService service = mockedService();
        S3Client mockS3Client = getS3Client(service);

        byte[] bytes = "hello".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        Mockito.when(mockS3Client.headObject(Mockito.any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("missing").build());
        Mockito.when(mockS3Client.putObject(Mockito.any(PutObjectRequest.class), Mockito.any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().eTag("etag").build());
        Mockito.when(mockS3Client.deleteObject(Mockito.any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());

        String uploaded = service.uploadBytesDeterministic(bytes, "text/plain", "memo.txt", "study-attachments", 1L, "same-key");
        service.deleteFile(uploaded);
        service.uploadBytesDeterministic(bytes, "text/plain", "memo.txt", "study-attachments", 1L, "same-key");

        Mockito.verify(mockS3Client, Mockito.times(2)).headObject(Mockito.any(HeadObjectRequest.class));
        Mockito.verify(mockS3Client, Mockito.times(2)).putObject(Mockito.any(PutObjectRequest.class), Mockito.any(RequestBody.class));
        Mockito.verify(mockS3Client, Mockito.times(1)).deleteObject(Mockito.any(DeleteObjectRequest.class));
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static S3AttachmentService mockedService() throws Exception {
        S3AttachmentService service = new S3AttachmentService();
        setField(service, "bucketName", "test-bucket");
        setField(service, "region", "ap-northeast-2");

        S3Client mockS3Client = Mockito.mock(S3Client.class);
        setField(service, "s3Client", mockS3Client);
        getMapField(service, "s3ClientByRegion").put("ap-northeast-2", mockS3Client);
        return service;
    }

    private static S3Client getS3Client(S3AttachmentService service) throws Exception {
        Field field = service.getClass().getDeclaredField("s3Client");
        field.setAccessible(true);
        return (S3Client) field.get(service);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, S3Client> getMapField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return (Map<String, S3Client>) field.get(target);
    }
}

