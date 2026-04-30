package com.xc.study.module.matching.controller;

import com.xc.study.common.ApiResponse;
import com.xc.study.module.matching.dto.CreateMatchingGameRequest;
import com.xc.study.module.matching.dto.UpdateMatchingGameRequest;
import com.xc.study.module.matching.service.MatchingGameService;
import com.xc.study.module.matching.vo.MatchingGameSessionVO;
import com.xc.study.module.matching.vo.MatchingStageGroupVO;
import com.xc.study.module.matching.vo.MatchingStageVO;
import com.xc.study.security.CurrentUserProvider;
import com.xc.study.security.RequireFullAccess;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/matching-games")
@RequireFullAccess
public class MatchingGameController {

    private final MatchingGameService matchingGameService;
    private final CurrentUserProvider currentUserProvider;

    public MatchingGameController(MatchingGameService matchingGameService, CurrentUserProvider currentUserProvider) {
        this.matchingGameService = matchingGameService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/stages")
    public ApiResponse<List<MatchingStageVO>> stages() {
        currentUserProvider.requireStudent();
        return ApiResponse.ok(matchingGameService.listStages());
    }

    @GetMapping("/stage-groups")
    public ApiResponse<List<MatchingStageGroupVO>> stageGroups(
            @RequestParam(required = false) String gameType,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) Long vocabListId,
            @RequestParam(required = false) String meaningLanguage
    ) {
        Long userId = currentUserProvider.requireStudent().id();
        return ApiResponse.ok(matchingGameService.listStageGroups(userId, gameType, sourceType, vocabListId, meaningLanguage));
    }

    @PostMapping
    public ApiResponse<MatchingGameSessionVO> create(@Valid @RequestBody CreateMatchingGameRequest request) {
        Long userId = currentUserProvider.requireStudent().id();
        return ApiResponse.ok(matchingGameService.createGame(userId, request));
    }

    @GetMapping("/{sessionId}")
    public ApiResponse<MatchingGameSessionVO> detail(@PathVariable Long sessionId) {
        Long userId = currentUserProvider.requireStudent().id();
        return ApiResponse.ok(matchingGameService.getGame(userId, sessionId));
    }

    @PutMapping("/{sessionId}")
    public ApiResponse<MatchingGameSessionVO> update(
            @PathVariable Long sessionId,
            @Valid @RequestBody UpdateMatchingGameRequest request
    ) {
        Long userId = currentUserProvider.requireStudent().id();
        return ApiResponse.ok(matchingGameService.updateGame(userId, sessionId, request));
    }
}
