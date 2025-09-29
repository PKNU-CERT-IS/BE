package org.certis.studyplatform.shared.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 공통 설정
 * - RedisTemplate의 직렬화(Serializer) 방식을 설정합니다.
 */
@Configuration
public class RedisConfig {

    /**
     * RedisTemplate 빈을 생성합니다. (String-String 타입)
     * Blog Redis Repository에서 사용하는 타입과 일치시킵니다.
     * Redisson의 stringRedisTemplate과 구분하기 위해 Primary로 설정합니다.
     *
     * @param connectionFactory 스프링 부트가 application.yml 설정을 바탕으로 자동 생성한 Redis 연결 팩토리
     * @return 설정이 완료된 RedisTemplate 객체
     */
    @Bean("redisStringTemplate")
    @Primary
    public RedisTemplate<String, String> redisStringTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);

        return template;
    }

    /**
     * Object 타입을 위한 RedisTemplate 빈을 생성합니다.
     * 다른 서비스에서 Object 타입이 필요한 경우 사용합니다.
     *
     * @param connectionFactory 스프링 부트가 application.yml 설정을 바탕으로 자동 생성한 Redis 연결 팩토리
     * @param objectMapper      다른 곳(JacksonConfiguration)에 이미 등록된 ObjectMapper 빈을 주입받아 사용
     * @return 설정이 완료된 RedisTemplate 객체
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        return template;
    }
}