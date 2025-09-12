package org.certis.studyplatform.study.application.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.study.domain.service.StudyDomainService;
import org.certis.studyplatform.study.domain.service.StudyParticipantDomainService;
import org.certis.studyplatform.study.domain.vo.StudyVo;
import org.certis.studyplatform.study.application.object.command.CreateStudyCommand;
import org.certis.studyplatform.study.application.object.command.DeleteStudyCommand;
import org.certis.studyplatform.study.application.object.command.UpdateStudyCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Study Command Service
 *
 * Clean Architecture Application Layer
 * 프로젝트 쓰기 작업 처리 (CQRS Command Side)
 *
 * Command 객체를 Domain Service로 전달하여 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StudyCommandService {

    private final StudyDomainService studyDomainService;
    private final StudyParticipantDomainService studyParticipantDomainService;

    /**
     * 프로젝트 생성
     */

    //TODO: 추후에 Transactional 결합도 낮추기
    // 1. 생성자 존재 확인
    // 2. 프로젝트 생성
    // 3. 생성자를 참가자로 등록
    @Transactional
    public StudyVo createStudy(CreateStudyCommand command) {
        log.info("Command: Creating study - {}", command.title());

        // Command 객체를 Domain Service로 전달
        StudyVo createdVo = studyDomainService.createStudy(command);

        studyParticipantDomainService.registerStudyCreatorAsParticipant(createdVo.id(), command.creatorId());

        log.info("Command: Study created successfully - ID: {}", createdVo.id());
        return createdVo;
    }

    /**
     * 프로젝트 수정
     */
    @Transactional
    public StudyVo updateStudy(UpdateStudyCommand command) {
        log.info("Command: Updating study - ID: {}", command.id());

        // Command 객체를 Domain Service로 전달
        StudyVo updatedVo = studyDomainService.updateStudy(command);

        log.info("Command: Study updated successfully - ID: {}", updatedVo.id());
        return updatedVo;
    }

    /**
     * 프로젝트 삭제
     */
    @Transactional
    public void deleteStudy(DeleteStudyCommand command) {
        log.info("Command: Deleting study - ID: {}", command.id());

        // Command 객체를 Domain Service로 전달
        studyDomainService.deleteStudy(command);

        log.info("Command: Study deleted successfully - ID: {}", command.id());
    }
}