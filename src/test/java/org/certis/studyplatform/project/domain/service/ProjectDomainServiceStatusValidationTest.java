package org.certis.studyplatform.project.domain.service;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.member.domain.MemberRole;
import org.certis.studyplatform.member.domain.service.MemberDomainService;
import org.certis.studyplatform.member.domain.vo.MemberVo;
import org.certis.studyplatform.project.application.object.command.EndProjectCommand;
import org.certis.studyplatform.project.domain.ProjectStatus;
import org.certis.studyplatform.project.domain.repository.ProjectCommandRepository;
import org.certis.studyplatform.project.domain.repository.ProjectParticipantQueryRepository;
import org.certis.studyplatform.project.domain.repository.ProjectQueryRepository;
import org.certis.studyplatform.project.domain.vo.ProjectVo;
import org.certis.studyplatform.shared.domain.ResultSubmitStatus;
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
class ProjectDomainServiceStatusValidationTest {

    @Mock
    private ProjectCommandRepository commandRepository;
    @Mock
    private ProjectQueryRepository queryRepository;
    @Mock
    private MemberDomainService memberDomainService;
    @Mock
    private ProjectParticipantQueryRepository projectParticipantQueryRepository;

    @InjectMocks
    private ProjectDomainService domainService;

    @Test
    void endProject_shouldFail_whenStatusIsCompleted() {
        Long projectId = 100L;
        Long creatorId = 10L;
        Long requesterId = creatorId; // creator -> passes permission

        ProjectVo completed = ProjectVo.of(
                projectId,
                "t", "d", "c",
                "cat", "sub",
                OffsetDateTime.now().minusDays(10),
                OffsetDateTime.now().minusDays(1),
                creatorId,
                "creator", "A", null,
                ProjectStatus.COMPLETED.name(),
                ResultSubmitStatus.READY,
                null, null, null, null,
                5, 0,
                false,
                java.util.Collections.emptyList(),
                null
        );

        when(queryRepository.findById(projectId)).thenReturn(Optional.of(completed));
        when(memberDomainService.getMemberVo(any())).thenReturn(
                MemberVo.of(requesterId, "name", "s", null, null, MemberRole.PLAYER, java.util.List.of(), null, null, OffsetDateTime.now(), OffsetDateTime.now())
        );

        EndProjectCommand cmd = EndProjectCommand.of(projectId, requesterId, null);

        assertThatThrownBy(() -> domainService.endProject(cmd))
                .isInstanceOf(DomainException.class)
                .hasMessage("이미 종료된 프로젝트입니다")
                .extracting("status")
                .isEqualTo(ExceptionStatus.PROJECT_DOMAIN_RULE_VIOLATION);
    }
}


