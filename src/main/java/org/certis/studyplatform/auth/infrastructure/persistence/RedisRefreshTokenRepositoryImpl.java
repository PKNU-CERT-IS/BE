package org.certis.studyplatform.auth.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.auth.domain.model.vo.RefreshTokenVo;
import org.certis.studyplatform.auth.domain.repository.RedisRefreshTokenRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisRefreshTokenRepositoryImpl implements RedisRefreshTokenRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String REFRESH_TOKEN_KEY_PREFIX = "refresh_token:";

    @Override
    public void save(RefreshTokenVo refreshTokenVo) {
        String key = getKey(refreshTokenVo.memberId());

        Duration ttl = Duration.between(LocalDateTime.now(),refreshTokenVo.expiredAt());

        if(ttl.isNegative() || ttl.isZero()){
            log.warn("만료된 리프레시 토큰 저장 시도: userId={}", refreshTokenVo.memberId());
            // 예외 처리
            return;
        }

        redisTemplate.opsForValue().set(key,refreshTokenVo,ttl);
        log.debug("리프레시 토큰 저장 완료: memberId={}, ttl={}초", refreshTokenVo.memberId(), ttl.getSeconds());
    }

    @Override
    public Optional<RefreshTokenVo> findByMemberId(Long memberId) {
        String key = getKey(memberId);
        Object value = redisTemplate.opsForValue().get(key);

        if(value instanceof RefreshTokenVo refreshTokenVo){
            if(refreshTokenVo.isExpiredRefreshToken()){
                deleteByMemberId(memberId);
                // 예외처리 (만료)
                return Optional.empty();
            }

            return Optional.of(refreshTokenVo);
        }
        // 예외처리 (타입 불일치)
        return Optional.empty();
    }

    @Override
    public void deleteByMemberId(Long memberId) {
        String key = getKey(memberId);
        Boolean deleted = redisTemplate.delete(key);
        log.debug("리프레시 토큰 삭제: memberId={}, deleted={}", memberId, deleted);

    }

    @Override
    public boolean existsByMemberId(Long memberId) {
        String key = getKey(memberId);
        return redisTemplate.hasKey(key);    }

    private String getKey(Long memberId){
        return REFRESH_TOKEN_KEY_PREFIX + memberId;
    }
}
