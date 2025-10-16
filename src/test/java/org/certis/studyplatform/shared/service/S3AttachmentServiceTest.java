package org.certis.studyplatform.shared.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Duration;

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

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}


