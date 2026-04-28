package com.xc.study.module.admin.service;

import com.xc.study.common.PageResult;
import com.xc.study.module.admin.dto.AdminAdjustMembershipDTO;
import com.xc.study.module.admin.dto.AdminUpdateUserStatusDTO;
import com.xc.study.module.admin.dto.AdminUserQueryDTO;
import com.xc.study.module.admin.vo.AdminUserDetailVO;
import com.xc.study.module.admin.vo.AdminUserListItemVO;
import com.xc.study.security.CurrentUser;

public interface AdminUserService {

    PageResult<AdminUserListItemVO> pageUsers(AdminUserQueryDTO query, CurrentUser admin);

    AdminUserDetailVO getUserDetail(Long userId, CurrentUser admin);

    AdminUserDetailVO updateUserStatus(Long userId, AdminUpdateUserStatusDTO request, CurrentUser admin, String ipAddress);

    AdminUserDetailVO adjustMembership(Long userId, AdminAdjustMembershipDTO request, CurrentUser admin, String ipAddress);
}
