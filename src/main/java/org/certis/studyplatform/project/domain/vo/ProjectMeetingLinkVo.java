package org.certis.studyplatform.project.domain.vo;

import java.time.OffsetDateTime;

/**
 * Project Meeting Link Value Object
 *
 * Clean Architecture Domain Layer
 * 프로젝트 회의록 링크 도메인 객체
 */
public record ProjectMeetingLinkVo(
        Long id,
        Long projectId,
        Long memberId,
        String name,
        String attachedUrl,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    /**
     * 생성용 정적 팩토리 메서드
     */
    public static ProjectMeetingLinkVo forCreation(Long projectId, Long memberId, String name, String attachedUrl) {
        return new ProjectMeetingLinkVo(
                null, // ID는 생성 시 null
                projectId,
                memberId,
                name,
                attachedUrl,
                null, // createdAt은 Repository에서 설정
                null  // updatedAt은 Repository에서 설정
        );
    }

    /**
     * 수정용 정적 팩토리 메서드
     */
    public static ProjectMeetingLinkVo forUpdate(Long id, String name, String attachedUrl) {
        return new ProjectMeetingLinkVo(
                id,
                null, // projectId는 변경하지 않음
                null, // memberId는 변경하지 않음
                name,
                attachedUrl,
                null, // createdAt은 변경하지 않음
                null  // updatedAt은 Repository에서 설정
        );
    }

    public static ProjectMeetingLinkVo of(Long id, Long projectId, Long memberId, String name, String attachedUrl, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        return new ProjectMeetingLinkVo(
                id,
                projectId,
                memberId, // memberId는 변경하지 않음
                name,
                attachedUrl,
                null, // createdAt은 변경하지 않음
                null  // updatedAt은 Repository에서 설정
        );

    }
}