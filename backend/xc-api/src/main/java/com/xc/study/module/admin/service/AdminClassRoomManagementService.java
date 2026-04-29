package com.xc.study.module.admin.service;

import com.xc.study.common.PageResult;
import com.xc.study.module.admin.dto.AdminClassRoomQueryDTO;
import com.xc.study.module.admin.dto.AdminCreateClassRoomDTO;
import com.xc.study.module.admin.dto.AdminRemoveClassMemberDTO;
import com.xc.study.module.admin.dto.AdminUpdateClassRoomDTO;
import com.xc.study.module.admin.dto.AdminUpdateClassRoomStatusDTO;
import com.xc.study.module.admin.vo.AdminClassMemberStatsVO;
import com.xc.study.module.admin.vo.AdminClassMemberVO;
import com.xc.study.module.admin.vo.AdminClassRoomDetailVO;
import com.xc.study.module.admin.vo.AdminClassRoomListItemVO;
import com.xc.study.security.CurrentUser;
import java.util.List;

public interface AdminClassRoomManagementService {

    PageResult<AdminClassRoomListItemVO> pageClassRooms(AdminClassRoomQueryDTO query, CurrentUser admin);

    AdminClassRoomDetailVO createClassRoom(AdminCreateClassRoomDTO request, CurrentUser admin, String ipAddress);

    AdminClassRoomDetailVO updateClassRoom(Long classId, AdminUpdateClassRoomDTO request, CurrentUser admin, String ipAddress);

    AdminClassRoomDetailVO getClassRoomDetail(Long classId, CurrentUser admin);

    List<AdminClassMemberVO> listMembers(Long classId, CurrentUser admin);

    List<AdminClassMemberStatsVO> listStats(Long classId, CurrentUser admin);

    AdminClassRoomDetailVO updateClassRoomStatus(Long classId, AdminUpdateClassRoomStatusDTO request, CurrentUser admin, String ipAddress);

    AdminClassMemberVO removeMember(Long classId, Long userId, AdminRemoveClassMemberDTO request, CurrentUser admin, String ipAddress);
}
