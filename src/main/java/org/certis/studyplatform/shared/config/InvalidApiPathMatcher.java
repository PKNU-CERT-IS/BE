package org.certis.studyplatform.shared.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;
/**
 * 잘못된 API 경로를 감지하는 커스텀 RequestMatcher
 * /api/v1/이 아닌 모든 /api/ 경로를 매칭
 */
public class InvalidApiPathMatcher implements RequestMatcher {
    
    @Override
    public boolean matches(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        
        // /api/로 시작하는지 확인
        if (!requestURI.startsWith("/api/")) {
            return false;
        }
        
        // /api/v1/로 시작하는 경우는 제외 (유효한 API)
        if (requestURI.startsWith("/api/v1/")) {
            return false;
        }
        
        // /api/로 시작하지만 /api/v1/이 아닌 모든 경로는 잘못된 경로로 간주
        return true;
    }
}
