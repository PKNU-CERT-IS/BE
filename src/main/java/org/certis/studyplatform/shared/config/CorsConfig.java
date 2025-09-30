package org.certis.studyplatform.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {
    
    @Value("${app.cors.allowed-origins:https://cert-is.com}")
    private String corsAllowedOrigins;
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 환경변수에서 허용된 오리진을 가져와서 파싱 (공백 제거)
        List<String> allowedOrigins = new ArrayList<>(Arrays.asList(corsAllowedOrigins.split(","))
                .stream()
                .map(String::trim)
                .toList());
        
        // 기본 개발환경 오리진들 추가
        List<String> defaultOrigins = Arrays.asList(
                "http://localhost:8080",
                "http://localhost:3000",      // React 개발 서버
                "http://127.0.0.1:3000",      // 동일한 주소의 다른 표현
                "https://localhost:3000",      // HTTPS 로컬
                "https://www.cert-is.com",
                "https://cert-is.com",
                "https://api.cert-is.com",    // API 서버 도메인
                "https://cert-is.vercel.app",
                "https://certis.mooo.com"
        );
        
        // 환경변수 오리진과 기본 오리진을 합침
        allowedOrigins.addAll(defaultOrigins);
        configuration.setAllowedOrigins(allowedOrigins);

        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        // 허용할 헤더
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // 인증 정보 포함 허용 (쿠키, Authorization 헤더 등)
        configuration.setAllowCredentials(true);

        // 클라이언트가 접근 가능한 응답 헤더
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Set-Cookie"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}

