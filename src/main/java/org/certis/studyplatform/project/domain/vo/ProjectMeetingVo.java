package org.certis.studyplatform.project.domain.vo;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;

import java.time.OffsetDateTime;

/**
 * Project Meeting Value Object
 *
 * 프로젝트 회의록 상세 정보를 나타내는 도메인 VO
 * Domain Layer의 불변 객체
 */
public record ProjectMeetingVo(
        Long id,
        Long projectId,
        String title,
        String content,
        Integer participantNumber,
        Long writerId,
        boolean isEditable,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    // 상수 정의
    private static final int MAX_TITLE_LENGTH = 30;
    // no content length upper bound
    private static final int MIN_TITLE_LENGTH = 1;
    private static final int MIN_CONTENT_LENGTH = 1;

    /**
     * 기본 팩토리 메서드
     */
    public static ProjectMeetingVo of(
            Long id,
            Long projectId,
            String title,
            String content,
            Integer participantNumber,
            Long writerId,
            boolean isEditable,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        validateTitle(title);
        validateContent(content);

        return new ProjectMeetingVo(
                id, projectId, title, content,
                participantNumber, writerId, isEditable,
                createdAt, updatedAt
        );
    }

    /**
     * 제목 검증
     */
    private static void validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new DomainException(ExceptionStatus.PROJECT_MEETING_DOMAIN_INVALID_TITLE);
        }

        if (title.length() < MIN_TITLE_LENGTH) {
            throw new DomainException(ExceptionStatus.PROJECT_MEETING_DOMAIN_TITLE_TOO_SHORT);
        }

        if (title.length() > MAX_TITLE_LENGTH) {
            throw new DomainException(ExceptionStatus.PROJECT_MEETING_DOMAIN_TITLE_TOO_LONG);
        }
    }

    /**
     * 내용 검증
     */
    private static void validateContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new DomainException(ExceptionStatus.PROJECT_MEETING_DOMAIN_INVALID_CONTENT);
        }

        if (content.length() < MIN_CONTENT_LENGTH) {
            throw new DomainException(ExceptionStatus.PROJECT_MEETING_DOMAIN_CONTENT_TOO_SHORT);
        }

        // no upper bound validation for content length
    }
}