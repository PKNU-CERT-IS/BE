package org.certis.studyplatform.blog.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.blog.domain.repository.BlogCommandRepository;
import org.certis.studyplatform.blog.domain.repository.BlogViewCommandRepository;
import org.certis.studyplatform.blog.infrastructure.persistence.entity.BlogViewEntity;
import org.certis.studyplatform.blog.infrastructure.persistence.jpa.BlogJpaRepository;
import org.certis.studyplatform.blog.infrastructure.persistence.jpa.BlogViewJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BlogViewCommandRepositoryImpl implements BlogViewCommandRepository {

    private final BlogViewJpaRepository jpaRepository;


    /**
     * 블로그 조회수 증가 (비동기)
     */
    @Override
    public void incrementViewCount(Long id) {
        log.debug("Command: Incrementing view count for blog - ID: {}", id);

        // 벌크 업데이트로 조회수 증가
        int affectedRows = jpaRepository.incrementViewCount(id);

        if (affectedRows > 0) {
            log.debug("Command: View count incremented successfully - ID: {}", id);
        } else {
            log.warn("Command: Blog not found for view count increment - ID: {}", id);
            // 조회수 증가 실패는 전체 작업을 중단시키지 않음
        }
    }

    /**
     * 블로그 조회수 업데이트 (Redis → RDB 동기화용)
     */
    @Override
    public void updateViewCount(Long id, Integer viewCount) {
        log.debug("Command: Updating view count for blog - ID: {}, count: {}", id, viewCount);

        // 벌크 업데이트로 조회수 설정
        int affectedRows = jpaRepository.updateViewCount(id, viewCount);

        if (affectedRows > 0) {
            log.debug("Command: View count updated successfully - ID: {}, count: {}", id, viewCount);
        } else {
            log.warn("Command: Blog not found for view count update - ID: {}", id);
            throw new IllegalArgumentException("Blog not found with ID: " + id);
        }
    }

    /**
     * 블로그 조회수 통계 저장/업데이트 (BlogViewEntity 사용)
     * Redis → RDB 동기화 시 사용
     */
    @Override
    public void saveOrUpdateBlogViewStats(Long blogId, Integer viewCount) {
        log.debug("Command: Saving/updating blog view stats - blogId: {}, viewCount: {}", blogId, viewCount);

        // 기존 통계가 있는지 확인
//        Optional<BlogViewEntity> existingStats = blogViewJpaRepository.findByBlogId(blogId);

        if (blogId != null && viewCount != null) {
            // 기존 통계 업데이트
            BlogViewEntity updatedStats = BlogViewEntity.builder()
                    .blogId(blogId)
                    .viewNumber(viewCount)
                    .build();

            jpaRepository.save(updatedStats);
            log.debug("Command: Blog view stats updated - blogId: {}, viewCount: {}", blogId, viewCount);
        } else {
            // 새로운 통계 생성
            BlogViewEntity newStats = BlogViewEntity.builder()
                    .blogId(blogId)
                    .viewNumber(viewCount)
                    .build();

            jpaRepository.save(newStats);
            log.debug("Command: Blog view stats created - blogId: {}, viewCount: {}", blogId, viewCount);
        }
    }
}
