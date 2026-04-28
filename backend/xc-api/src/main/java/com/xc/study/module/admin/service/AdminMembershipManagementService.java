package com.xc.study.module.admin.service;

import com.xc.study.common.PageResult;
import com.xc.study.module.admin.dto.AdminMembershipPlanQueryDTO;
import com.xc.study.module.admin.dto.AdminPaymentOrderQueryDTO;
import com.xc.study.module.admin.dto.AdminUpdateMembershipPlanStatusDTO;
import com.xc.study.module.admin.dto.AdminUpsertMembershipPlanDTO;
import com.xc.study.module.admin.vo.AdminMembershipPlanVO;
import com.xc.study.module.admin.vo.AdminPaymentOrderVO;
import com.xc.study.security.CurrentUser;

public interface AdminMembershipManagementService {

    PageResult<AdminMembershipPlanVO> pagePlans(AdminMembershipPlanQueryDTO query, CurrentUser admin);

    AdminMembershipPlanVO createPlan(AdminUpsertMembershipPlanDTO request, CurrentUser admin, String ipAddress);

    AdminMembershipPlanVO updatePlan(Long planId, AdminUpsertMembershipPlanDTO request, CurrentUser admin, String ipAddress);

    AdminMembershipPlanVO updatePlanStatus(Long planId, AdminUpdateMembershipPlanStatusDTO request, CurrentUser admin, String ipAddress);

    PageResult<AdminPaymentOrderVO> pageOrders(AdminPaymentOrderQueryDTO query, CurrentUser admin);

    AdminPaymentOrderVO getOrderDetail(Long orderId, CurrentUser admin);
}
