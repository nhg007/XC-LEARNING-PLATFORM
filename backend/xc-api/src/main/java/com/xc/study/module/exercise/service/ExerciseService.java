package com.xc.study.module.exercise.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.xc.study.common.BusinessException;
import com.xc.study.common.PageParams;
import com.xc.study.common.PageResult;
import com.xc.study.common.cache.MasterDataCache;
import com.xc.study.module.exercise.dto.CheckExerciseRequest;
import com.xc.study.module.exercise.entity.ExerciseAttempt;
import com.xc.study.module.exercise.entity.ExerciseSet;
import com.xc.study.module.exercise.entity.ExerciseSetItem;
import com.xc.study.module.exercise.entity.SentenceExercise;
import com.xc.study.module.exercise.entity.SentenceWordOption;
import com.xc.study.module.exercise.entity.UserSentenceFavorite;
import com.xc.study.module.exercise.entity.UserSentenceProgress;
import com.xc.study.module.exercise.mapper.ExerciseAttemptMapper;
import com.xc.study.module.exercise.mapper.ExerciseSetItemMapper;
import com.xc.study.module.exercise.mapper.ExerciseSetMapper;
import com.xc.study.module.exercise.mapper.SentenceExerciseMapper;
import com.xc.study.module.exercise.mapper.SentenceWordOptionMapper;
import com.xc.study.module.exercise.mapper.UserSentenceFavoriteMapper;
import com.xc.study.module.exercise.mapper.UserSentenceProgressMapper;
import com.xc.study.module.exercise.vo.ExerciseAnswerVO;
import com.xc.study.module.exercise.vo.ExerciseAttemptVO;
import com.xc.study.module.exercise.vo.ExerciseCheckResultVO;
import com.xc.study.module.exercise.vo.ExerciseSetVO;
import com.xc.study.module.exercise.vo.FavoriteSentenceExerciseVO;
import com.xc.study.module.exercise.vo.SentenceFavoriteStatusVO;
import com.xc.study.module.exercise.vo.SentenceExerciseVO;
import com.xc.study.module.exercise.vo.SentenceWordOptionVO;
import com.xc.study.module.media.entity.MediaAsset;
import com.xc.study.module.media.mapper.MediaAssetMapper;
import com.xc.study.module.stats.service.LearningStatsRecorder;
import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ExerciseService {

    private static final String SENTENCE_STATUS_LEARNING = "learning";
    private static final String SENTENCE_STATUS_LEARNED = "learned";
    private static final String SENTENCE_STATUS_REVIEWING = "reviewing";
    private static final String SENTENCE_STATUS_MASTERED = "mastered";
    private static final String DEFAULT_EXERCISE_TYPE = "audio_order";
    private static final List<String> DEFAULT_EXERCISE_TYPES = List.of(
            "audio_order",
            "translation_order",
            "audio_dictation",
            "pinyin_dictation"
    );

    private static final TypeReference<PageResult<ExerciseSetVO>> EXERCISE_SET_PAGE_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<PageResult<SentenceExerciseVO>> SENTENCE_EXERCISE_PAGE_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<ExerciseAnswerVO> EXERCISE_ANSWER_TYPE = new TypeReference<>() {
    };

    private record SentenceExerciseContext(SentenceExercise question, Long exerciseSetId, Integer sortOrder) {
    }

    private final ExerciseSetMapper exerciseSetMapper;
    private final ExerciseSetItemMapper exerciseSetItemMapper;
    private final SentenceExerciseMapper sentenceExerciseMapper;
    private final SentenceWordOptionMapper sentenceWordOptionMapper;
    private final ExerciseAttemptMapper exerciseAttemptMapper;
    private final UserSentenceFavoriteMapper userSentenceFavoriteMapper;
    private final UserSentenceProgressMapper userSentenceProgressMapper;
    private final MediaAssetMapper mediaAssetMapper;
    private final LearningStatsRecorder learningStatsRecorder;
    private final MasterDataCache masterDataCache;

    public ExerciseService(
            ExerciseSetMapper exerciseSetMapper,
            ExerciseSetItemMapper exerciseSetItemMapper,
            SentenceExerciseMapper sentenceExerciseMapper,
            SentenceWordOptionMapper sentenceWordOptionMapper,
            ExerciseAttemptMapper exerciseAttemptMapper,
            UserSentenceFavoriteMapper userSentenceFavoriteMapper,
            UserSentenceProgressMapper userSentenceProgressMapper,
            MediaAssetMapper mediaAssetMapper,
            LearningStatsRecorder learningStatsRecorder,
            MasterDataCache masterDataCache
    ) {
        this.exerciseSetMapper = exerciseSetMapper;
        this.exerciseSetItemMapper = exerciseSetItemMapper;
        this.sentenceExerciseMapper = sentenceExerciseMapper;
        this.sentenceWordOptionMapper = sentenceWordOptionMapper;
        this.exerciseAttemptMapper = exerciseAttemptMapper;
        this.userSentenceFavoriteMapper = userSentenceFavoriteMapper;
        this.userSentenceProgressMapper = userSentenceProgressMapper;
        this.mediaAssetMapper = mediaAssetMapper;
        this.learningStatsRecorder = learningStatsRecorder;
        this.masterDataCache = masterDataCache;
    }

    public PageResult<ExerciseSetVO> listSets(long page, long pageSize, String exerciseType, String level, Long parentId) {
        PageParams params = PageParams.normalize(page, pageSize);
        return masterDataCache.get(
                setCacheKey(params.page(), params.pageSize(), exerciseType, level, parentId),
                EXERCISE_SET_PAGE_TYPE,
                () -> loadSets(params.page(), params.pageSize(), exerciseType, level, parentId)
        );
    }

    private PageResult<ExerciseSetVO> loadSets(long page, long pageSize, String exerciseType, String level, Long parentId) {
        if (parentId != null) {
            ensureActiveSetHierarchy(parentId);
        }
        Page<ExerciseSet> result = exerciseSetMapper.selectPage(Page.of(page, pageSize), new LambdaQueryWrapper<ExerciseSet>()
                .eq(ExerciseSet::getStatus, "active")
                .isNull(parentId == null, ExerciseSet::getParentId)
                .eq(parentId != null, ExerciseSet::getParentId, parentId)
                .eq(exerciseType != null && !exerciseType.isBlank(), ExerciseSet::getExerciseType, exerciseType)
                .eq(level != null && !level.isBlank(), ExerciseSet::getLevel, level)
                .orderByAsc(ExerciseSet::getId));
        return PageResult.of(result.getRecords().stream().map(this::toSetVO).toList(), result.getTotal(), page, pageSize);
    }

    public PageResult<SentenceExerciseVO> listQuestions(Long userId, Long setId, long page, long pageSize, String exerciseType) {
        String mode = validExerciseType(exerciseType) ? exerciseType : DEFAULT_EXERCISE_TYPE;
        PageParams params = PageParams.normalize(page, pageSize);
        PageResult<SentenceExerciseVO> result = masterDataCache.get(
                questionCacheKey(setId, params.page(), params.pageSize(), mode),
                SENTENCE_EXERCISE_PAGE_TYPE,
                () -> loadQuestions(setId, params.page(), params.pageSize(), mode)
        );
        return withShuffledOptions(withUserFavorites(userId, withUserProgress(userId, result)));
    }

    private PageResult<SentenceExerciseVO> loadQuestions(Long setId, long page, long pageSize, String exerciseType) {
        List<ExerciseSet> scopeSets = resolveActiveSetScope(setId);
        List<Long> scopeIds = scopeSets.stream().map(ExerciseSet::getId).toList();
        if (scopeIds.isEmpty()) {
            return PageResult.of(List.of(), 0, page, pageSize);
        }
        Map<Long, Integer> setOrder = new HashMap<>();
        for (int i = 0; i < scopeIds.size(); i++) {
            setOrder.put(scopeIds.get(i), i);
        }
        List<ExerciseSetItem> assignments = exerciseSetItemMapper.selectList(new LambdaQueryWrapper<ExerciseSetItem>()
                .in(ExerciseSetItem::getExerciseSetId, scopeIds)
                .eq(ExerciseSetItem::getStatus, "active")
                .orderByAsc(ExerciseSetItem::getSortOrder)
                .orderByAsc(ExerciseSetItem::getId));
        List<ExerciseSetItem> orderedAssignments = assignments.stream()
                .sorted(Comparator
                        .comparing((ExerciseSetItem link) -> setOrder.getOrDefault(link.getExerciseSetId(), Integer.MAX_VALUE))
                        .thenComparing(link -> link.getSortOrder() == null ? Integer.MAX_VALUE : link.getSortOrder())
                        .thenComparing(ExerciseSetItem::getId))
                .toList();
        List<Long> questionIds = orderedAssignments.stream()
                .map(ExerciseSetItem::getSentenceExerciseId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (questionIds.isEmpty()) {
            return PageResult.of(List.of(), 0, page, pageSize);
        }
        Map<Long, SentenceExercise> questionById = sentenceExerciseMapper.selectBatchIds(questionIds)
                .stream()
                .filter(question -> "active".equals(question.getStatus()))
                .collect(Collectors.toMap(SentenceExercise::getId, Function.identity()));
        Set<Long> seenQuestionIds = new HashSet<>();
        List<SentenceExerciseContext> allQuestions = orderedAssignments.stream()
                .filter(link -> seenQuestionIds.add(link.getSentenceExerciseId()))
                .map(link -> new SentenceExerciseContext(
                        questionById.get(link.getSentenceExerciseId()),
                        link.getExerciseSetId(),
                        link.getSortOrder()
                ))
                .filter(context -> context.question() != null)
                .sorted(Comparator
                        .comparing((SentenceExerciseContext context) -> setOrder.getOrDefault(context.exerciseSetId(), Integer.MAX_VALUE))
                        .thenComparing(context -> context.sortOrder() == null ? Integer.MAX_VALUE : context.sortOrder())
                        .thenComparing(context -> context.question().getId()))
                .toList();
        List<SentenceExerciseContext> pageQuestions = sliceQuestionContexts(allQuestions, page, pageSize);
        Map<Long, List<SentenceWordOption>> optionsByExerciseId = loadOptionsByExerciseId(pageQuestions.stream()
                .map(context -> context.question().getId())
                .toList());
        Map<Long, MediaAsset> audioAssets = loadMediaAssets(pageQuestions.stream()
                .map(context -> context.question().getAudioZhAssetId())
                .toList());
        return PageResult.of(pageQuestions.stream()
                .map(context -> toQuestionVO(
                        context.question(),
                        context.exerciseSetId(),
                        exerciseType,
                        context.sortOrder(),
                        optionsByExerciseId.getOrDefault(context.question().getId(), List.of()),
                        audioAssets.get(context.question().getAudioZhAssetId()),
                        false
                ))
                .toList(), allQuestions.size(), page, pageSize);
    }

    @Transactional
    public ExerciseCheckResultVO checkAnswer(Long userId, Long exerciseId, CheckExerciseRequest request) {
        SentenceExercise exercise = sentenceExerciseMapper.selectById(exerciseId);
        if (exercise == null || !"active".equals(exercise.getStatus())) {
            throw BusinessException.notFound("题目不存在");
        }
        String submittedAnswer = resolveSubmittedAnswer(request);
        if (!StringUtils.hasText(submittedAnswer)) {
            throw new BusinessException("答案不能为空");
        }
        String standardAnswer = exercise.getHanziAnswer();
        String normalizedSubmitted = normalizeAnswer(submittedAnswer);
        String normalizedStandard = normalizeAnswer(standardAnswer);
        boolean correct = normalizedSubmitted.equals(normalizedStandard);
        Integer firstMismatchIndex = correct ? null : firstMismatchIndex(normalizedSubmitted, normalizedStandard);

        ExerciseAttempt attempt = new ExerciseAttempt();
        attempt.setUserId(userId);
        attempt.setExerciseId(exercise.getId());
        attempt.setExerciseType(validExerciseType(request.exerciseType()) ? request.exerciseType() : DEFAULT_EXERCISE_TYPE);
        attempt.setAnswerText(submittedAnswer);
        attempt.setIsCorrect(correct);
        attempt.setShowedAnswer(Boolean.TRUE.equals(request.showedAnswer()));
        attempt.setTranslationLanguage(request.translationLanguage());
        attempt.setAnsweredAt(OffsetDateTime.now());
        exerciseAttemptMapper.insert(attempt);

        UserSentenceProgress progress = updateSentenceProgress(userId, exercise, correct, attempt.getAnsweredAt());
        recordStudyEvent(userId, exercise.getId(), correct, request.durationSeconds());
        return new ExerciseCheckResultVO(
                attempt.getId(),
                exercise.getId(),
                correct,
                submittedAnswer,
                standardAnswer,
                firstMismatchIndex,
                correct ? "回答正确" : "回答有误，请继续修改",
                progress.getStatus(),
                progress.getAttemptCount(),
                progress.getCorrectCount(),
                progress.getLearnedAt(),
                progress.getLastPracticedAt(),
                progress.getLastCorrectAt(),
                progress.getNextReviewAt()
        );
    }

    public ExerciseAnswerVO getAnswer(Long exerciseId) {
        return masterDataCache.get(
                answerCacheKey(exerciseId),
                EXERCISE_ANSWER_TYPE,
                () -> loadAnswer(exerciseId)
        );
    }

    private ExerciseAnswerVO loadAnswer(Long exerciseId) {
        SentenceExercise exercise = sentenceExerciseMapper.selectById(exerciseId);
        if (exercise == null || !"active".equals(exercise.getStatus())) {
            throw BusinessException.notFound("题目不存在");
        }
        return new ExerciseAnswerVO(
                exercise.getId(),
                exercise.getHanziAnswer(),
                exercise.getPinyinPrompt(),
                exercise.getExplanation(),
                exercise.getTranslationEn(),
                exercise.getTranslationRu(),
                audioUrl(exercise.getAudioZhAssetId())
        );
    }

    public PageResult<ExerciseAttemptVO> listAttempts(Long userId, long page, long pageSize) {
        Page<ExerciseAttempt> result = exerciseAttemptMapper.selectPage(Page.of(page, pageSize), new LambdaQueryWrapper<ExerciseAttempt>()
                .eq(ExerciseAttempt::getUserId, userId)
                .orderByDesc(ExerciseAttempt::getAnsweredAt)
                .orderByDesc(ExerciseAttempt::getId));
        return PageResult.of(result.getRecords().stream().map(this::toAttemptVO).toList(), result.getTotal(), page, pageSize);
    }

    @Transactional
    public SentenceFavoriteStatusVO favorite(Long userId, Long exerciseId) {
        requireActiveQuestion(exerciseId);
        UserSentenceFavorite favorite = userSentenceFavoriteMapper.selectOne(new LambdaQueryWrapper<UserSentenceFavorite>()
                .eq(UserSentenceFavorite::getUserId, userId)
                .eq(UserSentenceFavorite::getSentenceExerciseId, exerciseId)
                .last("limit 1"));
        if (favorite == null) {
            favorite = new UserSentenceFavorite();
            favorite.setUserId(userId);
            favorite.setSentenceExerciseId(exerciseId);
            userSentenceFavoriteMapper.insert(favorite);
        }
        return new SentenceFavoriteStatusVO(exerciseId, true);
    }

    @Transactional
    public SentenceFavoriteStatusVO unfavorite(Long userId, Long exerciseId) {
        UserSentenceFavorite favorite = userSentenceFavoriteMapper.selectOne(new LambdaQueryWrapper<UserSentenceFavorite>()
                .eq(UserSentenceFavorite::getUserId, userId)
                .eq(UserSentenceFavorite::getSentenceExerciseId, exerciseId)
                .last("limit 1"));
        if (favorite != null) {
            userSentenceFavoriteMapper.deleteById(favorite.getId());
        }
        return new SentenceFavoriteStatusVO(exerciseId, false);
    }

    public PageResult<FavoriteSentenceExerciseVO> listFavorites(Long userId, long page, long pageSize) {
        PageParams params = PageParams.normalize(page, pageSize);
        List<UserSentenceFavorite> favorites = userSentenceFavoriteMapper.selectList(new LambdaQueryWrapper<UserSentenceFavorite>()
                .eq(UserSentenceFavorite::getUserId, userId)
                .orderByDesc(UserSentenceFavorite::getCreatedAt)
                .orderByDesc(UserSentenceFavorite::getId));
        List<Long> questionIds = favorites.stream().map(UserSentenceFavorite::getSentenceExerciseId).toList();
        if (questionIds.isEmpty()) {
            return PageResult.of(List.of(), 0, params.page(), params.pageSize());
        }
        Map<Long, SentenceExercise> questionById = sentenceExerciseMapper.selectBatchIds(questionIds)
                .stream()
                .collect(Collectors.toMap(SentenceExercise::getId, Function.identity()));
        Map<Long, MediaAsset> audioAssets = loadMediaAssets(questionById.values().stream()
                .map(SentenceExercise::getAudioZhAssetId)
                .toList());
        Map<Long, UserSentenceProgress> progressByExerciseId = sentenceProgressByExerciseId(userId, questionIds);
        Map<Long, ExerciseSet> setById = loadSetsById(questionById.values().stream()
                .map(this::primarySetId)
                .toList());
        List<FavoriteSentenceExerciseVO> records = questionIds.stream()
                .map(questionById::get)
                .filter(question -> question != null && "active".equals(question.getStatus()))
                .map(question -> toFavoriteQuestionVO(
                        question,
                        setById.get(primarySetId(question)),
                        audioAssets.get(question.getAudioZhAssetId()),
                        progressByExerciseId.get(question.getId())
                ))
                .toList();
        return PageResult.of(sliceRecords(records, params.page(), params.pageSize()), records.size(), params.page(), params.pageSize());
    }

    private ExerciseSetVO toSetVO(ExerciseSet set) {
        ExerciseSet parent = set.getParentId() == null ? null : exerciseSetMapper.selectById(set.getParentId());
        return new ExerciseSetVO(
                set.getId(),
                set.getTitle(),
                set.getParentId(),
                parent == null ? null : parent.getTitle(),
                set.getExerciseType(),
                set.getLevel(),
                countActiveChildren(set.getId()),
                countDirectActiveQuestions(set.getId()),
                countActiveQuestions(set.getId())
        );
    }

    private SentenceExerciseVO toQuestionVO(
            SentenceExercise question,
            Long exerciseSetId,
            String exerciseType,
            Integer sortOrder,
            List<SentenceWordOption> options,
            MediaAsset audioAsset,
            boolean shuffleOptions
    ) {
        return new SentenceExerciseVO(
                question.getId(),
                exerciseSetId,
                exerciseType,
                question.getPinyinPrompt(),
                question.getTranslationEn(),
                question.getTranslationRu(),
                question.getAudioZhAssetId(),
                audioAsset == null ? null : audioAsset.getUrl(),
                sortOrder == null ? question.getSortOrder() : sortOrder,
                toOptionVOs(options, shuffleOptions),
                false,
                null,
                0,
                0,
                null,
                null,
                null,
                null
        );
    }

    private String audioUrl(Long audioAssetId) {
        if (audioAssetId == null) {
            return null;
        }
        MediaAsset audio = mediaAssetMapper.selectById(audioAssetId);
        return audio == null || !"active".equals(audio.getStatus()) ? null : audio.getUrl();
    }

    private Map<Long, MediaAsset> loadMediaAssets(List<Long> ids) {
        List<Long> assetIds = ids.stream().filter(id -> id != null).distinct().toList();
        if (assetIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return mediaAssetMapper.selectList(new LambdaQueryWrapper<MediaAsset>()
                        .in(MediaAsset::getId, assetIds)
                        .eq(MediaAsset::getStatus, "active"))
                .stream()
                .collect(Collectors.toMap(MediaAsset::getId, Function.identity()));
    }

    private List<SentenceWordOptionVO> toOptionVOs(List<SentenceWordOption> options, boolean shuffle) {
        List<SentenceWordOption> shuffled = new ArrayList<>(options);
        if (shuffle) {
            Collections.shuffle(shuffled);
        }
        return shuffled.stream()
                .map(option -> new SentenceWordOptionVO(option.getId(), option.getWordText()))
                .toList();
    }

    private PageResult<SentenceExerciseVO> withShuffledOptions(PageResult<SentenceExerciseVO> result) {
        return PageResult.of(
                result.records().stream()
                        .map(this::withShuffledOptions)
                        .toList(),
                result.total(),
                result.page(),
                result.pageSize()
        );
    }

    private SentenceExerciseVO withShuffledOptions(SentenceExerciseVO question) {
        List<SentenceWordOptionVO> options = new ArrayList<>(question.wordOptions() == null ? List.of() : question.wordOptions());
        Collections.shuffle(options);
        return new SentenceExerciseVO(
                question.id(),
                question.exerciseSetId(),
                question.exerciseType(),
                question.pinyinPrompt(),
                question.translationEn(),
                question.translationRu(),
                question.audioZhAssetId(),
                question.audioUrl(),
                question.sortOrder(),
                options,
                question.favorite(),
                question.progressStatus(),
                question.attemptCount(),
                question.correctCount(),
                question.learnedAt(),
                question.lastPracticedAt(),
                question.lastCorrectAt(),
                question.nextReviewAt()
        );
    }

    private ExerciseAttemptVO toAttemptVO(ExerciseAttempt attempt) {
        return new ExerciseAttemptVO(
                attempt.getId(),
                attempt.getExerciseId(),
                attempt.getExerciseType(),
                attempt.getAnswerText(),
                Boolean.TRUE.equals(attempt.getIsCorrect()),
                Boolean.TRUE.equals(attempt.getShowedAnswer()),
                attempt.getTranslationLanguage(),
                attempt.getAnsweredAt()
        );
    }

    private Map<Long, List<SentenceWordOption>> loadOptionsByExerciseId(List<Long> exerciseIds) {
        if (exerciseIds.isEmpty()) {
            return Map.of();
        }
        return sentenceWordOptionMapper.selectList(new LambdaQueryWrapper<SentenceWordOption>()
                        .in(SentenceWordOption::getExerciseId, exerciseIds)
                        .orderByAsc(SentenceWordOption::getCorrectOrder)
                        .orderByAsc(SentenceWordOption::getId))
                .stream()
                .collect(Collectors.groupingBy(SentenceWordOption::getExerciseId));
    }

    private String resolveSubmittedAnswer(CheckExerciseRequest request) {
        if (request.orderedWords() != null && !request.orderedWords().isEmpty()) {
            return request.orderedWords().stream()
                    .filter(StringUtils::hasText)
                    .collect(Collectors.joining(""));
        }
        return request.answerText();
    }

    private String normalizeAnswer(String answer) {
        if (answer == null) {
            return "";
        }
        return Normalizer.normalize(answer, Normalizer.Form.NFKC)
                .replaceAll("[\\s\\p{Punct}，。！？、；：“”‘’（）《》【】]+", "")
                .toLowerCase();
    }

    private Integer firstMismatchIndex(String submitted, String standard) {
        int length = Math.min(submitted.length(), standard.length());
        for (int i = 0; i < length; i++) {
            if (submitted.charAt(i) != standard.charAt(i)) {
                return i;
            }
        }
        return length;
    }

    private String setCacheKey(long page, long pageSize, String exerciseType, String level, Long parentId) {
        return "exercise:sets:page:%d:size:%d:type:%s:level:%s:parent:%s".formatted(
                page,
                pageSize,
                cachePart(exerciseType),
                cachePart(level),
                parentId == null ? "_" : parentId
        );
    }

    private String answerCacheKey(Long exerciseId) {
        return "exercise:answers:id:%d".formatted(exerciseId);
    }

    private String questionCacheKey(Long setId, long page, long pageSize, String exerciseType) {
        return "exercise:questions:set:%d:type:%s:page:%d:size:%d".formatted(setId, cachePart(exerciseType), page, pageSize);
    }

    private String cachePart(String value) {
        return value == null || value.isBlank() ? "_" : value.trim().replace(":", "%3A");
    }

    private PageResult<SentenceExerciseVO> withUserProgress(Long userId, PageResult<SentenceExerciseVO> questionPage) {
        List<Long> questionIds = questionPage.records().stream().map(SentenceExerciseVO::id).toList();
        Map<Long, UserSentenceProgress> progressByExerciseId = sentenceProgressByExerciseId(userId, questionIds);
        return PageResult.of(
                questionPage.records().stream()
                        .map(question -> withSentenceProgress(question, progressByExerciseId.get(question.id())))
                        .toList(),
                questionPage.total(),
                questionPage.page(),
                questionPage.pageSize()
        );
    }

    private SentenceExerciseVO withSentenceProgress(SentenceExerciseVO question, UserSentenceProgress progress) {
        return new SentenceExerciseVO(
                question.id(),
                question.exerciseSetId(),
                question.exerciseType(),
                question.pinyinPrompt(),
                question.translationEn(),
                question.translationRu(),
                question.audioZhAssetId(),
                question.audioUrl(),
                question.sortOrder(),
                question.wordOptions(),
                question.favorite(),
                progress == null ? null : progress.getStatus(),
                progress == null ? 0 : progress.getAttemptCount(),
                progress == null ? 0 : progress.getCorrectCount(),
                progress == null ? null : progress.getLearnedAt(),
                progress == null ? null : progress.getLastPracticedAt(),
                progress == null ? null : progress.getLastCorrectAt(),
                progress == null ? null : progress.getNextReviewAt()
        );
    }

    private PageResult<SentenceExerciseVO> withUserFavorites(Long userId, PageResult<SentenceExerciseVO> questionPage) {
        List<Long> questionIds = questionPage.records().stream().map(SentenceExerciseVO::id).toList();
        Set<Long> favoriteIds = sentenceFavoriteIds(userId, questionIds);
        return PageResult.of(
                questionPage.records().stream()
                        .map(question -> withSentenceFavorite(question, favoriteIds.contains(question.id())))
                        .toList(),
                questionPage.total(),
                questionPage.page(),
                questionPage.pageSize()
        );
    }

    private SentenceExerciseVO withSentenceFavorite(SentenceExerciseVO question, boolean favorite) {
        return new SentenceExerciseVO(
                question.id(),
                question.exerciseSetId(),
                question.exerciseType(),
                question.pinyinPrompt(),
                question.translationEn(),
                question.translationRu(),
                question.audioZhAssetId(),
                question.audioUrl(),
                question.sortOrder(),
                question.wordOptions(),
                favorite,
                question.progressStatus(),
                question.attemptCount(),
                question.correctCount(),
                question.learnedAt(),
                question.lastPracticedAt(),
                question.lastCorrectAt(),
                question.nextReviewAt()
        );
    }

    private Set<Long> sentenceFavoriteIds(Long userId, List<Long> questionIds) {
        if (questionIds.isEmpty()) {
            return Set.of();
        }
        return userSentenceFavoriteMapper.selectList(new LambdaQueryWrapper<UserSentenceFavorite>()
                        .eq(UserSentenceFavorite::getUserId, userId)
                        .in(UserSentenceFavorite::getSentenceExerciseId, questionIds))
                .stream()
                .map(UserSentenceFavorite::getSentenceExerciseId)
                .collect(Collectors.toSet());
    }

    private Map<Long, UserSentenceProgress> sentenceProgressByExerciseId(Long userId, List<Long> questionIds) {
        if (questionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userSentenceProgressMapper.selectList(new LambdaQueryWrapper<UserSentenceProgress>()
                        .eq(UserSentenceProgress::getUserId, userId)
                        .in(UserSentenceProgress::getSentenceExerciseId, questionIds))
                .stream()
                .collect(Collectors.toMap(UserSentenceProgress::getSentenceExerciseId, Function.identity()));
    }

    private UserSentenceProgress updateSentenceProgress(
            Long userId,
            SentenceExercise exercise,
            boolean correct,
            OffsetDateTime practicedAt
    ) {
        String nextStatus = correct ? SENTENCE_STATUS_LEARNED : SENTENCE_STATUS_LEARNING;
        UserSentenceProgress progress = userSentenceProgressMapper.selectOne(new LambdaQueryWrapper<UserSentenceProgress>()
                .eq(UserSentenceProgress::getUserId, userId)
                .eq(UserSentenceProgress::getSentenceExerciseId, exercise.getId())
                .last("limit 1"));
        if (progress == null) {
            progress = new UserSentenceProgress();
            progress.setUserId(userId);
            progress.setExerciseSetId(primarySetId(exercise));
            progress.setSentenceExerciseId(exercise.getId());
            progress.setStatus(nextStatus);
            progress.setAttemptCount(1);
            progress.setCorrectCount(correct ? 1 : 0);
            progress.setLastPracticedAt(practicedAt);
            if (correct) {
                progress.setLearnedAt(practicedAt);
                progress.setLastCorrectAt(practicedAt);
            }
            userSentenceProgressMapper.insert(progress);
            return progress;
        }
        progress.setExerciseSetId(primarySetId(exercise));
        progress.setStatus(resolveNextSentenceStatus(progress.getStatus(), nextStatus));
        progress.setAttemptCount(safeInt(progress.getAttemptCount()) + 1);
        if (correct) {
            progress.setCorrectCount(safeInt(progress.getCorrectCount()) + 1);
            if (progress.getLearnedAt() == null) {
                progress.setLearnedAt(practicedAt);
            }
            progress.setLastCorrectAt(practicedAt);
        } else {
            progress.setCorrectCount(safeInt(progress.getCorrectCount()));
        }
        progress.setLastPracticedAt(practicedAt);
        progress.setUpdatedAt(practicedAt);
        userSentenceProgressMapper.updateById(progress);
        return progress;
    }

    private String resolveNextSentenceStatus(String previousStatus, String requestedStatus) {
        if (statusRank(requestedStatus) >= statusRank(previousStatus)) {
            return requestedStatus;
        }
        return previousStatus;
    }

    private int statusRank(String status) {
        return switch (status == null ? SENTENCE_STATUS_LEARNING : status) {
            case SENTENCE_STATUS_MASTERED -> 4;
            case SENTENCE_STATUS_REVIEWING -> 3;
            case SENTENCE_STATUS_LEARNED -> 2;
            default -> 1;
        };
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private boolean validExerciseType(String exerciseType) {
        return exerciseType != null && DEFAULT_EXERCISE_TYPES.contains(exerciseType);
    }

    private void recordStudyEvent(Long userId, Long exerciseId, boolean correct, Integer durationSeconds) {
        learningStatsRecorder.recordEvent(
                userId,
                "exercise",
                exerciseId,
                correct ? "correct" : "wrong",
                durationSeconds,
                OffsetDateTime.now()
        );
    }

    private ExerciseSet ensureActiveSetHierarchy(Long setId) {
        ExerciseSet set = exerciseSetMapper.selectById(setId);
        if (set == null || !"active".equals(set.getStatus())) {
            throw BusinessException.notFound("题组不存在");
        }
        Set<Long> visited = new HashSet<>();
        ExerciseSet current = set;
        while (current.getParentId() != null) {
            if (!visited.add(current.getId())) {
                throw BusinessException.conflict("题组层级存在循环引用");
            }
            ExerciseSet parent = exerciseSetMapper.selectById(current.getParentId());
            if (parent == null || !"active".equals(parent.getStatus())) {
                throw BusinessException.notFound("题组不存在");
            }
            current = parent;
        }
        return set;
    }

    private SentenceExercise requireActiveQuestion(Long exerciseId) {
        SentenceExercise exercise = sentenceExerciseMapper.selectById(exerciseId);
        if (exercise == null || !"active".equals(exercise.getStatus())) {
            throw BusinessException.notFound("题目不存在");
        }
        Long setId = primarySetId(exercise);
        if (setId != null) {
            ensureActiveSetHierarchy(setId);
        }
        return exercise;
    }

    private Map<Long, ExerciseSet> loadSetsById(List<Long> setIds) {
        List<Long> ids = setIds.stream().filter(id -> id != null).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return exerciseSetMapper.selectBatchIds(ids)
                .stream()
                .collect(Collectors.toMap(ExerciseSet::getId, Function.identity()));
    }

    private FavoriteSentenceExerciseVO toFavoriteQuestionVO(
            SentenceExercise question,
            ExerciseSet set,
            MediaAsset audioAsset,
            UserSentenceProgress progress
    ) {
        return new FavoriteSentenceExerciseVO(
                question.getId(),
                primarySetId(question),
                set == null ? null : set.getTitle(),
                question.getPinyinPrompt(),
                question.getHanziAnswer(),
                question.getTranslationEn(),
                question.getTranslationRu(),
                question.getAudioZhAssetId(),
                audioAsset == null ? null : audioAsset.getUrl(),
                question.getSortOrder(),
                progress == null ? null : progress.getStatus(),
                progress == null ? 0 : progress.getAttemptCount(),
                progress == null ? 0 : progress.getCorrectCount(),
                progress == null ? null : progress.getLearnedAt(),
                progress == null ? null : progress.getLastPracticedAt(),
                progress == null ? null : progress.getLastCorrectAt(),
                progress == null ? null : progress.getNextReviewAt(),
                true
        );
    }

    private List<ExerciseSet> resolveActiveSetScope(Long setId) {
        ExerciseSet root = ensureActiveSetHierarchy(setId);
        List<ExerciseSet> scope = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        ArrayDeque<ExerciseSet> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            ExerciseSet current = queue.removeFirst();
            if (!visited.add(current.getId())) {
                continue;
            }
            scope.add(current);
            List<ExerciseSet> children = exerciseSetMapper.selectList(new LambdaQueryWrapper<ExerciseSet>()
                    .eq(ExerciseSet::getParentId, current.getId())
                    .eq(ExerciseSet::getStatus, "active")
                    .orderByAsc(ExerciseSet::getId));
            queue.addAll(children);
        }
        return scope;
    }

    private Long primarySetId(SentenceExercise exercise) {
        if (exercise.getExerciseSetId() != null) {
            return exercise.getExerciseSetId();
        }
        return exerciseSetItemMapper.selectList(new LambdaQueryWrapper<ExerciseSetItem>()
                        .eq(ExerciseSetItem::getSentenceExerciseId, exercise.getId())
                        .eq(ExerciseSetItem::getStatus, "active")
                        .orderByAsc(ExerciseSetItem::getSortOrder)
                        .orderByAsc(ExerciseSetItem::getId))
                .stream()
                .map(ExerciseSetItem::getExerciseSetId)
                .filter(id -> id != null)
                .findFirst()
                .orElse(null);
    }

    private List<Long> activeQuestionIdsFromScope(List<Long> scopeIds) {
        List<Long> setIds = scopeIds.stream().filter(id -> id != null).distinct().toList();
        if (setIds.isEmpty()) {
            return List.of();
        }
        List<Long> questionIds = exerciseSetItemMapper.selectList(new LambdaQueryWrapper<ExerciseSetItem>()
                        .select(ExerciseSetItem::getSentenceExerciseId)
                        .in(ExerciseSetItem::getExerciseSetId, setIds)
                        .eq(ExerciseSetItem::getStatus, "active"))
                .stream()
                .map(ExerciseSetItem::getSentenceExerciseId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (questionIds.isEmpty()) {
            return List.of();
        }
        Set<Long> activeIds = sentenceExerciseMapper.selectList(new LambdaQueryWrapper<SentenceExercise>()
                        .select(SentenceExercise::getId)
                        .in(SentenceExercise::getId, questionIds)
                        .eq(SentenceExercise::getStatus, "active"))
                .stream()
                .map(SentenceExercise::getId)
                .collect(Collectors.toSet());
        return questionIds.stream().filter(activeIds::contains).toList();
    }

    private long countActiveQuestions(Long setId) {
        List<Long> scopeIds = resolveActiveSetScope(setId).stream().map(ExerciseSet::getId).toList();
        if (scopeIds.isEmpty()) {
            return 0;
        }
        return activeQuestionIdsFromScope(scopeIds).size();
    }

    private long countDirectActiveQuestions(Long setId) {
        return activeQuestionIdsFromScope(List.of(setId)).size();
    }

    private long countActiveChildren(Long setId) {
        return exerciseSetMapper.selectCount(new LambdaQueryWrapper<ExerciseSet>()
                .eq(ExerciseSet::getParentId, setId)
                .eq(ExerciseSet::getStatus, "active"));
    }

    private List<SentenceExerciseContext> sliceQuestionContexts(List<SentenceExerciseContext> questions, long page, long pageSize) {
        int from = Math.toIntExact(Math.min((page - 1) * pageSize, questions.size()));
        int to = Math.toIntExact(Math.min(from + pageSize, questions.size()));
        return questions.subList(from, to);
    }

    private <T> List<T> sliceRecords(List<T> records, long page, long pageSize) {
        int from = Math.toIntExact(Math.min((page - 1) * pageSize, records.size()));
        int to = Math.toIntExact(Math.min(from + pageSize, records.size()));
        return records.subList(from, to);
    }
}
