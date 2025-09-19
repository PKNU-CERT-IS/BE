package org.certis.studyplatform.project.domain.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Project Value Object
 *
 * Clean Architecture Domain Layer
 * 불변 데이터 전송 객체
 * ProjectDetailResponseDto와 매칭되는 완전한 프로젝트 정보
 */
public record ProjectVo(
        Long id,
        String title,
        String description,
        String content,
        String category,
        String subCategory,
        OffsetDateTime startDate,
        OffsetDateTime endDate,
        Long creatorId,
        String creatorName,
        String githubUrl,
        String externalUrl,
        String thumbnailUrl,
        Integer maxParticipants,
        Integer currentParticipants,
        List<ProjectMeetingSummaryVo> meetingSummaryVos
) {

    /**
     * Compact constructor with validation
     */
    public ProjectVo {
        // 제목/설명/내용/카테고리 길이 검증
        if (title == null || title.trim().isEmpty() || title.length() > 30) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_INVALID_TITLE, "프로젝트 제목은 필수입니다");
        }
        if (description != null && description.length() > 100) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_INVALID_DESCRIPTION);
        }
        if (content != null && content.length() > 255) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_RULE_VIOLATION);
        }
        if (category != null && category.length() > 20) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_RULE_VIOLATION);
        }
        if (subCategory != null && subCategory.length() > 100) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_RULE_VIOLATION);
        }
        if (thumbnailUrl != null && thumbnailUrl.length() > 1000) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_RULE_VIOLATION);
        }
        if (githubUrl != null && githubUrl.length() > 1000) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_RULE_VIOLATION);
        }
        if (externalUrl != null && externalUrl.length() > 1000) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_RULE_VIOLATION);
        }

        // 기간 검증
        if (startDate == null || endDate == null) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_INVALID_DATE, "프로젝트 시작일과 종료일은 필수입니다");
        }

        if (startDate.isAfter(endDate)) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_INVALID_DATE, "시작일은 종료일보다 빨라야 합니다");
        }

        // 과거 날짜 검증 (id가 null인 경우만 - 새로 생성하는 경우)
        if (id == null && startDate.isBefore(OffsetDateTime.now())) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_INVALID_DATE, "프로젝트 시작일은 현재보다 미래여야 합니다");
        }

        // 최대 참가자 수 검증
        if (maxParticipants == null || maxParticipants < 1) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_INVALID_PARTICIPANTS, "최대 참가자 수는 1명 이상이어야 합니다");
        }

        // 현재 참가자 수 검증
        if (currentParticipants != null && currentParticipants < 0) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_INVALID_PARTICIPANTS, "현재 참가자 수는 0명 이상이어야 합니다");
        }

        // 현재 참가자 수가 최대 참가자 수를 초과하는지 검증
        if (currentParticipants != null && currentParticipants > maxParticipants) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_INVALID_PARTICIPANTS, "현재 참가자 수는 최대 참가자 수를 초과할 수 없습니다");
        }

        // Creator ID 검증
        if (creatorId == null) {
            throw new DomainException(ExceptionStatus.PROJECT_DOMAIN_INVALID_CREATOR, "프로젝트 생성자 ID는 필수입니다");
        }
    }

    /**
     * 기본 팩토리 메서드 (ProjectDetailResponseDto와 매칭)
     */
    public static ProjectVo of(
            Long id,
            String title,
            String description,
            String content,
            String category,
            String subCategory,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            Long creatorId,
            String creatorName,
            String githubUrl,
            String externalUrl,
            String thumbnailUrl,
            Integer maxParticipants,
            Integer currentParticipants,
            List<ProjectMeetingSummaryVo> meetingSummaryVos
    ) {
        return new ProjectVo(
                id, title, description, content, category, subCategory,
                startDate, endDate, creatorId, creatorName, githubUrl,
                externalUrl, thumbnailUrl, maxParticipants, currentParticipants,
                meetingSummaryVos
        );
    }

    /**
     * 새 프로젝트 생성용 팩토리 메서드 (중복 검사는 외부에서 처리)
     */
    public static ProjectVo createNew(
            String title,
            String description,
            String content,
            String category,
            String subCategory,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            Long creatorId,
            String creatorName,
            String githubUrl,
            String externalUrl,
            String thumbnailUrl,
            Integer maxParticipants
    ) {
        return new ProjectVo(
                null, // id는 null (새 생성)
                title, description, content, category, subCategory,
                startDate, endDate, creatorId, creatorName, githubUrl,
                externalUrl, thumbnailUrl, maxParticipants, 0, // 초기 참가자는 0명
                null
        );
    }

    /**
     * 업데이트용 팩토리 메서드
     */
    public static ProjectVo updateFrom(ProjectVo existing,
                                       String title,
                                       String description,
                                       String content,
                                       String category,
                                       String subCategory,
                                       OffsetDateTime startDate,
                                       OffsetDateTime endDate,
                                       String githubUrl,
                                       String externalUrl,
                                       String thumbnailUrl,
                                       Integer maxParticipants) {
        return new ProjectVo(
                existing.id(),
                title != null ? title : existing.title(),
                description != null ? description : existing.description(),
                content != null ? content : existing.content(),
                category != null ? category : existing.category(),
                subCategory != null ? subCategory : existing.subCategory(),
                startDate != null ? startDate : existing.startDate(),
                endDate != null ? endDate : existing.endDate(),
                existing.creatorId(),
                existing.creatorName(),
                githubUrl != null ? githubUrl : existing.githubUrl(),
                externalUrl != null ? externalUrl : existing.externalUrl(),
                thumbnailUrl != null ? thumbnailUrl : existing.thumbnailUrl(),
                maxParticipants != null ? maxParticipants : existing.maxParticipants(),
                existing.currentParticipants(),
                existing.meetingSummaryVos()
        );
    }
}