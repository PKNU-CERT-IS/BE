package org.certis.studyplatform.shared.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson ObjectMapper Configuration
 *
 * JSON 직렬화/역직렬화를 위한 ObjectMapper Bean 설정
 * 전체 애플리케이션에서 공통으로 사용되는 설정
 */
@Configuration
public class JacksonConfiguration {

    /**
     * 공통 ObjectMapper Bean 등록
     *
     * @return 설정된 ObjectMapper
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Java 8 시간 API 지원
        mapper.registerModule(new JavaTimeModule());

        // 타임스탬프를 ISO 형식으로 출력
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 알 수 없는 속성 무시 (호환성)
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // null 값 무시
        mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);

        return mapper;
    }
} 