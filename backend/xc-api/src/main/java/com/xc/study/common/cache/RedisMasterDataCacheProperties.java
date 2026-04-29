package com.xc.study.common.cache;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConfigurationProperties(prefix = "app.cache.redis")
public class RedisMasterDataCacheProperties {

    private boolean enabled = false;
    private String keyPrefix = "xc:";
    private long masterDataTtlSeconds = 300;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public long getMasterDataTtlSeconds() {
        return masterDataTtlSeconds;
    }

    public void setMasterDataTtlSeconds(long masterDataTtlSeconds) {
        this.masterDataTtlSeconds = masterDataTtlSeconds;
    }

    public String normalizedKeyPrefix() {
        String normalized = StringUtils.hasText(keyPrefix) ? keyPrefix.trim() : "xc:";
        return normalized.endsWith(":") ? normalized : normalized + ":";
    }

    public Duration masterDataTtl() {
        return Duration.ofSeconds(Math.max(1, masterDataTtlSeconds));
    }
}
