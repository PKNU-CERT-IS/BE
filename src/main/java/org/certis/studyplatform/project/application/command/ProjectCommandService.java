package org.certis.studyplatform.project.application.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.project.application.object.command.CreateProjectCommand;
import org.certis.studyplatform.project.application.object.command.DeleteProjectCommand;
import org.certis.studyplatform.project.application.object.command.UpdateProjectCommand;
import org.certis.studyplatform.project.domain.service.ProjectDomainService;
import org.certis.studyplatform.project.domain.service.ProjectParticipantDomainService;
import org.certis.studyplatform.project.domain.vo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Project Command Service
 *
 * Clean Architecture Application Layer
 * 프로젝트 쓰기 작업 처리 (CQRS Command Side)
 *
 * Command 객체를 Domain Service로 전달하여 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectCommandService {

    private final ProjectDomainService projectDomainService;
    private final ProjectParticipantDomainService projectParticipantDomainService;

    /**
     * 프로젝트 생성
     */

    //TODO: 추후에 Transactional 결합도 낮추기
    // 1. 생성자 존재 확인
    // 2. 프로젝트 생성
    // 3. 생성자를 참가자로 등록
    @Transactional
    public ProjectVo createProject(CreateProjectCommand command) {
        log.info("Command: Creating project - {}", command.title());

        // Command 객체를 Domain Service로 전달
        ProjectVo createdVo = projectDomainService.createProject(command);

        projectParticipantDomainService.registerProjectCreatorAsParticipant(createdVo.id(), command.creatorId());

        log.info("Command: Project created successfully - ID: {}", createdVo.id());
        return createdVo;
    }

    /**
     * 프로젝트 수정
     */
    @Transactional
    public ProjectVo updateProject(UpdateProjectCommand command) {
        log.info("Command: Updating project - ID: {}", command.id());

        // Command 객체를 Domain Service로 전달
        ProjectVo updatedVo = projectDomainService.updateProject(command);

        log.info("Command: Project updated successfully - ID: {}", updatedVo.id());
        return updatedVo;
    }

    /**
     * 프로젝트 삭제
     */
    @Transactional
    public void deleteProject(DeleteProjectCommand command) {
        log.info("Command: Deleting project - ID: {}", command.id());

        // Command 객체를 Domain Service로 전달
        projectDomainService.deleteProject(command);

        log.info("Command: Project deleted successfully - ID: {}", command.id());
    }
}