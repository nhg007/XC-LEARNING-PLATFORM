package com.xc.study.module.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xc.study.common.BusinessException;
import com.xc.study.common.ErrorCode;
import com.xc.study.common.PageResult;
import com.xc.study.module.admin.dto.AdminBatchBindMediaAssetDTO;
import com.xc.study.module.admin.dto.AdminExerciseSetQueryDTO;
import com.xc.study.module.admin.dto.AdminSentenceExerciseQueryDTO;
import com.xc.study.module.admin.dto.AdminSentenceWordOptionDTO;
import com.xc.study.module.admin.dto.AdminUpdateContentStatusDTO;
import com.xc.study.module.admin.dto.AdminUpsertExerciseSetDTO;
import com.xc.study.module.admin.dto.AdminUpsertSentenceExerciseDTO;
import com.xc.study.module.admin.entity.AdminOperationLog;
import com.xc.study.module.admin.mapper.AdminOperationLogMapper;
import com.xc.study.module.admin.service.AdminExerciseManagementService;
import com.xc.study.module.admin.vo.AdminBatchBindMediaAssetResultVO;
import com.xc.study.module.admin.vo.AdminExerciseSetVO;
import com.xc.study.module.admin.vo.AdminSentenceExerciseVO;
import com.xc.study.module.admin.vo.AdminSentenceWordOptionVO;
import com.xc.study.module.exercise.entity.ExerciseSet;
import com.xc.study.module.exercise.entity.SentenceExercise;
import com.xc.study.module.exercise.entity.SentenceWordOption;
import com.xc.study.module.exercise.mapper.ExerciseSetMapper;
import com.xc.study.module.exercise.mapper.SentenceExerciseMapper;
import com.xc.study.module.exercise.mapper.SentenceWordOptionMapper;
import com.xc.study.module.media.entity.MediaAsset;
import com.xc.study.module.media.mapper.MediaAssetMapper;
import com.xc.study.security.CurrentUser;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminExerciseManagementServiceImpl implements AdminExerciseManagementService {

    private static final Set<String> ORDER_EXERCISE_TYPES = Set.of("audio_order", "translation_order");
    private static final Set<String> AUDIO_EXERCISE_TYPES = Set.of("audio_order", "audio_dictation");

    private final ExerciseSetMapper exerciseSetMapper;
    private final SentenceExerciseMapper sentenceExerciseMapper;
    private final SentenceWordOptionMapper sentenceWordOptionMapper;
    private final MediaAssetMapper mediaAssetMapper;
    private final AdminOperationLogMapper adminOperationLogMapper;
    private final ObjectMapper objectMapper;

    public AdminExerciseManagementServiceImpl(
            ExerciseSetMapper exerciseSetMapper,
            SentenceExerciseMapper sentenceExerciseMapper,
            SentenceWordOptionMapper sentenceWordOptionMapper,
            MediaAssetMapper mediaAssetMapper,
            AdminOperationLogMapper adminOperationLogMapper,
            ObjectMapper objectMapper
    ) {
        this.exerciseSetMapper = exerciseSetMapper;
        this.sentenceExerciseMapper = sentenceExerciseMapper;
        this.sentenceWordOptionMapper = sentenceWordOptionMapper;
        this.mediaAssetMapper = mediaAssetMapper;
        this.adminOperationLogMapper = adminOperationLogMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public PageResult<AdminExerciseSetVO> pageSets(AdminExerciseSetQueryDTO query, CurrentUser admin) {
        requirePermission(admin, "admin:content:read");
        int page = query.getPage() == null ? 1 : query.getPage();
        int pageSize = query.getPageSize() == null ? 20 : query.getPageSize();
        LambdaQueryWrapper<ExerciseSet> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getExerciseType())) {
            wrapper.eq(ExerciseSet::getExerciseType, query.getExerciseType());
        }
        if (StringUtils.hasText(query.getLevel())) {
            wrapper.eq(ExerciseSet::getLevel, query.getLevel().trim());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(ExerciseSet::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(item -> item.like(ExerciseSet::getTitle, keyword)
                    .or()
                    .like(ExerciseSet::getLevel, keyword));
        }
        wrapper.orderByAsc(ExerciseSet::getId);
        Page<ExerciseSet> result = exerciseSetMapper.selectPage(Page.of(page, pageSize), wrapper);
        return PageResult.of(
                result.getRecords().stream().map(this::toSetVO).toList(),
                result.getTotal(),
                result.getCurrent(),
                result.getSize()
        );
    }

    @Override
    @Transactional
    public AdminExerciseSetVO createSet(AdminUpsertExerciseSetDTO request, CurrentUser admin, String ipAddress) {
        requirePermission(admin, "admin:content:update");
        OffsetDateTime now = OffsetDateTime.now();
        ExerciseSet set = new ExerciseSet();
        fillSet(set, request);
        set.setStatus(StringUtils.hasText(request.status()) ? request.status() : "active");
        set.setCreatedAt(now);
        set.setUpdatedAt(now);
        exerciseSetMapper.insert(set);
        writeOperationLog(admin.id(), "content.exercise.set.create", "exercise_set", set.getId(), setSnapshot(set), ipAddress);
        return toSetVO(set);
    }

    @Override
    @Transactional
    public AdminExerciseSetVO updateSet(Long setId, AdminUpsertExerciseSetDTO request, CurrentUser admin, String ipAddress) {
        requirePermission(admin, "admin:content:update");
        ExerciseSet set = requireSet(setId);
        if (!Objects.equals(set.getExerciseType(), request.exerciseType()) && countExercises(setId, null) > 0) {
            throw new BusinessException("题组已有题目，不能修改题型");
        }
        Map<String, Object> before = setSnapshot(set);
        fillSet(set, request);
        if (StringUtils.hasText(request.status())) {
            set.setStatus(request.status());
        }
        set.setUpdatedAt(OffsetDateTime.now());
        exerciseSetMapper.updateById(set);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("before", before);
        detail.put("after", setSnapshot(set));
        writeOperationLog(admin.id(), "content.exercise.set.update", "exercise_set", setId, detail, ipAddress);
        return toSetVO(set);
    }

    @Override
    @Transactional
    public AdminExerciseSetVO updateSetStatus(Long setId, AdminUpdateContentStatusDTO request, CurrentUser admin, String ipAddress) {
        requirePermission(admin, "admin:content:update");
        ExerciseSet set = requireSet(setId);
        String beforeStatus = set.getStatus();
        set.setStatus(request.status());
        set.setUpdatedAt(OffsetDateTime.now());
        exerciseSetMapper.updateById(set);
        writeOperationLog(admin.id(), "content.exercise.set.status.update", "exercise_set", setId, Map.of(
                "beforeStatus", beforeStatus,
                "afterStatus", request.status(),
                "reason", request.reason() == null ? "" : request.reason()
        ), ipAddress);
        return toSetVO(set);
    }

    @Override
    public PageResult<AdminSentenceExerciseVO> pageSentenceExercises(AdminSentenceExerciseQueryDTO query, CurrentUser admin) {
        requirePermission(admin, "admin:content:read");
        int page = query.getPage() == null ? 1 : query.getPage();
        int pageSize = query.getPageSize() == null ? 20 : query.getPageSize();
        LambdaQueryWrapper<SentenceExercise> wrapper = new LambdaQueryWrapper<>();
        if (query.getExerciseSetId() != null) {
            wrapper.eq(SentenceExercise::getExerciseSetId, query.getExerciseSetId());
        }
        if (StringUtils.hasText(query.getExerciseType())) {
            wrapper.eq(SentenceExercise::getExerciseType, query.getExerciseType());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(SentenceExercise::getStatus, query.getStatus());
        }
        if (query.getHasAudio() != null) {
            if (query.getHasAudio()) {
                wrapper.isNotNull(SentenceExercise::getAudioZhAssetId);
            } else {
                wrapper.isNull(SentenceExercise::getAudioZhAssetId);
            }
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(item -> item.like(SentenceExercise::getHanziAnswer, keyword)
                    .or()
                    .like(SentenceExercise::getPinyinPrompt, keyword)
                    .or()
                    .like(SentenceExercise::getTranslationEn, keyword)
                    .or()
                    .like(SentenceExercise::getTranslationRu, keyword));
        }
        wrapper.orderByAsc(SentenceExercise::getExerciseSetId)
                .orderByAsc(SentenceExercise::getSortOrder)
                .orderByDesc(SentenceExercise::getUpdatedAt)
                .orderByDesc(SentenceExercise::getId);
        Page<SentenceExercise> result = sentenceExerciseMapper.selectPage(Page.of(page, pageSize), wrapper);
        return PageResult.of(toSentenceVOs(result.getRecords()), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional
    public AdminSentenceExerciseVO createSentenceExercise(AdminUpsertSentenceExerciseDTO request, CurrentUser admin, String ipAddress) {
        requirePermission(admin, "admin:content:update");
        validateSentenceRequest(request);
        OffsetDateTime now = OffsetDateTime.now();
        SentenceExercise exercise = new SentenceExercise();
        fillSentenceExercise(exercise, request);
        exercise.setStatus(StringUtils.hasText(request.status()) ? request.status() : "active");
        exercise.setCreatedAt(now);
        exercise.setUpdatedAt(now);
        sentenceExerciseMapper.insert(exercise);
        replaceWordOptions(exercise.getId(), request.wordOptions(), now);
        writeOperationLog(admin.id(), "content.sentence.exercise.create", "sentence_exercise", exercise.getId(), sentenceSnapshot(exercise), ipAddress);
        return toSentenceVOs(List.of(exercise)).get(0);
    }

    @Override
    @Transactional
    public AdminSentenceExerciseVO updateSentenceExercise(Long exerciseId, AdminUpsertSentenceExerciseDTO request, CurrentUser admin, String ipAddress) {
        requirePermission(admin, "admin:content:update");
        validateSentenceRequest(request);
        SentenceExercise exercise = requireSentenceExercise(exerciseId);
        Map<String, Object> before = sentenceSnapshot(exercise);
        fillSentenceExercise(exercise, request);
        if (StringUtils.hasText(request.status())) {
            exercise.setStatus(request.status());
        }
        exercise.setUpdatedAt(OffsetDateTime.now());
        sentenceExerciseMapper.updateById(exercise);
        replaceWordOptions(exerciseId, request.wordOptions(), OffsetDateTime.now());
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("before", before);
        detail.put("after", sentenceSnapshot(exercise));
        writeOperationLog(admin.id(), "content.sentence.exercise.update", "sentence_exercise", exerciseId, detail, ipAddress);
        return toSentenceVOs(List.of(exercise)).get(0);
    }

    @Override
    @Transactional
    public AdminSentenceExerciseVO updateSentenceExerciseStatus(Long exerciseId, AdminUpdateContentStatusDTO request, CurrentUser admin, String ipAddress) {
        requirePermission(admin, "admin:content:update");
        SentenceExercise exercise = requireSentenceExercise(exerciseId);
        String beforeStatus = exercise.getStatus();
        exercise.setStatus(request.status());
        exercise.setUpdatedAt(OffsetDateTime.now());
        sentenceExerciseMapper.updateById(exercise);
        writeOperationLog(admin.id(), "content.sentence.exercise.status.update", "sentence_exercise", exerciseId, Map.of(
                "beforeStatus", beforeStatus,
                "afterStatus", request.status(),
                "reason", request.reason() == null ? "" : request.reason()
        ), ipAddress);
        return toSentenceVOs(List.of(exercise)).get(0);
    }

    @Override
    @Transactional
    public AdminBatchBindMediaAssetResultVO bindSentenceExerciseAudio(
            AdminBatchBindMediaAssetDTO request,
            CurrentUser admin,
            String ipAddress
    ) {
        requirePermission(admin, "admin:content:update");
        OffsetDateTime now = OffsetDateTime.now();
        List<String> errors = new ArrayList<>();
        List<Map<String, Object>> successfulBindings = new ArrayList<>();
        for (var binding : request.bindings()) {
            Long exerciseId = binding.targetId();
            Long mediaAssetId = binding.mediaAssetId();
            SentenceExercise exercise = sentenceExerciseMapper.selectById(exerciseId);
            if (exercise == null) {
                errors.add("句子题 ID " + exerciseId + " 不存在");
                continue;
            }
            MediaAsset asset = mediaAssetMapper.selectById(mediaAssetId);
            if (!isActiveMediaAsset(asset, "audio")) {
                errors.add("句子题 ID " + exerciseId + " 的音频资源 ID " + mediaAssetId + " 不存在、已停用或类型不正确");
                continue;
            }
            Long beforeAudioAssetId = exercise.getAudioZhAssetId();
            exercise.setAudioZhAssetId(mediaAssetId);
            exercise.setUpdatedAt(now);
            sentenceExerciseMapper.updateById(exercise);
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("targetId", exerciseId);
            detail.put("beforeAudioZhAssetId", beforeAudioAssetId);
            detail.put("afterAudioZhAssetId", mediaAssetId);
            successfulBindings.add(detail);
        }
        writeOperationLog(admin.id(), "content.sentence.exercise.audio.batch_bind", "sentence_exercise", null, Map.of(
                "requestedCount", request.bindings().size(),
                "successCount", successfulBindings.size(),
                "errors", errors,
                "bindings", successfulBindings
        ), ipAddress);
        return new AdminBatchBindMediaAssetResultVO(request.bindings().size(), successfulBindings.size(), errors);
    }

    private void fillSet(ExerciseSet set, AdminUpsertExerciseSetDTO request) {
        set.setTitle(request.title().trim());
        set.setExerciseType(request.exerciseType());
        set.setLevel(blankToNull(request.level()));
    }

    private void fillSentenceExercise(SentenceExercise exercise, AdminUpsertSentenceExerciseDTO request) {
        exercise.setExerciseSetId(request.exerciseSetId());
        exercise.setExerciseType(request.exerciseType());
        exercise.setHanziAnswer(request.hanziAnswer().trim());
        exercise.setPinyinPrompt(blankToNull(request.pinyinPrompt()));
        exercise.setTranslationEn(blankToNull(request.translationEn()));
        exercise.setTranslationRu(blankToNull(request.translationRu()));
        exercise.setAudioZhAssetId(request.audioZhAssetId());
        exercise.setExplanation(blankToNull(request.explanation()));
        exercise.setSortOrder(request.sortOrder());
    }

    private void validateSentenceRequest(AdminUpsertSentenceExerciseDTO request) {
        ExerciseSet set = requireSet(request.exerciseSetId());
        if (!Objects.equals(set.getExerciseType(), request.exerciseType())) {
            throw new BusinessException("题目类型必须和题组类型一致");
        }
        if (AUDIO_EXERCISE_TYPES.contains(request.exerciseType()) && request.audioZhAssetId() == null) {
            throw new BusinessException("听力题必须选择中文音频");
        }
        if (request.audioZhAssetId() != null) {
            MediaAsset asset = mediaAssetMapper.selectById(request.audioZhAssetId());
            if (asset == null || !"audio".equals(asset.getMediaType()) || !"active".equals(asset.getStatus())) {
                throw BusinessException.notFound("音频资源不存在或已停用");
            }
        }
        List<AdminSentenceWordOptionDTO> options = request.wordOptions() == null ? List.of() : request.wordOptions();
        if (ORDER_EXERCISE_TYPES.contains(request.exerciseType()) && options.isEmpty()) {
            throw new BusinessException("排序题必须维护词块选项");
        }
        Set<Integer> orders = new HashSet<>();
        for (AdminSentenceWordOptionDTO option : options) {
            if (!orders.add(option.correctOrder())) {
                throw new BusinessException("词块顺序不能重复");
            }
        }
    }

    private boolean isActiveMediaAsset(MediaAsset asset, String mediaType) {
        return asset != null && mediaType.equals(asset.getMediaType()) && "active".equals(asset.getStatus());
    }

    private void replaceWordOptions(Long exerciseId, List<AdminSentenceWordOptionDTO> wordOptions, OffsetDateTime now) {
        sentenceWordOptionMapper.delete(new LambdaQueryWrapper<SentenceWordOption>()
                .eq(SentenceWordOption::getExerciseId, exerciseId));
        if (wordOptions == null || wordOptions.isEmpty()) {
            return;
        }
        wordOptions.stream()
                .sorted((left, right) -> left.correctOrder().compareTo(right.correctOrder()))
                .forEach(optionRequest -> {
                    SentenceWordOption option = new SentenceWordOption();
                    option.setExerciseId(exerciseId);
                    option.setWordText(optionRequest.wordText().trim());
                    option.setCorrectOrder(optionRequest.correctOrder());
                    option.setCreatedAt(now);
                    option.setUpdatedAt(now);
                    sentenceWordOptionMapper.insert(option);
                });
    }

    private AdminExerciseSetVO toSetVO(ExerciseSet set) {
        return new AdminExerciseSetVO(
                set.getId(),
                set.getTitle(),
                set.getExerciseType(),
                set.getLevel(),
                set.getStatus(),
                countExercises(set.getId(), "active"),
                countExercises(set.getId(), "inactive"),
                set.getCreatedAt(),
                set.getUpdatedAt()
        );
    }

    private List<AdminSentenceExerciseVO> toSentenceVOs(List<SentenceExercise> exercises) {
        if (exercises.isEmpty()) {
            return List.of();
        }
        Map<Long, ExerciseSet> sets = loadSets(exercises.stream().map(SentenceExercise::getExerciseSetId).toList());
        Map<Long, List<SentenceWordOption>> optionsByExerciseId = loadOptionsByExerciseId(exercises.stream()
                .map(SentenceExercise::getId)
                .toList());
        Map<Long, MediaAsset> audioAssets = loadAudioAssets(exercises.stream()
                .map(SentenceExercise::getAudioZhAssetId)
                .toList());
        return exercises.stream()
                .map(exercise -> {
                    ExerciseSet set = sets.get(exercise.getExerciseSetId());
                    MediaAsset asset = audioAssets.get(exercise.getAudioZhAssetId());
                    return new AdminSentenceExerciseVO(
                            exercise.getId(),
                            exercise.getExerciseSetId(),
                            set == null ? null : set.getTitle(),
                            exercise.getExerciseType(),
                            exercise.getHanziAnswer(),
                            exercise.getPinyinPrompt(),
                            exercise.getTranslationEn(),
                            exercise.getTranslationRu(),
                            exercise.getAudioZhAssetId(),
                            asset == null ? null : asset.getUrl(),
                            exercise.getExplanation(),
                            exercise.getSortOrder(),
                            exercise.getStatus(),
                            exercise.getCreatedAt(),
                            exercise.getUpdatedAt(),
                            toOptionVOs(optionsByExerciseId.getOrDefault(exercise.getId(), List.of()))
                    );
                })
                .toList();
    }

    private List<AdminSentenceWordOptionVO> toOptionVOs(List<SentenceWordOption> options) {
        return options.stream()
                .map(option -> new AdminSentenceWordOptionVO(
                        option.getId(),
                        option.getExerciseId(),
                        option.getWordText(),
                        option.getCorrectOrder(),
                        option.getCreatedAt(),
                        option.getUpdatedAt()
                ))
                .toList();
    }

    private Map<Long, ExerciseSet> loadSets(List<Long> ids) {
        List<Long> setIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (setIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return exerciseSetMapper.selectBatchIds(setIds)
                .stream()
                .collect(Collectors.toMap(ExerciseSet::getId, Function.identity()));
    }

    private Map<Long, List<SentenceWordOption>> loadOptionsByExerciseId(List<Long> ids) {
        List<Long> exerciseIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (exerciseIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return sentenceWordOptionMapper.selectList(new LambdaQueryWrapper<SentenceWordOption>()
                        .in(SentenceWordOption::getExerciseId, exerciseIds)
                        .orderByAsc(SentenceWordOption::getCorrectOrder)
                        .orderByAsc(SentenceWordOption::getId))
                .stream()
                .collect(Collectors.groupingBy(SentenceWordOption::getExerciseId));
    }

    private Map<Long, MediaAsset> loadAudioAssets(List<Long> ids) {
        List<Long> assetIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (assetIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return mediaAssetMapper.selectBatchIds(assetIds)
                .stream()
                .collect(Collectors.toMap(MediaAsset::getId, Function.identity()));
    }

    private long countExercises(Long setId, String status) {
        LambdaQueryWrapper<SentenceExercise> wrapper = new LambdaQueryWrapper<SentenceExercise>()
                .eq(SentenceExercise::getExerciseSetId, setId);
        if (status != null) {
            wrapper.eq(SentenceExercise::getStatus, status);
        }
        return sentenceExerciseMapper.selectCount(wrapper);
    }

    private ExerciseSet requireSet(Long setId) {
        ExerciseSet set = exerciseSetMapper.selectById(setId);
        if (set == null) {
            throw BusinessException.notFound("题组不存在");
        }
        return set;
    }

    private SentenceExercise requireSentenceExercise(Long exerciseId) {
        SentenceExercise exercise = sentenceExerciseMapper.selectById(exerciseId);
        if (exercise == null) {
            throw BusinessException.notFound("题目不存在");
        }
        return exercise;
    }

    private Map<String, Object> setSnapshot(ExerciseSet set) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("title", set.getTitle());
        snapshot.put("exerciseType", set.getExerciseType());
        snapshot.put("level", set.getLevel());
        snapshot.put("status", set.getStatus());
        return snapshot;
    }

    private Map<String, Object> sentenceSnapshot(SentenceExercise exercise) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("exerciseSetId", exercise.getExerciseSetId());
        snapshot.put("exerciseType", exercise.getExerciseType());
        snapshot.put("hanziAnswer", exercise.getHanziAnswer());
        snapshot.put("pinyinPrompt", exercise.getPinyinPrompt());
        snapshot.put("translationEn", exercise.getTranslationEn());
        snapshot.put("translationRu", exercise.getTranslationRu());
        snapshot.put("audioZhAssetId", exercise.getAudioZhAssetId());
        snapshot.put("explanation", exercise.getExplanation());
        snapshot.put("sortOrder", exercise.getSortOrder());
        snapshot.put("status", exercise.getStatus());
        return snapshot;
    }

    private void requirePermission(CurrentUser admin, String permission) {
        if (admin.permissions().contains("admin:*") || admin.permissions().contains(permission)) {
            return;
        }
        throw BusinessException.forbidden(ErrorCode.FORBIDDEN, "缺少后台权限：" + permission);
    }

    private void writeOperationLog(
            Long adminUserId,
            String action,
            String targetType,
            Long targetId,
            Map<String, Object> detail,
            String ipAddress
    ) {
        OffsetDateTime now = OffsetDateTime.now();
        AdminOperationLog log = new AdminOperationLog();
        log.setAdminUserId(adminUserId);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(toJson(detail));
        log.setIpAddress(ipAddress);
        log.setCreatedAt(now);
        log.setUpdatedAt(now);
        adminOperationLogMapper.insertLog(log);
    }

    private String toJson(Map<String, Object> detail) {
        try {
            return objectMapper.writeValueAsString(new HashMap<>(detail));
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }

    private String blankToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
