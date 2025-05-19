package com.petforwork.cashing;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizer;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;


@Configuration
public class CashingConfiguration {
    private final static Long DEFAULT_EXPIRED_TIME = 5L;
    private final static Long DEFAULT_AUTO_EXPIRED_TIME = 1L;

    @Bean("caffeine")
    public CacheManager caffeineCacheManager() {
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
                .expireAfterWrite(DEFAULT_EXPIRED_TIME, TimeUnit.MINUTES); //default конфигурация кофеина для всех кешей
        CaffeineCacheManager manager = new CaffeineCacheManager("users");
        manager.setCaffeine(caffeine);
        return manager; //НЕ поддерживает разный TTL внутри одного CacheManager как редис, по этому придется создавать еще один с другим TTL.
    }
    //Плюсы:
    //
    //Быстрый in-memory кэш.
    //
    //Устанавливается TTL через .expireAfterWrite(...).
    //
    //Минусы:
    //
    //Один TTL на весь менеджер — нельзя указать разные TTL для разных кэшей.
    //
    //Нет распределённости.


    @Bean("redis")
    @Primary
    public CacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(DEFAULT_EXPIRED_TIME));
        //дефолтный кфг для созданных в ручную не вписанных кешей

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(redisCacheConfiguration)
                .withInitialCacheConfigurations(
                        Collections.singletonMap("users", RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(DEFAULT_EXPIRED_TIME)))
                )
                .disableCreateOnMissingCache() // запрещает создавать кеш объекта автоматически, решение проблемы ниже.
                .build();
    }
    // Плюсы:
    //
    //Поддержка разных TTL для разных кэшей.
    //
    //Кэш может быть общим между инстансами.

    //Создаёт RedisCacheManager
    //
    //Устанавливает TTL 1 минута только для кэша с именем "users"
    //
    //Все другие кэши не настроены явно → будут создаваться с настройками по умолчанию (TTL = null, бессрочно)

    @Bean("customCache")
    public CacheManager customCacheManager() {
        return new SimpleCacheManager(DEFAULT_EXPIRED_TIME, DEFAULT_AUTO_EXPIRED_TIME);
    }

    @Bean
    public CacheManager defaultCacheManager(){
        return new ConcurrentMapCacheManager("users"); // дефолтная конфигурация кешменеджера  спринговского
        // так просто в локал памяти создать кеш
    }


    @Bean
    public CacheManagerCustomizer<ConcurrentMapCacheManager> cacheManagerCustomizer() {
        return cacheManager -> {
            cacheManager.setCacheNames(List.of("users"));
            cacheManager.setAllowNullValues(true);
        };
    }
    //Еще один миниконфигуратор кеша, который теперь дает еще и нуллвальюсы поставить
}
