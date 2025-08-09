package org.certis.studyplatform.auth.domain.repository;

import org.certis.studyplatform.auth.domain.model.vo.RefreshTokenVo;
import org.certis.studyplatform.member.domain.vo.MemberIdVo;

import java.time.Duration;
import java.util.Optional;

public interface RedisRefreshTokenRepository {

    void save(RefreshTokenVo refreshTokenVo, Duration ttl);

    Optional<RefreshTokenVo> findByMemberId(MemberIdVo memberIdVo);

    void deleteByMemberId(MemberIdVo memberIdVo);

    boolean existsByMemberId(Long memberId);

}
