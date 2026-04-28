package com.xc.study.module.stats.controller;

import com.xc.study.common.ApiResponse;
import com.xc.study.common.PageResult;
import com.xc.study.module.stats.service.StatsService;
import com.xc.study.module.stats.vo.LeaderboardEntryVO;
import com.xc.study.security.CurrentUserProvider;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/leaderboards")
public class LeaderboardController {

    private final StatsService statsService;
    private final CurrentUserProvider currentUserProvider;

    public LeaderboardController(StatsService statsService, CurrentUserProvider currentUserProvider) {
        this.statsService = statsService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public ApiResponse<PageResult<LeaderboardEntryVO>> list(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize,
            @RequestParam(required = false) @Pattern(regexp = "daily|weekly|monthly|all") String periodType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam(required = false) @Pattern(regexp = "streak|accuracy|vocab_count|game_score") String metricType
    ) {
        currentUserProvider.requireStudent();
        return ApiResponse.ok(statsService.listLeaderboards(page, pageSize, periodType, periodStart, metricType));
    }
}
