package org.certis.studyplatform.project.application.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.project.application.object.command.CreateProjectMeetingCommand;
import org.certis.studyplatform.project.application.object.command.DeleteProjectMeetingCommand;
import org.certis.studyplatform.project.application.object.command.UpdateProjectMeetingCommand;
import org.certis.studyplatform.project.domain.service.ProjectMeetingDomainService;
import org.certis.studyplatform.project.domain.vo.ProjectMeetingCreatedVo;
import org.certis.studyplatform.project.domain.vo.ProjectMeetingUpdatedVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Project Meeting Command Service
 *
 * Clean Architecture Application Layer
 * 프로젝트 회의록 쓰기 작업 처리 (CQRS Command Side)
 *
 * Command 객체를 Domain Service로 전달하여 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectMeetingCommandService {

    private final ProjectMeetingDomainService projectMeetingDomainService;

    /**
     * 프로젝트 회의록 생성
     */
    @Transactional
    public ProjectMeetingCreatedVo createProjectMeeting(CreateProjectMeetingCommand command) {
        log.info("MeetingCommand: Creating project meeting - projectId: {}, title: {}", 
                command.projectId(), command.title());

        // Command 객체를 Domain Service로 전달
        ProjectMeetingCreatedVo createdVo = projectMeetingDomainService.createProjectMeeting(command);

        log.info("MeetingCommand: Project meeting created successfully - ID: {}", createdVo.id());
        return createdVo;
    }

    /**
     * 프로젝트 회의록 수정
     */
    @Transactional
    public ProjectMeetingUpdatedVo updateProjectMeeting(UpdateProjectMeetingCommand command) {
        log.info("MeetingCommand: Updating project meeting - meetingId: {}", command.meetingId());

        // Command 객체를 Domain Service로 전달
        ProjectMeetingUpdatedVo updatedVo = projectMeetingDomainService.updateProjectMeeting(command);

        log.info("MeetingCommand: Project meeting updated successfully - ID: {}", updatedVo.id());
        return updatedVo;
    }

    /**
     * 프로젝트 회의록 삭제
     */
    @Transactional
    public void deleteProjectMeeting(DeleteProjectMeetingCommand command) {
        log.info("MeetingCommand: Deleting project meeting - meetingId: {}", command.meetingId());

        // Command 객체를 Domain Service로 전달
        projectMeetingDomainService.deleteProjectMeeting(command);

        log.info("MeetingCommand: Project meeting deleted successfully - ID: {}", command.meetingId());
    }
} 