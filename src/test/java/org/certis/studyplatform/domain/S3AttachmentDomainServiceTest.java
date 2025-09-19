package org.certis.studyplatform.domain;

import org.certis.studyplatform.board.domain.service.BoardAttachmentDomainService;
import org.certis.studyplatform.project.domain.service.ProjectAttachmentDomainService;
import org.certis.studyplatform.schedule.domain.service.ScheduleAttachmentDomainService;
import org.certis.studyplatform.shared.service.S3FileService;
import org.certis.studyplatform.study.domain.service.StudyAttachmentDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("S3 첨부파일 도메인 서비스 단위 테스트")
class S3AttachmentDomainServiceTest {

    @Mock
    private S3FileService s3FileService;

    private BoardAttachmentDomainService boardAttachmentDomainService;
    private ProjectAttachmentDomainService projectAttachmentDomainService;
    private StudyAttachmentDomainService studyAttachmentDomainService;
    private ScheduleAttachmentDomainService scheduleAttachmentDomainService;

    @BeforeEach
    void setUp() {
        boardAttachmentDomainService = new BoardAttachmentDomainService(s3FileService);
        projectAttachmentDomainService = new ProjectAttachmentDomainService(s3FileService);
        studyAttachmentDomainService = new StudyAttachmentDomainService(s3FileService);
        scheduleAttachmentDomainService = new ScheduleAttachmentDomainService(s3FileService);
    }

    @Test
    @DisplayName("Board 첨부파일 업로드 성공 테스트")
    void testBoardAttachmentUploadSuccess() {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "test content".getBytes());
        List<MultipartFile> files = List.of(file);
        
        when(s3FileService.uploadFile(any(MultipartFile.class), anyString()))
                .thenReturn("https://test-bucket.s3.amazonaws.com/board-attachments/test-file-uuid.txt");

        // When
        List<String> result = boardAttachmentDomainService.uploadAttachments(files);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).contains("board-attachments");
        verify(s3FileService, times(1)).uploadFile(file, "board-attachments");
    }

    @Test
    @DisplayName("Board 첨부파일 업로드 실패 시 예외 발생 및 정리 테스트")
    void testBoardAttachmentUploadFailureWithCleanup() {
        // Given
        MockMultipartFile file1 = new MockMultipartFile("file1", "test1.txt", "text/plain", "test content 1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("file2", "test2.txt", "text/plain", "test content 2".getBytes());
        List<MultipartFile> files = List.of(file1, file2);
        
        when(s3FileService.uploadFile(eq(file1), anyString()))
                .thenReturn("https://test-bucket.s3.amazonaws.com/board-attachments/test-file1-uuid.txt");
        when(s3FileService.uploadFile(eq(file2), anyString()))
                .thenThrow(new RuntimeException("S3 upload failed"));

        // When & Then
        assertThatThrownBy(() -> boardAttachmentDomainService.uploadAttachments(files))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("게시판 첨부파일 업로드에 실패했습니다");

        // 첫 번째 파일은 업로드 성공 후 정리되어야 함
        verify(s3FileService, times(1)).deleteFile("https://test-bucket.s3.amazonaws.com/board-attachments/test-file1-uuid.txt");
    }

    @Test
    @DisplayName("Project 썸네일 업로드 성공 테스트")
    void testProjectThumbnailUploadSuccess() {
        // Given
        MockMultipartFile thumbnailFile = new MockMultipartFile("thumbnail", "thumbnail.jpg", "image/jpeg", "thumbnail content".getBytes());
        
        when(s3FileService.uploadFile(any(MultipartFile.class), anyString()))
                .thenReturn("https://test-bucket.s3.amazonaws.com/project-thumbnails/thumbnail-uuid.jpg");

        // When
        String result = projectAttachmentDomainService.uploadThumbnail(thumbnailFile);

        // Then
        assertThat(result).contains("project-thumbnails");
        verify(s3FileService, times(1)).uploadFile(thumbnailFile, "project-thumbnails");
    }

    @Test
    @DisplayName("Project 썸네일 업로드 실패 시 예외 발생 테스트")
    void testProjectThumbnailUploadFailure() {
        // Given
        MockMultipartFile thumbnailFile = new MockMultipartFile("thumbnail", "thumbnail.jpg", "image/jpeg", "thumbnail content".getBytes());
        
        when(s3FileService.uploadFile(any(MultipartFile.class), anyString()))
                .thenThrow(new RuntimeException("S3 upload failed"));

        // When & Then
        assertThatThrownBy(() -> projectAttachmentDomainService.uploadThumbnail(thumbnailFile))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("프로젝트 썸네일 업로드에 실패했습니다");
    }

    @Test
    @DisplayName("Study 첨부파일 조회 성공 테스트")
    void testStudyAttachmentGetUrlSuccess() {
        // Given
        String fileKey = "study-attachments/test-file-uuid.txt";
        String expectedUrl = "https://test-bucket.s3.amazonaws.com/" + fileKey;
        
        when(s3FileService.getFileUrl(fileKey)).thenReturn(expectedUrl);

        // When
        String result = studyAttachmentDomainService.getAttachmentUrl(fileKey);

        // Then
        assertThat(result).isEqualTo(expectedUrl);
        verify(s3FileService, times(1)).getFileUrl(fileKey);
    }

    @Test
    @DisplayName("Study 첨부파일 조회 실패 시 예외 발생 테스트")
    void testStudyAttachmentGetUrlFailure() {
        // Given
        String fileKey = "study-attachments/non-existent-file.txt";
        
        when(s3FileService.getFileUrl(fileKey)).thenThrow(new RuntimeException("File not found in S3"));

        // When & Then
        assertThatThrownBy(() -> studyAttachmentDomainService.getAttachmentUrl(fileKey))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("스터디 첨부파일 조회에 실패했습니다");
    }

    @Test
    @DisplayName("Schedule 첨부파일 삭제 성공 테스트")
    void testScheduleAttachmentDeleteSuccess() {
        // Given
        String fileUrl = "https://test-bucket.s3.amazonaws.com/schedule-attachments/test-file-uuid.txt";

        // When
        scheduleAttachmentDomainService.deleteAttachment(fileUrl);

        // Then
        verify(s3FileService, times(1)).deleteFile(fileUrl);
    }

    @Test
    @DisplayName("Schedule 첨부파일 삭제 실패 시 예외 발생 테스트")
    void testScheduleAttachmentDeleteFailure() {
        // Given
        String fileUrl = "https://test-bucket.s3.amazonaws.com/schedule-attachments/test-file-uuid.txt";
        
        doThrow(new RuntimeException("S3 delete failed")).when(s3FileService).deleteFile(fileUrl);

        // When & Then
        assertThatThrownBy(() -> scheduleAttachmentDomainService.deleteAttachment(fileUrl))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("스케줄 첨부파일 삭제에 실패했습니다");
    }

    @Test
    @DisplayName("빈 파일 리스트 처리 테스트")
    void testEmptyFileListHandling() {
        // Given
        List<MultipartFile> emptyFiles = List.of();

        // When
        List<String> result = boardAttachmentDomainService.uploadAttachments(emptyFiles);

        // Then
        assertThat(result).isEmpty();
        verify(s3FileService, never()).uploadFile(any(), any());
    }

    @Test
    @DisplayName("null 파일 리스트 처리 테스트")
    void testNullFileListHandling() {
        // When
        List<String> result = boardAttachmentDomainService.uploadAttachments(null);

        // Then
        assertThat(result).isEmpty();
        verify(s3FileService, never()).uploadFile(any(), any());
    }
}
