package org.certis.studyplatform.member.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 스케줄 정보 응답 DTO
 * 
 * 프로필에서 사용하는 오늘의 스케줄 정보
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleInfoResponseDto {
    
    private Long id;
    private String title;
    private String place;
    private String type;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime startTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime endTime;
}
