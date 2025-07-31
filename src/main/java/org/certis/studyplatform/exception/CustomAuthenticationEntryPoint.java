package org.certis.studyplatform.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public CustomAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        // 로그 기록
        // 이곳에 로그를 추가할 수도 있습니다.

        CustomExceptionStatus errorResponse = new CustomExceptionStatus(
                ExceptionStatus.AUTH_COOKIE_UNAUTHORIZED,
                ExceptionStatus.AUTH_COOKIE_UNAUTHORIZED.getMessage()
        );

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(ExceptionStatus.AUTH_COOKIE_UNAUTHORIZED.getStatusCode());

        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
