package com.xc.study.module.admin.vo;

public record AdminDashboardSummaryVO(
        long userCount,
        long activeUserCount,
        long disabledUserCount,
        long trialUserCount,
        long todayNewUserCount,
        long activeMembershipCount,
        long activePlanCount,
        long todayOrderCount,
        long pendingOrderCount,
        long paidOrderCount,
        java.math.BigDecimal todayPaidAmount,
        long classCount,
        long classMemberCount,
        long pendingClassMemberCount,
        long todayActiveClassCount,
        long todayStudyEventCount,
        long vocabListCount,
        long inactiveVocabListCount,
        long vocabItemCount,
        long exerciseSetCount,
        long inactiveExerciseSetCount,
        long sentenceExerciseCount,
        long videoMaterialCount,
        long inactiveVideoMaterialCount,
        long dialogueLineCount
) {
}
