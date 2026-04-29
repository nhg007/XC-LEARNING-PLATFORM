package com.xc.study.module.classroom.service;

import com.xc.study.common.BusinessException;
import com.xc.study.common.ErrorCode;
import com.xc.study.module.classroom.dto.AddClassMemberRequest;
import com.xc.study.module.classroom.dto.CreateClassRoomRequest;
import com.xc.study.module.classroom.entity.ClassMember;
import com.xc.study.module.classroom.entity.ClassRoom;
import com.xc.study.module.classroom.mapper.ClassMemberMapper;
import com.xc.study.module.classroom.mapper.ClassRoomMapper;
import com.xc.study.module.classroom.vo.ClassMemberVO;
import com.xc.study.module.stats.mapper.StudyEventMapper;
import com.xc.study.module.user.entity.User;
import com.xc.study.module.user.mapper.UserMapper;
import com.xc.study.security.CurrentUser;
import com.xc.study.security.UserType;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClassRoomServiceTests {

    @Test
    void studentCannotCreateClassRoom() {
        ClassRoomMapper classRoomMapper = mock(ClassRoomMapper.class);
        ClassRoomService service = service(classRoomMapper, mock(ClassMemberMapper.class), mock(UserMapper.class));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.create(student(), new CreateClassRoomRequest("HSK1", "demo"))
        );

        assertEquals(ErrorCode.CLASSROOM_PERMISSION_DENIED, ex.getErrorCode());
        verify(classRoomMapper, never()).insert(any(ClassRoom.class));
    }

    @Test
    void nonTeacherCannotAddClassMember() {
        ClassRoomMapper classRoomMapper = mock(ClassRoomMapper.class);
        ClassMemberMapper classMemberMapper = mock(ClassMemberMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        ClassRoomService service = service(classRoomMapper, classMemberMapper, userMapper);

        when(classRoomMapper.selectById(10L)).thenReturn(activeRoom());
        when(classMemberMapper.selectOne(any())).thenReturn(null);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.addMember(100L, 10L, new AddClassMemberRequest(200L, null))
        );

        assertEquals(ErrorCode.CLASSROOM_PERMISSION_DENIED, ex.getErrorCode());
        verify(userMapper, never()).selectById(any());
    }

    @Test
    void studentMembersIncludesActiveStudentMembers() {
        ClassRoomMapper classRoomMapper = mock(ClassRoomMapper.class);
        ClassMemberMapper classMemberMapper = mock(ClassMemberMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        ClassRoomService service = service(classRoomMapper, classMemberMapper, userMapper);

        ClassMember student = activeMember(10L, 100L, "member");
        ClassMember classmate = activeMember(10L, 101L, "member");
        when(classRoomMapper.selectById(10L)).thenReturn(activeRoom());
        when(classMemberMapper.selectOne(any())).thenReturn(student);
        when(classMemberMapper.selectList(any())).thenReturn(List.of(student, classmate));
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(user(100L, "student@example.com"), user(101L, "classmate@example.com")));

        List<ClassMemberVO> members = service.members(100L, 10L);

        assertEquals(2, members.size());
        assertEquals(100L, members.get(0).userId());
        assertEquals(101L, members.get(1).userId());
    }

    private ClassRoomService service(
            ClassRoomMapper classRoomMapper,
            ClassMemberMapper classMemberMapper,
            UserMapper userMapper
    ) {
        return new ClassRoomService(
                classRoomMapper,
                classMemberMapper,
                userMapper,
                mock(StudyEventMapper.class)
        );
    }

    private CurrentUser student() {
        return new CurrentUser(100L, "student@example.com", UserType.STUDENT, Set.of("student"), Set.of("student:self"));
    }

    private ClassRoom activeRoom() {
        ClassRoom room = new ClassRoom();
        room.setId(10L);
        room.setName("HSK1");
        room.setStatus("active");
        return room;
    }

    private ClassMember activeMember(Long classId, Long userId, String role) {
        ClassMember member = new ClassMember();
        member.setId(userId);
        member.setClassId(classId);
        member.setUserId(userId);
        member.setMemberRole(role);
        member.setStatus("active");
        return member;
    }

    private User user(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setStatus("active");
        return user;
    }
}
