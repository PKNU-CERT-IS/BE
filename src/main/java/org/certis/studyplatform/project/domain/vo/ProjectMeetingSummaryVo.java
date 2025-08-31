package org.certis.studyplatform.project.domain.vo;

/**
 * Project Meeting Summary Value Object
 *
 * 프로젝트 회의록 요약 정보를 나타내는 도메인 VO
 * Domain Layer의 불변 객체
 */
public record ProjectMeetingSummaryVo(
    Long id,
    String title,
    Integer participantNumber,
    String creatorName,
    boolean isEditable
) {
    /**
     * 기본 생성자
     */
    public static ProjectMeetingSummaryVo of(
            Long id,
            String title,
            Integer participantNumber,
            String creatorName,
            boolean isEditable) {
        return new ProjectMeetingSummaryVo(id, title, participantNumber, creatorName, isEditable);
    }
} 