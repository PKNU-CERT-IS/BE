package org.certis.studyplatform.project.domain.repository;

import org.certis.studyplatform.project.domain.vo.ProjectUpdateVo;
import org.certis.studyplatform.project.domain.vo.ProjectVo;

/**
 * Project Command Repository Interface
 *
 * CQRS Command 측면의 Repository (Write 작업)
 * Domain Layer의 인터페이스
 * Infrastructure Layer에서 JPA로 구현
 * VO 기반으로 Entity 변환은 Infrastructure Layer에서 처리
 */
public interface ProjectCommandRepository {

    /**
     * 프로젝트 생성/수정
     *
     * @param projectVo 저장할 프로젝트 VO
     * @return 저장된 프로젝트 VO
     */
    ProjectVo save(ProjectVo projectVo);

    /**
     * 프로젝트 삭제 (Soft Delete)
     *
     * @param id 삭제할 프로젝트 ID
     */
    void deleteById(Long id);
}