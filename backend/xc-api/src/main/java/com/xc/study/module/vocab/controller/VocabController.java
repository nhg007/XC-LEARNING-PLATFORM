package com.xc.study.module.vocab.controller;

import com.xc.study.common.ApiResponse;
import com.xc.study.common.PageResult;
import com.xc.study.module.vocab.dto.UpdateVocabProgressRequest;
import com.xc.study.module.vocab.service.VocabService;
import com.xc.study.module.vocab.vo.FavoriteStatusVO;
import com.xc.study.module.vocab.vo.VocabItemVO;
import com.xc.study.module.vocab.vo.VocabListVO;
import com.xc.study.module.vocab.vo.VocabProgressVO;
import com.xc.study.security.CurrentUserProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/vocab")
public class VocabController {

    private final VocabService vocabService;
    private final CurrentUserProvider currentUserProvider;

    public VocabController(VocabService vocabService, CurrentUserProvider currentUserProvider) {
        this.vocabService = vocabService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/lists")
    public ApiResponse<PageResult<VocabListVO>> lists(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize,
            @RequestParam(required = false) String listType,
            @RequestParam(required = false) String level
    ) {
        currentUserProvider.requireStudent();
        return ApiResponse.ok(vocabService.listLists(page, pageSize, listType, level));
    }

    @GetMapping("/lists/{id}/items")
    public ApiResponse<PageResult<VocabItemVO>> items(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize
    ) {
        Long userId = currentUserProvider.requireStudent().id();
        return ApiResponse.ok(vocabService.listItems(userId, id, page, pageSize));
    }

    @GetMapping("/lists/{id}/progress")
    public ApiResponse<VocabProgressVO> progress(@PathVariable Long id) {
        Long userId = currentUserProvider.requireStudent().id();
        return ApiResponse.ok(vocabService.getProgress(userId, id));
    }

    @PutMapping("/lists/{id}/progress")
    public ApiResponse<VocabProgressVO> updateProgress(
            @PathVariable Long id,
            @Valid @RequestBody UpdateVocabProgressRequest request
    ) {
        Long userId = currentUserProvider.requireStudent().id();
        return ApiResponse.ok(vocabService.updateProgress(userId, id, request));
    }

    @PostMapping("/items/{id}/favorite")
    public ApiResponse<FavoriteStatusVO> favorite(@PathVariable Long id) {
        Long userId = currentUserProvider.requireStudent().id();
        return ApiResponse.ok(vocabService.favorite(userId, id));
    }

    @DeleteMapping("/items/{id}/favorite")
    public ApiResponse<FavoriteStatusVO> unfavorite(@PathVariable Long id) {
        Long userId = currentUserProvider.requireStudent().id();
        return ApiResponse.ok(vocabService.unfavorite(userId, id));
    }

    @GetMapping("/favorites")
    public ApiResponse<PageResult<VocabItemVO>> favorites(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize
    ) {
        Long userId = currentUserProvider.requireStudent().id();
        return ApiResponse.ok(vocabService.listFavorites(userId, page, pageSize));
    }
}
