package org.certis.studyplatform.study.domain.vo;

import java.time.OffsetDateTime;
import java.util.List;

public record StudyMeetingDetailVo(
        Long id,
        Long studyId,
        String title,
        String content,
        Integer participantNumber,
        Long writerId,
        boolean isEditable,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<StudyMeetingLinkVo> attachedLinks
) {

    /**
     * StudyMeetingVo와 링크 목록으로부터 생성하는 정적 팩토리 메서드
     */
    public static StudyMeetingDetailVo from(StudyMeetingVo meetingVo, List<StudyMeetingLinkVo> links) {
        return new StudyMeetingDetailVo(
                meetingVo.id(),
                meetingVo.studyId(),
                meetingVo.title(),
                meetingVo.content(),
                meetingVo.participantNumber(),
                meetingVo.writerId(),
                meetingVo.isEditable(),
                meetingVo.createdAt(),
                meetingVo.updatedAt(),
                links != null ? links : List.of()
        );
    }

    /**
     * 링크 개수 반환
     */
    public int getLinkCount() {
        return attachedLinks != null ? attachedLinks.size() : 0;
    }

    /**
     * 링크가 있는지 확인
     */
    public boolean hasLinks() {
        return attachedLinks != null && !attachedLinks.isEmpty();
    }

    /**
     * 특정 타입의 링크 필터링 (향후 확장용)
     */
    public List<StudyMeetingLinkVo> getLinksByName(String name) {
        if (attachedLinks == null) {
            return List.of();
        }
        return attachedLinks.stream()
                .filter(link -> name.equals(link.name()))
                .toList();
    }
}