package com.xc.study.module.admin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xc.study.common.BusinessException;
import com.xc.study.common.ErrorCode;
import com.xc.study.module.admin.dto.AdminCreateClassRoomDTO;
import com.xc.study.module.admin.dto.AdminUpdateClassRoomDTO;
import com.xc.study.module.admin.entity.AdminUser;
import com.xc.study.module.admin.entity.AdminOperationLog;
import com.xc.study.module.admin.mapper.AdminOperationLogMapper;
import com.xc.study.module.admin.mapper.AdminUserMapper;
import com.xc.study.module.admin.vo.AdminClassRoomDetailVO;
import com.xc.study.module.classroom.entity.ClassMember;
import com.xc.study.module.classroom.entity.ClassRoom;
import com.xc.study.module.classroom.mapper.ClassMemberMapper;
import com.xc.study.module.classroom.mapper.ClassRoomMapper;
import com.xc.study.module.stats.mapper.StudyEventMapper;
import com.xc.study.module.user.entity.User;
import com.xc.study.module.user.mapper.UserMapper;
import com.xc.study.security.CurrentUser;
import com.xc.study.security.UserType;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminClassRoomManagementServiceImplTests {

    @Test
    void createClassRoomRequiresUpdatePermission() {
        ClassRoomMapper classRoomMapper = mock(ClassRoomMapper.class);
        AdminUserMapper adminUserMapper = mock(AdminUserMapper.class);
        AdminClassRoomManagementServiceImpl service = service(
                classRoomMapper,
                mock(ClassMemberMapper.class),
                mock(UserMapper.class),
                adminUserMapper,
                mock(StudyEventMapper.class),
                mock(AdminOperationLogMapper.class)
        );

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.createClassRoom(
                        new AdminCreateClassRoomDTO("HSK 1", null, 200L, null),
                        readOnlyAdmin(),
                        "127.0.0.1"
                )
        );

        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
        verify(adminUserMapper, never()).selectById(any());
        verify(classRoomMapper, never()).insert(any(ClassRoom.class));
    }

    @Test
    void createClassRoomLinksBackendTeacherWithoutCreatingStudentTeacher() {
        ClassRoomMapper classRoomMapper = mock(ClassRoomMapper.class);
        ClassMemberMapper classMemberMapper = mock(ClassMemberMapper.class);
        AdminUserMapper adminUserMapper = mock(AdminUserMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        StudyEventMapper studyEventMapper = mock(StudyEventMapper.class);
        AdminOperationLogMapper operationLogMapper = mock(AdminOperationLogMapper.class);
        AdminClassRoomManagementServiceImpl service = service(
                classRoomMapper,
                classMemberMapper,
                userMapper,
                adminUserMapper,
                studyEventMapper,
                operationLogMapper
        );
        AtomicReference<ClassRoom> createdRoom = new AtomicReference<>();
        AdminUser teacher = activeTeacherAdmin();

        when(adminUserMapper.selectById(200L)).thenReturn(teacher);
        when(classRoomMapper.selectCount(any())).thenReturn(0L);
        doAnswer(invocation -> {
            ClassRoom room = invocation.getArgument(0);
            room.setId(10L);
            createdRoom.set(room);
            return 1;
        }).when(classRoomMapper).insert(any(ClassRoom.class));
        when(classRoomMapper.selectById(10L)).thenAnswer(invocation -> createdRoom.get());
        when(classMemberMapper.selectList(any())).thenReturn(List.of());

        AdminClassRoomDetailVO detail = service.createClassRoom(
                new AdminCreateClassRoomDTO(" HSK 1 班 ", " 入门班 ", 200L, null),
                admin(),
                "127.0.0.1"
        );

        assertEquals(10L, detail.id());
        assertEquals("HSK 1 班", detail.name());
        assertEquals("入门班", detail.description());
        assertEquals(200L, detail.teacherAdminUserId());
        assertEquals("teacher@example.com", detail.teacherUsername());
        assertNotNull(detail.inviteCode());
        assertEquals(0L, detail.activeMemberCount());
        verify(classMemberMapper, never()).insert(any(ClassMember.class));
        verify(operationLogMapper).insertLog(any(AdminOperationLog.class));
    }

    @Test
    void updateClassRoomUpdatesNameAndDescription() {
        ClassRoomMapper classRoomMapper = mock(ClassRoomMapper.class);
        ClassMemberMapper classMemberMapper = mock(ClassMemberMapper.class);
        AdminUserMapper adminUserMapper = mock(AdminUserMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        StudyEventMapper studyEventMapper = mock(StudyEventMapper.class);
        AdminOperationLogMapper operationLogMapper = mock(AdminOperationLogMapper.class);
        AdminClassRoomManagementServiceImpl service = service(
                classRoomMapper,
                classMemberMapper,
                userMapper,
                adminUserMapper,
                studyEventMapper,
                operationLogMapper
        );
        ClassRoom room = activeRoom();
        AdminUser teacher = activeTeacherAdmin();

        when(classRoomMapper.selectById(10L)).thenReturn(room);
        when(adminUserMapper.selectById(200L)).thenReturn(teacher);
        when(classMemberMapper.selectList(any())).thenReturn(List.of());

        AdminClassRoomDetailVO detail = service.updateClassRoom(
                10L,
                new AdminUpdateClassRoomDTO(" HSK 1 强化班 ", " 课后复习 "),
                admin(),
                "127.0.0.1"
        );

        assertEquals("HSK 1 强化班", detail.name());
        assertEquals("课后复习", detail.description());
        verify(classRoomMapper).updateById(room);
        verify(operationLogMapper).insertLog(any(AdminOperationLog.class));
    }

    private AdminClassRoomManagementServiceImpl service(
            ClassRoomMapper classRoomMapper,
            ClassMemberMapper classMemberMapper,
            UserMapper userMapper,
            AdminUserMapper adminUserMapper,
            StudyEventMapper studyEventMapper,
            AdminOperationLogMapper operationLogMapper
    ) {
        return new AdminClassRoomManagementServiceImpl(
                classRoomMapper,
                classMemberMapper,
                userMapper,
                adminUserMapper,
                studyEventMapper,
                operationLogMapper,
                new ObjectMapper()
        );
    }

    private CurrentUser admin() {
        return new CurrentUser(1L, "admin", UserType.ADMIN, Set.of("super_admin"), Set.of("admin:classrooms:update"));
    }

    private CurrentUser readOnlyAdmin() {
        return new CurrentUser(1L, "admin", UserType.ADMIN, Set.of("operator"), Set.of("admin:classrooms:read"));
    }

    private AdminUser activeTeacherAdmin() {
        AdminUser adminUser = new AdminUser();
        adminUser.setId(200L);
        adminUser.setUsername("teacher@example.com");
        adminUser.setDisplayName("Teacher");
        adminUser.setStatus("active");
        return adminUser;
    }

    private ClassRoom activeRoom() {
        ClassRoom room = new ClassRoom();
        room.setId(10L);
        room.setName("HSK 1 班");
        room.setDescription("入门班");
        room.setTeacherAdminUserId(200L);
        room.setInviteCode("DEMO2026");
        room.setStatus("active");
        return room;
    }
}
