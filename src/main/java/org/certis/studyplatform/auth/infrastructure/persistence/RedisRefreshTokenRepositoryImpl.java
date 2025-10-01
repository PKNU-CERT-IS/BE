package org.certis.studyplatform.auth.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.auth.domain.model.vo.RefreshTokenVo;
import org.certis.studyplatform.auth.domain.repository.RedisRefreshTokenRepository;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.exception.InfrastructureException;
import org.certis.studyplatform.member.domain.vo.MemberIdVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisRefreshTokenRepositoryImpl implements RedisRefreshTokenRepository {

    @Autowired(required = false)
    @Qualifier("redisObjectTemplate")
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    private static final String REFRESH_TOKEN_KEY_PREFIX = "refresh_token:";

    @Override
    public void save(RefreshTokenVo refreshTokenVo,Duration ttl) {
        String key = getKey(refreshTokenVo.memberId());

        redisTemplate.opsForValue().set(key,refreshTokenVo,ttl);
        log.debug("리프레시 토큰 저장 완료: memberId={}, ttl={}초", refreshTokenVo.memberId(), ttl.getSeconds());
    }

    @Override
    public Optional<RefreshTokenVo> findByMemberId(MemberIdVo memberIdVo) {
        String key = getKey(memberIdVo.value());
        
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                // LinkedHashMap을 RefreshTokenVo로 변환
                RefreshTokenVo refreshTokenVo = objectMapper.convertValue(value, RefreshTokenVo.class);

                // 만료된 토큰은 즉시 삭제하고 빈 결과 반환
                if (refreshTokenVo.isExpiredRefreshToken()) {
                    log.debug("만료된 RefreshToken 발견, 삭제 처리: memberId={}", memberIdVo.value());
                    // 비동기 삭제로 성능 최적화
                    redisTemplate.delete(key);
                    return Optional.empty();
                }

                return Optional.of(refreshTokenVo);
            }
        } catch (Exception e) {
            log.error("RefreshToken 조회 실패: memberId={}, error={}", memberIdVo.value(), e.getMessage(), e);
            // Redis 오류 시 빈 결과 반환하여 서비스 중단 방지
        }
        
        return Optional.empty();
    }

    @Override
    public void deleteByMemberId(MemberIdVo memberIdVo) {
        String key = getKey(memberIdVo.value());
        try {
            Boolean deleted = redisTemplate.delete(key);
            log.debug("리프레시 토큰 삭제: memberId={}, deleted={}", memberIdVo.value(), deleted);

        } catch (Exception e) {
            log.error("Redis 처리 오류: memberId={}, key={}, error={}", memberIdVo.value(), key, e.getMessage(), e);
            throw new InfrastructureException(ExceptionStatus.AUTH_INFRASTRUCTURE_REDIS_ERROR);
        }
    }

    @Override
    public boolean existsByMemberId(Long memberId) {
        String key = getKey(memberId);
        return redisTemplate.hasKey(key);    }

    private String getKey(Long memberId){
        return REFRESH_TOKEN_KEY_PREFIX + memberId;
    }
}
