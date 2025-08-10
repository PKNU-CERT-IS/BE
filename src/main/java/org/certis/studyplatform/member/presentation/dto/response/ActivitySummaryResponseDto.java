package org.certis.studyplatform.member.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.certis.studyplatform.member.domain.vo.ActivitySummaryVo;


@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivitySummaryResponseDto {

    private Long recentStudies;
    private Long recentProjects;
    private Long recentBlogs;
    private Long totalActivities;
    private Boolean isActiveUser;
    private String primaryFocus; // STUDY, PROJECT, BLOG, BALANCED

    /**
     * Domain VO → Response DTO 변환
     */
    public static ActivitySummaryResponseDto from(ActivitySummaryVo vo) {
        String primaryFocus = determinePrimaryFocus(vo);

        return ActivitySummaryResponseDto.builder()
                .recentStudies(vo.recentStudies())
                .recentProjects(vo.recentProjects())
                .recentBlogs(vo.recentBlogs())
                .totalActivities(vo.getTotalActivities())
                .isActiveUser(vo.isActiveUser())
                .primaryFocus(primaryFocus)
                .build();
    }

    /**
     * 주요 활동 영역 판단
     */
    private static String determinePrimaryFocus(ActivitySummaryVo vo) {
        if (vo.isBalancedActivity()) {
            return "BALANCED";
        } else if (vo.isStudyFocused()) {
            return "STUDY";
        } else if (vo.isProjectFocused()) {
            return "PROJECT";
        } else if (vo.isBlogFocused()) {
            return "BLOG";
        } else {
            return "NONE";
        }
    }
}
