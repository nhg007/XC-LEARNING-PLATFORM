package com.xc.study.module.exercise.controller;

import com.xc.study.common.ApiResponse;
import com.xc.study.common.PageResult;
import com.xc.study.module.exercise.dto.CheckExerciseRequest;
import com.xc.study.module.exercise.service.ExerciseService;
import com.xc.study.module.exercise.vo.ExerciseAnswerVO;
import com.xc.study.module.exercise.vo.ExerciseCheckResultVO;
import com.xc.study.module.exercise.vo.ExerciseSetVO;
import com.xc.study.module.exercise.vo.FavoriteSentenceExerciseVO;
import com.xc.study.module.exercise.vo.SentenceFavoriteStatusVO;
import com.xc.study.module.exercise.vo.SentenceExerciseVO;
import com.xc.study.security.CurrentUserProvider;
import com.xc.study.security.RequireFullAccess;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/exercises")
@RequireFullAccess
public class ExerciseController {

    private final ExerciseService exerciseService;
    private final CurrentUserProvider currentUserProvider;

    public ExerciseController(
            ExerciseService exerciseService,
            CurrentUserProvider currentUserProvider
    ) {
        this.exerciseService = exerciseService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/sets")
    public ApiResponse<PageResult<ExerciseSetVO>> sets(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize,
            @RequestParam(required = false) String exerciseType,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) Long parentId
    ) {
        currentUserProvider.requireStudent();
        return ApiResponse.ok(exerciseService.listSets(page, pageSize, exerciseType, level, parentId));
    }

    @GetMapping("/sets/{id}/questions")
    public ApiResponse<PageResult<SentenceExerciseVO>> questions(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize
    ) {
        Long userId = currentUserProvider.requireStudent().id();
        return ApiResponse.ok(exerciseService.listQuestions(userId, id, page, pageSize));
    }

    @PostMapping("/{id}/check")
    public ApiResponse<ExerciseCheckResultVO> check(
            @PathVariable Long id,
            @Valid @RequestBody CheckExerciseRequest request
    ) {
        Long userId = currentUserProvider.requireStudent().id();
        return ApiResponse.ok(exerciseService.checkAnswer(userId, id, request));
    }

    @GetMapping("/{id}/answer")
    public ApiResponse<ExerciseAnswerVO> answer(@PathVariable Long id) {
        currentUserProvider.requireStudent();
        return ApiResponse.ok(exerciseService.getAnswer(id));
    }

    @PostMapping("/{id}/favorite")
    public ApiResponse<SentenceFavoriteStatusVO> favorite(@PathVariable Long id) {
        Long userId = currentUserProvider.requireStudent().id();
        return ApiResponse.ok(exerciseService.favorite(userId, id));
    }

    @DeleteMapping("/{id}/favorite")
    public ApiResponse<SentenceFavoriteStatusVO> unfavorite(@PathVariable Long id) {
        Long userId = currentUserProvider.requireStudent().id();
        return ApiResponse.ok(exerciseService.unfavorite(userId, id));
    }

    @GetMapping("/favorites")
    public ApiResponse<PageResult<FavoriteSentenceExerciseVO>> favorites(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize
    ) {
        Long userId = currentUserProvider.requireStudent().id();
        return ApiResponse.ok(exerciseService.listFavorites(userId, page, pageSize));
    }
}
