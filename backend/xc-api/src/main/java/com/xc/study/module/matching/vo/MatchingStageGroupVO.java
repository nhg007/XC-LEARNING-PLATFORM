package com.xc.study.module.matching.vo;

import java.util.List;
import java.util.Map;

public record MatchingStageGroupVO(
        String code,
        Map<String, String> labels,
        Boolean enabled,
        Integer sortOrder,
        List<MatchingStageLevelVO> levels
) {
}
