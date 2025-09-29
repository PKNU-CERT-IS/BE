package org.certis.studyplatform.study.domain.repository;

import org.certis.studyplatform.study.domain.vo.StudyAttachedVo;

import java.util.List;

/**
 * Study Attached Command Repository Interface
 *
 * CQRS Command 측면의 Repository (Write 작업)
 * Domain Layer의 인터페이스
 * Infrastructure Layer에서 JPA로 구현
 * VO 기반으로 Entity 변환은 Infrastructure Layer에서 처리
 */
public interface StudyAttachedCommandRepository {

    /**
     * 스터디 첨부파일 저장
     *
     * @param studyId 스터디 ID
     * @param attachment 저장할 첨부파일 VO
     */
    void save(Long studyId, StudyAttachedVo attachment);

    /**
     * 스터디 첨부파일 삭제 (스터디 ID로 전체 삭제)
     *
     * @param studyId 삭제할 스터디 ID
     */
    void deleteByStudyId(Long studyId);

}
