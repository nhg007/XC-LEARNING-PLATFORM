package com.xc.study.module.exercise.controller;

import com.xc.study.common.ApiResponse;
import com.xc.study.common.PageResult;
import com.xc.study.module.exercise.service.ExerciseService;
import com.xc.study.module.exercise.vo.ExerciseAttemptVO;
import com.xc.study.security.CurrentUserProvider;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/exercise-attempts")
public class ExerciseAttemptController {

    private final ExerciseService exerciseService;
    private final CurrentUserProvider currentUserProvider;

    public ExerciseAttemptController(ExerciseService exerciseService, CurrentUserProvider currentUserProvider) {
        this.exerciseService = exerciseService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public ApiResponse<PageResult<ExerciseAttemptVO>> attempts(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize
    ) {
        Long userId = currentUserProvider.requireStudent().id();
        return ApiResponse.ok(exerciseService.listAttempts(userId, page, pageSize));
    }
}
