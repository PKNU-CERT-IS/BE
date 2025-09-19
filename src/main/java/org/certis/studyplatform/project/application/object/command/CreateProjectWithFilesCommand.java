package org.certis.studyplatform.project.application.object.command;

import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 파일 업로드를 포함한 프로젝트 생성 명령
 * 도메인 계층에서 S3 업로드를 처리하기 위해 MultipartFile을 포함
 */
public record CreateProjectWithFilesCommand(
        String title,
        String description,
        String content,
        String category,
        String subCategory,
        OffsetDateTime startDate,
        OffsetDateTime endDate,
        Long creatorId,
        String githubUrl,
        String externalUrl,
        MultipartFile thumbnailFile, // 썸네일 이미지 파일
        Integer maxParticipants,
        List<MultipartFile> attachmentFiles // 첨부파일들
) {
    public static CreateProjectWithFilesCommand of(
            String title, String description, String content, String category, String subCategory,
            OffsetDateTime startDate, OffsetDateTime endDate, Long creatorId,
            String githubUrl, String externalUrl, MultipartFile thumbnailFile, Integer maxParticipants,
            List<MultipartFile> attachmentFiles) {
        return new CreateProjectWithFilesCommand(title, description, content, category, subCategory,
                startDate, endDate, creatorId, githubUrl, externalUrl, thumbnailFile, maxParticipants,
                attachmentFiles);
    }
}
