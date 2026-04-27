package com.xc.study.module.stats.controller;

import com.xc.study.common.ApiResponse;
import com.xc.study.common.PageResult;
import com.xc.study.module.stats.service.StatsService;
import com.xc.study.module.stats.vo.DailyStatVO;
import com.xc.study.module.stats.vo.LearningSummaryVO;
import com.xc.study.module.stats.vo.StudyEventVO;
import com.xc.study.security.CurrentUserProvider;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/stats")
public class StatsController {

    private final StatsService statsService;
    private final CurrentUserProvider currentUserProvider;

    public StatsController(StatsService statsService, CurrentUserProvider currentUserProvider) {
        this.statsService = statsService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/summary")
    public ApiResponse<LearningSummaryVO> summary() {
        return ApiResponse.ok(statsService.getSummary(currentUserProvider.requireStudent().id()));
    }

    @GetMapping("/events")
    public ApiResponse<PageResult<StudyEventVO>> events(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize,
            @RequestParam(required = false) String eventType
    ) {
        return ApiResponse.ok(statsService.listEvents(currentUserProvider.requireStudent().id(), page, pageSize, eventType));
    }

    @GetMapping("/daily")
    public ApiResponse<List<DailyStatVO>> daily(@RequestParam(defaultValue = "30") @Min(1) @Max(366) int days) {
        return ApiResponse.ok(statsService.listDailyStats(currentUserProvider.requireStudent().id(), days));
    }
}
