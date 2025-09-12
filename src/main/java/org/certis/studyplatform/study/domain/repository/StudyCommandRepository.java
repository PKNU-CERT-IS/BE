package org.certis.studyplatform.study.domain.repository;

import org.certis.studyplatform.study.domain.vo.StudyVo;

/**
 * Study Command Repository Interface
 *
 * CQRS Command 측면의 Repository (Write 작업)
 * Domain Layer의 인터페이스
 * Infrastructure Layer에서 JPA로 구현
 * VO 기반으로 Entity 변환은 Infrastructure Layer에서 처리
 */
public interface StudyCommandRepository {

    /**
     * 스터디 생성/수정
     *
     * @param studyVo 저장할 스터디 VO
     * @return 저장된 스터디 VO
     */
    StudyVo save(StudyVo studyVo);

    /**
     * 스터디 삭제 (Soft Delete)
     *
     * @param id 삭제할 스터디 ID
     */
    void deleteById(Long id);
}