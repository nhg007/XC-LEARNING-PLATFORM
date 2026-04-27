package com.xc.study.module.stats.vo;

import java.time.OffsetDateTime;

public record StudyEventVO(
        Long id,
        String eventType,
        Long targetId,
        String result,
        Integer durationSeconds,
        OffsetDateTime occurredAt
) {
}
