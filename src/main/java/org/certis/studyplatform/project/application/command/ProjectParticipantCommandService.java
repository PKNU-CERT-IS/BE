package org.certis.studyplatform.project.application.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.project.application.object.command.*;
import org.certis.studyplatform.project.domain.service.ProjectParticipantDomainService;
import org.certis.studyplatform.project.domain.vo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Project Participant Command Service
 *
 * Clean Architecture Application Layer
 * 프로젝트 참가 관련 쓰기 작업 처리 (CQRS Command Side)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectParticipantCommandService {

    private final ProjectParticipantDomainService domainService;

    /**
     * 프로젝트 참가 신청
     */
    @Transactional
    public ProjectParticipantCreatedVo createParticipant(CreateProjectParticipantCommand command) {
        log.info("Command: Creating participant request - projectId: {}, memberId: {}",
                command.projectId(), command.memberId());

        ProjectParticipantCreatedVo result = domainService.createParticipant(command);

        log.info("Command: Participant request created successfully - ID: {}", result.id());
        return result;
    }

    /**
     * 프로젝트 참가 신청 취소
     */
    @Transactional
    public void cancelParticipant(CancelProjectParticipantCommand command) {
        log.info("Command: Cancelling participant request - projectId: {}, memberId: {}",
                command.projectId(), command.memberId());

        domainService.cancelParticipant(command);

        log.info("Command: Participant request cancelled successfully - projectId: {}, memberId: {}",
                command.projectId(), command.memberId());
    }

    /**
     * 프로젝트 참가 승인
     */
    @Transactional
    public ProjectParticipantStatusUpdatedVo approveParticipant(UpdateProjectParticipantStatusCommand command) {
        log.info("Command: Approving participant - participantId: {}, requesterId: {}",
                command.participantId(), command.requesterId());

        ProjectParticipantStatusUpdatedVo result = domainService.approveParticipant(command);

        log.info("Command: Participant approved successfully - ID: {}", result.id());
        return result;
    }

    @Transactional
    public ProjectParticipantStatusUpdatedVo approveParticipant(Long projectId, Long memberId, Long requesterId) {
        log.info("Command: Approving participant by project/member - projectId: {}, memberId: {}, requesterId: {}",
                projectId, memberId, requesterId);

        ProjectParticipantStatusUpdatedVo result = domainService.approveParticipant(projectId, memberId, requesterId);

        log.info("Command: Participant approved successfully - ID: {}", result.id());
        return result;
    }

    /**
     * 프로젝트 참가 거절
     */
    @Transactional
    public ProjectParticipantStatusUpdatedVo rejectParticipant(UpdateProjectParticipantStatusCommand command) {
        log.info("Command: Rejecting participant - participantId: {}, requesterId: {}",
                command.participantId(), command.requesterId());

        ProjectParticipantStatusUpdatedVo result = domainService.rejectParticipant(command);

        log.info("Command: Participant rejected successfully - ID: {}", result.id());
        return result;
    }

    /**
     * 소프트 삭제된 참가 신청 복원 (있으면 true)
     */
    @Transactional
    public boolean restoreLatestSoftDeleted(Long projectId, Long memberId) {
        return domainService.restoreLatestSoftDeleted(projectId, memberId);
    }
}
