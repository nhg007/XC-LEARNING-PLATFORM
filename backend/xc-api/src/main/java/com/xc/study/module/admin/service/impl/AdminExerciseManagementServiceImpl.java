package com.xc.study.module.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xc.study.common.BusinessException;
import com.xc.study.common.ErrorCode;
import com.xc.study.common.PageResult;
import com.xc.study.common.cache.MasterDataCache;
import com.xc.study.module.admin.dto.AdminBatchBindMediaAssetDTO;
import com.xc.study.module.admin.dto.AdminBatchUpdateContentAssignmentsDTO;
import com.xc.study.module.admin.dto.AdminBatchUpdateContentStatusDTO;
import com.xc.study.module.admin.dto.AdminExerciseSetQueryDTO;
import com.xc.study.module.admin.dto.AdminSentenceExerciseQueryDTO;
import com.xc.study.module.admin.dto.AdminSentenceWordOptionDTO;
import com.xc.study.module.admin.dto.AdminUpdateContentStatusDTO;
import com.xc.study.module.admin.dto.AdminUpsertExerciseSetDTO;
import com.xc.study.module.admin.dto.AdminUpsertSentenceExerciseDTO;
import com.xc.study.module.admin.entity.AdminOperationLog;
import com.xc.study.module.admin.mapper.AdminOperationLogMapper;
import com.xc.study.module.admin.service.AdminExerciseManagementService;
import com.xc.study.module.admin.service.support.AdminSorts;
import com.xc.study.module.admin.vo.AdminBatchBindMediaAssetResultVO;
import com.xc.study.module.admin.vo.AdminBatchContentStatusResultVO;
import com.xc.study.module.admin.vo.AdminExerciseSetVO;
import com.xc.study.module.admin.vo.AdminSentenceExerciseVO;
import com.xc.study.module.admin.vo.AdminSentenceWordOptionVO;
import com.xc.study.module.exercise.entity.ExerciseSet;
import com.xc.study.module.exercise.entity.ExerciseSetItem;
import com.xc.study.module.exercise.entity.SentenceExercise;
import com.xc.study.module.exercise.entity.SentenceWordOption;
import com.xc.study.module.exercise.mapper.ExerciseSetItemMapper;
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
    private static final List<String> DEFAULT_SENTENCE_EXERCISE_TYPES = List.of(
            "audio_order",
            "translation_order",
            "audio_dictation",
            "pinyin_dictation"
    );
    private static final String DEFAULT_SENTENCE_EXERCISE_TYPE = "audio_order";
    private static final Map<String, String> DEFAULT_EXERCISE_SET_TITLES = Map.of(
            "audio_order", "听音频排序",
            "translation_order", "按拼音排序",
            "audio_dictation", "听写汉字",
            "pinyin_dictation", "拼音写句"
    );

    private final ExerciseSetMapper exerciseSetMapper;
    private final ExerciseSetItemMapper exerciseSetItemMapper;
    private final SentenceExerciseMapper sentenceExerciseMapper;
    private final SentenceWordOptionMapper sentenceWordOptionMapper;
    private final MediaAssetMapper mediaAssetMapper;
    private final AdminOperationLogMapper adminOperationLogMapper;
    private final ObjectMapper objectMapper;
    private final MasterDataCache masterDataCache;

    public AdminExerciseManagementServiceImpl(
            ExerciseSetMapper exerciseSetMapper,
            ExerciseSetItemMapper exerciseSetItemMapper,
            SentenceExerciseMapper sentenceExerciseMapper,
            SentenceWordOptionMapper sentenceWordOptionMapper,
            MediaAssetMapper mediaAssetMapper,
            AdminOperationLogMapper adminOperationLogMapper,
            ObjectMapper objectMapper,
            MasterDataCache masterDataCache
    ) {
        this.exerciseSetMapper = exerciseSetMapper;
        this.exerciseSetItemMapper = exerciseSetItemMapper;
        this.sentenceExerciseMapper = sentenceExerciseMapper;
        this.sentenceWordOptionMapper = sentenceWordOptionMapper;
        this.mediaAssetMapper = mediaAssetMapper;
        this.adminOperationLogMapper = adminOperationLogMapper;
        this.objectMapper = objectMapper;
        this.masterDataCache = masterDataCache;
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
        if (query.getParentId() != null) {
            wrapper.eq(ExerciseSet::getParentId, query.getParentId());
        } else if (Boolean.TRUE.equals(query.getRootOnly())) {
            wrapper.isNull(ExerciseSet::getParentId);
        }
        boolean sorted = AdminSorts.apply(wrapper, query.getSortBy(), query.getSortDirection(), Map.of(
                "id", ExerciseSet::getId,
                "title", ExerciseSet::getTitle,
                "exerciseType", ExerciseSet::getExerciseType,
                "level", ExerciseSet::getLevel,
                "status", ExerciseSet::getStatus,
                "createdAt", ExerciseSet::getCreatedAt,
                "updatedAt", ExerciseSet::getUpdatedAt
        ));
        if (!sorted) {
            wrapper.orderByAsc(ExerciseSet::getId);
        }
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
        evictExerciseSetCache();
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
        evictExerciseSetCache();
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
        evictExerciseSetCache();
        return toSetVO(set);
    }

    @Override
    @Transactional
    public AdminBatchContentStatusResultVO updateSetStatuses(
            AdminBatchUpdateContentStatusDTO request,
            CurrentUser admin,
            String ipAddress
    ) {
        requirePermission(admin, "admin:content:update");
        OffsetDateTime now = OffsetDateTime.now();
        List<String> errors = new ArrayList<>();
        List<Map<String, Object>> changes = new ArrayList<>();
        for (Long setId : request.ids()) {
            ExerciseSet set = exerciseSetMapper.selectById(setId);
            if (set == null) {
                errors.add("题组 ID " + setId + " 不存在");
                continue;
            }
            String beforeStatus = set.getStatus();
            set.setStatus(request.status());
            set.setUpdatedAt(now);
            exerciseSetMapper.updateById(set);
            changes.add(Map.of(
                    "id", setId,
                    "beforeStatus", beforeStatus == null ? "" : beforeStatus,
                    "afterStatus", request.status()
            ));
        }
        writeOperationLog(admin.id(), "content.exercise.set.status.batch_update", "exercise_set", null, Map.of(
                "requestedCount", request.ids().size(),
                "successCount", changes.size(),
                "afterStatus", request.status(),
                "reason", request.reason() == null ? "" : request.reason(),
                "errors", errors,
                "changes", changes
        ), ipAddress);
        evictExerciseSetCache();
        return new AdminBatchContentStatusResultVO(request.ids().size(), changes.size(), errors);
    }

    @Override
    public PageResult<AdminSentenceExerciseVO> pageSentenceExercises(AdminSentenceExerciseQueryDTO query, CurrentUser admin) {
        requirePermission(admin, "admin:content:read");
        int page = query.getPage() == null ? 1 : query.getPage();
        int pageSize = query.getPageSize() == null ? 20 : query.getPageSize();
        LambdaQueryWrapper<SentenceExercise> wrapper = new LambdaQueryWrapper<>();
        if (query.getExerciseSetId() != null) {
            List<Long> exerciseIds = assignedSentenceExerciseIds(collectSetTreeIds(query.getExerciseSetId()));
            if (exerciseIds.isEmpty()) {
                return PageResult.of(List.of(), 0, page, pageSize);
            }
            wrapper.in(SentenceExercise::getId, exerciseIds);
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
        boolean sorted = AdminSorts.apply(wrapper, query.getSortBy(), query.getSortDirection(), Map.of(
                "id", SentenceExercise::getId,
                "exerciseSetId", SentenceExercise::getExerciseSetId,
                "hanziAnswer", SentenceExercise::getHanziAnswer,
                "exerciseType", SentenceExercise::getExerciseType,
                "sortOrder", SentenceExercise::getSortOrder,
                "status", SentenceExercise::getStatus,
                "createdAt", SentenceExercise::getCreatedAt,
                "updatedAt", SentenceExercise::getUpdatedAt
        ));
        if (!sorted) {
            wrapper.orderByAsc(SentenceExercise::getExerciseSetId)
                    .orderByAsc(SentenceExercise::getSortOrder)
                    .orderByDesc(SentenceExercise::getUpdatedAt);
        }
        wrapper.orderByDesc(SentenceExercise::getId);
        Page<SentenceExercise> result = sentenceExerciseMapper.selectPage(Page.of(page, pageSize), wrapper);
        return PageResult.of(toSentenceVOs(result.getRecords()), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional
    public AdminSentenceExerciseVO createSentenceExercise(AdminUpsertSentenceExerciseDTO request, CurrentUser admin, String ipAddress) {
        requirePermission(admin, "admin:content:update");
        List<AdminSentenceWordOptionDTO> wordOptions = validateSentenceRequest(request);
        List<Long> targetSetIds = targetSetIdsOrDefaultSets(request);
        SentenceExercise existing = findDuplicateSentenceExercise(request, null);
        if (existing != null) {
            syncExerciseSetAssignments(existing, targetSetIds, request.sortOrder(), false);
            replaceWordOptions(existing.getId(), wordOptions, OffsetDateTime.now());
            writeOperationLog(admin.id(), "content.sentence.exercise.bind_existing", "sentence_exercise", existing.getId(), sentenceSnapshot(existing), ipAddress);
            evictExerciseCache();
            return toSentenceVOs(List.of(sentenceExerciseMapper.selectById(existing.getId()))).get(0);
        }
        OffsetDateTime now = OffsetDateTime.now();
        SentenceExercise exercise = new SentenceExercise();
        fillSentenceExercise(exercise, request);
        exercise.setStatus(StringUtils.hasText(request.status()) ? request.status() : "active");
        exercise.setCreatedAt(now);
        exercise.setUpdatedAt(now);
        sentenceExerciseMapper.insert(exercise);
        syncExerciseSetAssignments(exercise, targetSetIds, request.sortOrder(), true);
        replaceWordOptions(exercise.getId(), wordOptions, now);
        writeOperationLog(admin.id(), "content.sentence.exercise.create", "sentence_exercise", exercise.getId(), sentenceSnapshot(exercise), ipAddress);
        evictExerciseCache();
        return toSentenceVOs(List.of(exercise)).get(0);
    }

    @Override
    @Transactional
    public AdminSentenceExerciseVO updateSentenceExercise(Long exerciseId, AdminUpsertSentenceExerciseDTO request, CurrentUser admin, String ipAddress) {
        requirePermission(admin, "admin:content:update");
        SentenceExercise exercise = requireSentenceExercise(exerciseId);
        requireEditableSentenceExercise(exercise);
        List<AdminSentenceWordOptionDTO> wordOptions = validateSentenceRequest(request);
        List<Long> targetSetIds = normalizeTargetSetIds(request);
        Map<String, Object> before = sentenceSnapshot(exercise);
        fillSentenceExercise(exercise, request);
        if (StringUtils.hasText(request.status())) {
            exercise.setStatus(request.status());
        }
        exercise.setUpdatedAt(OffsetDateTime.now());
        sentenceExerciseMapper.updateById(exercise);
        syncExerciseSetAssignments(exercise, targetSetIds, exercise.getSortOrder(), request.exerciseSetIds() != null);
        replaceWordOptions(exerciseId, wordOptions, OffsetDateTime.now());
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("before", before);
        detail.put("after", sentenceSnapshot(exercise));
        writeOperationLog(admin.id(), "content.sentence.exercise.update", "sentence_exercise", exerciseId, detail, ipAddress);
        evictExerciseCache();
        return toSentenceVOs(List.of(exercise)).get(0);
    }

    @Override
    @Transactional
    public AdminSentenceExerciseVO updateSentenceExerciseStatus(Long exerciseId, AdminUpdateContentStatusDTO request, CurrentUser admin, String ipAddress) {
        requirePermission(admin, "admin:content:update");
        SentenceExercise exercise = requireSentenceExercise(exerciseId);
        requireEditableSentenceExercise(exercise);
        String beforeStatus = exercise.getStatus();
        exercise.setStatus(request.status());
        exercise.setUpdatedAt(OffsetDateTime.now());
        sentenceExerciseMapper.updateById(exercise);
        writeOperationLog(admin.id(), "content.sentence.exercise.status.update", "sentence_exercise", exerciseId, Map.of(
                "beforeStatus", beforeStatus,
                "afterStatus", request.status(),
                "reason", request.reason() == null ? "" : request.reason()
        ), ipAddress);
        evictExerciseCache();
        return toSentenceVOs(List.of(exercise)).get(0);
    }

    @Override
    @Transactional
    public AdminBatchContentStatusResultVO updateSentenceExerciseStatuses(
            AdminBatchUpdateContentStatusDTO request,
            CurrentUser admin,
            String ipAddress
    ) {
        requirePermission(admin, "admin:content:update");
        OffsetDateTime now = OffsetDateTime.now();
        List<String> errors = new ArrayList<>();
        List<Map<String, Object>> changes = new ArrayList<>();
        for (Long exerciseId : request.ids()) {
            SentenceExercise exercise = sentenceExerciseMapper.selectById(exerciseId);
            if (exercise == null) {
                errors.add("句子题 ID " + exerciseId + " 不存在");
                continue;
            }
            if (!isEditableSentenceExercise(exercise)) {
                errors.add("句子题 ID " + exerciseId + " 所属题组已停用，不能操作");
                continue;
            }
            String beforeStatus = exercise.getStatus();
            exercise.setStatus(request.status());
            exercise.setUpdatedAt(now);
            sentenceExerciseMapper.updateById(exercise);
            changes.add(Map.of(
                    "id", exerciseId,
                    "beforeStatus", beforeStatus == null ? "" : beforeStatus,
                    "afterStatus", request.status()
            ));
        }
        writeOperationLog(admin.id(), "content.sentence.exercise.status.batch_update", "sentence_exercise", null, Map.of(
                "requestedCount", request.ids().size(),
                "successCount", changes.size(),
                "afterStatus", request.status(),
                "reason", request.reason() == null ? "" : request.reason(),
                "errors", errors,
                "changes", changes
        ), ipAddress);
        evictExerciseCache();
        return new AdminBatchContentStatusResultVO(request.ids().size(), changes.size(), errors);
    }

    @Override
    @Transactional
    public AdminBatchContentStatusResultVO updateSentenceExerciseSetAssignments(
            AdminBatchUpdateContentAssignmentsDTO request,
            CurrentUser admin,
            String ipAddress
    ) {
        requirePermission(admin, "admin:content:update");
        List<Long> targetSetIds = request.targetIds().stream().filter(Objects::nonNull).distinct().toList();
        targetSetIds.forEach(this::requireSet);
        List<String> errors = new ArrayList<>();
        List<Map<String, Object>> changes = new ArrayList<>();
        for (Long exerciseId : request.ids()) {
            SentenceExercise exercise = sentenceExerciseMapper.selectById(exerciseId);
            if (exercise == null) {
                errors.add("句子题 ID " + exerciseId + " 不存在");
                continue;
            }
            if (!isEditableSentenceExercise(exercise)) {
                errors.add("句子题 ID " + exerciseId + " 所属题组已停用，不能操作");
                continue;
            }
            Map<String, Object> before = sentenceSnapshot(exercise);
            syncExerciseSetAssignments(exercise, targetSetIds, exercise.getSortOrder(), true);
            SentenceExercise updated = sentenceExerciseMapper.selectById(exerciseId);
            changes.add(Map.of(
                    "id", exerciseId,
                    "beforeExerciseSetIds", before.get("exerciseSetIds"),
                    "afterExerciseSetIds", sentenceSnapshot(updated).get("exerciseSetIds")
            ));
        }
        writeOperationLog(admin.id(), "content.sentence.exercise.set.batch_update", "sentence_exercise", null, Map.of(
                "requestedCount", request.ids().size(),
                "successCount", changes.size(),
                "targetSetIds", targetSetIds,
                "errors", errors,
                "changes", changes
        ), ipAddress);
        evictExerciseCache();
        return new AdminBatchContentStatusResultVO(request.ids().size(), changes.size(), errors);
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
            if (!isEditableSentenceExercise(exercise)) {
                errors.add("句子题 ID " + exerciseId + " 所属题组已停用，不能操作");
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
        evictExerciseCache();
        return new AdminBatchBindMediaAssetResultVO(request.bindings().size(), successfulBindings.size(), errors);
    }

    private void fillSet(ExerciseSet set, AdminUpsertExerciseSetDTO request) {
        validateParentSet(set.getId(), request.parentId(), request.exerciseType());
        set.setTitle(request.title().trim());
        set.setParentId(request.parentId());
        set.setExerciseType(request.exerciseType());
        set.setLevel(blankToNull(request.level()));
    }

    private void fillSentenceExercise(SentenceExercise exercise, AdminUpsertSentenceExerciseDTO request) {
        List<Long> targetSetIds = normalizeTargetSetIds(request);
        if (exercise.getId() == null || request.exerciseSetId() != null || request.exerciseSetIds() != null || !targetSetIds.isEmpty()) {
            exercise.setExerciseSetId(targetSetIds.isEmpty() ? null : targetSetIds.get(0));
        }
        exercise.setExerciseType(sentenceExerciseType(request));
        exercise.setHanziAnswer(request.hanziAnswer().trim());
        exercise.setPinyinPrompt(blankToNull(request.pinyinPrompt()));
        exercise.setTranslationEn(blankToNull(request.translationEn()));
        exercise.setTranslationRu(blankToNull(request.translationRu()));
        exercise.setAudioZhAssetId(request.audioZhAssetId());
        exercise.setExplanation(blankToNull(request.explanation()));
        exercise.setSortOrder(request.sortOrder());
    }

    private List<AdminSentenceWordOptionDTO> validateSentenceRequest(AdminUpsertSentenceExerciseDTO request) {
        for (Long setId : normalizeTargetSetIds(request)) {
            requireSet(setId);
        }
        if (request.audioZhAssetId() != null) {
            MediaAsset asset = mediaAssetMapper.selectById(request.audioZhAssetId());
            if (asset == null || !"audio".equals(asset.getMediaType()) || !"active".equals(asset.getStatus())) {
                throw BusinessException.notFound("音频资源不存在或已停用");
            }
        }
        List<AdminSentenceWordOptionDTO> options = normalizedWordOptions(request);
        if (ORDER_EXERCISE_TYPES.contains(sentenceExerciseType(request)) && options.isEmpty()) {
            throw new BusinessException("排序题必须维护词块选项");
        }
        Set<Integer> orders = new HashSet<>();
        for (AdminSentenceWordOptionDTO option : options) {
            if (!orders.add(option.correctOrder())) {
                throw new BusinessException("词块顺序不能重复");
            }
        }
        return options;
    }

    private List<AdminSentenceWordOptionDTO> normalizedWordOptions(AdminUpsertSentenceExerciseDTO request) {
        List<AdminSentenceWordOptionDTO> options = request.wordOptions() == null ? List.of() : request.wordOptions().stream()
                .filter(option -> option != null && StringUtils.hasText(option.wordText()))
                .toList();
        if (!ORDER_EXERCISE_TYPES.contains(sentenceExerciseType(request)) || !options.isEmpty()) {
            return options;
        }
        List<String> generatedWords = splitSentenceIntoOptions(request.hanziAnswer());
        List<AdminSentenceWordOptionDTO> generatedOptions = new ArrayList<>();
        for (int i = 0; i < generatedWords.size(); i++) {
            generatedOptions.add(new AdminSentenceWordOptionDTO(generatedWords.get(i), i + 1));
        }
        return generatedOptions;
    }

    private List<String> splitSentenceIntoOptions(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        String trimmed = value.trim();
        if (trimmed.contains("|") || trimmed.matches(".*\\s+.*")) {
            return List.of(trimmed.split("[|\\s]+")).stream()
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .limit(100)
                    .toList();
        }
        List<String> words = new ArrayList<>();
        trimmed.codePoints()
                .mapToObj(Character::toString)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .filter(token -> !"，。！？；：、,.!?;:".contains(token))
                .limit(100)
                .forEach(words::add);
        return words;
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
                set.getParentId(),
                parentSetTitle(set.getParentId()),
                set.getExerciseType(),
                set.getLevel(),
                set.getStatus(),
                countChildren(set.getId()),
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
        List<Long> exerciseIds = exercises.stream().map(SentenceExercise::getId).filter(Objects::nonNull).toList();
        Map<Long, List<ExerciseSetItem>> linksByExerciseId = loadSetLinksByExerciseId(exerciseIds);
        List<Long> setIds = new ArrayList<>();
        for (SentenceExercise exercise : exercises) {
            if (exercise.getExerciseSetId() != null) {
                setIds.add(exercise.getExerciseSetId());
            }
            linksByExerciseId.getOrDefault(exercise.getId(), List.of()).stream()
                    .map(ExerciseSetItem::getExerciseSetId)
                    .forEach(setIds::add);
        }
        Map<Long, ExerciseSet> sets = loadSets(setIds);
        Map<Long, List<SentenceWordOption>> optionsByExerciseId = loadOptionsByExerciseId(exercises.stream()
                .map(SentenceExercise::getId)
                .toList());
        Map<Long, MediaAsset> audioAssets = loadAudioAssets(exercises.stream()
                .map(SentenceExercise::getAudioZhAssetId)
                .toList());
        return exercises.stream()
                .map(exercise -> {
                    List<Long> linkedSetIds = linkedSetIds(exercise, linksByExerciseId.getOrDefault(exercise.getId(), List.of()));
                    Long primarySetId = primarySetId(exercise, linkedSetIds);
                    ExerciseSet set = sets.get(primarySetId);
                    MediaAsset asset = audioAssets.get(exercise.getAudioZhAssetId());
                    return new AdminSentenceExerciseVO(
                            exercise.getId(),
                            primarySetId,
                            set == null ? null : set.getTitle(),
                            set == null ? null : set.getStatus(),
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
                            toOptionVOs(optionsByExerciseId.getOrDefault(exercise.getId(), List.of())),
                            linkedSetIds,
                            linkedSetIds.stream()
                                    .map(sets::get)
                                    .filter(Objects::nonNull)
                                    .map(ExerciseSet::getTitle)
                                    .toList()
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

    private Map<Long, List<ExerciseSetItem>> loadSetLinksByExerciseId(List<Long> exerciseIds) {
        List<Long> ids = exerciseIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return exerciseSetItemMapper.selectList(new LambdaQueryWrapper<ExerciseSetItem>()
                        .in(ExerciseSetItem::getSentenceExerciseId, ids)
                        .orderByAsc(ExerciseSetItem::getSortOrder)
                        .orderByAsc(ExerciseSetItem::getId))
                .stream()
                .collect(Collectors.groupingBy(ExerciseSetItem::getSentenceExerciseId));
    }

    private List<Long> linkedSetIds(SentenceExercise exercise, List<ExerciseSetItem> links) {
        List<Long> ids = new ArrayList<>();
        if (exercise.getExerciseSetId() != null) {
            ids.add(exercise.getExerciseSetId());
        }
        links.stream()
                .map(ExerciseSetItem::getExerciseSetId)
                .filter(Objects::nonNull)
                .forEach(ids::add);
        return ids.stream().distinct().toList();
    }

    private Long primarySetId(SentenceExercise exercise, List<Long> linkedSetIds) {
        if (exercise.getExerciseSetId() != null) {
            return exercise.getExerciseSetId();
        }
        return linkedSetIds.isEmpty() ? null : linkedSetIds.get(0);
    }

    private List<Long> normalizeTargetSetIds(AdminUpsertSentenceExerciseDTO request) {
        List<Long> ids = new ArrayList<>();
        if (request.exerciseSetId() != null) {
            ids.add(request.exerciseSetId());
        }
        if (request.exerciseSetIds() != null) {
            request.exerciseSetIds().stream()
                    .filter(Objects::nonNull)
                    .forEach(ids::add);
        }
        return ids.stream().distinct().toList();
    }

    private List<Long> targetSetIdsOrDefaultSets(AdminUpsertSentenceExerciseDTO request) {
        List<Long> targetSetIds = normalizeTargetSetIds(request);
        if (!targetSetIds.isEmpty()) {
            return targetSetIds;
        }
        return defaultExerciseSetIds();
    }

    private String sentenceExerciseType(AdminUpsertSentenceExerciseDTO request) {
        return StringUtils.hasText(request.exerciseType()) ? request.exerciseType() : DEFAULT_SENTENCE_EXERCISE_TYPE;
    }

    private List<Long> defaultExerciseSetIds() {
        return DEFAULT_SENTENCE_EXERCISE_TYPES.stream()
                .map(this::ensureDefaultExerciseSet)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private Long ensureDefaultExerciseSet(String exerciseType) {
        String title = DEFAULT_EXERCISE_SET_TITLES.get(exerciseType);
        if (!StringUtils.hasText(title)) {
            return null;
        }
        ExerciseSet existing = exerciseSetMapper.selectOne(new LambdaQueryWrapper<ExerciseSet>()
                .isNull(ExerciseSet::getParentId)
                .eq(ExerciseSet::getExerciseType, exerciseType)
                .eq(ExerciseSet::getTitle, title)
                .last("limit 1"));
        OffsetDateTime now = OffsetDateTime.now();
        if (existing != null) {
            if (!"active".equals(existing.getStatus())) {
                existing.setStatus("active");
                existing.setUpdatedAt(now);
                exerciseSetMapper.updateById(existing);
            }
            return existing.getId();
        }
        ExerciseSet set = new ExerciseSet();
        set.setTitle(title);
        set.setExerciseType(exerciseType);
        set.setStatus("active");
        set.setCreatedAt(now);
        set.setUpdatedAt(now);
        exerciseSetMapper.insert(set);
        return set.getId();
    }

    private void syncExerciseSetAssignments(
            SentenceExercise exercise,
            List<Long> targetSetIds,
            Integer sortOrder,
            boolean replace
    ) {
        if (exercise.getId() == null) {
            return;
        }
        if (replace) {
            if (targetSetIds.isEmpty()) {
                exerciseSetItemMapper.delete(new LambdaQueryWrapper<ExerciseSetItem>()
                        .eq(ExerciseSetItem::getSentenceExerciseId, exercise.getId()));
            } else {
                exerciseSetItemMapper.delete(new LambdaQueryWrapper<ExerciseSetItem>()
                        .eq(ExerciseSetItem::getSentenceExerciseId, exercise.getId())
                        .notIn(ExerciseSetItem::getExerciseSetId, targetSetIds));
            }
        }
        for (Long setId : targetSetIds) {
            ExerciseSetItem link = exerciseSetItemMapper.selectOne(new LambdaQueryWrapper<ExerciseSetItem>()
                    .eq(ExerciseSetItem::getExerciseSetId, setId)
                    .eq(ExerciseSetItem::getSentenceExerciseId, exercise.getId())
                    .last("limit 1"));
            if (link == null) {
                link = new ExerciseSetItem();
                link.setExerciseSetId(setId);
                link.setSentenceExerciseId(exercise.getId());
                link.setSortOrder(sortOrder == null ? 0 : sortOrder);
                link.setStatus("active");
                exerciseSetItemMapper.insert(link);
            } else {
                link.setSortOrder(sortOrder == null ? link.getSortOrder() : sortOrder);
                link.setStatus("active");
                link.setUpdatedAt(OffsetDateTime.now());
                exerciseSetItemMapper.updateById(link);
            }
        }
        if (replace || (exercise.getExerciseSetId() == null && !targetSetIds.isEmpty())) {
            exercise.setExerciseSetId(targetSetIds.isEmpty() ? null : targetSetIds.get(0));
            exercise.setUpdatedAt(OffsetDateTime.now());
            sentenceExerciseMapper.updateById(exercise);
        }
    }

    private List<Long> assignedSentenceExerciseIds(List<Long> setIds) {
        List<Long> ids = setIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return exerciseSetItemMapper.selectList(new LambdaQueryWrapper<ExerciseSetItem>()
                        .in(ExerciseSetItem::getExerciseSetId, ids)
                        .eq(ExerciseSetItem::getStatus, "active")
                        .orderByAsc(ExerciseSetItem::getSortOrder)
                        .orderByAsc(ExerciseSetItem::getId))
                .stream()
                .map(ExerciseSetItem::getSentenceExerciseId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private SentenceExercise findDuplicateSentenceExercise(AdminUpsertSentenceExerciseDTO request, Long excludeExerciseId) {
        LambdaQueryWrapper<SentenceExercise> wrapper = new LambdaQueryWrapper<SentenceExercise>()
                .eq(SentenceExercise::getHanziAnswer, request.hanziAnswer().trim());
        if (StringUtils.hasText(request.pinyinPrompt())) {
            wrapper.eq(SentenceExercise::getPinyinPrompt, request.pinyinPrompt().trim());
        } else {
            wrapper.isNull(SentenceExercise::getPinyinPrompt);
        }
        if (excludeExerciseId != null) {
            wrapper.ne(SentenceExercise::getId, excludeExerciseId);
        }
        return sentenceExerciseMapper.selectList(wrapper.orderByAsc(SentenceExercise::getId))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private long countExercises(Long setId, String status) {
        List<Long> exerciseIds = assignedSentenceExerciseIds(List.of(setId));
        if (exerciseIds.isEmpty()) {
            return 0;
        }
        LambdaQueryWrapper<SentenceExercise> wrapper = new LambdaQueryWrapper<SentenceExercise>()
                .in(SentenceExercise::getId, exerciseIds);
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

    private ExerciseSet requireActiveSet(Long setId) {
        if (setId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "题组不能为空");
        }
        ExerciseSet set = requireSet(setId);
        if (!isActiveSetHierarchy(set)) {
            throw new BusinessException(ErrorCode.CONFLICT, "所属题组已停用，句子题只能查看，不能操作");
        }
        return set;
    }

    private boolean isActiveSet(Long setId) {
        if (setId == null) {
            return true;
        }
        ExerciseSet set = exerciseSetMapper.selectById(setId);
        return set != null && isActiveSetHierarchy(set);
    }

    private void requireEditableSentenceExercise(SentenceExercise exercise) {
        if (!isEditableSentenceExercise(exercise)) {
            throw new BusinessException(ErrorCode.CONFLICT, "所属题组已停用，句子题只能查看，不能操作");
        }
    }

    private boolean isEditableSentenceExercise(SentenceExercise exercise) {
        return exercise != null;
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
        snapshot.put("parentId", set.getParentId());
        snapshot.put("exerciseType", set.getExerciseType());
        snapshot.put("level", set.getLevel());
        snapshot.put("status", set.getStatus());
        return snapshot;
    }

    private void validateParentSet(Long setId, Long parentId, String exerciseType) {
        if (parentId == null) {
            return;
        }
        if (Objects.equals(setId, parentId)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "上级题组不能选择自己");
        }
        ExerciseSet parent = requireSet(parentId);
        if (!Objects.equals(parent.getExerciseType(), exerciseType)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "子题组必须和上级题组题型一致");
        }
        Set<Long> visited = new HashSet<>();
        Long currentParentId = parent.getParentId();
        while (currentParentId != null) {
            if (Objects.equals(currentParentId, setId)) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "上级题组不能选择自己的下级");
            }
            if (!visited.add(currentParentId)) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "题组层级存在循环引用");
            }
            ExerciseSet current = exerciseSetMapper.selectById(currentParentId);
            currentParentId = current == null ? null : current.getParentId();
        }
    }

    private String parentSetTitle(Long parentId) {
        if (parentId == null) {
            return null;
        }
        ExerciseSet parent = exerciseSetMapper.selectById(parentId);
        return parent == null ? null : parent.getTitle();
    }

    private List<Long> collectSetTreeIds(Long rootId) {
        if (rootId == null) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        List<Long> currentLevel = List.of(rootId);
        while (!currentLevel.isEmpty()) {
            List<Long> currentIds = currentLevel.stream()
                    .filter(visited::add)
                    .toList();
            if (currentIds.isEmpty()) {
                break;
            }
            ids.addAll(currentIds);
            currentLevel = exerciseSetMapper.selectList(new LambdaQueryWrapper<ExerciseSet>()
                            .in(ExerciseSet::getParentId, currentIds))
                    .stream()
                    .map(ExerciseSet::getId)
                    .toList();
        }
        return ids;
    }

    private long countChildren(Long setId) {
        return exerciseSetMapper.selectCount(new LambdaQueryWrapper<ExerciseSet>()
                .eq(ExerciseSet::getParentId, setId));
    }

    private boolean isActiveSetHierarchy(ExerciseSet set) {
        if (set == null || !"active".equals(set.getStatus())) {
            return false;
        }
        Set<Long> visited = new HashSet<>();
        ExerciseSet current = set;
        while (current.getParentId() != null) {
            if (!visited.add(current.getId())) {
                return false;
            }
            ExerciseSet parent = exerciseSetMapper.selectById(current.getParentId());
            if (parent == null || !"active".equals(parent.getStatus())) {
                return false;
            }
            current = parent;
        }
        return true;
    }

    private Map<String, Object> sentenceSnapshot(SentenceExercise exercise) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("exerciseSetId", exercise.getExerciseSetId());
        snapshot.put("exerciseSetIds", linkedSetIds(
                exercise,
                exercise.getId() == null ? List.of() : loadSetLinksByExerciseId(List.of(exercise.getId())).getOrDefault(exercise.getId(), List.of())
        ));
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

    private void evictExerciseSetCache() {
        masterDataCache.evictByPrefixesAfterCommit("exercise:sets:", "exercise:questions:");
    }

    private void evictExerciseAnswerCache() {
        masterDataCache.evictByPrefixesAfterCommit("exercise:answers:");
    }

    private void evictExerciseQuestionCache() {
        masterDataCache.evictByPrefixesAfterCommit("exercise:questions:");
    }

    private void evictExerciseContentCache() {
        evictExerciseQuestionCache();
        evictExerciseAnswerCache();
    }

    private void evictExerciseCache() {
        evictExerciseSetCache();
        evictExerciseAnswerCache();
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
