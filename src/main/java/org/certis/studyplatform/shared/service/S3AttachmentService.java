package org.certis.studyplatform.shared.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.exception.InfrastructureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.core.sync.RequestBody;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.UUID;

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

    @Value("${aws.s3.bucket:${AWS_S3_BUCKET:test-bucket}}")
    private String bucketName;

    @Value("${aws.region:${AWS_DEFAULT_REGION:ap-northeast-2}}")
    private String region;

    @Value("${aws.access-key-id:${AWS_ACCESS_KEY_ID:}}")
    private String accessKeyId;

    @Value("${aws.secret-access-key:${AWS_SECRET_ACCESS_KEY:}}")
    private String secretAccessKey;

    private S3Client s3Client;

    @PostConstruct
    public void initializeS3Client() {
        try {
            S3ClientBuilder builder = S3Client.builder()
                    .region(Region.of(region));
            
            // Use configured credentials if available, otherwise fall back to environment variables
            if (accessKeyId != null && !accessKeyId.isEmpty() && secretAccessKey != null && !secretAccessKey.isEmpty()) {
                builder.credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)));
                log.info("S3Client 초기화 완료 (설정된 자격증명 사용): region={}, bucket={}", region, bucketName);
            } else {
                builder.credentialsProvider(EnvironmentVariableCredentialsProvider.create());
                log.info("S3Client 초기화 완료 (환경변수 자격증명 사용): region={}, bucket={}", region, bucketName);
            }
            
            this.s3Client = builder.build();
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
            
            // S3 URL 생성
            String s3Url = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, s3Key);
            
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
            
            // S3 URL 생성
            String s3Url = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, s3Key);
            
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
     * S3에서 파일 삭제
     */
    public void deleteFile(String s3Url) {
        try {
            if (s3Url == null || s3Url.isEmpty()) {
                return;
            }

            // S3 URL에서 키 추출
            String s3Key = extractS3KeyFromUrl(s3Url);
            if (s3Key == null) {
                log.warn("잘못된 S3 URL 형식: {}", s3Url);
                return;
            }
            
            // S3에서 실제 파일 삭제
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            
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
            if (s3Key == null) {
                return false;
            }

            // S3에서 실제 파일 존재 여부 확인
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.headObject(headObjectRequest);
            
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
            if (s3Key == null) {
                return null;
            }

            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            var head = s3Client.headObject(headObjectRequest);

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
            // https://bucket-name.s3.region.amazonaws.com/key 형식에서 key 추출
            // 다양한 AWS 리전과 버킷명을 지원하도록 패턴을 더 유연하게 처리
            if (s3Url.startsWith("https://") && s3Url.contains(".s3.") && s3Url.contains(".amazonaws.com/")) {
                // .amazonaws.com/ 이후의 부분이 키
                String amazonawsPart = ".amazonaws.com/";
                int amazonawsIndex = s3Url.indexOf(amazonawsPart);
                if (amazonawsIndex != -1) {
                    return s3Url.substring(amazonawsIndex + amazonawsPart.length());
                }
            }
            return null;
        } catch (Exception e) {
            log.error("S3 URL에서 키 추출 실패: url={}, error={}", s3Url, e.getMessage());
            return null;
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
}
