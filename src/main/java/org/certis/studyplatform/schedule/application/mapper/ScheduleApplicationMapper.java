package org.certis.studyplatform.schedule.application.mapper;

import org.certis.studyplatform.schedule.domain.model.vo.AdminScheduleVo;
import org.certis.studyplatform.schedule.domain.model.vo.ScheduleVo;
import org.certis.studyplatform.schedule.presentation.dto.response.AdminScheduleResponseDto;
import org.certis.studyplatform.schedule.presentation.dto.response.ScheduleResponseDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ScheduleApplicationMapper {

    /**
     * ScheduleVo → ScheduleResponseDto 변환
     */
    public ScheduleResponseDto toScheduleResponseDto(ScheduleVo scheduleVo) {
        if (scheduleVo == null) {
            return null;
        }

        return ScheduleResponseDto.builder()
                .scheduleId(scheduleVo.id())
                .title(scheduleVo.title())
                .description(scheduleVo.description())
                .type(scheduleVo.type())
                .place(scheduleVo.place())
                .startedAt(scheduleVo.startedAt())
                .endedAt(scheduleVo.endedAt())
                .status(scheduleVo.status())
                .createdAt(scheduleVo.createdAt())
                .build();
    }

    /**
     * ScheduleVo List → ScheduleResponseDto List 변환
     */
    public List<ScheduleResponseDto> toScheduleResponseDtoList(List<ScheduleVo> scheduleVos) {
        if (scheduleVos == null) {
            return List.of();
        }

        return scheduleVos.stream()
                .map(this::toScheduleResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * AdminScheduleVo → AdminScheduleResponseDto 변환
     */
    public AdminScheduleResponseDto toAdminScheduleResponseDto(AdminScheduleVo adminScheduleVo) {
        if (adminScheduleVo == null) {
            return null;
        }

        return AdminScheduleResponseDto.builder()
                .scheduleId(adminScheduleVo.id())
                .title(adminScheduleVo.title())
                .description(adminScheduleVo.description())
                .type(adminScheduleVo.type())
                .place(adminScheduleVo.place())
                .startedAt(adminScheduleVo.startedAt())
                .endedAt(adminScheduleVo.endedAt())
                .status(adminScheduleVo.status())
                .createdAt(adminScheduleVo.createdAt())
                .memberInfo(AdminScheduleResponseDto.MemberInfo.builder()
                        .memberId(adminScheduleVo.memberId())
                        .memberName(adminScheduleVo.memberName())
                        .build())
                .build();
    }

    /**
     * AdminScheduleVo List → AdminScheduleResponseDto List 변환
     */
    public List<AdminScheduleResponseDto> toAdminScheduleResponseDtoList(List<AdminScheduleVo> adminScheduleVos) {
        if (adminScheduleVos == null) {
            return List.of();
        }

        return adminScheduleVos.stream()
                .map(this::toAdminScheduleResponseDto)
                .collect(Collectors.toList());
    }
}
