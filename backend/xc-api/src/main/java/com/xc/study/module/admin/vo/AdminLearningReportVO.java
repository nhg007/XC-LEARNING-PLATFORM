package com.xc.study.module.admin.vo;

import com.xc.study.common.PageResult;
import java.util.List;

public record AdminLearningReportVO(
        AdminLearningReportSummaryVO summary,
        List<AdminDailyLearningReportVO> dailyStats,
        PageResult<AdminUserLearningReportVO> users
) {
}
