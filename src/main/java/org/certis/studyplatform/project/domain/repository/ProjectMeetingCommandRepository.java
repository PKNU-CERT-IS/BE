package org.certis.studyplatform.project.domain.repository;

import org.certis.studyplatform.project.domain.vo.ProjectMeetingCreatedVo;
import org.certis.studyplatform.project.domain.vo.ProjectMeetingUpdatedVo;
import org.certis.studyplatform.project.domain.vo.ProjectMeetingVo;

/**
 * Project Meeting Command Repository Interface
 *
 * CQRS Command 측면의 Repository (Write 작업)
 * Domain Layer의 인터페이스
 * Infrastructure Layer에서 JPA로 구현
 * 
 * ✅ CQRS 패턴 준수:
 * - 쓰기 작업(Create, Update, Delete)만 담당
 * - 조회 관련 메서드는 QueryRepository로 분리
 * - 쓰기 작업 시 필요한 검증은 내부적으로 처리
 */
public interface ProjectMeetingCommandRepository {

    /**
     * 프로젝트 회의록 생성
     *
     * @param projectMeetingVo 저장할 회의록 VO
     * @return 생성된 회의록 결과 VO
     */
    ProjectMeetingCreatedVo save(ProjectMeetingVo projectMeetingVo);

    /**
     * 프로젝트 회의록 수정
     * 
     * ✅ CQRS 준수: 존재 여부 확인, 권한 검증, 업데이트를 한 번에 처리
     *
     * @param projectMeetingVo 수정할 회의록 VO (null 필드는 기존 값 유지)
     * @return 수정된 회의록 결과 VO
     * @throws org.certis.studyplatform.exception.DomainException 회의록이 존재하지 않거나 권한이 없는 경우
     */
    ProjectMeetingUpdatedVo update(ProjectMeetingVo projectMeetingVo);

    /**
     * 프로젝트 회의록 삭제 (권한 검증 포함)
     * 
     * ✅ CQRS 준수: 존재 여부 확인, 권한 검증, 삭제를 한 번에 처리
     *
     * @param meetingId 삭제할 회의록 ID
     * @param requesterId 삭제 요청자 ID
     * @throws org.certis.studyplatform.exception.DomainException 회의록이 존재하지 않거나 권한이 없는 경우
     */
    void deleteByIdWithPermission(Long meetingId, Long requesterId);
} 