package org.certis.studyplatform.schedule.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminScheduleResponseDto {

    private Long scheduleId;
    private String title;
    private String description;
    private String type;
    private String place;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
    private String status;
    private OffsetDateTime createdAt;

    // 어드민 전용 회원 정보
    private MemberInfo memberInfo;

    /**
     * 회원 정보 중첩 클래스
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MemberInfo {
        private Long memberId;
        private String memberName;
    }
}
