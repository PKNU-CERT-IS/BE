package org.certis.studyplatform.study.domain.vo;

public record StudyMeetingSummaryWithLinksVo(
        Long id,
        String title,
        Integer participantNumber,
        String creatorName,
        boolean isEditable,
        Integer linkCount
) {

    /**
     * StudyMeetingSummaryVo와 링크 개수로부터 생성하는 정적 팩토리 메서드
     */
    public static StudyMeetingSummaryWithLinksVo from(StudyMeetingSummaryVo summaryVo, int linkCount) {
        return new StudyMeetingSummaryWithLinksVo(
                summaryVo.id(),
                summaryVo.title(),
                summaryVo.participantNumber(),
                summaryVo.creatorName(),
                summaryVo.isEditable(),
                linkCount
        );
    }

    /**
     * 링크가 있는지 확인
     */
    public boolean hasLinks() {
        return linkCount != null && linkCount > 0;
    }

    /**
     * 링크 개수 안전한 반환
     */
    public int safeLinkCount() {
        return linkCount != null ? linkCount : 0;
    }
}
