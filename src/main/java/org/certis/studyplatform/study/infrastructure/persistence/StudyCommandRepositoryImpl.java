package org.certis.studyplatform.study.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.study.domain.repository.StudyCommandRepository;
import org.certis.studyplatform.study.domain.vo.StudyVo;
import org.certis.studyplatform.study.infrastructure.mapper.StudyInfrastructureMapper;
import org.certis.studyplatform.study.infrastructure.persistence.entity.StudyEntity;
import org.certis.studyplatform.study.infrastructure.persistence.jpa.StudyJpaRepository;
import org.certis.studyplatform.study.infrastructure.persistence.jpa.StudyAttachedJpaRepository;
import org.certis.studyplatform.study.infrastructure.persistence.entity.StudyAttachedEntity;
import org.certis.studyplatform.shared.service.S3FileService;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
                    .build();

            studyAttachedJpaRepository.save(entity);

            log.debug("Command Infrastructure: Study attachment uploaded successfully for study ID: {}, member ID: {}", studyId, memberId);
            return attachmentUrl;

        } catch (Exception e) {
            log.error("Error uploading study attachment for study ID {}, member ID {}: {}", studyId, memberId, e.getMessage());
            throw new RuntimeException("Failed to upload study attachment", e);
        }
    }
}