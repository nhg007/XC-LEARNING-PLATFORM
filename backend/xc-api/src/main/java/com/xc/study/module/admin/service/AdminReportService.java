package com.xc.study.module.admin.service;

import com.xc.study.common.PageResult;
import com.xc.study.module.admin.dto.AdminLeaderboardQueryDTO;
import com.xc.study.module.admin.dto.AdminLearningReportQueryDTO;
import com.xc.study.module.admin.vo.AdminLeaderboardEntryVO;
import com.xc.study.module.admin.vo.AdminLearningReportVO;
import com.xc.study.security.CurrentUser;

public interface AdminReportService {

    AdminLearningReportVO learningReport(AdminLearningReportQueryDTO query, CurrentUser admin);

    PageResult<AdminLeaderboardEntryVO> pageLeaderboards(AdminLeaderboardQueryDTO query, CurrentUser admin);
}
