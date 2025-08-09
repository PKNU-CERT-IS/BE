package org.certis.studyplatform.auth.domain.repository;

import org.certis.studyplatform.auth.domain.model.vo.RefreshTokenVo;

import java.time.Duration;
import java.util.Optional;

public interface RedisRefreshTokenRepository {

    void save(RefreshTokenVo refreshTokenVo, Duration ttl);

    Optional<RefreshTokenVo> findByMemberId(Long memberId);

    void deleteByMemberId(Long memberId);

    boolean existsByMemberId(Long memberId);

}
