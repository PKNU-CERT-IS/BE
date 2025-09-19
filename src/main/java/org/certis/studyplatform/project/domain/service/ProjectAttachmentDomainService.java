package org.certis.studyplatform.project.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.shared.service.S3FileService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectAttachmentDomainService {

    private final S3FileService s3FileService;

    /**
     * 프로젝트 첨부파일들을 S3에 업로드하고 URL 리스트를 반환
     * 실패 시 즉시 예외를 발생시킴
     */
    public List<String> uploadAttachments(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> uploadedUrls = new ArrayList<>();
        
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                try {
                    String fileUrl = s3FileService.uploadFile(file, "project-attachments");
                    uploadedUrls.add(fileUrl);
                    log.info("Project attachment uploaded successfully: {}", fileUrl);
                } catch (Exception e) {
                    log.error("Failed to upload project attachment: {}", file.getOriginalFilename(), e);
                    // 이미 업로드된 파일들 정리
                    cleanupUploadedFiles(uploadedUrls);
                    throw new RuntimeException("프로젝트 첨부파일 업로드에 실패했습니다: " + file.getOriginalFilename(), e);
                }
            }
        }
        
        return uploadedUrls;
    }

    /**
     * 프로젝트 썸네일 이미지를 S3에 업로드하고 URL 반환
     * 실패 시 즉시 예외를 발생시킴
     */
    public String uploadThumbnail(MultipartFile thumbnailFile) {
        if (thumbnailFile == null || thumbnailFile.isEmpty()) {
            return null;
        }

        try {
            String fileUrl = s3FileService.uploadFile(thumbnailFile, "project-thumbnails");
            log.info("Project thumbnail uploaded successfully: {}", fileUrl);
            return fileUrl;
        } catch (Exception e) {
            log.error("Failed to upload project thumbnail: {}", thumbnailFile.getOriginalFilename(), e);
            throw new RuntimeException("프로젝트 썸네일 업로드에 실패했습니다: " + thumbnailFile.getOriginalFilename(), e);
        }
    }

    /**
     * S3에서 첨부파일 조회 (URL 유효성 검증)
     * 실패 시 즉시 예외를 발생시킴
     */
    public String getAttachmentUrl(String fileKey) {
        try {
            String fileUrl = s3FileService.getFileUrl(fileKey);
            log.info("Project attachment URL retrieved successfully: {}", fileUrl);
            return fileUrl;
        } catch (Exception e) {
            log.error("Failed to get project attachment URL: {}", fileKey, e);
            throw new RuntimeException("프로젝트 첨부파일 조회에 실패했습니다: " + fileKey, e);
        }
    }

    /**
     * S3에서 썸네일 조회 (URL 유효성 검증)
     * 실패 시 즉시 예외를 발생시킴
     */
    public String getThumbnailUrl(String fileKey) {
        try {
            String fileUrl = s3FileService.getFileUrl(fileKey);
            log.info("Project thumbnail URL retrieved successfully: {}", fileUrl);
            return fileUrl;
        } catch (Exception e) {
            log.error("Failed to get project thumbnail URL: {}", fileKey, e);
            throw new RuntimeException("프로젝트 썸네일 조회에 실패했습니다: " + fileKey, e);
        }
    }

    /**
     * S3에서 첨부파일 삭제
     * 실패 시 즉시 예외를 발생시킴
     */
    public void deleteAttachment(String fileUrl) {
        try {
            s3FileService.deleteFile(fileUrl);
            log.info("Project attachment deleted successfully: {}", fileUrl);
        } catch (Exception e) {
            log.error("Failed to delete project attachment: {}", fileUrl, e);
            throw new RuntimeException("프로젝트 첨부파일 삭제에 실패했습니다: " + fileUrl, e);
        }
    }

    /**
     * 여러 첨부파일 삭제
     */
    public void deleteAttachments(List<String> fileUrls) {
        if (fileUrls == null || fileUrls.isEmpty()) {
            return;
        }

        for (String fileUrl : fileUrls) {
            deleteAttachment(fileUrl);
        }
    }

    /**
     * 업로드 실패 시 이미 업로드된 파일들 정리
     */
    private void cleanupUploadedFiles(List<String> uploadedUrls) {
        for (String url : uploadedUrls) {
            try {
                s3FileService.deleteFile(url);
                log.info("Cleaned up uploaded file: {}", url);
            } catch (Exception cleanupException) {
                log.warn("Failed to cleanup uploaded file: {}", url, cleanupException);
            }
        }
    }
}
