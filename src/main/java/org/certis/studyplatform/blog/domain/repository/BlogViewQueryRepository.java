package org.certis.studyplatform.blog.domain.repository;

import org.certis.studyplatform.blog.domain.vo.BlogIdVo;

public interface BlogViewQueryRepository {

    Integer getViewCount(BlogIdVo blogIdVo);
}
