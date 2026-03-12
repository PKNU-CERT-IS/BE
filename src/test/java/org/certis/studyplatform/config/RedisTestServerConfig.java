package org.certis.studyplatform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Configuration
@Profile("redis-test")
public class RedisTestServerConfig {

    @Bean(name = "redisTestServer", destroyMethod = "destroyForcibly")
    public Process redisTestServer(@Value("${embedded.redis.port:16380}") int port) throws Exception {
        Process process = new ProcessBuilder(
                "redis-server",
                "--port", String.valueOf(port),
                "--save", "",
                "--appendonly", "no",
                "--maxmemory", "128M",
                "--maxmemory-policy", "allkeys-lru"
        ).redirectErrorStream(true).start();

        waitUntilRedisReady(process, port);
        return process;
    }

    @Bean
    @Primary
    @DependsOn("redisTestServer")
    public RedisConnectionFactory redisConnectionFactory(
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:16380}") int port
    ) {
        LettuceConnectionFactory connectionFactory =
                new LettuceConnectionFactory(new RedisStandaloneConfiguration(host, port));
        connectionFactory.afterPropertiesSet();
        return connectionFactory;
    }

    private void waitUntilRedisReady(Process process, int port) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (System.nanoTime() < deadline) {
            if (!process.isAlive()) {
                throw new RuntimeException("redis-server exited early: " + readOutput(process));
            }
            try (java.net.Socket ignored = new java.net.Socket("127.0.0.1", port)) {
                return;
            } catch (IOException ignored) {
                Thread.sleep(100);
            }
        }

        throw new RuntimeException("redis-server did not start on port " + port + ": " + readOutput(process));
    }

    private String readOutput(Process process) throws IOException {
        try (InputStream inputStream = process.getInputStream();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            inputStream.transferTo(outputStream);
            return outputStream.toString(StandardCharsets.UTF_8);
        }
    }
}
