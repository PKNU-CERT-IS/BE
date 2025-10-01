package org.certis.studyplatform.study.application;

import org.certis.studyplatform.member.application.query.MemberQueryService;
import org.certis.studyplatform.study.application.command.StudyParticipantCommandService;
import org.certis.studyplatform.study.application.mapper.StudyApplicationCommandMapper;
import org.certis.studyplatform.study.application.mapper.StudyApplicationDtoMapper;
import org.certis.studyplatform.study.application.object.query.GetStudyByIdQuery;
import org.certis.studyplatform.study.application.query.StudyParticipantQueryService;
import org.certis.studyplatform.study.application.query.StudyQueryService;
import org.certis.studyplatform.study.domain.StudyParticipantStatus;
import org.certis.studyplatform.study.domain.vo.StudyParticipantStatusUpdatedVo;
import org.certis.studyplatform.study.domain.vo.StudyParticipantVo;
import org.certis.studyplatform.study.domain.vo.StudyVo;
import org.certis.studyplatform.study.presentation.dto.request.StudyJoinApproveRequestDto;
import org.certis.studyplatform.study.presentation.dto.request.StudyJoinRejectRequestDto;
import org.certis.studyplatform.study.presentation.dto.response.StudyParticipantStatusUpdateResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class StudyParticipantFacadeServiceTest {

    private final StudyParticipantCommandService commandService = mock(StudyParticipantCommandService.class);
    private final StudyParticipantQueryService participantQueryService = mock(StudyParticipantQueryService.class);
    private final StudyApplicationCommandMapper commandMapper = mock(StudyApplicationCommandMapper.class, Mockito.RETURNS_DEEP_STUBS);
    private final StudyApplicationDtoMapper dtoMapper = mock(StudyApplicationDtoMapper.class, Mockito.RETURNS_DEEP_STUBS);
    private final MemberQueryService memberQueryService = mock(MemberQueryService.class);
    private final StudyQueryService studyQueryService = mock(StudyQueryService.class);

    private final StudyParticipantFacadeService facade = new StudyParticipantFacadeService(
            commandService,
            participantQueryService,
            commandMapper,
            dtoMapper,
            memberQueryService,
            studyQueryService
    );

    private StudyVo studyVo(Long studyId, Long creatorId) {
        return StudyVo.createForTest(
                studyId,
                "title",
                "desc",
                "content",
                "CTF",
                "PWN",
                OffsetDateTime.now().minusDays(1),
                OffsetDateTime.now().plusDays(10),
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                creatorId,
                "creator",
                org.certis.studyplatform.member.domain.MemberGrade.FRESHMAN,
                null, // creatorProfileImageUrl
                "2025-2",
                "ACTIVE",
                10,
                0,
                true,
                java.util.Collections.emptyList(),
                java.util.Collections.emptyList(),
                java.util.Collections.emptyList()
        );
    }

    private StudyParticipantVo participantVo(Long id, Long studyId, Long memberId, StudyParticipantStatus status) {
        return new StudyParticipantVo(id, studyId, memberId, "member", status, OffsetDateTime.now(), OffsetDateTime.now());
    }

    @Test
    @DisplayName("스터디 생성자가 승인 성공")
    void approve_by_creator_success() {
        Long studyId = 1L;
        Long memberId = 2L;
        Long creatorId = 10L;
        Long participantId = 100L;

        when(studyQueryService.getStudyById(GetStudyByIdQuery.of(studyId))).thenReturn(studyVo(studyId, creatorId));
        when(participantQueryService.getByStudyIdAndMemberId(studyId, memberId)).thenReturn(Optional.of(participantVo(participantId, studyId, memberId, StudyParticipantStatus.PENDING)));
        when(commandService.approveParticipant(any())).thenReturn(new StudyParticipantStatusUpdatedVo(
                participantId, studyId, memberId, StudyParticipantStatus.PENDING, StudyParticipantStatus.APPROVED, OffsetDateTime.now(), creatorId
        ));

        StudyJoinApproveRequestDto req = new StudyJoinApproveRequestDto();
        req.setStudyId(studyId);
        req.setMemberId(memberId);

        StudyParticipantStatusUpdateResponseDto res = facade.approveJoinStudy(req, creatorId);
        assertThat(res.getCurrentStatus()).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("스터디 비생성자 승인 시 권한 예외")
    void approve_non_creator_forbidden() {
        Long studyId = 1L;
        Long memberId = 2L;
        Long requesterId = 99L; // not creator

        when(studyQueryService.getStudyById(GetStudyByIdQuery.of(studyId))).thenReturn(studyVo(studyId, 10L));

        StudyJoinApproveRequestDto req = new StudyJoinApproveRequestDto();
        req.setStudyId(studyId);
        req.setMemberId(memberId);

        assertThatThrownBy(() -> facade.approveJoinStudy(req, requesterId))
                .isInstanceOf(org.certis.studyplatform.exception.ApplicationException.class)
                .hasMessageContaining("승인/거절 권한");
    }
}


