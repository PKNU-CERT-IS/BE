package org.certis.studyplatform.study.domain.vo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Study Meeting Page Result Value Object
 *
 * Clean Architecture Domain Layer
 * 페이징된 회의록 목록과 링크 통계 정보를 포함하는 응답 객체
 */
public record StudyMeetingPageResultVo(
        Page<StudyMeetingSummaryWithLinksVo> meetings,
        long totalLinkCount,
        Map<Long, Integer> linkCountByMeetingId,
        Map<Long, List<StudyMeetingLinkVo>> linksByMeetingId
) {

    /**
     * 페이징된 회의록 목록과 전체 링크 목록으로부터 생성하는 정적 팩토리 메서드
     */
    public static StudyMeetingPageResultVo from(
            Page<StudyMeetingSummaryVo> meetingPage,
            List<StudyMeetingLinkVo> allLinks) {

        Map<Long, List<StudyMeetingLinkVo>> linksByMeetingId = allLinks.stream()
                .collect(Collectors.groupingBy(StudyMeetingLinkVo::meetingId));

        // 회의록별 링크 개수 계산
        Map<Long, Integer> linkCountByMeetingId = linksByMeetingId.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().size()
                ));

        // 회의록 요약 정보와 링크 개수를 조합
        List<StudyMeetingSummaryWithLinksVo> meetingsWithLinks = meetingPage.getContent().stream()
                .map(summary -> StudyMeetingSummaryWithLinksVo.from(
                        summary,
                        linkCountByMeetingId.getOrDefault(summary.id(), 0)
                ))
                .toList();

        // 새로운 페이지 객체 생성
        Page<StudyMeetingSummaryWithLinksVo> pageWithLinks = new PageImpl<>(
                meetingsWithLinks,
                meetingPage.getPageable(),
                meetingPage.getTotalElements()
        );

        return new StudyMeetingPageResultVo(
                pageWithLinks,
                allLinks.size(),
                linkCountByMeetingId,
                linksByMeetingId
        );
    }

    /**
     * 평균 링크 개수 계산
     */
    public double getAverageLinkCount() {
        if (meetings.isEmpty()) {
            return 0.0;
        }
        return (double) totalLinkCount / meetings.getTotalElements();
    }

    /**
     * 링크가 있는 회의록 개수
     */
    public long getMeetingsWithLinksCount() {
        return linkCountByMeetingId.values().stream()
                .mapToLong(count -> count > 0 ? 1 : 0)
                .sum();
    }
}
