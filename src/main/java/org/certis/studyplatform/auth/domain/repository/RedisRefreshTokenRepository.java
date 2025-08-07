package org.certis.studyplatform.auth.domain.repository;

import org.certis.studyplatform.auth.domain.model.vo.RefreshTokenVo;

import java.util.Optional;

public interface RedisRefreshTokenRepository {

    void save(RefreshTokenVo refreshTokenVo);

    Optional<RefreshTokenVo> findByUserId(Long userId);

    void deleteByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
