package com.xc.study.module.matching.vo;

import java.time.OffsetDateTime;
import java.util.List;

public record MatchingGameSessionVO(
        Long id,
        String gameType,
        String sourceType,
        Long vocabListId,
        String meaningLanguage,
        String difficulty,
        Integer totalPairs,
        Integer matchedPairs,
        Integer wrongCount,
        Integer elapsedSeconds,
        Integer timeLimitSeconds,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime completedAt,
        List<MatchingGameCardVO> cards
) {
}
