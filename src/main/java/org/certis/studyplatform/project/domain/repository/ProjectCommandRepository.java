package org.certis.studyplatform.project.domain.repository;

import org.certis.studyplatform.project.domain.vo.ProjectVo;
import org.springframework.web.multipart.MultipartFile;

import org.certis.studyplatform.shared.domain.ResultSubmitStatus;

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

    /**
     * 프로젝트 첨부파일 업로드
     *
     * @param projectId 프로젝트 ID
     * @param memberId 사용자 ID
     * @param file 업로드할 파일
     * @return 업로드된 파일 URL
     */
    String uploadProjectAttachment(Long projectId, Long memberId, MultipartFile file);

    // ===== End submission and lifecycle commands =====
    void updateResultSubmission(Long projectId, java.time.OffsetDateTime submittedAt,
                                ResultSubmitStatus status,
                                String attachmentUrl);

    void approveEnd(Long projectId, java.time.OffsetDateTime endedAt,
                    ResultSubmitStatus status);

    void rejectEnd(Long projectId, ResultSubmitStatus status,
                   java.time.OffsetDateTime now);

    void bulkSoftDeleteById(Long projectId, java.time.OffsetDateTime deletedAt);

    java.util.Optional<String> getResultAttachmentUrlById(Long projectId);

    /**
     * 프로젝트 엔티티 직접 저장 (상태 업데이트용)
     * 
     * @param projectEntity 저장할 프로젝트 엔티티
     * @return 저장된 프로젝트 엔티티
     */
    org.certis.studyplatform.project.infrastructure.persistence.entity.ProjectEntity save(org.certis.studyplatform.project.infrastructure.persistence.entity.ProjectEntity projectEntity);

    /**
     * 프로젝트 생성 승인 - status를 APPROVED로 변경
     * 
     * @param projectId 승인할 프로젝트 ID
     */
    void approveCreation(Long projectId);

    boolean tryClaimApprovedSlot(Long projectId);

    void releaseApprovedSlot(Long projectId);
}
