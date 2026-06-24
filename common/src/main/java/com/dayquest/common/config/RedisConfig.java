package com.dayquest.common.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Enterprise-grade Redis configuration for DayQuest.
 * Provides distributed caching, connection pooling, and optimized serialization.
 *
 * Enable by setting: spring.data.redis.enabled=true
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = false)
public class RedisConfig {

    private static final Logger logger = LoggerFactory.getLogger(RedisConfig.class);

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.database:0}")
    private int redisDatabase;

    @Value("${spring.data.redis.lettuce.pool.max-active:50}")
    private int maxActive;

    @Value("${spring.data.redis.lettuce.pool.max-idle:20}")
    private int maxIdle;

    @Value("${spring.data.redis.lettuce.pool.min-idle:5}")
    private int minIdle;

    @Value("${spring.data.redis.timeout:5000}")
    private long timeout;

    /**
     * Configures Lettuce connection factory with connection pooling.
     */
    @Bean
    @Primary
    public LettuceConnectionFactory redisConnectionFactory() {
        logger.info("Initializing Redis connection to {}:{}", redisHost, redisPort);

        RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration();
        serverConfig.setHostName(redisHost);
        serverConfig.setPort(redisPort);
        serverConfig.setDatabase(redisDatabase);

        if (redisPassword != null && !redisPassword.isEmpty()) {
            serverConfig.setPassword(redisPassword);
        }

        // Connection pool configuration
        @SuppressWarnings("rawtypes")
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setMaxTotal(maxActive);
        poolConfig.setMaxIdle(maxIdle);
        poolConfig.setMinIdle(minIdle);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(30));

        LettucePoolingClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
                .poolConfig(poolConfig)
                .commandTimeout(Duration.ofMillis(timeout))
                .build();

        return new LettuceConnectionFactory(serverConfig, clientConfig);
    }

    /**
     * Primary RedisTemplate with JSON serialization.
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(redisObjectMapper());
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.setEnableTransactionSupport(true);
        template.afterPropertiesSet();

        logger.info("Redis template configured with JSON serialization");
        return template;
    }

    /**
     * StringRedisTemplate for simple string operations.
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    /**
     * Cache manager with per-cache TTL configuration.
     */
    @Bean
    @Primary
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(redisObjectMapper());

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // User caches - shorter TTL for frequently changing data
        cacheConfigurations.put("users", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("userProfiles", defaultConfig.entryTtl(Duration.ofMinutes(2)));
        cacheConfigurations.put("usersByUsername", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // Video caches
        cacheConfigurations.put("videos", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("videoMetadata", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("trendingVideos", defaultConfig.entryTtl(Duration.ofSeconds(30)));
        cacheConfigurations.put("videosByQuest", defaultConfig.entryTtl(Duration.ofMinutes(2)));

        // Quest caches
        cacheConfigurations.put("quests", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("activeQuests", defaultConfig.entryTtl(Duration.ofMinutes(1)));
        cacheConfigurations.put("topQuests", defaultConfig.entryTtl(Duration.ofMinutes(1)));

        // Social caches
        cacheConfigurations.put("comments", defaultConfig.entryTtl(Duration.ofMinutes(2)));
        cacheConfigurations.put("followers", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("following", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // Content caches
        cacheConfigurations.put("streaks", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("badges", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put("hashtags", defaultConfig.entryTtl(Duration.ofMinutes(1)));

        logger.info("Redis cache manager configured with {} cache definitions", cacheConfigurations.size());

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }

    /**
     * ObjectMapper configured for Redis serialization.
     */
    private ObjectMapper redisObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        return objectMapper;
    }
}

