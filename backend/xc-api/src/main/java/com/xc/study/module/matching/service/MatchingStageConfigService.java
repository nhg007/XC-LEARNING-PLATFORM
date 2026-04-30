package com.xc.study.module.matching.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xc.study.common.BusinessException;
import com.xc.study.common.ErrorCode;
import com.xc.study.module.admin.entity.SystemConfig;
import com.xc.study.module.admin.mapper.SystemConfigMapper;
import com.xc.study.module.matching.vo.MatchingStageVO;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MatchingStageConfigService {

    public static final String CONFIG_KEY = "matching.stages";

    private static final String CONFIG_GROUP = "learning";
    private static final String CONFIG_DESCRIPTION = "连连看/消消乐关卡配置";
    private static final TypeReference<List<StageConfig>> STAGE_CONFIG_LIST_TYPE = new TypeReference<>() {
    };
    private static final List<StageConfig> DEFAULT_STAGES = List.of(
            new StageConfig("4x4", Map.of("zh", "入门", "en", "Easy", "ru", "Легко"), 4, true, 1),
            new StageConfig("7x7", Map.of("zh", "进阶", "en", "Medium", "ru", "Средне"), 7, true, 2),
            new StageConfig("10x10", Map.of("zh", "挑战", "en", "Hard", "ru", "Сложно"), 10, true, 3)
    );

    private final SystemConfigMapper systemConfigMapper;
    private final ObjectMapper objectMapper;

    public MatchingStageConfigService(SystemConfigMapper systemConfigMapper, ObjectMapper objectMapper) {
        this.systemConfigMapper = systemConfigMapper;
        this.objectMapper = objectMapper;
    }

    public List<StageConfig> listStages() {
        SystemConfig config = findConfig();
        if (config == null || !StringUtils.hasText(config.getConfigValue())) {
            return DEFAULT_STAGES;
        }
        try {
            List<StageConfig> stages = objectMapper.readValue(config.getConfigValue(), STAGE_CONFIG_LIST_TYPE);
            List<StageConfig> normalized = normalizeStages(stages);
            return normalized.isEmpty() ? DEFAULT_STAGES : normalized;
        } catch (Exception ex) {
            return DEFAULT_STAGES;
        }
    }

    public List<StageConfig> listActiveStages() {
        return listStages().stream()
                .filter(StageConfig::enabled)
                .sorted(Comparator.comparing(StageConfig::sortOrder).thenComparing(StageConfig::code))
                .toList();
    }

    public StageConfig requireActiveStage(String code) {
        return listActiveStages().stream()
                .filter(stage -> stage.code().equals(code))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR, "关卡不存在或已停用"));
    }

    public List<MatchingStageVO> listActiveStageVos() {
        return listActiveStages().stream().map(this::toStageVO).toList();
    }

    public List<MatchingStageVO> listStageVos() {
        return listStages().stream().map(this::toStageVO).toList();
    }

    public List<MatchingStageVO> upsertStages(List<StageConfig> stages, Long updatedBy) {
        List<StageConfig> normalized = normalizeStages(stages);
        if (normalized.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "至少需要配置一个关卡");
        }

        OffsetDateTime now = OffsetDateTime.now();
        SystemConfig config = findConfig();
        String configValue = writeConfigValue(normalized);
        if (config == null) {
            config = new SystemConfig();
            config.setConfigKey(CONFIG_KEY);
            config.setConfigGroup(CONFIG_GROUP);
            config.setDescription(CONFIG_DESCRIPTION);
            config.setConfigValue(configValue);
            config.setUpdatedBy(updatedBy);
            config.setCreatedAt(now);
            config.setUpdatedAt(now);
            systemConfigMapper.insert(config);
        } else {
            config.setConfigValue(configValue);
            config.setConfigGroup(CONFIG_GROUP);
            if (!StringUtils.hasText(config.getDescription())) {
                config.setDescription(CONFIG_DESCRIPTION);
            }
            config.setUpdatedBy(updatedBy);
            config.setUpdatedAt(now);
            systemConfigMapper.updateById(config);
        }
        return normalized.stream().map(this::toStageVO).toList();
    }

    public MatchingStageVO toStageVO(StageConfig stage) {
        return new MatchingStageVO(
                stage.code(),
                stage.labels(),
                stage.pairCount(),
                stage.pairCount() * 2,
                stage.enabled(),
                stage.sortOrder()
        );
    }

    private SystemConfig findConfig() {
        return systemConfigMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, CONFIG_KEY)
                .last("limit 1"));
    }

    private List<StageConfig> normalizeStages(List<StageConfig> stages) {
        if (stages == null) {
            return List.of();
        }
        Set<String> codes = new LinkedHashSet<>();
        return stages.stream()
                .map(this::normalizeStage)
                .filter(stage -> stage != null)
                .peek(stage -> {
                    if (!codes.add(stage.code())) {
                        throw new BusinessException(ErrorCode.VALIDATION_ERROR, "关卡编码不能重复");
                    }
                })
                .sorted(Comparator.comparing(StageConfig::sortOrder).thenComparing(StageConfig::code))
                .toList();
    }

    private StageConfig normalizeStage(StageConfig stage) {
        if (stage == null || !StringUtils.hasText(stage.code())) {
            return null;
        }
        String code = stage.code().trim();
        if (!code.matches("[A-Za-z0-9_-]{1,30}")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "关卡编码只能包含字母、数字、下划线和短横线");
        }
        int pairCount = stage.pairCount() == null ? 4 : Math.max(2, Math.min(30, stage.pairCount()));
        int sortOrder = stage.sortOrder() == null ? 100 : stage.sortOrder();
        Map<String, String> labels = normalizeLabels(code, stage.labels());
        return new StageConfig(code, labels, pairCount, !Boolean.FALSE.equals(stage.enabled()), sortOrder);
    }

    private Map<String, String> normalizeLabels(String code, Map<String, String> labels) {
        String zh = labelOrDefault(labels, "zh", code);
        String en = labelOrDefault(labels, "en", zh);
        String ru = labelOrDefault(labels, "ru", zh);
        return Map.of("zh", zh, "en", en, "ru", ru);
    }

    private String labelOrDefault(Map<String, String> labels, String language, String fallback) {
        if (labels == null) {
            return fallback;
        }
        String label = labels.get(language);
        return StringUtils.hasText(label) ? label.trim() : fallback;
    }

    private String writeConfigValue(List<StageConfig> stages) {
        try {
            return objectMapper.writeValueAsString(stages);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "关卡配置序列化失败");
        }
    }

    public record StageConfig(
            String code,
            Map<String, String> labels,
            Integer pairCount,
            Boolean enabled,
            Integer sortOrder
    ) {
    }
}
