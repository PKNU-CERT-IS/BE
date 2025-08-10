package org.certis.studyplatform.shared.security;


// Jwt 토큰 생성/파싱/검증 담당
// AccessToken 과 RefreshToken 모두 JWT 기반 처리

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.auth.domain.model.vo.AccessTokenVo;
import org.certis.studyplatform.auth.domain.model.vo.RefreshTokenVo;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.exception.InfrastructureException;
import org.certis.studyplatform.member.domain.MemberRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtTokenProvider(@Value("${jwt.secret}") String secretKey,
                            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
                            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        // 비밀 키 256비트(32바이트)
        this.key = Keys.hmacShaKeyFor(Arrays.copyOf(secretKey.getBytes(), 32));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    // 토큰 생성 로직
    private String createToken(Long userId, MemberRole role, String tokenType, long expirationTime) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);

        Map<String, Object> claims = new HashMap<>();

        Optional.ofNullable(role)
                .ifPresent(r -> claims.put("role", r.toAuthorityString()));

        claims.put("type", tokenType);

        return Jwts.builder()
                .subject(userId.toString())
                .claims(claims)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    // 액세스 토큰 생성
    public AccessTokenVo generateAccessToken(Long userId, MemberRole role) {
        String token = createToken(userId, role, "access", accessTokenExpiration);

        // 밀리초를 올바르게 LocalDateTime에 추가
        LocalDateTime expiredAt = LocalDateTime.now()
                .plus(accessTokenExpiration, ChronoUnit.MILLIS);

        return new AccessTokenVo(token, expiredAt);
    }


    // 리프레시 토큰 생성
    public RefreshTokenVo generateRefreshToken(Long userId) {
        String token = createToken(userId, null, "refresh", refreshTokenExpiration);

        // 밀리초를 올바르게 LocalDateTime에 추가
        LocalDateTime expiredAt = LocalDateTime.now()
                .plus(refreshTokenExpiration,ChronoUnit.MILLIS);

        return new RefreshTokenVo(token, expiredAt, userId);
    }

    // 토큰 유효성 검증 (이후 전역 예외 처리)
    public boolean isValidateToken(String token){
        //for test
        return true;
//        try{
//            Jwts.parser()
//                    .verifyWith(key)
//                    .build()
//                    .parseSignedClaims(token);
//                    return true;
//        } catch (ExpiredJwtException e) {
//            log.debug("JWT token expired: {}", e.getMessage()); // 만료는 debug 레벨
//            throw new InfrastructureException(ExceptionStatus.AUTH_INFRASTRUCTURE_JWT_TOKEN_EXPIRED);
//        } catch (UnsupportedJwtException e) {
//            log.warn("Unsupported JWT token: {}", e.getMessage());
//            throw new InfrastructureException(ExceptionStatus.AUTH_INFRASTRUCTURE_JWT_TOKEN_UNSUPPORTED);
//        } catch (MalformedJwtException e) {
//            log.warn("Malformed JWT token: {}", e.getMessage());
//            throw new InfrastructureException(ExceptionStatus.AUTH_INFRASTRUCTURE_JWT_TOKEN_INVALID_FORMAT);
//        } catch (SecurityException e) {
//            log.warn("Invalid JWT signature: {}", e.getMessage());
//            throw new InfrastructureException(ExceptionStatus.AUTH_INFRASTRUCTURE_JWT_TOKEN_INVALID_SIGNATURE);
//        } catch (IllegalArgumentException e) {
//            log.warn("JWT token compact invalid: {}", e.getMessage());
//            throw new InfrastructureException(ExceptionStatus.AUTH_INFRASTRUCTURE_JWT_TOKEN_MISSING_CLAIMS);
//        } catch (JwtException e) {
//            log.warn("JWT 유효성 검증 실패: {}", e.getMessage());
//            throw new InfrastructureException(ExceptionStatus.AUTH_INFRASTRUCTURE_JWT_TOKEN_PARSE_ERROR);
//        }
    }

    private Claims getClaimsFromToken(String token){
        return  Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // 토큰에서 사용자 ID 추출
    public Long getUserIdFromToken(String token){
        String subject = getClaimsFromToken(token).getSubject();
        return Long.parseLong(subject);
    }

    // 엑세스 토큰에서 권한 추출
    public MemberRole getRoleFromAccessToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return MemberRole.fromAuthorityString(claims.get("role", String.class));
    }

    public boolean isAccessToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return "access".equals(claims.get("type", String.class));
        } catch (JwtException e) {
            throw new InfrastructureException(ExceptionStatus.AUTH_INFRASTRUCTURE_INVALID_ACCESS_TOKEN);
        }
    }

}
