package org.certis.studyplatform.shared.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 비동기 처리를 위한 완전한 설정
 * Java 21 Virtual Thread와 기존 Thread Pool 모두 지원
 */
@Slf4j
@Configuration
@EnableAsync
@EnableConfigurationProperties(AsyncConfig.AsyncProperties.class)
public class AsyncConfig implements AsyncConfigurer {

    private final AsyncProperties asyncProperties;

    public AsyncConfig(AsyncProperties asyncProperties) {
        this.asyncProperties = asyncProperties;
    }

    /**
     * Virtual Thread 기반 Primary Executor
     * 대부분의 I/O 바운드 작업에 사용
     */
    @Bean("virtualThreadTaskExecutor")
    @Primary
    public TaskExecutor virtualThreadTaskExecutor() {
        log.info("Creating Virtual Thread Task Executor");

        var executor = Executors.newVirtualThreadPerTaskExecutor();
        var adapter = new TaskExecutorAdapter(executor);

        // Virtual Thread는 이름을 직접 설정할 수 없으므로 로깅으로 대체
        log.info("Virtual Thread Task Executor created successfully");

        return adapter;
    }

    /**
     * CPU 집약적 작업을 위한 전통적인 Thread Pool
     * CPU 바운드 작업이나 Virtual Thread가 적합하지 않은 경우 사용
     */
    @Bean("cpuBoundTaskExecutor")
    public TaskExecutor cpuBoundTaskExecutor() {
        log.info("Creating CPU Bound Task Executor");

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // CPU 코어 수 기반 설정
        int corePoolSize = asyncProperties.getCpuBound().getCorePoolSize();
        int maxPoolSize = asyncProperties.getCpuBound().getMaxPoolSize();

        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(asyncProperties.getCpuBound().getQueueCapacity());
        executor.setKeepAliveSeconds(asyncProperties.getCpuBound().getKeepAliveSeconds());
        executor.setThreadNamePrefix("CPU-Bound-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(asyncProperties.getCpuBound().getAwaitTerminationSeconds());

        // 거부 정책 설정
        executor.setRejectedExecutionHandler(new CustomRejectedExecutionHandler());

        // 스레드 데몬 설정
        executor.setDaemon(false);

        executor.initialize();

        log.info("CPU Bound Task Executor created - Core: {}, Max: {}, Queue: {}",
                corePoolSize, maxPoolSize, asyncProperties.getCpuBound().getQueueCapacity());

        return executor;
    }

    /**
     * 긴급하지 않은 백그라운드 작업용 Executor
     * 로깅, 메트릭 수집, 정리 작업 등에 사용
     */
    @Bean("backgroundTaskExecutor")
    public TaskExecutor backgroundTaskExecutor() {
        log.info("Creating Background Task Executor");

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(asyncProperties.getBackground().getCorePoolSize());
        executor.setMaxPoolSize(asyncProperties.getBackground().getMaxPoolSize());
        executor.setQueueCapacity(asyncProperties.getBackground().getQueueCapacity());
        executor.setKeepAliveSeconds(asyncProperties.getBackground().getKeepAliveSeconds());
        executor.setThreadNamePrefix("Background-");
        executor.setWaitForTasksToCompleteOnShutdown(false); // 백그라운드 작업은 즉시 종료
        executor.setAwaitTerminationSeconds(5);

        // 백그라운드 작업은 낮은 우선순위
        executor.setThreadPriority(Thread.MIN_PRIORITY);
        executor.setDaemon(true);

        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());

        executor.initialize();

        log.info("Background Task Executor created - Core: {}, Max: {}",
                asyncProperties.getBackground().getCorePoolSize(),
                asyncProperties.getBackground().getMaxPoolSize());

        return executor;
    }

    /**
     * CompletableFuture 등에서 사용할 수 있는 일반 Executor
     */
    @Bean("virtualThreadExecutor")
    public Executor virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Spring @Async의 기본 Executor 설정
     */
    @Override
    public Executor getAsyncExecutor() {
        return virtualThreadTaskExecutor();
    }

    /**
     * 비동기 작업에서 발생하는 예외 처리
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new CustomAsyncUncaughtExceptionHandler();
    }

    /**
     * 커스텀 예외 핸들러
     */
    @Slf4j
    static class CustomAsyncUncaughtExceptionHandler implements AsyncUncaughtExceptionHandler {

        @Override
        public void handleUncaughtException(Throwable ex, Method method, Object... params) {
            log.error("Async method execution failed - Method: {}.{}, Parameters: {}",
                    method.getDeclaringClass().getSimpleName(),
                    method.getName(),
                    params,
                    ex);

            // 필요시 추가 처리 (알림, 메트릭 등)
            // 예: alertService.sendAlert("Async task failed", ex);
            // 예: meterRegistry.counter("async.task.failed").increment();
        }
    }

    /**
     * 커스텀 거부 실행 핸들러
     */
    @Slf4j
    static class CustomRejectedExecutionHandler implements RejectedExecutionHandler {

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            log.warn("Task rejected - Active: {}, Completed: {}, Queue: {}, Pool: {}",
                    executor.getActiveCount(),
                    executor.getCompletedTaskCount(),
                    executor.getQueue().size(),
                    executor.getPoolSize());

            // 긴급한 작업의 경우 호출 스레드에서 실행
            if (!executor.isShutdown()) {
                try {
                    r.run();
                    log.info("Rejected task executed in caller thread");
                } catch (Exception e) {
                    log.error("Failed to execute rejected task in caller thread", e);
                }
            }
        }
    }

    /**
     * 비동기 처리 관련 설정 프로퍼티
     */
    @ConfigurationProperties(prefix = "app.async")
    public static class AsyncProperties {

        private CpuBoundProperties cpuBound = new CpuBoundProperties();
        private BackgroundProperties background = new BackgroundProperties();

        public CpuBoundProperties getCpuBound() {
            return cpuBound;
        }

        public void setCpuBound(CpuBoundProperties cpuBound) {
            this.cpuBound = cpuBound;
        }

        public BackgroundProperties getBackground() {
            return background;
        }

        public void setBackground(BackgroundProperties background) {
            this.background = background;
        }

        public static class CpuBoundProperties {
            private int corePoolSize = Runtime.getRuntime().availableProcessors();
            private int maxPoolSize = Runtime.getRuntime().availableProcessors() * 2;
            private int queueCapacity = 100;
            private int keepAliveSeconds = 60;
            private int awaitTerminationSeconds = 20;

            // getters and setters
            public int getCorePoolSize() { return corePoolSize; }
            public void setCorePoolSize(int corePoolSize) { this.corePoolSize = corePoolSize; }
            public int getMaxPoolSize() { return maxPoolSize; }
            public void setMaxPoolSize(int maxPoolSize) { this.maxPoolSize = maxPoolSize; }
            public int getQueueCapacity() { return queueCapacity; }
            public void setQueueCapacity(int queueCapacity) { this.queueCapacity = queueCapacity; }
            public int getKeepAliveSeconds() { return keepAliveSeconds; }
            public void setKeepAliveSeconds(int keepAliveSeconds) { this.keepAliveSeconds = keepAliveSeconds; }
            public int getAwaitTerminationSeconds() { return awaitTerminationSeconds; }
            public void setAwaitTerminationSeconds(int awaitTerminationSeconds) { this.awaitTerminationSeconds = awaitTerminationSeconds; }
        }

        public static class BackgroundProperties {
            private int corePoolSize = 2;
            private int maxPoolSize = 4;
            private int queueCapacity = 500;
            private int keepAliveSeconds = 300;

            // getters and setters
            public int getCorePoolSize() { return corePoolSize; }
            public void setCorePoolSize(int corePoolSize) { this.corePoolSize = corePoolSize; }
            public int getMaxPoolSize() { return maxPoolSize; }
            public void setMaxPoolSize(int maxPoolSize) { this.maxPoolSize = maxPoolSize; }
            public int getQueueCapacity() { return queueCapacity; }
            public void setQueueCapacity(int queueCapacity) { this.queueCapacity = queueCapacity; }
            public int getKeepAliveSeconds() { return keepAliveSeconds; }
            public void setKeepAliveSeconds(int keepAliveSeconds) { this.keepAliveSeconds = keepAliveSeconds; }
        }
    }
}
