package com.xc.study.module.matching.vo;

import java.util.Map;

public record MatchingStageVO(
        String code,
        Map<String, String> labels,
        Integer pairCount,
        Integer cardCount,
        Boolean enabled,
        Integer sortOrder
) {
}
