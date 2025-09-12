package org.certis.studyplatform.study.application.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.study.domain.service.StudyMeetingDomainService;
import org.certis.studyplatform.study.domain.vo.StudyMeetingCreatedVo;
import org.certis.studyplatform.study.domain.vo.StudyMeetingUpdatedVo;
import org.certis.studyplatform.study.application.object.command.CreateStudyMeetingCommand;
import org.certis.studyplatform.study.application.object.command.DeleteStudyMeetingCommand;
import org.certis.studyplatform.study.application.object.command.UpdateStudyMeetingCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Study Meeting Command Service
 *
 * Clean Architecture Application Layer
 * 프로젝트 회의록 쓰기 작업 처리 (CQRS Command Side)
 *
 * Command 객체를 Domain Service로 전달하여 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StudyMeetingCommandService {

    private final StudyMeetingDomainService studyMeetingDomainService;

    /**
     * 프로젝트 회의록 생성
     */
    @Transactional
    public StudyMeetingCreatedVo createStudyMeeting(CreateStudyMeetingCommand command) {
        log.info("MeetingCommand: Creating study meeting - studyId: {}, title: {}",
                command.studyId(), command.title());

        // Command 객체를 Domain Service로 전달
        StudyMeetingCreatedVo createdVo = studyMeetingDomainService.createStudyMeeting(command);

        log.info("MeetingCommand: Study meeting created successfully - ID: {}", createdVo.id());
        return createdVo;
    }

    /**
     * 프로젝트 회의록 수정
     */
    @Transactional
    public StudyMeetingUpdatedVo updateStudyMeeting(UpdateStudyMeetingCommand command) {
        log.info("MeetingCommand: Updating study meeting - meetingId: {}", command.meetingId());

        // Command 객체를 Domain Service로 전달
        StudyMeetingUpdatedVo updatedVo = studyMeetingDomainService.updateStudyMeeting(command);

        log.info("MeetingCommand: Study meeting updated successfully - ID: {}", updatedVo.id());
        return updatedVo;
    }

    /**
     * 프로젝트 회의록 삭제
     */
    @Transactional
    public void deleteStudyMeeting(DeleteStudyMeetingCommand command) {
        log.info("MeetingCommand: Deleting study meeting - meetingId: {}", command.meetingId());

        // Command 객체를 Domain Service로 전달
        studyMeetingDomainService.deleteStudyMeeting(command);

        log.info("MeetingCommand: Study meeting deleted successfully - ID: {}", command.meetingId());
    }
} 