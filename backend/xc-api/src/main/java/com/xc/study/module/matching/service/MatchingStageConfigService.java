package com.xc.study.module.matching.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xc.study.common.BusinessException;
import com.xc.study.common.ErrorCode;
import com.xc.study.module.admin.entity.SystemConfig;
import com.xc.study.module.admin.mapper.SystemConfigMapper;
import com.xc.study.module.matching.vo.MatchingStageGroupVO;
import com.xc.study.module.matching.vo.MatchingStageLevelVO;
import com.xc.study.module.matching.vo.MatchingStageVO;
import java.time.OffsetDateTime;
import java.util.ArrayList;
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
    private static final String CONFIG_DESCRIPTION = "连连看难度/消消乐关卡组配置";
    private static final TypeReference<List<StageConfig>> STAGE_CONFIG_LIST_TYPE = new TypeReference<>() {
    };
    private static final List<StageConfig> DEFAULT_STAGE_GROUPS = List.of(
            new StageConfig(
                    "default",
                    Map.of("zh", "默认关卡", "en", "Default", "ru", "По умолчанию"),
                    null,
                    List.of(
                            new LevelConfig("4x4", Map.of("zh", "入门", "en", "Easy", "ru", "Легко"), 4, 60, true, 1),
                            new LevelConfig("7x7", Map.of("zh", "进阶", "en", "Medium", "ru", "Средне"), 7, 90, true, 2),
                            new LevelConfig("10x10", Map.of("zh", "挑战", "en", "Hard", "ru", "Сложно"), 10, 120, true, 3)
                    ),
                    true,
                    1
            )
    );

    private final SystemConfigMapper systemConfigMapper;
    private final ObjectMapper objectMapper;

    public MatchingStageConfigService(SystemConfigMapper systemConfigMapper, ObjectMapper objectMapper) {
        this.systemConfigMapper = systemConfigMapper;
        this.objectMapper = objectMapper;
    }

    public List<StageConfig> listStageGroups() {
        SystemConfig config = findConfig();
        if (config == null || !StringUtils.hasText(config.getConfigValue())) {
            return DEFAULT_STAGE_GROUPS;
        }
        try {
            List<StageConfig> stages = objectMapper.readValue(config.getConfigValue(), STAGE_CONFIG_LIST_TYPE);
            List<StageConfig> normalized = normalizeStageGroups(stages);
            return normalized.isEmpty() ? DEFAULT_STAGE_GROUPS : normalized;
        } catch (Exception ex) {
            return DEFAULT_STAGE_GROUPS;
        }
    }

    public List<StageConfig> listActiveStageGroups() {
        return listStageGroups().stream()
                .filter(StageConfig::enabled)
                .sorted(Comparator.comparing(StageConfig::sortOrder).thenComparing(StageConfig::code))
                .toList();
    }

    public List<LevelConfig> listActiveLevels() {
        return listActiveStageGroups().stream()
                .flatMap(stage -> stage.levels().stream())
                .filter(LevelConfig::enabled)
                .toList();
    }

    public LevelConfig requireActiveStage(String code) {
        return listActiveLevels().stream()
                .filter(stage -> stage.code().equals(code))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR, "关卡不存在或已停用"));
    }

    public List<MatchingStageVO> listActiveStageVos() {
        return listActiveLevels().stream().map(level -> toStageVO(level, false, false, null)).toList();
    }

    public List<MatchingStageVO> listStageVos() {
        return listStageGroups().stream()
                .flatMap(stage -> stage.levels().stream())
                .map(level -> toStageVO(level, false, false, null))
                .toList();
    }

    public List<MatchingStageGroupVO> listActiveStageGroupVos() {
        return listActiveStageGroupVos(Map.of());
    }

    public List<MatchingStageGroupVO> listStageGroupVos() {
        return listStageGroups().stream().map(stage -> toStageGroupVO(stage, Map.of())).toList();
    }

    public List<MatchingStageGroupVO> listActiveStageGroupVos(Map<String, LevelProgress> progressByCode) {
        return listActiveStageGroups().stream()
                .map(this::onlyEnabledLevels)
                .filter(stage -> !stage.levels().isEmpty())
                .map(stage -> toStageGroupVO(stage, progressByCode))
                .toList();
    }

    private StageConfig onlyEnabledLevels(StageConfig stage) {
        return new StageConfig(
                stage.code(),
                stage.labels(),
                stage.pairCount(),
                stage.levels().stream().filter(LevelConfig::enabled).toList(),
                stage.enabled(),
                stage.sortOrder()
        );
    }

    public List<MatchingStageGroupVO> upsertStages(List<StageConfig> stages, Long updatedBy) {
        List<StageConfig> normalized = normalizeStageGroups(stages);
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
        return normalized.stream().map(stage -> toStageGroupVO(stage, Map.of())).toList();
    }

    public MatchingStageVO toStageVO(LevelConfig stage, boolean unlocked, boolean completed, Integer bestElapsedSeconds) {
        return new MatchingStageVO(
                stage.code(),
                stage.labels(),
                stage.pairCount(),
                stage.pairCount() * 2,
                stage.timeLimitSeconds(),
                stage.enabled(),
                stage.sortOrder(),
                unlocked,
                completed,
                bestElapsedSeconds
        );
    }

    public MatchingStageGroupVO toStageGroupVO(StageConfig stage, Map<String, LevelProgress> progressByCode) {
        List<LevelConfig> enabledLevels = stage.levels().stream()
                .filter(LevelConfig::enabled)
                .toList();
        return new MatchingStageGroupVO(
                stage.code(),
                stage.labels(),
                stage.enabled(),
                stage.sortOrder(),
                stage.levels().stream()
                        .map(level -> toStageLevelVO(level, isLevelUnlocked(enabledLevels, level, progressByCode), progressByCode.get(level.code())))
                        .toList()
        );
    }

    private MatchingStageLevelVO toStageLevelVO(LevelConfig level, boolean unlocked, LevelProgress progress) {
        boolean completed = progress != null && progress.completed();
        return new MatchingStageLevelVO(
                level.code(),
                level.labels(),
                level.pairCount(),
                level.pairCount() * 2,
                level.timeLimitSeconds(),
                level.enabled(),
                level.sortOrder(),
                unlocked,
                completed,
                progress == null ? null : progress.bestElapsedSeconds()
        );
    }

    private boolean isLevelUnlocked(List<LevelConfig> enabledLevels, LevelConfig level, Map<String, LevelProgress> progressByCode) {
        int index = enabledLevels.indexOf(level);
        if (index <= 0) {
            return index == 0;
        }
        LevelConfig previousLevel = enabledLevels.get(index - 1);
        LevelProgress previousProgress = progressByCode.get(previousLevel.code());
        return previousProgress != null && previousProgress.completed();
    }

    private SystemConfig findConfig() {
        return systemConfigMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, CONFIG_KEY)
                .last("limit 1"));
    }

    private List<StageConfig> normalizeStageGroups(List<StageConfig> stages) {
        if (stages == null) {
            return List.of();
        }
        Set<String> codes = new LinkedHashSet<>();
        Set<String> levelCodes = new LinkedHashSet<>();
        List<StageConfig> normalized = new ArrayList<>();
        for (StageConfig stage : stages) {
            StageConfig normalizedStage = normalizeStageGroup(stage);
            if (normalizedStage == null) {
                continue;
            }
            if (!codes.add(normalizedStage.code())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "关卡组编码不能重复");
            }
            for (LevelConfig level : normalizedStage.levels()) {
                if (!levelCodes.add(level.code())) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR, "关卡编码不能重复");
                }
            }
            normalized.add(normalizedStage);
        }
        return normalized.stream()
                .sorted(Comparator.comparing(StageConfig::sortOrder).thenComparing(StageConfig::code))
                .toList();
    }

    private StageConfig normalizeStageGroup(StageConfig stage) {
        if (stage == null || !StringUtils.hasText(stage.code())) {
            return null;
        }
        String code = stage.code().trim();
        if (!code.matches("[A-Za-z0-9_-]{1,30}")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "关卡组编码只能包含字母、数字、下划线和短横线");
        }
        int sortOrder = stage.sortOrder() == null ? 100 : stage.sortOrder();
        boolean enabled = !Boolean.FALSE.equals(stage.enabled());
        Map<String, String> labels = normalizeLabels(code, stage.labels());
        List<LevelConfig> levels;
        if (stage.levels() == null || stage.levels().isEmpty()) {
            Integer legacyPairCount = stage.pairCount() == null ? 4 : stage.pairCount();
            levels = List.of(normalizeLevel(
                    new LevelConfig(code, labels, legacyPairCount, null, enabled, sortOrder),
                    code,
                    labels,
                    1
            ));
        } else {
            List<LevelConfig> normalizedLevels = new ArrayList<>();
            int index = 1;
            for (LevelConfig level : stage.levels()) {
                LevelConfig normalizedLevel = normalizeLevel(level, code, labels, index);
                if (normalizedLevel != null) {
                    normalizedLevels.add(normalizedLevel);
                    index++;
                }
            }
            levels = normalizedLevels.stream()
                    .sorted(Comparator.comparing(LevelConfig::sortOrder).thenComparing(LevelConfig::code))
                    .toList();
        }
        if (levels.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "每个关卡组至少需要一个关卡");
        }
        return new StageConfig(code, labels, null, levels, enabled, sortOrder);
    }

    private LevelConfig normalizeLevel(LevelConfig level, String groupCode, Map<String, String> groupLabels, int index) {
        if (level == null) {
            return null;
        }
        String fallbackCode = groupCode + "_" + index;
        String code = StringUtils.hasText(level.code()) ? level.code().trim() : fallbackCode;
        if (!code.matches("[A-Za-z0-9_-]{1,30}")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "关卡编码只能包含字母、数字、下划线和短横线");
        }
        int pairCount = level.pairCount() == null ? 4 : Math.max(2, Math.min(30, level.pairCount()));
        int timeLimitSeconds = level.timeLimitSeconds() == null
                ? defaultTimeLimitSeconds(pairCount)
                : Math.max(15, Math.min(3600, level.timeLimitSeconds()));
        int sortOrder = level.sortOrder() == null ? index : level.sortOrder();
        Map<String, String> labels = normalizeLabels(groupLabels.getOrDefault("zh", code), level.labels() == null ? groupLabels : level.labels());
        return new LevelConfig(code, labels, pairCount, timeLimitSeconds, !Boolean.FALSE.equals(level.enabled()), sortOrder);
    }

    private int defaultTimeLimitSeconds(int pairCount) {
        return Math.max(45, Math.min(300, pairCount * 10 + 20));
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
            List<LevelConfig> levels,
            Boolean enabled,
            Integer sortOrder
    ) {
    }

    public record LevelConfig(
            String code,
            Map<String, String> labels,
            Integer pairCount,
            Integer timeLimitSeconds,
            Boolean enabled,
            Integer sortOrder
    ) {
    }

    public record LevelProgress(
            boolean completed,
            Integer bestElapsedSeconds
    ) {
    }
}
