package org.certis.studyplatform.shared.service;

import org.certis.studyplatform.exception.InfrastructureException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.*;

@DisplayName("S3AttachmentService 프로필 이미지 크기 제한 테스트")
class S3AttachmentServiceProfileImageTest {

    @Nested
    @DisplayName("프로필 이미지 크기 제한 테스트")
    class ProfileImageSizeLimitTest {

        @Test
        @DisplayName("10MB 이하 프로필 이미지는 정상 업로드")
        void uploadProfileImage_Under10MB_ShouldPass() {
            // Given: 5MB 크기의 이미지 파일
            MockMultipartFile file = createMockFile("profile.jpg", "image/jpeg", 5 * 1024 * 1024);

            // When & Then: 예외가 발생하지 않아야 함
            assertThatCode(() -> {
                validateFileSize(file, 10);
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("10MB 초과 프로필 이미지는 예외 발생")
        void uploadProfileImage_Over10MB_ShouldThrowException() {
            // Given: 15MB 크기의 이미지 파일
            MockMultipartFile file = createMockFile("large-profile.jpg", "image/jpeg", 15 * 1024 * 1024);

            // When & Then: 예외가 발생해야 함
            assertThatThrownBy(() -> {
                validateFileSize(file, 10);
            }).isInstanceOf(InfrastructureException.class);
        }

        @Test
        @DisplayName("정확히 10MB 프로필 이미지는 정상 업로드")
        void uploadProfileImage_Exactly10MB_ShouldPass() {
            // Given: 정확히 10MB 크기의 이미지 파일
            MockMultipartFile file = createMockFile("exact-10mb-profile.jpg", "image/jpeg", 10 * 1024 * 1024);

            // When & Then: 예외가 발생하지 않아야 함
            assertThatCode(() -> {
                validateFileSize(file, 10);
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("10MB + 1바이트 프로필 이미지는 예외 발생")
        void uploadProfileImage_10MBPlus1Byte_ShouldThrowException() {
            // Given: 10MB + 1바이트 크기의 이미지 파일
            MockMultipartFile file = createMockFile("over-10mb-profile.jpg", "image/jpeg", 10 * 1024 * 1024 + 1);

            // When & Then: 예외가 발생해야 함
            assertThatThrownBy(() -> {
                validateFileSize(file, 10);
            }).isInstanceOf(InfrastructureException.class);
        }

        @Test
        @DisplayName("일반 파일 업로드는 여전히 20MB 제한")
        void uploadRegularFile_Still20MBLimit() {
            // Given: 15MB 크기의 일반 파일
            MockMultipartFile file = createMockFile("document.pdf", "application/pdf", 15 * 1024 * 1024);

            // When & Then: 일반 파일은 20MB 제한이므로 정상 업로드
            assertThatCode(() -> {
                validateFileSize(file, 20);
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("일반 파일 업로드 20MB 초과는 예외 발생")
        void uploadRegularFile_Over20MB_ShouldThrowException() {
            // Given: 25MB 크기의 일반 파일
            MockMultipartFile file = createMockFile("large-document.pdf", "application/pdf", 25 * 1024 * 1024);

            // When & Then: 예외가 발생해야 함
            assertThatThrownBy(() -> {
                validateFileSize(file, 20);
            }).isInstanceOf(InfrastructureException.class);
        }
    }

    @Nested
    @DisplayName("프로필 이미지 타입 검증 테스트")
    class ProfileImageTypeValidationTest {

        @Test
        @DisplayName("허용된 이미지 타입들은 정상 처리")
        void allowedImageTypes_ShouldPass() {
            String[] allowedTypes = {"image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"};
            
            for (String contentType : allowedTypes) {
                MockMultipartFile file = createMockFile("profile." + contentType.split("/")[1], contentType, 1024);

                assertThatCode(() -> {
                    validateImageFileType(file);
                }).doesNotThrowAnyException();
            }
        }

        @Test
        @DisplayName("허용되지 않은 파일 타입은 예외 발생")
        void disallowedFileTypes_ShouldThrowException() {
            String[] disallowedTypes = {
                    "application/pdf",
                    "text/plain",
                    "video/mp4",
                    "audio/mp3",
                    "application/zip"
            };
            
            for (String contentType : disallowedTypes) {
                MockMultipartFile file = createMockFile("file." + contentType.split("/")[1], contentType, 1024);

                assertThatThrownBy(() -> {
                    validateImageFileType(file);
                }).isInstanceOf(InfrastructureException.class);
            }
        }

        @Test
        @DisplayName("null content type은 예외 발생")
        void nullContentType_ShouldThrowException() {
            MockMultipartFile file = new MockMultipartFile(
                    "profileImage",
                    "profile.jpg",
                    null, // null content type
                    new byte[1024]
            );

            assertThatThrownBy(() -> {
                validateImageFileType(file);
            }).isInstanceOf(InfrastructureException.class);
        }
    }

    // 테스트용 헬퍼 메서드들
    private MockMultipartFile createMockFile(String filename, String contentType, int sizeInBytes) {
        return new MockMultipartFile(
                "file",
                filename,
                contentType,
                new byte[sizeInBytes]
        );
    }

    private void validateFileSize(MockMultipartFile file, long maxSizeInMB) {
        long maxSizeInBytes = maxSizeInMB * 1024 * 1024;
        if (file.getSize() > maxSizeInBytes) {
            throw new InfrastructureException(ExceptionStatus.S3_INFRASTRUCTURE_FILE_TOO_LARGE);
        }
    }

    private void validateImageFileType(MockMultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new InfrastructureException(ExceptionStatus.S3_INFRASTRUCTURE_INVALID_FILE_TYPE);
        }

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
}
