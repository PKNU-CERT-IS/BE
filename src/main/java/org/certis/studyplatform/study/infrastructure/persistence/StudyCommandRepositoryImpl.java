package org.certis.studyplatform.study.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.study.domain.repository.StudyCommandRepository;
import org.certis.studyplatform.study.domain.vo.StudyVo;
import org.certis.studyplatform.study.infrastructure.mapper.StudyInfrastructureMapper;
import org.certis.studyplatform.study.infrastructure.persistence.entity.StudyEntity;
import org.certis.studyplatform.study.infrastructure.persistence.jpa.StudyJpaRepository;
import org.certis.studyplatform.study.infrastructure.persistence.jpa.StudyAttachedJpaRepository;
import org.certis.studyplatform.exception.InfrastructureException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.study.infrastructure.persistence.entity.StudyAttachedEntity;
import org.certis.studyplatform.shared.service.S3FileService;
import org.certis.studyplatform.study.application.object.command.CreateStudyAttachedCommand;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import org.certis.studyplatform.shared.domain.ResultSubmitStatus;
import java.time.OffsetDateTime;

/**
 * Study Command Repository Implementation
 *
 * CQRS Command 측면의 Repository 구현체 (Write 작업)
 * Infrastructure Layer
 * VO를 받아서 Mapper를 통해 Entity로 변환하여 JPA 처리
 * Command 측면에서는 JPA만 사용하여 데이터 일관성과 트랜잭션 보장
 */
@Repository
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StudyCommandRepositoryImpl implements StudyCommandRepository {

    private final StudyJpaRepository jpaRepository;
    private final StudyAttachedJpaRepository studyAttachedJpaRepository;
    private final StudyInfrastructureMapper mapper;
    private final S3FileService s3FileService;

    /**
     * 새로운 프로젝트 생성
     */
    @Override
    public StudyVo save(StudyVo studyVo) {
        log.debug("Command: Creating new study - {}", studyVo.title());

        // VO → Entity 변환 (CREATE용)
        StudyEntity entity = mapper.toEntity(studyVo);

        // JPA Repository를 통한 저장
        StudyEntity savedEntity = jpaRepository.save(entity);

        // Entity → VO 변환
        StudyVo savedStudyVo = mapper.toVo(savedEntity);

        log.debug("Command: Study created successfully - ID: {}", savedStudyVo.id());
        return savedStudyVo;
    }

    /**
     * 프로젝트 소프트 삭제
     */
    @Override
    public void deleteById(Long id) {
        log.debug("Command: Soft deleting study by ID - {}", id);

        // 조회 없이 바로 벌크 업데이트
        int affectedRows = jpaRepository.bulkSoftDeleteById(id, OffsetDateTime.now());

        if (affectedRows > 0) {
            log.debug("Command: Study soft deleted successfully - ID: {}", id);
        } else {
            log.warn("Command: Study not found for deletion - ID: {}", id);
            throw new IllegalArgumentException("Study not found with ID: " + id);
        }
    }

    /**
     * 스터디 첨부파일 업로드
     */
    @Override
    @Transactional
    public String uploadStudyAttachment(Long studyId, Long memberId, MultipartFile file) {
        log.debug("Command Infrastructure: Uploading study attachment for study ID: {}, member ID: {}", studyId, memberId);

        try {
            // S3에 첨부파일 업로드
            String attachmentUrl = s3FileService.uploadFile(file, "study");

            // 첨부파일 정보를 DB에 저장
            StudyAttachedEntity entity = StudyAttachedEntity.builder()
                    .studyId(studyId)
                    .memberId(memberId)
                    .attachedUrl(attachmentUrl)
                    .name(file.getOriginalFilename())
                    .type(file.getContentType())
                    .size(String.valueOf(file.getSize()))
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build();

            studyAttachedJpaRepository.save(entity);

            log.debug("Command Infrastructure: Study attachment uploaded successfully for study ID: {}, member ID: {}", studyId, memberId);
            return attachmentUrl;

        } catch (Exception e) {
            log.error("Error uploading study attachment for study ID {}, member ID {}: {}", studyId, memberId, e.getMessage());
            throw new RuntimeException("Failed to upload study attachment", e);
        }
    }

    @Override
    public void updateStudyAttachments(Long studyId, Long requesterId, java.util.List<CreateStudyAttachedCommand> attachments) {
        // 정책: attachments == null -> 변경 없음, attachments 제공됨(빈 포함) -> 기존 전체 삭제(S3 포함) 후 신규로 덮어쓰기
        if (attachments == null) {
            return;
        }

        // 기존 첨부 전체 삭제 (소프트 딜리트) + S3 원본 삭제(차등)
        var existing = studyAttachedJpaRepository.findByStudyId(studyId);
        if (!existing.isEmpty()) {
            // attachments == null 또는 빈 배열이면 DB만 비움 (S3 삭제 없음)
            if (attachments == null || attachments.isEmpty()) {
                studyAttachedJpaRepository.deleteAll(existing);
                return;
            }

            // 차등 처리: DB 삭제만, S3는 유지
            java.util.Set<String> desired = new java.util.LinkedHashSet<>();
            for (var a : attachments) {
                if (a.url() != null) desired.add(s3FileService.normalizeUrl(a.url()));
            }
            java.util.Set<String> existingSet = new java.util.LinkedHashSet<>();
            for (StudyAttachedEntity e : existing) existingSet.add(s3FileService.normalizeUrl(e.getAttachedUrl()));

            java.util.Set<String> toRemove = new java.util.LinkedHashSet<>(existingSet);
            toRemove.removeAll(desired);
            if (!toRemove.isEmpty()) {
                var removeEntities = existing.stream()
                        .filter(e -> toRemove.contains(s3FileService.normalizeUrl(e.getAttachedUrl())))
                        .toList();
                studyAttachedJpaRepository.deleteAll(removeEntities);
            }

            // 기존에 있던 것과 동일한 URL은 남기기 위해 별도 조치 불필요
        }

        // 빈 리스트면 여기서 종료 (완전 삭제 상태 유지)
        if (attachments.isEmpty()) {
            return;
        }

        // 신규 첨부 저장 (DB에만 추가; URL은 이미 업로드/정규화됨)
        for (var file : attachments) {
            StudyAttachedEntity entity = StudyAttachedEntity.builder()
                    .studyId(studyId)
                    .memberId(requesterId)
                    .attachedUrl(file.url())
                    .name(file.name())
                    .type(file.type() != null ? file.type().name() : "TEXT")
                    .size(file.size() != null ? String.valueOf(file.size()) : "0")
                    .createdAt(java.time.OffsetDateTime.now())
                    .updatedAt(java.time.OffsetDateTime.now())
                    .build();
            studyAttachedJpaRepository.save(entity);
        }
    }

    @Override
    public void updateResultSubmission(Long studyId, OffsetDateTime submittedAt,
                                       ResultSubmitStatus status,
                                       String attachmentUrl) {
        jpaRepository.updateResultSubmission(studyId, submittedAt, status, attachmentUrl);
    }

    @Override
    public void approveEnd(Long studyId, OffsetDateTime endedAt,
                           ResultSubmitStatus status) {
        jpaRepository.approveEnd(studyId, endedAt, status);
    }

    @Override
    public void rejectEnd(Long studyId, ResultSubmitStatus status,
                          OffsetDateTime now) {
        jpaRepository.rejectEnd(studyId, status, now);
    }

    @Override
    public void bulkSoftDeleteById(Long studyId, OffsetDateTime deletedAt) {
        jpaRepository.bulkSoftDeleteById(studyId, deletedAt);
    }

    @Override
    public java.util.Optional<String> getResultAttachmentUrlById(Long studyId) {
        return jpaRepository.findById(studyId).map(StudyEntity::getResultAttachmentUrl);
    }

    @Override
    public StudyEntity save(StudyEntity studyEntity) {
        log.debug("Command: Saving study entity - ID: {}", studyEntity.getId());
        return jpaRepository.save(studyEntity);
    }

    @Override
    public void approveCreation(Long studyId) {
        log.debug("Command: Approving study creation - ID: {}", studyId);
        int affectedRows = jpaRepository.approveCreation(studyId, OffsetDateTime.now());
        if (affectedRows == 0) {
            throw new InfrastructureException(ExceptionStatus.STUDY_INFRASTRUCTURE_NOT_FOUND,
                    "스터디를 찾을 수 없습니다: " + studyId);
        }
        log.debug("Command: Study creation approved successfully - ID: {}", studyId);
    }
}