package org.certis.studyplatform.project.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.project.domain.repository.ProjectCommandRepository;
import org.certis.studyplatform.project.domain.vo.ProjectUpdateVo;
import org.certis.studyplatform.project.domain.vo.ProjectVo;
import org.certis.studyplatform.project.infrastructure.persistence.jpa.ProjectJpaRepository;
import org.certis.studyplatform.project.infrastructure.persistence.jpa.ProjectAttachedJpaRepository;
import org.certis.studyplatform.project.infrastructure.persistence.entity.ProjectEntity;
import org.certis.studyplatform.project.infrastructure.persistence.entity.ProjectAttachedEntity;
import org.certis.studyplatform.project.infrastructure.mapper.ProjectInfrastructureMapper;
import org.certis.studyplatform.shared.service.S3FileService;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Project Command Repository Implementation
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
public class ProjectCommandRepositoryImpl implements ProjectCommandRepository {

    private final ProjectJpaRepository jpaRepository;
    private final ProjectAttachedJpaRepository projectAttachedJpaRepository;
    private final ProjectInfrastructureMapper mapper;
    private final S3FileService s3FileService;

    /**
     * 새로운 프로젝트 생성
     */
    @Override
    public ProjectVo save(ProjectVo projectVo) {
        log.debug("Command: Creating new project - {}", projectVo.title());

        // VO → Entity 변환 (CREATE용)
        ProjectEntity entity = mapper.toEntity(projectVo);

        // JPA Repository를 통한 저장
        ProjectEntity savedEntity = jpaRepository.save(entity);

        // Entity → VO 변환
        ProjectVo savedProjectVo = mapper.toVo(savedEntity);

        log.debug("Command: Project created successfully - ID: {}", savedProjectVo.id());
        return savedProjectVo;
    }

    /**
     * 프로젝트 소프트 삭제
     */
    @Override
    public void deleteById(Long id) {
        log.debug("Command: Soft deleting project by ID - {}", id);

        // 조회 없이 바로 벌크 업데이트
        int affectedRows = jpaRepository.bulkSoftDeleteById(id, OffsetDateTime.now());

        if (affectedRows > 0) {
            log.debug("Command: Project soft deleted successfully - ID: {}", id);
        } else {
            log.warn("Command: Project not found for deletion - ID: {}", id);
            throw new IllegalArgumentException("Project not found with ID: " + id);
        }
    }

    /**
     * 프로젝트 첨부파일 업로드
     */
    @Override
    @Transactional
    public String uploadProjectAttachment(Long projectId, Long memberId, MultipartFile file) {
        log.debug("Command Infrastructure: Uploading project attachment for project ID: {}, member ID: {}", projectId, memberId);

        try {
            // S3에 첨부파일 업로드
            String attachmentUrl = s3FileService.uploadFile(file, "project");

            // 첨부파일 정보를 DB에 저장
            ProjectAttachedEntity entity = ProjectAttachedEntity.builder()
                    .projectId(projectId)
                    .memberId(memberId)
                    .attachedUrl(attachmentUrl)
                    .name(file.getOriginalFilename())
                    .type(file.getContentType())
                    .size(String.valueOf(file.getSize()))
                    .createdAt(OffsetDateTime.now())
                    .build();

            projectAttachedJpaRepository.save(entity);

            log.debug("Command Infrastructure: Project attachment uploaded successfully for project ID: {}, member ID: {}", projectId, memberId);
            return attachmentUrl;

        } catch (Exception e) {
            log.error("Error uploading project attachment for project ID {}, member ID {}: {}", projectId, memberId, e.getMessage());
            throw new RuntimeException("Failed to upload project attachment", e);
        }
    }
}