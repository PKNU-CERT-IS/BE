package org.certis.studyplatform.study.application.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.study.domain.service.StudyParticipantDomainService;
import org.certis.studyplatform.study.domain.vo.StudyParticipantCreatedVo;
import org.certis.studyplatform.study.domain.vo.StudyParticipantStatusUpdatedVo;
import org.certis.studyplatform.study.application.object.command.CancelStudyParticipantCommand;
import org.certis.studyplatform.study.application.object.command.CreateStudyParticipantCommand;
import org.certis.studyplatform.study.application.object.command.UpdateStudyParticipantStatusCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Study Participant Command Service
 *
 * Clean Architecture Application Layer
 * 프로젝트 참가 관련 쓰기 작업 처리 (CQRS Command Side)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StudyParticipantCommandService {

    private final StudyParticipantDomainService domainService;

    /**
     * 프로젝트 참가 신청
     */
    @Transactional
    public StudyParticipantCreatedVo createParticipant(CreateStudyParticipantCommand command) {
        log.info("Command: Creating participant request - studyId: {}, memberId: {}",
                command.studyId(), command.memberId());

        StudyParticipantCreatedVo result = domainService.createParticipant(command);

        log.info("Command: Participant request created successfully - ID: {}", result.id());
        return result;
    }

    /**
     * 프로젝트 참가 신청 취소
     */
    @Transactional
    public void cancelParticipant(CancelStudyParticipantCommand command) {
        log.info("Command: Cancelling participant request - studyId: {}, memberId: {}",
                command.studyId(), command.memberId());

        domainService.cancelParticipant(command);

        log.info("Command: Participant request cancelled successfully - studyId: {}, memberId: {}",
                command.studyId(), command.memberId());
    }

    /**
     * 프로젝트 참가 승인
     */
    @Transactional
    public StudyParticipantStatusUpdatedVo approveParticipant(UpdateStudyParticipantStatusCommand command) {
        log.info("Command: Approving participant - participantId: {}, requesterId: {}",
                command.participantId(), command.requesterId());

        StudyParticipantStatusUpdatedVo result = domainService.approveParticipant(command);

        log.info("Command: Participant approved successfully - ID: {}", result.id());
        return result;
    }

    @Transactional
    public StudyParticipantStatusUpdatedVo approveParticipant(Long studyId, Long memberId, Long requesterId) {
        log.info("Command: Approving participant by study/member - studyId: {}, memberId: {}, requesterId: {}",
                studyId, memberId, requesterId);

        StudyParticipantStatusUpdatedVo result = domainService.approveParticipant(studyId, memberId, requesterId);

        log.info("Command: Participant approved successfully - ID: {}", result.id());
        return result;
    }

    /**
     * 프로젝트 참가 거절
     */
    @Transactional
    public StudyParticipantStatusUpdatedVo rejectParticipant(UpdateStudyParticipantStatusCommand command) {
        log.info("Command: Rejecting participant - participantId: {}, requesterId: {}",
                command.participantId(), command.requesterId());

        StudyParticipantStatusUpdatedVo result = domainService.rejectParticipant(command);

        log.info("Command: Participant rejected successfully - ID: {}", result.id());
        return result;
    }
}
