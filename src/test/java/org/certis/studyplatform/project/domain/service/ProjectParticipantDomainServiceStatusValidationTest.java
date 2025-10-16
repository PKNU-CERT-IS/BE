package org.certis.studyplatform.project.domain.service;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.member.domain.repository.query.MemberQueryRepository;
import org.certis.studyplatform.project.domain.repository.ProjectParticipantCommandRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectParticipantDomainServiceStatusValidationTest {

    @Mock
    private ProjectParticipantCommandRepository commandRepository;
    @Mock
    private ProjectParticipantQueryRepository queryRepository;
    @Mock
    private ProjectQueryRepository projectQueryRepository;
    @Mock
    private MemberQueryRepository memberQueryRepository;

    @InjectMocks
    private ProjectParticipantDomainService domainService;

    @Test
    void createParticipant_shouldFail_whenProjectStatusIsCompleted() {
        Long projectId = 300L;
        Long memberId = 20L;

        ProjectVo completed = ProjectVo.of(
                projectId,
                "t", "d", "c",
                "cat", "sub",
                OffsetDateTime.now().minusDays(10),
                OffsetDateTime.now().minusDays(1),
                9L,
                "creator", "A", null,
                org.certis.studyplatform.project.domain.ProjectStatus.COMPLETED.name(),
                ResultSubmitStatus.READY,
                null, null, null, null,
                5, 0,
                false,
                java.util.Collections.emptyList(),
                null
        );

        when(projectQueryRepository.findByIdAndDeletedAtIsNull(projectId)).thenReturn(Optional.of(completed));

        var cmd = new org.certis.studyplatform.project.application.object.command.CreateProjectParticipantCommand(projectId, memberId);

        assertThatThrownBy(() -> domainService.createParticipant(cmd))
                .isInstanceOf(DomainException.class)
                .extracting("status")
                .isEqualTo(ExceptionStatus.PROJECT_DOMAIN_DEADLINE_PASSED);
    }
}


