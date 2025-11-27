package org.certis.studyplatform.study.domain.service;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.member.domain.MemberRole;
import org.certis.studyplatform.member.domain.service.MemberDomainService;
import org.certis.studyplatform.member.domain.vo.MemberVo;
import org.certis.studyplatform.study.application.object.command.EndStudyCommand;
import org.certis.studyplatform.study.domain.StudyStatus;
import org.certis.studyplatform.study.domain.repository.StudyCommandRepository;
import org.certis.studyplatform.study.domain.repository.StudyParticipantQueryRepository;
import org.certis.studyplatform.study.domain.repository.StudyQueryRepository;
import org.certis.studyplatform.study.domain.vo.StudyVo;
import org.certis.studyplatform.shared.domain.ResultSubmitStatus;
import org.certis.studyplatform.study.infrastructure.persistence.entity.StudyEntity;
import org.certis.studyplatform.study.domain.StudyStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudyDomainServiceStatusValidationTest {

    @Mock
    private StudyCommandRepository commandRepository;
    @Mock
    private StudyQueryRepository queryRepository;
    @Mock
    private MemberDomainService memberDomainService;
    @Mock
    private StudyParticipantQueryRepository studyParticipantQueryRepository;
    @Mock
    private org.certis.studyplatform.project.domain.repository.ProjectParticipantQueryRepository projectParticipantQueryRepository;
    @Mock
    private org.certis.studyplatform.project.domain.repository.ProjectQueryRepository projectQueryRepository;

    @InjectMocks
    private StudyDomainService domainService;

    @Test
    void endStudy_shouldFail_whenStatusIsCompleted() {
        Long studyId = 200L;
        Long creatorId = 11L;
        Long requesterId = creatorId; // creator

        StudyVo completed = StudyVo.of(
                studyId,
                "t", "d", "c",
                "cat", "sub",
                OffsetDateTime.now().minusDays(10),
                OffsetDateTime.now().minusDays(1),
                OffsetDateTime.now().minusDays(10),
                OffsetDateTime.now().minusDays(1),
                creatorId,
                "creator", null, null,
                null,
                StudyStatus.COMPLETED.name(),
                ResultSubmitStatus.READY,
                5, 0,
                false,
                java.util.Collections.emptyList()
        );

        when(queryRepository.findById(studyId)).thenReturn(Optional.of(completed));
        when(queryRepository.findVoByIdForStatusCheck(studyId)).thenReturn(Optional.of(
                StudyVo.of(
                        studyId,
                        "t", "d", "c",
                        "cat", "sub",
                        OffsetDateTime.now().minusDays(10),
                        OffsetDateTime.now().minusDays(1),
                        OffsetDateTime.now().minusDays(10),
                        OffsetDateTime.now().minusDays(1),
                        creatorId,
                        "creator", null, null,
                        null,
                        StudyStatus.COMPLETED.name(),
                        ResultSubmitStatus.READY,
                        5, 0,
                        false,
                        java.util.Collections.emptyList()
                )
        ));
        when(memberDomainService.getMemberVo(any())).thenReturn(
                MemberVo.of(requesterId, "name", "s", null, null, MemberRole.PLAYER, java.util.List.of(), null, null, OffsetDateTime.now(), OffsetDateTime.now())
        );

        EndStudyCommand cmd = EndStudyCommand.of(studyId, requesterId, null);

        assertThatThrownBy(() -> domainService.endStudy(cmd))
                .isInstanceOf(DomainException.class)
                .hasMessage("이미 종료된 스터디입니다")
                .extracting("status")
                .isEqualTo(ExceptionStatus.STUDY_DOMAIN_RULE_VIOLATION);
    }
}


