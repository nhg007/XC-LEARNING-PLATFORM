package com.xc.study.module.admin.vo;

import java.time.OffsetDateTime;

public record AdminMembershipRecordVO(
        Long id,
        Long planId,
        String planName,
        OffsetDateTime startedAt,
        OffsetDateTime endsAt,
        String source,
        Long adjustedByAdminId,
        String adjustReason,
        String status,
        OffsetDateTime createdAt
) {
}
