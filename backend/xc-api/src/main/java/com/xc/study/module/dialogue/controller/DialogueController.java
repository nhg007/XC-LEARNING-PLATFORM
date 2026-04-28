package com.xc.study.module.dialogue.controller;

import com.xc.study.common.ApiResponse;
import com.xc.study.common.PageResult;
import com.xc.study.module.dialogue.dto.CheckDialogueLineRequest;
import com.xc.study.module.dialogue.service.DialogueService;
import com.xc.study.module.dialogue.vo.DialogueLineAnalysisVO;
import com.xc.study.module.dialogue.vo.DialogueLineCheckResultVO;
import com.xc.study.module.dialogue.vo.DialogueLineVO;
import com.xc.study.module.dialogue.vo.VideoMaterialVO;
import com.xc.study.module.membership.service.MembershipService;
import com.xc.study.security.CurrentUserProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class DialogueController {

    private final DialogueService dialogueService;
    private final MembershipService membershipService;
    private final CurrentUserProvider currentUserProvider;

    public DialogueController(
            DialogueService dialogueService,
            MembershipService membershipService,
            CurrentUserProvider currentUserProvider
    ) {
        this.dialogueService = dialogueService;
        this.membershipService = membershipService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/video-materials")
    public ApiResponse<PageResult<VideoMaterialVO>> materials(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize,
            @RequestParam(required = false) @Pattern(regexp = "drama|short_video|cartoon") String materialType
    ) {
        Long userId = currentUserProvider.requireStudent().id();
        membershipService.requireFullAccess(userId);
        return ApiResponse.ok(dialogueService.listMaterials(page, pageSize, materialType));
    }

    @GetMapping("/video-materials/{id}/lines")
    public ApiResponse<List<DialogueLineVO>> lines(@PathVariable Long id) {
        Long userId = currentUserProvider.requireStudent().id();
        membershipService.requireFullAccess(userId);
        return ApiResponse.ok(dialogueService.listLines(id));
    }

    @GetMapping("/dialogue-lines/{id}/analysis")
    public ApiResponse<DialogueLineAnalysisVO> analysis(@PathVariable Long id) {
        Long userId = currentUserProvider.requireStudent().id();
        membershipService.requireFullAccess(userId);
        return ApiResponse.ok(dialogueService.getAnalysis(id));
    }

    @PostMapping("/dialogue-lines/{id}/check")
    public ApiResponse<DialogueLineCheckResultVO> check(
            @PathVariable Long id,
            @Valid @RequestBody CheckDialogueLineRequest request
    ) {
        Long userId = currentUserProvider.requireStudent().id();
        membershipService.requireFullAccess(userId);
        return ApiResponse.ok(dialogueService.checkLine(userId, id, request));
    }
}
