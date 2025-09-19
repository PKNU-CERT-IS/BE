package org.certis.studyplatform.project.domain.repository;

import org.certis.studyplatform.project.domain.vo.ProjectParticipantCreatedVo;
import org.certis.studyplatform.project.domain.vo.ProjectParticipantStatusUpdatedVo;
import org.certis.studyplatform.project.domain.vo.ProjectParticipantVo;

/**
 * Project Participant Command Repository Interface
 *
 * CQRS Command 측면의 Repository (Write 작업)
 * Domain Layer의 인터페이스
 * Infrastructure Layer에서 JPA로 구현
 */
public interface ProjectParticipantCommandRepository {

    /**
     * 프로젝트 참가 신청 생성
     */
    ProjectParticipantCreatedVo save(ProjectParticipantVo participantVo);

    /**
     * 프로젝트 참가자 상태 업데이트
     */
    ProjectParticipantStatusUpdatedVo updateStatus(ProjectParticipantVo participantVo);

    /**
     * 프로젝트 참가 신청 취소 (소프트 삭제)
     */
    void deleteByProjectIdAndMemberId(Long projectId, Long memberId);

    /**
     * 참가 신청 단건 하드 삭제 (승인 취소 등)
     */
    void deleteByIdHard(Long participantId);

    /**
     * 참가 신청 단건 소프트 삭제 (거절 등)
     */
    void softDeleteById(Long participantId);
}