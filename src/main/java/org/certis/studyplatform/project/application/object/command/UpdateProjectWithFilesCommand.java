package org.certis.studyplatform.project.application.object.command;

import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 파일 업로드를 포함한 프로젝트 수정 명령
 * 도메인 계층에서 S3 업로드를 처리하기 위해 MultipartFile을 포함
 */
public record UpdateProjectWithFilesCommand(
        Long id,
        Long requesterId,
        String title,
        String description,
        String content,
        String category,
        String subCategory,
        OffsetDateTime startDate,
        OffsetDateTime endDate,
        String githubUrl,
        String externalUrl,
        MultipartFile newThumbnailFile, // 새 썸네일 이미지 파일 (null이면 기존 유지)
        Boolean keepExistingThumbnail, // 기존 썸네일 유지 여부
        Integer maxParticipants,
        List<String> existingAttachmentUrls, // 유지할 기존 첨부파일 URL들
        List<MultipartFile> newAttachmentFiles // 새로 추가할 첨부파일들
) {
    public static UpdateProjectWithFilesCommand of(
            Long id, Long requesterId, String title, String description, String content,
            String category, String subCategory, OffsetDateTime startDate, OffsetDateTime endDate,
            String githubUrl, String externalUrl, MultipartFile newThumbnailFile, Boolean keepExistingThumbnail,
            Integer maxParticipants, List<String> existingAttachmentUrls, List<MultipartFile> newAttachmentFiles) {
        return new UpdateProjectWithFilesCommand(id, requesterId, title, description, content,
                category, subCategory, startDate, endDate, githubUrl, externalUrl,
                newThumbnailFile, keepExistingThumbnail, maxParticipants,
                existingAttachmentUrls, newAttachmentFiles);
    }
}
