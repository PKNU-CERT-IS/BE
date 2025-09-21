package org.certis.studyplatform.project.domain.vo;

import org.certis.studyplatform.member.domain.MemberGrade;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Project Summary Value Object
 *
 * 프로젝트 목록 조회 시 사용되는 요약 정보
 */
public record ProjectSummaryVo(
    Long id,
    String title,
    String description,
    String category,
    String subcategory,
    OffsetDateTime startDate,
    OffsetDateTime endDate,
    String projectCreatorName,
    MemberGrade projectCreatorGrade,
    String semester,
    String status,
    boolean isParticipantable,
    String githubUrl,
    ExternalUrlVo externalUrl,
    String demoUrl,
    Integer maxParticipantNumber,
    Integer currentParticipantNumber
) {
    public static ProjectSummaryVo of(
        Long id,
        String title,
        String description,
        String category,
        String subcategory,
        OffsetDateTime startDate,
        OffsetDateTime endDate,
        String projectCreatorName,
        MemberGrade projectCreatorGrade,
        String semester,
        String status,
        boolean isParticipantable,
        String githubUrl,
        ExternalUrlVo externalUrl,
        String demoUrl,
        Integer maxParticipantNumber,
        Integer currentParticipantNumber
    ) {
        return new ProjectSummaryVo(
            id, title, description, category, subcategory,
            startDate, endDate, projectCreatorName, projectCreatorGrade,
            semester, status, isParticipantable, githubUrl, externalUrl,
            demoUrl, maxParticipantNumber, currentParticipantNumber
        );
    }
}