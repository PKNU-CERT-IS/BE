package org.certis.studyplatform.study.domain.repository;

import org.certis.studyplatform.study.domain.vo.StudyVo;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;

import org.certis.studyplatform.shared.domain.ResultSubmitStatus;
import org.certis.studyplatform.study.application.object.command.CreateStudyAttachedCommand;

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

    /**
     * 스터디 첨부파일 업로드
     *
     * @param studyId 스터디 ID
     * @param memberId 사용자 ID
     * @param file 업로드할 파일
     * @return 업로드된 파일 URL
     */
    String uploadStudyAttachment(Long studyId, Long memberId, MultipartFile file);

    // ===== End submission and lifecycle commands =====
    void updateResultSubmission(Long studyId, OffsetDateTime submittedAt,
                                ResultSubmitStatus status,
                                String attachmentUrl);

    void approveEnd(Long studyId, OffsetDateTime endedAt,
                    ResultSubmitStatus status);

    void rejectEnd(Long studyId, ResultSubmitStatus status,
                   OffsetDateTime now);

    void bulkSoftDeleteById(Long studyId, OffsetDateTime deletedAt);

    java.util.Optional<String> getResultAttachmentUrlById(Long studyId);

    /**
     * 스터디 엔티티 직접 저장 (상태 업데이트용)
     * 
     * @param studyEntity 저장할 스터디 엔티티
     * @return 저장된 스터디 엔티티
     */
    org.certis.studyplatform.study.infrastructure.persistence.entity.StudyEntity save(org.certis.studyplatform.study.infrastructure.persistence.entity.StudyEntity studyEntity);

    /**
     * 스터디 생성 승인 - status를 APPROVED로 변경
     * 
     * @param studyId 승인할 스터디 ID
     */
    void approveCreation(Long studyId);

    /**
     * 스터디 첨부파일 업데이트 (전체 교체 정책)
     * attachments == null -> 기존 첨부 전체 삭제
     * attachments 비어있지 않음 -> 기존 첨부 전체 삭제 후 신규 저장
     */
    void updateStudyAttachments(Long studyId, Long requesterId, java.util.List<CreateStudyAttachedCommand> attachments);
}