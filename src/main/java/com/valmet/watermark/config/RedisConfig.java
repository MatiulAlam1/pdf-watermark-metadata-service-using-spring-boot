package com.valmet.watermark.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.time.Duration;

/**
 * Configuration class for setting up Redis connection using Lettuce. Configures
 * the Redis host, port, and client options such as timeouts and auto-reconnect.
 */
@Configuration
public class RedisConfig {
    /**
     * Redis server host, configurable via 'spring.redis.host' property.
     */
    @Value("${spring.redis.host:localhost}")
    private String redisHost;
    /**
     * Redis server port, configurable via 'spring.redis.port' property.
     */
    @Value("${spring.redis.port:6379}")
    private int redisPort;

    /**
     * Creates a {@link LettuceConnectionFactory} bean with custom client options.
     * Sets connection timeout, command timeout, shutdown timeout, and enables
     * auto-reconnect.
     *
     * @return configured LettuceConnectionFactory
     */
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
	RedisStandaloneConfiguration redisConf = new RedisStandaloneConfiguration(redisHost, redisPort);

	LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
		.clientOptions(ClientOptions.builder()
			.socketOptions(SocketOptions.builder().connectTimeout(Duration.ofMillis(100)).build())
			.autoReconnect(true).build())
		.commandTimeout(Duration.ofMillis(200)).shutdownTimeout(Duration.ofMillis(100)).build();

	return new LettuceConnectionFactory(redisConf, clientConfig);
    }
}