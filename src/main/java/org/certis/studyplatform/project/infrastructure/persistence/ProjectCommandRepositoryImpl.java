package org.certis.studyplatform.project.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.project.domain.repository.ProjectCommandRepository;
import org.certis.studyplatform.project.domain.vo.ProjectUpdateVo;
import org.certis.studyplatform.project.domain.vo.ProjectVo;
import org.certis.studyplatform.project.infrastructure.persistence.jpa.ProjectJpaRepository;
import org.certis.studyplatform.project.infrastructure.persistence.entity.ProjectEntity;
import org.certis.studyplatform.project.infrastructure.mapper.ProjectInfrastructureMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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
    private final ProjectInfrastructureMapper mapper;

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
}