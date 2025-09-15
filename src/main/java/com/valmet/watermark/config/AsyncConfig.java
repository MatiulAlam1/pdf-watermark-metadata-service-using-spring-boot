package com.valmet.watermark.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Configuration class for setting up asynchronous execution with a custom thread pool.
 *
 * @author BJIT
 * @version 1.0
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    private static final int N_CORE = Runtime.getRuntime ().availableProcessors ();
    private static final int TPTE_CORE_POOL_SIZE = 2 * N_CORE; // initial thread size in pool
    private static final int TPTE_MAX_POOL_SIZE = 128 * TPTE_CORE_POOL_SIZE;
    private static final int TPTE_QUEUE_CAPACITY = 2 * TPTE_MAX_POOL_SIZE;
    private static final int TPTE_KEEP_ALIVE_SECONDS = (int) TimeUnit.MINUTES.toSeconds (1);

    /**
     * Creates and configures a ThreadPoolTaskExecutor for asynchronous method execution.
     *
     * @return the configured Executor instance
     */
    @Bean (name = "taskExecutor")
    public Executor getAsyncExecutor () {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor ();
        log.info ("Creating async executor with core pool size: {}, max pool size: {}, queue capacity: {}, keep alive seconds: {}",
                TPTE_CORE_POOL_SIZE, TPTE_MAX_POOL_SIZE, TPTE_QUEUE_CAPACITY, TPTE_KEEP_ALIVE_SECONDS);
        executor.setCorePoolSize (TPTE_CORE_POOL_SIZE);
        executor.setMaxPoolSize (TPTE_MAX_POOL_SIZE);
        executor.setQueueCapacity (TPTE_QUEUE_CAPACITY);
        executor.setKeepAliveSeconds (TPTE_KEEP_ALIVE_SECONDS);
        executor.setThreadNamePrefix ("AsyncExecutor-");
        executor.initialize ();
        return executor;
    }
}