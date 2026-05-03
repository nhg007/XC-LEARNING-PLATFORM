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
import com.xc.study.module.exercise.entity.SentenceExercise;
import com.xc.study.module.exercise.entity.SentenceWordOption;
import com.xc.study.module.exercise.entity.UserSentenceProgress;
import com.xc.study.module.exercise.mapper.ExerciseAttemptMapper;
import com.xc.study.module.exercise.mapper.ExerciseSetMapper;
import com.xc.study.module.exercise.mapper.SentenceExerciseMapper;
import com.xc.study.module.exercise.mapper.SentenceWordOptionMapper;
import com.xc.study.module.exercise.mapper.UserSentenceProgressMapper;
import com.xc.study.module.exercise.vo.ExerciseAnswerVO;
import com.xc.study.module.exercise.vo.ExerciseAttemptVO;
import com.xc.study.module.exercise.vo.ExerciseCheckResultVO;
import com.xc.study.module.exercise.vo.ExerciseSetVO;
import com.xc.study.module.exercise.vo.SentenceExerciseVO;
import com.xc.study.module.exercise.vo.SentenceWordOptionVO;
import com.xc.study.module.media.entity.MediaAsset;
import com.xc.study.module.media.mapper.MediaAssetMapper;
import com.xc.study.module.stats.service.LearningStatsRecorder;
import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

    private static final TypeReference<PageResult<ExerciseSetVO>> EXERCISE_SET_PAGE_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<PageResult<SentenceExerciseVO>> SENTENCE_EXERCISE_PAGE_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<ExerciseAnswerVO> EXERCISE_ANSWER_TYPE = new TypeReference<>() {
    };

    private final ExerciseSetMapper exerciseSetMapper;
    private final SentenceExerciseMapper sentenceExerciseMapper;
    private final SentenceWordOptionMapper sentenceWordOptionMapper;
    private final ExerciseAttemptMapper exerciseAttemptMapper;
    private final UserSentenceProgressMapper userSentenceProgressMapper;
    private final MediaAssetMapper mediaAssetMapper;
    private final LearningStatsRecorder learningStatsRecorder;
    private final MasterDataCache masterDataCache;

    public ExerciseService(
            ExerciseSetMapper exerciseSetMapper,
            SentenceExerciseMapper sentenceExerciseMapper,
            SentenceWordOptionMapper sentenceWordOptionMapper,
            ExerciseAttemptMapper exerciseAttemptMapper,
            UserSentenceProgressMapper userSentenceProgressMapper,
            MediaAssetMapper mediaAssetMapper,
            LearningStatsRecorder learningStatsRecorder,
            MasterDataCache masterDataCache
    ) {
        this.exerciseSetMapper = exerciseSetMapper;
        this.sentenceExerciseMapper = sentenceExerciseMapper;
        this.sentenceWordOptionMapper = sentenceWordOptionMapper;
        this.exerciseAttemptMapper = exerciseAttemptMapper;
        this.userSentenceProgressMapper = userSentenceProgressMapper;
        this.mediaAssetMapper = mediaAssetMapper;
        this.learningStatsRecorder = learningStatsRecorder;
        this.masterDataCache = masterDataCache;
    }

    public PageResult<ExerciseSetVO> listSets(long page, long pageSize, String exerciseType, String level) {
        PageParams params = PageParams.normalize(page, pageSize);
        return masterDataCache.get(
                setCacheKey(params.page(), params.pageSize(), exerciseType, level),
                EXERCISE_SET_PAGE_TYPE,
                () -> loadSets(params.page(), params.pageSize(), exerciseType, level)
        );
    }

    private PageResult<ExerciseSetVO> loadSets(long page, long pageSize, String exerciseType, String level) {
        Page<ExerciseSet> result = exerciseSetMapper.selectPage(Page.of(page, pageSize), new LambdaQueryWrapper<ExerciseSet>()
                .eq(ExerciseSet::getStatus, "active")
                .eq(exerciseType != null && !exerciseType.isBlank(), ExerciseSet::getExerciseType, exerciseType)
                .eq(level != null && !level.isBlank(), ExerciseSet::getLevel, level)
                .orderByAsc(ExerciseSet::getId));
        return PageResult.of(result.getRecords().stream().map(this::toSetVO).toList(), result.getTotal(), page, pageSize);
    }

    public PageResult<SentenceExerciseVO> listQuestions(Long userId, Long setId, long page, long pageSize) {
        PageParams params = PageParams.normalize(page, pageSize);
        PageResult<SentenceExerciseVO> result = masterDataCache.get(
                questionCacheKey(setId, params.page(), params.pageSize()),
                SENTENCE_EXERCISE_PAGE_TYPE,
                () -> loadQuestions(setId, params.page(), params.pageSize())
        );
        return withShuffledOptions(withUserProgress(userId, result));
    }

    private PageResult<SentenceExerciseVO> loadQuestions(Long setId, long page, long pageSize) {
        if (exerciseSetMapper.selectById(setId) == null) {
            throw BusinessException.notFound("题组不存在");
        }
        Page<SentenceExercise> result = sentenceExerciseMapper.selectPage(Page.of(page, pageSize), new LambdaQueryWrapper<SentenceExercise>()
                .eq(SentenceExercise::getExerciseSetId, setId)
                .eq(SentenceExercise::getStatus, "active")
                .orderByAsc(SentenceExercise::getSortOrder)
                .orderByAsc(SentenceExercise::getId));
        Map<Long, List<SentenceWordOption>> optionsByExerciseId = loadOptionsByExerciseId(result.getRecords().stream()
                .map(SentenceExercise::getId)
                .toList());
        Map<Long, MediaAsset> audioAssets = loadMediaAssets(result.getRecords().stream()
                .map(SentenceExercise::getAudioZhAssetId)
                .toList());
        return PageResult.of(result.getRecords().stream()
                .map(question -> toQuestionVO(
                        question,
                        optionsByExerciseId.getOrDefault(question.getId(), List.of()),
                        audioAssets.get(question.getAudioZhAssetId()),
                        false
                ))
                .toList(), result.getTotal(), page, pageSize);
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
        attempt.setExerciseType(exercise.getExerciseType());
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

    private ExerciseSetVO toSetVO(ExerciseSet set) {
        return new ExerciseSetVO(set.getId(), set.getTitle(), set.getExerciseType(), set.getLevel());
    }

    private SentenceExerciseVO toQuestionVO(
            SentenceExercise question,
            List<SentenceWordOption> options,
            MediaAsset audioAsset,
            boolean shuffleOptions
    ) {
        return new SentenceExerciseVO(
                question.getId(),
                question.getExerciseSetId(),
                question.getExerciseType(),
                question.getPinyinPrompt(),
                question.getTranslationEn(),
                question.getTranslationRu(),
                question.getAudioZhAssetId(),
                audioAsset == null ? null : audioAsset.getUrl(),
                question.getSortOrder(),
                toOptionVOs(options, shuffleOptions),
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

    private String setCacheKey(long page, long pageSize, String exerciseType, String level) {
        return "exercise:sets:page:%d:size:%d:type:%s:level:%s".formatted(
                page,
                pageSize,
                cachePart(exerciseType),
                cachePart(level)
        );
    }

    private String answerCacheKey(Long exerciseId) {
        return "exercise:answers:id:%d".formatted(exerciseId);
    }

    private String questionCacheKey(Long setId, long page, long pageSize) {
        return "exercise:questions:set:%d:page:%d:size:%d".formatted(setId, page, pageSize);
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
                progress == null ? null : progress.getStatus(),
                progress == null ? 0 : progress.getAttemptCount(),
                progress == null ? 0 : progress.getCorrectCount(),
                progress == null ? null : progress.getLearnedAt(),
                progress == null ? null : progress.getLastPracticedAt(),
                progress == null ? null : progress.getLastCorrectAt(),
                progress == null ? null : progress.getNextReviewAt()
        );
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
            progress.setExerciseSetId(exercise.getExerciseSetId());
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
        progress.setExerciseSetId(exercise.getExerciseSetId());
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
}
