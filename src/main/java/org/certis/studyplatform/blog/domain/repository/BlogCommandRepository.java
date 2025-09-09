package org.certis.studyplatform.blog.domain.repository;

import org.certis.studyplatform.blog.domain.vo.BlogVo;
import org.certis.studyplatform.blog.domain.vo.BlogIdVo;

/**
 * Blog Command Repository Interface
 *
 * CQRS Command 측면의 Repository (Write 작업)
 * Domain Layer의 인터페이스
 * Infrastructure Layer에서 JPA로 구현
 */
public interface BlogCommandRepository {

    /**
     * 블로그 저장 (생성/수정)
     */
    BlogVo save(BlogVo blogVo);

    /**
     * 블로그 소프트 삭제
     */
    void deleteById(Long id);
}