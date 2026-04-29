package com.xc.study.common.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Component
public class MasterDataCache {

    private static final Logger log = LoggerFactory.getLogger(MasterDataCache.class);

    private final RedisMasterDataCacheProperties properties;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final ObjectMapper objectMapper;

    public MasterDataCache(
            RedisMasterDataCacheProperties properties,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.redisTemplateProvider = redisTemplateProvider;
        this.objectMapper = objectMapper;
    }

    public <T> T get(String keySuffix, TypeReference<T> typeReference, Supplier<T> loader) {
        StringRedisTemplate redisTemplate = redisTemplate();
        if (redisTemplate == null) {
            return loader.get();
        }

        String key = cacheKey(keySuffix);
        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (StringUtils.hasText(cached)) {
                return objectMapper.readValue(cached, typeReference);
            }
        } catch (Exception ex) {
            log.warn("Failed to read master data cache key {}", key, ex);
        }

        T value = loader.get();
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), properties.masterDataTtl());
        } catch (Exception ex) {
            log.warn("Failed to write master data cache key {}", key, ex);
        }
        return value;
    }

    public void evictByPrefix(String prefixSuffix) {
        StringRedisTemplate redisTemplate = redisTemplate();
        if (redisTemplate == null) {
            return;
        }

        String pattern = cacheKey(prefixSuffix) + "*";
        List<String> keys = new ArrayList<>();
        try (Cursor<String> cursor = redisTemplate.scan(ScanOptions.scanOptions()
                .match(pattern)
                .count(1000)
                .build())) {
            cursor.forEachRemaining(keys::add);
        } catch (Exception ex) {
            log.warn("Failed to scan master data cache keys by pattern {}", pattern, ex);
            return;
        }

        if (CollectionUtils.isEmpty(keys)) {
            return;
        }
        try {
            redisTemplate.delete(keys);
        } catch (Exception ex) {
            log.warn("Failed to evict master data cache keys by pattern {}", pattern, ex);
        }
    }

    public void evictByPrefixesAfterCommit(String... prefixSuffixes) {
        List<String> prefixes = Arrays.stream(prefixSuffixes)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
        if (prefixes.isEmpty()) {
            return;
        }
        Runnable eviction = () -> prefixes.forEach(this::evictByPrefix);
        if (!TransactionSynchronizationManager.isSynchronizationActive()
                || !TransactionSynchronizationManager.isActualTransactionActive()) {
            eviction.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eviction.run();
            }
        });
    }

    private StringRedisTemplate redisTemplate() {
        if (!properties.isEnabled()) {
            return null;
        }
        return redisTemplateProvider.getIfAvailable();
    }

    private String cacheKey(String keySuffix) {
        String suffix = StringUtils.hasText(keySuffix) ? keySuffix.trim() : "";
        while (suffix.startsWith(":")) {
            suffix = suffix.substring(1);
        }
        return properties.normalizedKeyPrefix() + "master:" + suffix;
    }
}
