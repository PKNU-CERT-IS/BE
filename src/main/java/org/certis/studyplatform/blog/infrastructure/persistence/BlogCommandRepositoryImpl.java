package org.certis.studyplatform.blog.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.blog.domain.repository.BlogCommandRepository;
import org.certis.studyplatform.blog.domain.vo.BlogVo;
import org.certis.studyplatform.blog.domain.vo.BlogIdVo;
import org.certis.studyplatform.blog.infrastructure.persistence.jpa.BlogJpaRepository;
import org.certis.studyplatform.blog.infrastructure.persistence.jpa.BlogViewJpaRepository;
import org.certis.studyplatform.blog.infrastructure.persistence.entity.BlogEntity;
import org.certis.studyplatform.blog.infrastructure.persistence.entity.BlogViewEntity;
import org.certis.studyplatform.blog.infrastructure.mapper.BlogInfrastructureMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Blog Command Repository Implementation
 *
 * CQRS Command 측면의 Repository 구현체 (Write 작업)
 * Infrastructure Layer
 * VO를 받아서 Mapper를 통해 Entity로 변환하여 JPA 처리
 * Command 측면에서는 JPA만 사용하여 데이터 일관성과 트랜잭션 보장
 *
 * ✅ ViewCount Fallback 지원: Redis → BlogViewEntity 순서로 조회
 */
@Repository
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BlogCommandRepositoryImpl implements BlogCommandRepository {

    private final BlogJpaRepository jpaRepository;
    private final BlogViewJpaRepository blogViewJpaRepository;
    private final BlogInfrastructureMapper mapper;

    /**
     * 새로운 블로그 생성/수정
     */
    @Override
    public BlogVo save(BlogVo blogVo) {
        if (blogVo.id() == null) {
            log.debug("Command: Creating new blog - {}", blogVo.title());
        } else {
            log.debug("Command: Updating existing blog - ID: {}, title: {}", blogVo.id(), blogVo.title());
        }

        // VO → Entity 변환
        BlogEntity entity = mapper.toEntity(blogVo);

        // JPA Repository를 통한 저장
        BlogEntity savedEntity = jpaRepository.save(entity);

        // Entity → VO 변환
        BlogVo savedBlogVo = mapper.toVo(savedEntity);

        log.debug("Command: Blog saved successfully - ID: {}", savedBlogVo.id());
        return savedBlogVo;
    }

    /**
     * 블로그 소프트 삭제
     */
    @Override
    public void deleteById(Long id) {
        log.debug("Command: Soft deleting blog by ID - {}", id);

        // 조회 없이 바로 벌크 업데이트
        int affectedRows = jpaRepository.bulkSoftDeleteById(id, OffsetDateTime.now());

        if (affectedRows > 0) {
            log.debug("Command: Blog soft deleted successfully - ID: {}", id);
        } else {
            log.warn("Command: Blog not found for deletion - ID: {}", id);
            throw new IllegalArgumentException("Blog not found with ID: " + id);
        }
    }

}