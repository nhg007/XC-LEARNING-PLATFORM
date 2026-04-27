package com.xc.study.module.admin.vo;

public record AdminDashboardSummaryVO(
        long userCount,
        long activeMembershipCount,
        long classCount,
        long todayStudyEventCount,
        long vocabListCount,
        long exerciseSetCount
) {
}
