package org.certis.studyplatform.shared.service;

import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.exception.InfrastructureException;
import org.certis.studyplatform.shared.type.ImageCategory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * S3 첨부파일 서비스
 * 
 * 모든 도메인의 첨부파일을 S3에 업로드/다운로드/삭제하는 공통 서비스
 * 기존 엔터티 구조를 깨트리지 않고 S3 URL만 저장
 * 
 * AWS SDK v2를 사용한 실제 S3 연동 구현
 */
@Service
@Slf4j
public class S3AttachmentService {

    @Value("${aws.s3.bucket-name:${AWS_S3_BUCKET:test-bucket}}")
    private String bucketName;

    @Value("${aws.s3.image-bucket-name:${AWS_S3_IMAGE_BUCKET:test-bucket}}")
    private String imageBucketName;

    @Value("${aws.s3.region:${AWS_DEFAULT_REGION:ap-northeast-2}}")
    private String region;

    @Value("${aws.s3.access-key-id:${AWS_ACCESS_KEY_ID:}}")
    private String accessKeyId;

    @Value("${aws.s3.secret-access-key:${AWS_SECRET_ACCESS_KEY:}}")
    private String secretAccessKey;

    private S3Client s3Client;
    private S3Presigner s3Presigner;
    private AwsCredentialsProvider credentialsProvider;
    private final Map<String, S3Client> s3ClientByRegion = new ConcurrentHashMap<>();
    private final Map<String, S3Presigner> s3PresignerByRegion = new ConcurrentHashMap<>();

    @PostConstruct
    public void initializeS3Client() {
        try {
            S3ClientBuilder builder = S3Client.builder()
                    .region(Region.of(region));

            // Prefer explicitly configured static credentials; otherwise use the AWS default provider chain
            if (accessKeyId != null && !accessKeyId.isEmpty() && secretAccessKey != null && !secretAccessKey.isEmpty()) {
                this.credentialsProvider = StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey));
                log.info("S3 자격증명: 설정된 access key 사용 (region={}, bucket={})", region, bucketName);
            } else {
                this.credentialsProvider = DefaultCredentialsProvider.create();
                log.info("S3 자격증명: 기본 공급자 체인 사용 (region={}, bucket={})", region, bucketName);
            }

            this.s3Client = builder.credentialsProvider(this.credentialsProvider).build();
            this.s3ClientByRegion.put(region, this.s3Client);

            // Presigner uses the same region and credentials provider to ensure signature validity
            this.s3Presigner = S3Presigner.builder()
                    .region(Region.of(region))
                    .credentialsProvider(this.credentialsProvider)
                    .build();
            this.s3PresignerByRegion.put(region, this.s3Presigner);
        } catch (Exception e) {
            log.error("S3Client 초기화 실패: {}", e.getMessage());
            throw new InfrastructureException(ExceptionStatus.S3_INFRASTRUCTURE_CONNECTION_FAILED);
        }
    }

    @PreDestroy
    public void closeS3Client() {
        if (s3Client != null) {
            s3Client.close();
            log.info("S3Client 연결 종료");
        }
        if (s3Presigner != null) {
            s3Presigner.close();
            log.info("S3Presigner 종료");
        }
        // cached regional clients/presigners
        s3ClientByRegion.values().forEach(c -> { try { c.close(); } catch (Exception ignored) {} });
        s3PresignerByRegion.values().forEach(p -> { try { p.close(); } catch (Exception ignored) {} });
    }

    /**
     * 파일을 S3에 업로드하고 URL 반환
     */
    public String uploadFile(MultipartFile file, String domain, Long entityId) {
        try {
            // 파일 유효성 검증
            if (file == null || file.isEmpty()) {
                throw new InfrastructureException(ExceptionStatus.S3_INFRASTRUCTURE_INVALID_FILE_TYPE);
            }

            // 파일 크기 제한: 20MB
            validateFileSize(file, 20);
            
            // 고유한 파일명 생성
            String originalFileName = file.getOriginalFilename();
            String extension = originalFileName != null && originalFileName.contains(".") 
                    ? originalFileName.substring(originalFileName.lastIndexOf("."))
                    : "";
            String uniqueFileName = UUID.randomUUID().toString() + extension;
            
            // S3 키 생성 (도메인별로 폴더 구분)
            String s3Key = String.format("%s/%d/%s", domain, entityId, uniqueFileName);
            
            // S3에 실제 파일 업로드
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));
            
            // S3 URL 생성 (경로 안전 인코딩)
            String s3Url = buildS3Url(bucketName, region, s3Key);
            
            log.info("파일 업로드 성공: domain={}, entityId={}, s3Key={}, url={}, size={}bytes", 
                    domain, entityId, s3Key, s3Url, file.getSize());
            
            return s3Url;

        } catch (IOException e) {
            log.error("파일 읽기 중 오류 발생: domain={}, entityId={}, error={}", 
                    domain, entityId, e.getMessage());
            throw new InfrastructureException(ExceptionStatus.S3_INFRASTRUCTURE_UPLOAD_FAILED);
        } catch (S3Exception e) {
            log.error("S3 업로드 중 오류 발생: domain={}, entityId={}, error={}", 
                    domain, entityId, e.getMessage());
            throw new InfrastructureException(ExceptionStatus.S3_INFRASTRUCTURE_UPLOAD_FAILED);
        } catch (Exception e) {
            log.error("S3 업로드 중 예상치 못한 오류 발생: domain={}, entityId={}, error={}", 
                    domain, entityId, e.getMessage());
            throw new InfrastructureException(ExceptionStatus.S3_INFRASTRUCTURE_UPLOAD_FAILED);
        }
    }


    /**
     * 파일을 S3에 업로드하고 URL 반환 (커스텀 파일명 사용)
     */
    public String uploadFileWithCustomName(MultipartFile file, String domain, Long entityId, String customFilename) {
        try {
            // 파일 유효성 검증
            if (file == null || file.isEmpty()) {
                throw new InfrastructureException(ExceptionStatus.S3_INFRASTRUCTURE_INVALID_FILE_TYPE);
            }

            // 파일 크기 제한: 20MB
            validateFileSize(file, 20);
            
            // 커스텀 파일명 사용 (확장자는 원본 파일에서 가져옴)
            String originalFileName = file.getOriginalFilename();
            String extension = originalFileName != null && originalFileName.contains(".") 
                    ? originalFileName.substring(originalFileName.lastIndexOf("."))
                    : "";
            String finalFileName = customFilename + extension;
            
            // S3 키 생성 (도메인별로 폴더 구분)
            String s3Key = String.format("%s/%d/%s", domain, entityId, finalFileName);
            
            // S3에 실제 파일 업로드
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));
            
            // S3 URL 생성 (경로 안전 인코딩)
            String s3Url = buildS3Url(bucketName, region, s3Key);
            
            log.info("파일 업로드 성공 (커스텀 파일명): domain={}, entityId={}, s3Key={}, url={}, size={}bytes", 
                    domain, entityId, s3Key, s3Url, file.getSize());
            
            return s3Url;

        } catch (IOException e) {
            log.error("파일 읽기 중 오류 발생: domain={}, entityId={}, error={}", 
                    domain, entityId, e.getMessage());
            throw new InfrastructureException(ExceptionStatus.S3_INFRASTRUCTURE_UPLOAD_FAILED);
        } catch (S3Exception e) {
            log.error("S3 업로드 중 오류 발생: domain={}, entityId={}, error={}", 
                    domain, entityId, e.getMessage());
            throw new InfrastructureException(ExceptionStatus.S3_INFRASTRUCTURE_UPLOAD_FAILED);
        } catch (Exception e) {
            log.error("S3 업로드 중 예상치 못한 오류 발생: domain={}, entityId={}, error={}", 
                    domain, entityId, e.getMessage());
            throw new InfrastructureException(ExceptionStatus.S3_INFRASTRUCTURE_UPLOAD_FAILED);
        }
    }

    /**
     * 바이트 배열을 S3에 업로드하고 URL 반환 (Base64 등에서 변환된 데이터용)
     */
    public String uploadBytes(byte[] bytes, String contentType, String originalFileName, String domain, Long entityId) {
        try {
            if (bytes == null || bytes.length == 0) {
                throw new InfrastructureException(ExceptionStatus.S3_INFRASTRUCTURE_INVALID_FILE_TYPE);
            }

            // 파일 크기 제한: 20MB
            int maxMb = 20;
            long sizeMb = Math.round(bytes.length / 1024.0 / 1024.0);
            if (sizeMb > maxMb) {
                throw new InfrastructureException(ExceptionStatus.S3_INFRASTRUCTURE_UPLOAD_FAILED);
            }

            String extension = originalFileName != null && originalFileName.contains(".")
                    ? originalFileName.substring(originalFileName.lastIndexOf('.'))
                    : "";
            String uniqueFileName = java.util.UUID.randomUUID().toString() + extension;

            String s3Key = String.format("%s/%d/%s", domain, entityId, uniqueFileName);

            software.amazon.awssdk.services.s3.model.PutObjectRequest putObjectRequest = software.amazon.awssdk.services.s3.model.PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putObjectRequest, software.amazon.awssdk.core.sync.RequestBody.fromBytes(bytes));

            String s3Url = buildS3Url(bucketName, region, s3Key);
            log.info("바이트 업로드 성공: domain={}, entityId={}, s3Key={}, url={}, size={}bytes", domain, entityId, s3Key, s3Url, bytes.length);
            return s3Url;
        } catch (Exception e) {
            log.error("S3 바이트 업로드 중 오류: domain={}, entityId={}, error={}", domain, entityId, e.getMessage());
            throw new InfrastructureException(ExceptionStatus.S3_INFRASTRUCTURE_UPLOAD_FAILED);
        }
    }

    /**
     * S3에서 파일 삭제
     */
    public void deleteFile(String s3Url) {
        try {
            if (s3Url == null || s3Url.isEmpty()) {
                return;
            }

            // S3 URL에서 키 추출
            String s3Key = extractS3KeyFromUrl(s3Url);
            String targetBucket = extractBucketFromUrl(s3Url);
            String targetRegion = extractRegionFromUrl(s3Url);
            if (s3Key == null) {
                log.warn("잘못된 S3 URL 형식: {}", s3Url);
                return;
            }
            String decodedKey = decodeS3KeyPath(s3Key);
            
            // S3에서 실제 파일 삭제
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(targetBucket != null ? targetBucket : bucketName)
                    .key(decodedKey)
                    .build();

            getRegionalS3Client(targetRegion != null ? targetRegion : region).deleteObject(deleteObjectRequest);
            
            log.info("파일 삭제 성공: s3Key={}, url={}", s3Key, s3Url);

        } catch (S3Exception e) {
            log.error("S3 파일 삭제 중 오류 발생: url={}, error={}", s3Url, e.getMessage());
            throw new InfrastructureException(ExceptionStatus.S3_INFRASTRUCTURE_DELETE_FAILED);
        } catch (Exception e) {
            log.error("S3 파일 삭제 중 예상치 못한 오류 발생: url={}, error={}", s3Url, e.getMessage());
            throw new InfrastructureException(ExceptionStatus.S3_INFRASTRUCTURE_DELETE_FAILED);
        }
    }

    /**
     * 파일 존재 여부 확인
     */
    public boolean fileExists(String s3Url) {
        try {
            if (s3Url == null || s3Url.isEmpty()) {
                return false;
            }

            String s3Key = extractS3KeyFromUrl(s3Url);
            String targetBucket = extractBucketFromUrl(s3Url);
            String targetRegion = extractRegionFromUrl(s3Url);
            if (s3Key == null) {
                return false;
            }
            String decodedKey = decodeS3KeyPath(s3Key);

            // S3에서 실제 파일 존재 여부 확인
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(targetBucket != null ? targetBucket : bucketName)
                    .key(decodedKey)
                    .build();

            getRegionalS3Client(targetRegion != null ? targetRegion : region).headObject(headObjectRequest);
            
            log.debug("파일 존재 확인 성공: s3Key={}", s3Key);
            return true;

        } catch (NoSuchKeyException e) {
            log.debug("파일이 존재하지 않음: url={}", s3Url);
            return false;
        } catch (S3Exception e) {
            log.error("S3 파일 존재 확인 중 오류 발생: url={}, error={}", s3Url, e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("S3 파일 존재 확인 중 예상치 못한 오류 발생: url={}, error={}", s3Url, e.getMessage());
            return false;
        }
    }

    /**
     * 객체 메타데이터 조회
     */
    public S3ObjectInfo getObjectInfo(String s3Url) {
        try {
            if (s3Url == null || s3Url.isEmpty()) {
                return null;
            }

            String s3Key = extractS3KeyFromUrl(s3Url);
            String targetBucket = extractBucketFromUrl(s3Url);
            String targetRegion = extractRegionFromUrl(s3Url);
            if (s3Key == null) {
                return null;
            }
            String decodedKey = decodeS3KeyPath(s3Key);

            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(targetBucket != null ? targetBucket : bucketName)
                    .key(decodedKey)
                    .build();

            var head = getRegionalS3Client(targetRegion != null ? targetRegion : region).headObject(headObjectRequest);

            String contentType = head.contentType();
            Long contentLength = head.contentLength();
            String name = s3Key.contains("/") ? s3Key.substring(s3Key.lastIndexOf('/') + 1) : s3Key;

            return new S3ObjectInfo(name, contentType, contentLength, s3Url);

        } catch (Exception e) {
            log.error("S3 객체 메타데이터 조회 실패: url={}, error={}", s3Url, e.getMessage());
            return null;
        }
    }

    /**
     * S3 URL에서 키 추출
     */
    private String extractS3KeyFromUrl(String s3Url) {
        try {
            if (s3Url == null || s3Url.isEmpty()) {
                return null;
            }

            java.net.URI uri = java.net.URI.create(s3Url);
            String host = uri.getHost();
            String path = uri.getRawPath(); // keeps encoding as-is
            if (host == null || path == null) {
                return null;
            }

            // Normalize path (remove leading slash only for processing)
            String normalizedPath = path.startsWith("/") ? path.substring(1) : path;

            // Supported host patterns:
            // 1) Virtual-hosted style
            //    - bucket.s3.amazonaws.com/key
            //    - bucket.s3.<region>.amazonaws.com/key
            //    - bucket.s3-accelerate.amazonaws.com/key
            //    - bucket.s3-accelerate.dualstack.amazonaws.com/key
            //    - bucket.s3-<region>.amazonaws.com/key (legacy dash form)
            // 2) Path-style
            //    - s3.amazonaws.com/bucket/key
            //    - s3.<region>.amazonaws.com/bucket/key
            //    - s3-<region>.amazonaws.com/bucket/key (legacy dash form)
            //    - s3-accelerate.amazonaws.com/bucket/key
            //    - s3-accelerate.dualstack.amazonaws.com/bucket/key

            boolean isAmazon = host.endsWith("amazonaws.com");
            if (!isAmazon) {
                return null; // not an S3 URL we handle
            }

            // Virtual-hosted style: host starts with <bucket>.
            // We consider it virtual-hosted if host contains ".s3" or "s3-accelerate" after the first dot.
            int firstDot = host.indexOf('.');
            if (firstDot > 0) {
                // first label is bucket when virtual-hosted; we don't need its value for key extraction
                String remainder = host.substring(firstDot + 1);
                if (remainder.startsWith("s3") || remainder.startsWith("s3-accelerate")) {
                    // Virtual-hosted: key is the whole path after host
                    return normalizedPath; // may be empty if pointing to bucket root
                }
            }

            // Path-style: host is an s3 endpoint. Expect path `bucket/key...`
            // If there is at least one '/', the segment before the first '/' is the bucket.
            if (!normalizedPath.isEmpty()) {
                int slash = normalizedPath.indexOf('/');
                if (slash >= 0 && slash < normalizedPath.length() - 1) {
                    // skip the bucket segment and return the remainder as key
                    return normalizedPath.substring(slash + 1);
                }
            }

            return null;
        } catch (Exception e) {
            log.error("S3 URL에서 키 추출 실패: url={}, error={}", s3Url, e.getMessage());
            return null;
        }
    }

    private String decodeS3KeyPath(String key) {
        try {
            // Decode each segment to avoid treating '/' as data
            String[] segments = key.split("/");
            StringBuilder decoded = new StringBuilder();
            for (int i = 0; i < segments.length; i++) {
                if (i > 0) decoded.append('/');
                decoded.append(java.net.URLDecoder.decode(segments[i], java.nio.charset.StandardCharsets.UTF_8));
            }
            return decoded.toString();
        } catch (Exception e) {
            return key;
        }
    }

    private String extractBucketFromUrl(String s3Url) {
        try {
            if (s3Url == null || s3Url.isEmpty()) {
                return null;
            }
            java.net.URI uri = java.net.URI.create(s3Url);
            String host = uri.getHost();
            String path = uri.getRawPath();
            if (host == null) {
                return null;
            }

            boolean isAmazon = host.endsWith("amazonaws.com");
            if (!isAmazon) {
                return null;
            }

            int firstDot = host.indexOf('.');
            if (firstDot > 0) {
                String remainder = host.substring(firstDot + 1);
                if (remainder.startsWith("s3") || remainder.startsWith("s3-accelerate")) {
                    // virtual-hosted bucket
                    return host.substring(0, firstDot);
                }
            }

            // path-style: bucket is first segment in path
            if (path != null) {
                String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
                int slash = normalizedPath.indexOf('/');
                if (slash > 0) {
                    return normalizedPath.substring(0, slash);
                }
            }
            return null;
        } catch (Exception e) {
            log.error("S3 URL에서 버킷 추출 실패: url={}, error={}", s3Url, e.getMessage());
            return null;
        }
    }

    private String extractRegionFromUrl(String s3Url) {
        try {
            if (s3Url == null || s3Url.isEmpty()) {
                return null;
            }
            java.net.URI uri = java.net.URI.create(s3Url);
            String host = uri.getHost();
            if (host == null) {
                return null;
            }
            return extractRegionFromHost(host);
        } catch (Exception e) {
            log.error("S3 URL에서 리전 추출 실패: url={}, error={}", s3Url, e.getMessage());
            return null;
        }
    }

    private String extractRegionFromHost(String host) {
        try {
            // patterns:
            // bucket.s3.<region>.amazonaws.com
            // s3.<region>.amazonaws.com
            // s3-<region>.amazonaws.com (legacy)
            // accelerate forms have no region
            if (host.contains("s3-accelerate")) {
                return null;
            }
            String remainder = host;
            int idx = remainder.indexOf("s3.");
            if (idx >= 0) {
                String after = remainder.substring(idx + 3);
                int dot = after.indexOf('.');
                if (dot > 0) {
                    return after.substring(0, dot);
                }
            }
            idx = remainder.indexOf("s3-");
            if (idx >= 0) {
                String after = remainder.substring(idx + 3);
                int dot = after.indexOf('.');
                if (dot > 0) {
                    return after.substring(0, dot);
                }
            }
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private S3Client getRegionalS3Client(String regionName) {
        String key = regionName != null ? regionName : region;
        return s3ClientByRegion.computeIfAbsent(key, r -> S3Client.builder()
                .region(Region.of(r))
                .credentialsProvider(this.credentialsProvider)
                .build());
    }

    private S3Presigner getRegionalPresigner(String regionName) {
        String key = regionName != null ? regionName : region;
        return s3PresignerByRegion.computeIfAbsent(key, r -> S3Presigner.builder()
                .region(Region.of(r))
                .credentialsProvider(this.credentialsProvider)
                .build());
    }

    /**
     * Presigned URL 생성 (기본 1시간 유효)
     */
    public String generatePresignedUrl(String s3Url, java.time.Duration duration) {
        try {
            if (s3Url == null || s3Url.isEmpty()) {
                return null;
            }
            String s3Key = extractS3KeyFromUrl(s3Url);
            String targetBucket = extractBucketFromUrl(s3Url);
            String targetRegion = extractRegionFromUrl(s3Url);
            if (s3Key == null || targetBucket == null) {
                return s3Url; // 외부 URL 혹은 식별 불가
            }
            String decodedKey = decodeS3KeyPath(s3Key);

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(targetBucket)
                    .key(decodedKey)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(duration != null ? duration : java.time.Duration.ofHours(1))
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presigned = getRegionalPresigner(targetRegion != null ? targetRegion : region)
                    .presignGetObject(presignRequest);
            return presigned.url().toString();
        } catch (Exception e) {
            log.error("Presigned URL 생성 실패: url={}, error={}", s3Url, e.getMessage());
            return s3Url;
        }
    }

    /**
     * 프로필 이미지 전용 업로드 (10MB 제한)
     */
    public String uploadProfileImage(MultipartFile file, String domain, Long entityId) {
        try {
            // 파일 유효성 검증
            if (file == null || file.isEmpty()) {
                throw new InfrastructureException(ExceptionStatus.S3_INFRASTRUCTURE_INVALID_FILE_TYPE);
            }

            // 프로필 이미지 파일 크기 제한: 10MB
            validateFileSize(file, 10);
            
            // 이미지 파일 타입 검증
            validateImageFileType(file);
            
            // 고유한 파일명 생성
            String originalFileName = file.getOriginalFilename();
            String extension =getExtension(originalFileName);

            String uniqueFileName = UUID.randomUUID().toString() + extension;
            
            // S3 키 생성 (도메인별로 폴더 구분)
            String s3Key = String.format("%s/%d/%s", domain, entityId, uniqueFileName);
            
            // S3에 실제 파일 업로드
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));
            
            // S3 URL 생성 (경로 안전 인코딩)
            String s3Url = buildS3Url(bucketName, region, s3Key);
            
            log.info("프로필 이미지 업로드 성공: domain={}, entityId={}, s3Key={}, url={}, size={}bytes", 
                    domain, entityId, s3Key, s3Url, file.getSize());
            
            return s3Url;

        } catch (IOException e) {
            log.error("프로필 이미지 읽기 중 오류 발생: domain={}, entityId={}, error={}", 
                    domain, entityId, e.getMessage());
            throw new InfrastructureException(ExceptionStatus.S3_INFRASTRUCTURE_UPLOAD_FAILED);
        } catch (S3Exception e) {
            log.error("프로필 이미지 S3 업로드 중 오류 발생: domain={}, entityId={}, error={}", 
                    domain, entityId, e.getMessage());
            throw new InfrastructureException(ExceptionStatus.S3_INFRASTRUCTURE_UPLOAD_FAILED);
        } catch (Exception e) {
            log.error("프로필 이미지 S3 업로드 중 예상치 못한 오류 발생: domain={}, entityId={}, error={}", 
                    domain, entityId, e.getMessage());
            throw new InfrastructureException(ExceptionStatus.S3_INFRASTRUCTURE_UPLOAD_FAILED);
        }
    }


    /**
     * Image 파일을 S3에 업로드하고 URL 반환 (10MB 제한)
     */
    public String uploadImageByCategory(MultipartFile file, ImageCategory imageCategory){
    try {


        if (file == null || file.isEmpty()) {
            throw new InfrastructureException(ExceptionStatus.S3_INFRASTRUCTURE_INVALID_FILE_TYPE);
        }

        // 프로필 이미지 파일 크기 제한: 10MB
        validateFileSize(file, 10);

        // 이미지 파일 타입 검증
        validateImageFileType(file);

        String datePath = LocalDateTime.now().toString().replace("-", "/");

        String extension = getExtension(file.getOriginalFilename());
        String uniqueFileName = UUID.randomUUID() + extension;

        String s3Key = String.format("%s/%s/%s", imageCategory.getS3Folder(), datePath, uniqueFileName);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(imageBucketName)
                .key(s3Key)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

        return buildS3Url(bucketName, region, s3Key);

        }catch (Exception e) {
        log.error("프로필 이미지 S3 업로드 중 예상치 못한 오류 발생:  error={}", e.getMessage());
        throw new InfrastructureException(ExceptionStatus.S3_INFRASTRUCTURE_UPLOAD_FAILED);
    }
    }

    /**
     * 파일 크기 제한 검증
     */
    public void validateFileSize(MultipartFile file, long maxSizeInMB) {
        long maxSizeInBytes = maxSizeInMB * 1024 * 1024;
        if (file.getSize() > maxSizeInBytes) {
            throw new InfrastructureException(ExceptionStatus.S3_INFRASTRUCTURE_FILE_TOO_LARGE);
        }
    }

    /**
     * 이미지 파일 타입 검증
     */
    public void validateImageFileType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new InfrastructureException(ExceptionStatus.S3_INFRASTRUCTURE_INVALID_FILE_TYPE);
        }

        // 이미지 파일 타입만 허용
        String[] allowedImageTypes = {"image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"};
        boolean isValidImageType = false;
        
        for (String allowedType : allowedImageTypes) {
            if (contentType.equals(allowedType)) {
                isValidImageType = true;
                break;
            }
        }
        
        if (!isValidImageType) {
            throw new InfrastructureException(ExceptionStatus.S3_INFRASTRUCTURE_INVALID_FILE_TYPE);
        }
    }

    /**
     * 파일 타입 검증
     */
    public void validateFileType(MultipartFile file, String[] allowedTypes) {
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new InfrastructureException(ExceptionStatus.S3_INFRASTRUCTURE_INVALID_FILE_TYPE);
        }

        for (String allowedType : allowedTypes) {
            if (contentType.startsWith(allowedType)) {
                return;
            }
        }
        
        throw new InfrastructureException(ExceptionStatus.S3_INFRASTRUCTURE_INVALID_FILE_TYPE);
    }

    private String buildS3Url(String bucket, String regionName, String key) {
        try {
            // Encode each path segment to avoid encoding slashes
            String[] segments = key.split("/");
            StringBuilder encodedPath = new StringBuilder();
            for (int i = 0; i < segments.length; i++) {
                if (i > 0) {
                    encodedPath.append('/');
                }
                String enc = java.net.URLEncoder.encode(segments[i], java.nio.charset.StandardCharsets.UTF_8);
                // Convert application/x-www-form-urlencoded to RFC 3986 for path: '+' -> '%20'
                enc = enc.replace("+", "%20");
                encodedPath.append(enc);
            }
            return "https://" + bucket + ".s3." + regionName + ".amazonaws.com/" + encodedPath;
        } catch (Exception e) {
            // Fallback to raw key if encoding fails (should not happen)
            return "https://" + bucket + ".s3." + regionName + ".amazonaws.com/" + key;
        }
    }

    private String getExtension(String fileName) {
        return fileName != null && fileName.contains(".")
                ? fileName.substring(fileName.lastIndexOf("."))
                : "";
    }
}
