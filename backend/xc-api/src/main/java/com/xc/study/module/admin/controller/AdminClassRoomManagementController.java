package com.xc.study.module.admin.controller;

import com.xc.study.common.ApiResponse;
import com.xc.study.common.PageResult;
import com.xc.study.module.admin.dto.AdminClassRoomQueryDTO;
import com.xc.study.module.admin.dto.AdminCreateClassRoomDTO;
import com.xc.study.module.admin.dto.AdminRemoveClassMemberDTO;
import com.xc.study.module.admin.dto.AdminUpdateClassRoomDTO;
import com.xc.study.module.admin.dto.AdminUpdateClassRoomStatusDTO;
import com.xc.study.module.admin.service.AdminClassRoomManagementService;
import com.xc.study.module.admin.vo.AdminClassMemberStatsVO;
import com.xc.study.module.admin.vo.AdminClassMemberVO;
import com.xc.study.module.admin.vo.AdminClassRoomDetailVO;
import com.xc.study.module.admin.vo.AdminClassRoomListItemVO;
import com.xc.study.security.CurrentUser;
import com.xc.study.security.CurrentUserProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/admin/classes")
public class AdminClassRoomManagementController {

    private final AdminClassRoomManagementService adminClassRoomManagementService;
    private final CurrentUserProvider currentUserProvider;

    public AdminClassRoomManagementController(
            AdminClassRoomManagementService adminClassRoomManagementService,
            CurrentUserProvider currentUserProvider
    ) {
        this.adminClassRoomManagementService = adminClassRoomManagementService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public ApiResponse<PageResult<AdminClassRoomListItemVO>> pageClassRooms(@Valid AdminClassRoomQueryDTO query) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminClassRoomManagementService.pageClassRooms(query, admin));
    }

    @PostMapping
    public ApiResponse<AdminClassRoomDetailVO> createClassRoom(
            @Valid @RequestBody AdminCreateClassRoomDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminClassRoomManagementService.createClassRoom(request, admin, clientIp(servletRequest)));
    }

    @GetMapping("/{classId}")
    public ApiResponse<AdminClassRoomDetailVO> getClassRoomDetail(@PathVariable Long classId) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminClassRoomManagementService.getClassRoomDetail(classId, admin));
    }

    @PutMapping("/{classId}")
    public ApiResponse<AdminClassRoomDetailVO> updateClassRoom(
            @PathVariable Long classId,
            @Valid @RequestBody AdminUpdateClassRoomDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminClassRoomManagementService.updateClassRoom(classId, request, admin, clientIp(servletRequest)));
    }

    @GetMapping("/{classId}/members")
    public ApiResponse<List<AdminClassMemberVO>> listMembers(@PathVariable Long classId) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminClassRoomManagementService.listMembers(classId, admin));
    }

    @GetMapping("/{classId}/stats")
    public ApiResponse<List<AdminClassMemberStatsVO>> listStats(@PathVariable Long classId) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminClassRoomManagementService.listStats(classId, admin));
    }

    @PutMapping("/{classId}/status")
    public ApiResponse<AdminClassRoomDetailVO> updateClassRoomStatus(
            @PathVariable Long classId,
            @Valid @RequestBody AdminUpdateClassRoomStatusDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminClassRoomManagementService.updateClassRoomStatus(classId, request, admin, clientIp(servletRequest)));
    }

    @PutMapping("/{classId}/members/{userId}/remove")
    public ApiResponse<AdminClassMemberVO> removeMember(
            @PathVariable Long classId,
            @PathVariable Long userId,
            @Valid @RequestBody AdminRemoveClassMemberDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminClassRoomManagementService.removeMember(classId, userId, request, admin, clientIp(servletRequest)));
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
