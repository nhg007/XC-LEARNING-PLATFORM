package com.xc.study.module.admin.controller;

import com.xc.study.common.ApiResponse;
import com.xc.study.common.PageResult;
import com.xc.study.module.admin.dto.AdminCreateOfflinePaymentOrderDTO;
import com.xc.study.module.admin.dto.AdminFailPaymentOrderDTO;
import com.xc.study.module.admin.dto.AdminMembershipPlanQueryDTO;
import com.xc.study.module.admin.dto.AdminOrderExceptionSummaryQueryDTO;
import com.xc.study.module.admin.dto.AdminPaymentNotificationQueryDTO;
import com.xc.study.module.admin.dto.AdminPaymentOrderQueryDTO;
import com.xc.study.module.admin.dto.AdminUpdateMembershipPlanStatusDTO;
import com.xc.study.module.admin.dto.AdminUpsertMembershipPlanDTO;
import com.xc.study.module.admin.service.AdminMembershipManagementService;
import com.xc.study.module.admin.vo.AdminMembershipPlanVO;
import com.xc.study.module.admin.vo.AdminOrderExceptionSummaryVO;
import com.xc.study.module.admin.vo.AdminOperationLogVO;
import com.xc.study.module.admin.vo.AdminPaymentNotificationVO;
import com.xc.study.module.admin.vo.AdminPaymentOrderVO;
import com.xc.study.security.CurrentUser;
import com.xc.study.security.CurrentUserProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
@RequestMapping("/admin")
public class AdminMembershipManagementController {

    private final AdminMembershipManagementService adminMembershipManagementService;
    private final CurrentUserProvider currentUserProvider;

    public AdminMembershipManagementController(
            AdminMembershipManagementService adminMembershipManagementService,
            CurrentUserProvider currentUserProvider
    ) {
        this.adminMembershipManagementService = adminMembershipManagementService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/membership/plans")
    public ApiResponse<PageResult<AdminMembershipPlanVO>> pagePlans(@Valid AdminMembershipPlanQueryDTO query) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminMembershipManagementService.pagePlans(query, admin));
    }

    @PostMapping("/membership/plans")
    public ApiResponse<AdminMembershipPlanVO> createPlan(
            @Valid @RequestBody AdminUpsertMembershipPlanDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminMembershipManagementService.createPlan(request, admin, clientIp(servletRequest)));
    }

    @PutMapping("/membership/plans/{planId}")
    public ApiResponse<AdminMembershipPlanVO> updatePlan(
            @PathVariable Long planId,
            @Valid @RequestBody AdminUpsertMembershipPlanDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminMembershipManagementService.updatePlan(planId, request, admin, clientIp(servletRequest)));
    }

    @PutMapping("/membership/plans/{planId}/status")
    public ApiResponse<AdminMembershipPlanVO> updatePlanStatus(
            @PathVariable Long planId,
            @Valid @RequestBody AdminUpdateMembershipPlanStatusDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminMembershipManagementService.updatePlanStatus(planId, request, admin, clientIp(servletRequest)));
    }

    @GetMapping("/orders")
    public ApiResponse<PageResult<AdminPaymentOrderVO>> pageOrders(@Valid AdminPaymentOrderQueryDTO query) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminMembershipManagementService.pageOrders(query, admin));
    }

    @PostMapping("/orders/offline-payments")
    public ApiResponse<AdminPaymentOrderVO> createOfflinePaymentOrder(
            @Valid @RequestBody AdminCreateOfflinePaymentOrderDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminMembershipManagementService.createOfflinePaymentOrder(request, admin, clientIp(servletRequest)));
    }

    @GetMapping("/orders/exception-summary")
    public ApiResponse<AdminOrderExceptionSummaryVO> orderExceptionSummary(
            @Valid AdminOrderExceptionSummaryQueryDTO query
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminMembershipManagementService.orderExceptionSummary(query, admin));
    }

    @GetMapping("/orders/{orderId}")
    public ApiResponse<AdminPaymentOrderVO> getOrderDetail(@PathVariable Long orderId) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminMembershipManagementService.getOrderDetail(orderId, admin));
    }

    @GetMapping("/orders/{orderId}/operation-logs")
    public ApiResponse<PageResult<AdminOperationLogVO>> pageOrderOperationLogs(
            @PathVariable Long orderId,
            @Min(1) Integer page,
            @Min(1) @Max(100) Integer pageSize
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminMembershipManagementService.pageOrderOperationLogs(orderId, page, pageSize, admin));
    }

    @PutMapping("/orders/{orderId}/failed")
    public ApiResponse<AdminPaymentOrderVO> markOrderFailed(
            @PathVariable Long orderId,
            @Valid @RequestBody AdminFailPaymentOrderDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminMembershipManagementService.markOrderFailed(orderId, request, admin, clientIp(servletRequest)));
    }

    @GetMapping("/payment-notifications")
    public ApiResponse<PageResult<AdminPaymentNotificationVO>> pagePaymentNotifications(
            @Valid AdminPaymentNotificationQueryDTO query
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminMembershipManagementService.pagePaymentNotifications(query, admin));
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
