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
import com.xc.study.module.stats.service.LearningStatsRecorder;
import com.xc.study.module.vocab.entity.UserVocabFavorite;
import com.xc.study.module.vocab.entity.VocabItem;
import com.xc.study.module.vocab.entity.VocabList;
import com.xc.study.module.vocab.mapper.UserVocabFavoriteMapper;
import com.xc.study.module.vocab.mapper.VocabItemMapper;
import com.xc.study.module.vocab.mapper.VocabListMapper;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MatchingGameService {

    private static final Map<String, Integer> DIFFICULTY_PAIRS = Map.of(
            "4x4", 4,
            "7x7", 7,
            "10x10", 10
    );

    private final MatchingGameSessionMapper matchingGameSessionMapper;
    private final VocabListMapper vocabListMapper;
    private final VocabItemMapper vocabItemMapper;
    private final UserVocabFavoriteMapper userVocabFavoriteMapper;
    private final LearningStatsRecorder learningStatsRecorder;

    public MatchingGameService(
            MatchingGameSessionMapper matchingGameSessionMapper,
            VocabListMapper vocabListMapper,
            VocabItemMapper vocabItemMapper,
            UserVocabFavoriteMapper userVocabFavoriteMapper,
            LearningStatsRecorder learningStatsRecorder
    ) {
        this.matchingGameSessionMapper = matchingGameSessionMapper;
        this.vocabListMapper = vocabListMapper;
        this.vocabItemMapper = vocabItemMapper;
        this.userVocabFavoriteMapper = userVocabFavoriteMapper;
        this.learningStatsRecorder = learningStatsRecorder;
    }

    @Transactional
    public MatchingGameSessionVO createGame(Long userId, CreateMatchingGameRequest request) {
        int pairCount = pairCount(request.difficulty());
        List<VocabItem> items = loadSourceItems(userId, request.sourceType(), request.vocabListId(), request.meaningLanguage());
        if (items.size() < pairCount) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "当前词汇数量不足，无法创建该难度游戏");
        }

        OffsetDateTime now = OffsetDateTime.now();
        MatchingGameSession session = new MatchingGameSession();
        session.setUserId(userId);
        session.setSourceType(request.sourceType());
        session.setVocabListId("vocab_list".equals(request.sourceType()) ? request.vocabListId() : null);
        session.setMeaningLanguage(request.meaningLanguage());
        session.setDifficulty(request.difficulty());
        session.setTotalPairs(pairCount);
        session.setMatchedPairs(0);
        session.setWrongCount(0);
        session.setElapsedSeconds(0);
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
        } else if ("abandoned".equals(request.status())) {
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
                session.getSourceType(),
                session.getVocabListId(),
                session.getMeaningLanguage(),
                session.getDifficulty(),
                session.getTotalPairs(),
                session.getMatchedPairs(),
                session.getWrongCount(),
                session.getElapsedSeconds(),
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
            VocabList list = vocabListMapper.selectById(vocabListId);
            if (list == null || !"active".equals(list.getStatus())) {
                throw BusinessException.notFound("词汇表不存在");
            }
            items = vocabItemMapper.selectList(new LambdaQueryWrapper<VocabItem>()
                    .eq(VocabItem::getVocabListId, vocabListId)
                    .eq(VocabItem::getStatus, "active")
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

    private MatchingGameSession requireSession(Long userId, Long sessionId) {
        MatchingGameSession session = matchingGameSessionMapper.selectById(sessionId);
        if (session == null || !userId.equals(session.getUserId())) {
            throw BusinessException.notFound("连连看记录不存在");
        }
        return session;
    }

    private int pairCount(String difficulty) {
        return DIFFICULTY_PAIRS.getOrDefault(difficulty, 4);
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
}
