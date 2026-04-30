package com.xc.study.module.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xc.study.module.admin.entity.SystemConfig;
import com.xc.study.module.admin.mapper.SystemConfigMapper;
import java.time.OffsetDateTime;
import java.util.Set;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RuntimeConfigService {

    public static final String MEMBERSHIP_TRIAL_DAYS = "membership.trial_days";
    public static final String PAYMENT_MOCK_ENABLED = "payment.mock_enabled";
    public static final String PAYMENT_WECHAT_ENABLED = "payment.wechat_pay.enabled";
    public static final String PAYMENT_WECHAT_URL_PREFIX = "payment.wechat_pay.payment_url_prefix";
    public static final String PAYMENT_WECHAT_NOTIFY_SECRET = "payment.wechat_pay.notify_secret";
    public static final String PAYMENT_ALIPAY_ENABLED = "payment.alipay.enabled";
    public static final String PAYMENT_ALIPAY_URL_PREFIX = "payment.alipay.payment_url_prefix";
    public static final String PAYMENT_ALIPAY_NOTIFY_SECRET = "payment.alipay.notify_secret";
    public static final String ASR_PROVIDER = "asr.provider";
    public static final String ASR_WORKER_ENABLED = "asr.worker_enabled";
    public static final String ASR_ENGINE_NAME = "asr.engine_name";
    public static final String ASR_SERVICE_URL = "asr.service_url";
    public static final String ASR_SERVICE_PATH = "asr.service_path";
    public static final String ASR_TIMEOUT_MS = "asr.timeout_ms";
    public static final String ASR_BATCH_SIZE = "asr.batch_size";
    public static final String ASR_INITIAL_DELAY_MS = "asr.initial_delay_ms";
    public static final String ASR_POLL_DELAY_MS = "asr.poll_delay_ms";
    public static final String ASR_MOCK_RECOGNIZED_TEXT = "asr.mock_recognized_text";
    public static final String UPLOAD_MAX_FILE_SIZE = "upload.max_file_size";
    public static final String UPLOAD_MAX_REQUEST_SIZE = "upload.max_request_size";
    public static final String UPLOAD_AUDIO_EXTENSIONS = "upload.audio_extensions";
    public static final String UPLOAD_IMAGE_EXTENSIONS = "upload.image_extensions";
    public static final String UPLOAD_VIDEO_EXTENSIONS = "upload.video_extensions";

    public static final Set<String> MANAGED_CONFIG_KEYS = Set.of(
            MEMBERSHIP_TRIAL_DAYS,
            PAYMENT_MOCK_ENABLED,
            PAYMENT_WECHAT_ENABLED,
            PAYMENT_WECHAT_URL_PREFIX,
            PAYMENT_WECHAT_NOTIFY_SECRET,
            PAYMENT_ALIPAY_ENABLED,
            PAYMENT_ALIPAY_URL_PREFIX,
            PAYMENT_ALIPAY_NOTIFY_SECRET,
            ASR_PROVIDER,
            ASR_WORKER_ENABLED,
            ASR_ENGINE_NAME,
            ASR_SERVICE_URL,
            ASR_SERVICE_PATH,
            ASR_TIMEOUT_MS,
            ASR_BATCH_SIZE,
            ASR_INITIAL_DELAY_MS,
            ASR_POLL_DELAY_MS,
            ASR_MOCK_RECOGNIZED_TEXT,
            UPLOAD_MAX_FILE_SIZE,
            UPLOAD_MAX_REQUEST_SIZE,
            UPLOAD_AUDIO_EXTENSIONS,
            UPLOAD_IMAGE_EXTENSIONS,
            UPLOAD_VIDEO_EXTENSIONS
    );

    private final SystemConfigMapper systemConfigMapper;
    private final Environment environment;

    public RuntimeConfigService(SystemConfigMapper systemConfigMapper, Environment environment) {
        this.systemConfigMapper = systemConfigMapper;
        this.environment = environment;
    }

    public String getString(String key, String fallback) {
        SystemConfig config = find(key);
        if (config != null && config.getConfigValue() != null) {
            return config.getConfigValue().trim();
        }
        return fallback == null ? "" : fallback;
    }

    public String getEnvString(String propertyName, String fallback) {
        return environment.getProperty(propertyName, fallback);
    }

    public boolean getBoolean(String key, boolean fallback) {
        String value = getString(key, String.valueOf(fallback));
        return Boolean.parseBoolean(value);
    }

    public int getInt(String key, int fallback) {
        String value = getString(key, String.valueOf(fallback));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    public long getLong(String key, long fallback) {
        String value = getString(key, String.valueOf(fallback));
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    public boolean isConfigured(String key) {
        SystemConfig config = find(key);
        return config != null && StringUtils.hasText(config.getConfigValue());
    }

    public boolean exists(String key) {
        return find(key) != null;
    }

    public void upsert(String key, String value, String group, String description, Long updatedBy) {
        OffsetDateTime now = OffsetDateTime.now();
        SystemConfig config = find(key);
        if (config == null) {
            config = new SystemConfig();
            config.setConfigKey(key);
            config.setConfigGroup(group);
            config.setDescription(description);
            config.setConfigValue(value);
            config.setUpdatedBy(updatedBy);
            config.setCreatedAt(now);
            config.setUpdatedAt(now);
            systemConfigMapper.insert(config);
            return;
        }
        config.setConfigGroup(group);
        config.setDescription(description);
        config.setConfigValue(value);
        config.setUpdatedBy(updatedBy);
        config.setUpdatedAt(now);
        systemConfigMapper.updateById(config);
    }

    private SystemConfig find(String key) {
        return systemConfigMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, key)
                .last("limit 1"));
    }
}
