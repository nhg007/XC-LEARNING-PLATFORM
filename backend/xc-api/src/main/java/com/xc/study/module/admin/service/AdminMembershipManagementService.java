package com.xc.study.module.admin.service;

import com.xc.study.common.PageResult;
import com.xc.study.module.admin.dto.AdminFailPaymentOrderDTO;
import com.xc.study.module.admin.dto.AdminCreateOfflinePaymentOrderDTO;
import com.xc.study.module.admin.dto.AdminMembershipPlanQueryDTO;
import com.xc.study.module.admin.dto.AdminOrderExceptionSummaryQueryDTO;
import com.xc.study.module.admin.dto.AdminPaymentNotificationQueryDTO;
import com.xc.study.module.admin.dto.AdminPaymentOrderQueryDTO;
import com.xc.study.module.admin.dto.AdminUpdateMembershipPlanStatusDTO;
import com.xc.study.module.admin.dto.AdminUpsertMembershipPlanDTO;
import com.xc.study.module.admin.vo.AdminMembershipPlanVO;
import com.xc.study.module.admin.vo.AdminOrderExceptionSummaryVO;
import com.xc.study.module.admin.vo.AdminOperationLogVO;
import com.xc.study.module.admin.vo.AdminPaymentNotificationVO;
import com.xc.study.module.admin.vo.AdminPaymentOrderVO;
import com.xc.study.security.CurrentUser;

public interface AdminMembershipManagementService {

    PageResult<AdminMembershipPlanVO> pagePlans(AdminMembershipPlanQueryDTO query, CurrentUser admin);

    AdminMembershipPlanVO createPlan(AdminUpsertMembershipPlanDTO request, CurrentUser admin, String ipAddress);

    AdminMembershipPlanVO updatePlan(Long planId, AdminUpsertMembershipPlanDTO request, CurrentUser admin, String ipAddress);

    AdminMembershipPlanVO updatePlanStatus(Long planId, AdminUpdateMembershipPlanStatusDTO request, CurrentUser admin, String ipAddress);

    PageResult<AdminPaymentOrderVO> pageOrders(AdminPaymentOrderQueryDTO query, CurrentUser admin);

    AdminPaymentOrderVO createOfflinePaymentOrder(AdminCreateOfflinePaymentOrderDTO request, CurrentUser admin, String ipAddress);

    AdminOrderExceptionSummaryVO orderExceptionSummary(AdminOrderExceptionSummaryQueryDTO query, CurrentUser admin);

    AdminPaymentOrderVO getOrderDetail(Long orderId, CurrentUser admin);

    PageResult<AdminOperationLogVO> pageOrderOperationLogs(Long orderId, Integer page, Integer pageSize, CurrentUser admin);

    AdminPaymentOrderVO markOrderFailed(Long orderId, AdminFailPaymentOrderDTO request, CurrentUser admin, String ipAddress);

    PageResult<AdminPaymentNotificationVO> pagePaymentNotifications(AdminPaymentNotificationQueryDTO query, CurrentUser admin);
}
