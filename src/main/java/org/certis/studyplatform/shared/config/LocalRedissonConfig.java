package org.certis.studyplatform.shared.config;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"local", "dev"})
@Slf4j
public class LocalRedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Bean
    @Primary
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + redisHost + ":" + redisPort)
                .setDatabase(0)
                .setConnectionPoolSize(16)
                .setConnectionMinimumIdleSize(4)
                .setConnectTimeout(5000)
                .setTimeout(3000)
                .setRetryAttempts(2)
                .setRetryInterval(500)
                .setKeepAlive(true)
                .setTcpNoDelay(true);

        if (redisPassword != null && !redisPassword.isBlank()) {
            config.useSingleServer().setPassword(redisPassword);
        }

        log.info("Local/Dev Redisson client created: redis://{}:{}", redisHost, redisPort);
        return Redisson.create(config);
    }
}
