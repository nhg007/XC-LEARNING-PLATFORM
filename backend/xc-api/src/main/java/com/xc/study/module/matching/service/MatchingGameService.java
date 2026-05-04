package com.xc.study.module.matching.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xc.study.common.BusinessException;
import com.xc.study.common.ErrorCode;
import com.xc.study.module.matching.dto.CreateMatchingGameRequest;
import com.xc.study.module.matching.dto.UpdateMatchingGameRequest;
import com.xc.study.module.matching.entity.MatchingGameSession;
import com.xc.study.module.matching.mapper.MatchingGameSessionMapper;
import com.xc.study.module.matching.vo.MatchingGameCardVO;
import com.xc.study.module.matching.vo.MatchingGameSessionVO;
import com.xc.study.module.matching.vo.MatchingStageGroupVO;
import com.xc.study.module.matching.vo.MatchingStageVO;
import com.xc.study.module.stats.service.LearningStatsRecorder;
import com.xc.study.module.vocab.entity.UserVocabFavorite;
import com.xc.study.module.vocab.entity.VocabItem;
import com.xc.study.module.vocab.entity.VocabList;
import com.xc.study.module.vocab.mapper.UserVocabFavoriteMapper;
import com.xc.study.module.vocab.mapper.VocabItemMapper;
import com.xc.study.module.vocab.mapper.VocabListMapper;
import java.time.OffsetDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MatchingGameService {

    private final MatchingGameSessionMapper matchingGameSessionMapper;
    private final VocabListMapper vocabListMapper;
    private final VocabItemMapper vocabItemMapper;
    private final UserVocabFavoriteMapper userVocabFavoriteMapper;
    private final LearningStatsRecorder learningStatsRecorder;
    private final MatchingStageConfigService matchingStageConfigService;

    public MatchingGameService(
            MatchingGameSessionMapper matchingGameSessionMapper,
            VocabListMapper vocabListMapper,
            VocabItemMapper vocabItemMapper,
            UserVocabFavoriteMapper userVocabFavoriteMapper,
            LearningStatsRecorder learningStatsRecorder,
            MatchingStageConfigService matchingStageConfigService
    ) {
        this.matchingGameSessionMapper = matchingGameSessionMapper;
        this.vocabListMapper = vocabListMapper;
        this.vocabItemMapper = vocabItemMapper;
        this.userVocabFavoriteMapper = userVocabFavoriteMapper;
        this.learningStatsRecorder = learningStatsRecorder;
        this.matchingStageConfigService = matchingStageConfigService;
    }

    public List<MatchingStageVO> listStages() {
        return matchingStageConfigService.listActiveStageVos();
    }

    public List<MatchingStageGroupVO> listStageGroups() {
        return matchingStageConfigService.listActiveStageGroupVos();
    }

    public List<MatchingStageGroupVO> listStageGroups(Long userId, String gameType, String sourceType, Long vocabListId, String meaningLanguage) {
        return matchingStageConfigService.listActiveStageGroupVos(loadLevelProgress(userId, gameType, sourceType, vocabListId, meaningLanguage));
    }

    @Transactional
    public MatchingGameSessionVO createGame(Long userId, CreateMatchingGameRequest request) {
        MatchingStageConfigService.LevelConfig stage = matchingStageConfigService.requireActiveStage(request.difficulty());
        String gameType = StringUtils.hasText(request.gameType()) ? request.gameType() : "matching";
        if ("elimination".equals(gameType)) {
            ensureLevelUnlocked(userId, request);
        }
        int pairCount = stage.pairCount();
        List<VocabItem> items = loadSourceItems(userId, request.sourceType(), request.vocabListId(), request.meaningLanguage());
        if (items.size() < pairCount) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "当前词汇数量不足，无法创建该难度游戏");
        }

        OffsetDateTime now = OffsetDateTime.now();
        MatchingGameSession session = new MatchingGameSession();
        session.setUserId(userId);
        session.setGameType(gameType);
        session.setSourceType(request.sourceType());
        session.setVocabListId("vocab_list".equals(request.sourceType()) ? request.vocabListId() : null);
        session.setMeaningLanguage(request.meaningLanguage());
        session.setDifficulty(request.difficulty());
        session.setTotalPairs(pairCount);
        session.setMatchedPairs(0);
        session.setWrongCount(0);
        session.setElapsedSeconds(0);
        session.setTimeLimitSeconds(stage.timeLimitSeconds());
        session.setStatus("playing");
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        matchingGameSessionMapper.insert(session);
        return toSessionVO(session);
    }

    public MatchingGameSessionVO getGame(Long userId, Long sessionId) {
        MatchingGameSession session = requireSession(userId, sessionId);
        return toSessionVO(session);
    }

    @Transactional
    public MatchingGameSessionVO updateGame(Long userId, Long sessionId, UpdateMatchingGameRequest request) {
        MatchingGameSession session = requireSession(userId, sessionId);
        if ("completed".equals(session.getStatus()) && !"completed".equals(request.status())) {
            throw BusinessException.conflict("已完成的游戏不能改回进行中");
        }
        if ("failed".equals(session.getStatus()) && !"failed".equals(request.status())) {
            throw BusinessException.conflict("已失败的游戏不能改回进行中");
        }
        int matchedPairs = Math.min(request.matchedPairs(), session.getTotalPairs());
        OffsetDateTime now = OffsetDateTime.now();
        String previousStatus = session.getStatus();
        session.setMatchedPairs(matchedPairs);
        session.setWrongCount(request.wrongCount());
        session.setElapsedSeconds(request.elapsedSeconds());
        session.setStatus(request.status());
        if ("completed".equals(request.status())) {
            if (matchedPairs < session.getTotalPairs()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "完成游戏前必须匹配全部词汇");
            }
            if (!"completed".equals(previousStatus)) {
                session.setCompletedAt(now);
                recordCompletedEvent(userId, session, request.elapsedSeconds());
            }
        } else if ("abandoned".equals(request.status()) || "failed".equals(request.status())) {
            session.setCompletedAt(now);
        }
        session.setUpdatedAt(now);
        matchingGameSessionMapper.updateById(session);
        return toSessionVO(session);
    }

    private MatchingGameSessionVO toSessionVO(MatchingGameSession session) {
        List<MatchingGameCardVO> cards = selectCards(session).stream()
                .map(item -> new MatchingGameCardVO(
                        item.getId(),
                        item.getHanzi(),
                        item.getPinyin(),
                        "ru".equals(session.getMeaningLanguage()) ? item.getMeaningRu() : item.getMeaningEn()
                ))
                .toList();
        return new MatchingGameSessionVO(
                session.getId(),
                StringUtils.hasText(session.getGameType()) ? session.getGameType() : "matching",
                session.getSourceType(),
                session.getVocabListId(),
                session.getMeaningLanguage(),
                session.getDifficulty(),
                session.getTotalPairs(),
                session.getMatchedPairs(),
                session.getWrongCount(),
                session.getElapsedSeconds(),
                session.getTimeLimitSeconds(),
                session.getStatus(),
                session.getCreatedAt(),
                session.getCompletedAt(),
                cards
        );
    }

    private List<VocabItem> selectCards(MatchingGameSession session) {
        List<VocabItem> items = loadSourceItems(session.getUserId(), session.getSourceType(), session.getVocabListId(), session.getMeaningLanguage());
        List<VocabItem> shuffled = new ArrayList<>(items);
        Collections.shuffle(shuffled, new Random(session.getId()));
        return shuffled.stream()
                .limit(session.getTotalPairs())
                .toList();
    }

    private List<VocabItem> loadSourceItems(Long userId, String sourceType, Long vocabListId, String meaningLanguage) {
        List<VocabItem> items;
        if ("vocab_list".equals(sourceType)) {
            List<Long> scopeIds = resolveActiveListScope(vocabListId).stream().map(VocabList::getId).toList();
            items = vocabItemMapper.selectList(new LambdaQueryWrapper<VocabItem>()
                    .in(VocabItem::getVocabListId, scopeIds)
                    .eq(VocabItem::getStatus, "active")
                    .orderByAsc(VocabItem::getVocabListId)
                    .orderByAsc(VocabItem::getSortOrder)
                    .orderByAsc(VocabItem::getId));
        } else if ("favorites".equals(sourceType)) {
            List<UserVocabFavorite> favorites = userVocabFavoriteMapper.selectList(new LambdaQueryWrapper<UserVocabFavorite>()
                    .eq(UserVocabFavorite::getUserId, userId)
                    .orderByDesc(UserVocabFavorite::getCreatedAt)
                    .orderByDesc(UserVocabFavorite::getId));
            if (favorites.isEmpty()) {
                return List.of();
            }
            List<Long> itemIds = favorites.stream().map(UserVocabFavorite::getVocabItemId).toList();
            Map<Long, VocabItem> itemById = vocabItemMapper.selectBatchIds(itemIds)
                    .stream()
                    .collect(Collectors.toMap(VocabItem::getId, Function.identity()));
            items = itemIds.stream()
                    .map(itemById::get)
                    .filter(item -> item != null && "active".equals(item.getStatus()))
                    .toList();
        } else {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "词汇来源不支持");
        }
        return items.stream()
                .filter(item -> StringUtils.hasText(item.getHanzi()))
                .filter(item -> StringUtils.hasText("ru".equals(meaningLanguage) ? item.getMeaningRu() : item.getMeaningEn()))
                .sorted(Comparator.comparing(VocabItem::getId))
                .toList();
    }

    private Map<String, MatchingStageConfigService.LevelProgress> loadLevelProgress(
            Long userId,
            String gameType,
            String sourceType,
            Long vocabListId,
            String meaningLanguage
    ) {
        LambdaQueryWrapper<MatchingGameSession> wrapper = new LambdaQueryWrapper<MatchingGameSession>()
                .eq(MatchingGameSession::getUserId, userId)
                .eq(MatchingGameSession::getStatus, "completed");
        if (StringUtils.hasText(gameType)) {
            wrapper.eq(MatchingGameSession::getGameType, gameType);
        }
        if (StringUtils.hasText(sourceType)) {
            wrapper.eq(MatchingGameSession::getSourceType, sourceType);
            if ("vocab_list".equals(sourceType)) {
                wrapper.eq(vocabListId != null, MatchingGameSession::getVocabListId, vocabListId);
            } else if ("favorites".equals(sourceType)) {
                wrapper.isNull(MatchingGameSession::getVocabListId);
            }
        }
        if (StringUtils.hasText(meaningLanguage)) {
            wrapper.eq(MatchingGameSession::getMeaningLanguage, meaningLanguage);
        }
        List<MatchingGameSession> sessions = matchingGameSessionMapper.selectList(wrapper);
        Map<String, MatchingStageConfigService.LevelProgress> progress = new HashMap<>();
        for (MatchingGameSession session : sessions) {
            String code = session.getDifficulty();
            MatchingStageConfigService.LevelProgress previous = progress.get(code);
            Integer elapsedSeconds = session.getElapsedSeconds();
            Integer bestElapsedSeconds = previous == null || previous.bestElapsedSeconds() == null
                    ? elapsedSeconds
                    : Math.min(previous.bestElapsedSeconds(), elapsedSeconds);
            progress.put(code, new MatchingStageConfigService.LevelProgress(true, bestElapsedSeconds));
        }
        return progress;
    }

    private void ensureLevelUnlocked(Long userId, CreateMatchingGameRequest request) {
        Map<String, MatchingStageConfigService.LevelProgress> progress = loadLevelProgress(
                userId,
                "elimination",
                request.sourceType(),
                request.vocabListId(),
                request.meaningLanguage()
        );
        for (MatchingStageConfigService.StageConfig group : matchingStageConfigService.listActiveStageGroups()) {
            List<MatchingStageConfigService.LevelConfig> levels = group.levels().stream()
                    .filter(MatchingStageConfigService.LevelConfig::enabled)
                    .toList();
            for (int index = 0; index < levels.size(); index++) {
                MatchingStageConfigService.LevelConfig level = levels.get(index);
                if (!level.code().equals(request.difficulty())) {
                    continue;
                }
                if (index == 0) {
                    return;
                }
                MatchingStageConfigService.LevelProgress previousProgress = progress.get(levels.get(index - 1).code());
                if (previousProgress != null && previousProgress.completed()) {
                    return;
                }
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "请先完成上一关");
            }
        }
    }

    private MatchingGameSession requireSession(Long userId, Long sessionId) {
        MatchingGameSession session = matchingGameSessionMapper.selectById(sessionId);
        if (session == null || !userId.equals(session.getUserId())) {
            throw BusinessException.notFound("连连看记录不存在");
        }
        return session;
    }

    private void recordCompletedEvent(Long userId, MatchingGameSession session, Integer durationSeconds) {
        learningStatsRecorder.recordEvent(
                userId,
                "matching_game",
                session.getId(),
                "completed",
                durationSeconds,
                OffsetDateTime.now()
        );
    }

    private VocabList ensureActiveListHierarchy(Long vocabListId) {
        VocabList list = vocabListMapper.selectById(vocabListId);
        if (list == null || !"active".equals(list.getStatus())) {
            throw BusinessException.notFound("词汇表不存在");
        }
        Set<Long> visited = new HashSet<>();
        VocabList current = list;
        while (current.getParentId() != null) {
            if (!visited.add(current.getId())) {
                throw BusinessException.conflict("词汇表层级存在循环引用");
            }
            VocabList parent = vocabListMapper.selectById(current.getParentId());
            if (parent == null || !"active".equals(parent.getStatus())) {
                throw BusinessException.notFound("词汇表不存在");
            }
            current = parent;
        }
        return list;
    }

    private List<VocabList> resolveActiveListScope(Long vocabListId) {
        VocabList root = ensureActiveListHierarchy(vocabListId);
        List<VocabList> scope = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        ArrayDeque<VocabList> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            VocabList current = queue.removeFirst();
            if (!visited.add(current.getId())) {
                continue;
            }
            scope.add(current);
            List<VocabList> children = vocabListMapper.selectList(new LambdaQueryWrapper<VocabList>()
                    .eq(VocabList::getParentId, current.getId())
                    .eq(VocabList::getStatus, "active")
                    .orderByAsc(VocabList::getSortOrder)
                    .orderByAsc(VocabList::getId));
            queue.addAll(children);
        }
        return scope;
    }

}
