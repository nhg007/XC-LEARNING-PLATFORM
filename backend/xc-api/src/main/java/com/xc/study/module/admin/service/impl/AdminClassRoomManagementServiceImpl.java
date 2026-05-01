package com.xc.study.module.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xc.study.common.BusinessException;
import com.xc.study.common.ErrorCode;
import com.xc.study.common.PageResult;
import com.xc.study.module.admin.dto.AdminAddClassMemberDTO;
import com.xc.study.module.admin.dto.AdminClassRoomQueryDTO;
import com.xc.study.module.admin.dto.AdminCreateClassRoomDTO;
import com.xc.study.module.admin.dto.AdminRemoveClassMemberDTO;
import com.xc.study.module.admin.dto.AdminReviewClassMemberDTO;
import com.xc.study.module.admin.dto.AdminUpdateClassRoomDTO;
import com.xc.study.module.admin.dto.AdminUpdateClassRoomStatusDTO;
import com.xc.study.module.admin.entity.AdminOperationLog;
import com.xc.study.module.admin.entity.AdminRole;
import com.xc.study.module.admin.entity.AdminUser;
import com.xc.study.module.admin.entity.AdminUserRole;
import com.xc.study.module.admin.mapper.AdminOperationLogMapper;
import com.xc.study.module.admin.mapper.AdminRoleMapper;
import com.xc.study.module.admin.mapper.AdminUserMapper;
import com.xc.study.module.admin.mapper.AdminUserRoleMapper;
import com.xc.study.module.admin.service.AdminClassRoomManagementService;
import com.xc.study.module.admin.service.support.AdminSorts;
import com.xc.study.module.admin.vo.AdminClassMemberStatsVO;
import com.xc.study.module.admin.vo.AdminClassMemberVO;
import com.xc.study.module.admin.vo.AdminClassRoomDetailVO;
import com.xc.study.module.admin.vo.AdminClassRoomListItemVO;
import com.xc.study.module.classroom.entity.ClassMember;
import com.xc.study.module.classroom.entity.ClassRoom;
import com.xc.study.module.classroom.mapper.ClassMemberMapper;
import com.xc.study.module.classroom.mapper.ClassRoomMapper;
import com.xc.study.module.stats.entity.StudyEvent;
import com.xc.study.module.stats.mapper.StudyEventMapper;
import com.xc.study.module.user.entity.User;
import com.xc.study.module.user.mapper.UserMapper;
import com.xc.study.security.CurrentUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminClassRoomManagementServiceImpl implements AdminClassRoomManagementService {

    private static final String INVITE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final String ROLE_TEACHER_ADMIN = "teacher_admin";
    private static final String PERMISSION_ALL = "admin:*";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ClassRoomMapper classRoomMapper;
    private final ClassMemberMapper classMemberMapper;
    private final UserMapper userMapper;
    private final AdminUserMapper adminUserMapper;
    private final AdminUserRoleMapper adminUserRoleMapper;
    private final AdminRoleMapper adminRoleMapper;
    private final StudyEventMapper studyEventMapper;
    private final AdminOperationLogMapper adminOperationLogMapper;
    private final ObjectMapper objectMapper;

    public AdminClassRoomManagementServiceImpl(
            ClassRoomMapper classRoomMapper,
            ClassMemberMapper classMemberMapper,
            UserMapper userMapper,
            AdminUserMapper adminUserMapper,
            AdminUserRoleMapper adminUserRoleMapper,
            AdminRoleMapper adminRoleMapper,
            StudyEventMapper studyEventMapper,
            AdminOperationLogMapper adminOperationLogMapper,
            ObjectMapper objectMapper
    ) {
        this.classRoomMapper = classRoomMapper;
        this.classMemberMapper = classMemberMapper;
        this.userMapper = userMapper;
        this.adminUserMapper = adminUserMapper;
        this.adminUserRoleMapper = adminUserRoleMapper;
        this.adminRoleMapper = adminRoleMapper;
        this.studyEventMapper = studyEventMapper;
        this.adminOperationLogMapper = adminOperationLogMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public AdminClassRoomDetailVO createClassRoom(AdminCreateClassRoomDTO request, CurrentUser admin, String ipAddress) {
        requirePermission(admin, "admin:classrooms:update");
        AdminUser teacher = resolveTeacher(request, admin);
        OffsetDateTime now = OffsetDateTime.now();

        ClassRoom room = new ClassRoom();
        room.setName(request.name().trim());
        room.setDescription(StringUtils.hasText(request.description()) ? request.description().trim() : null);
        room.setTeacherAdminUserId(teacher.getId());
        room.setOwnerUserId(null);
        room.setInviteCode(generateUniqueInviteCode());
        room.setStatus("active");
        room.setCreatedAt(now);
        room.setUpdatedAt(now);
        classRoomMapper.insert(room);

        writeOperationLog(admin.id(), "classroom.create", "classroom", room.getId(), Map.of(
                "name", room.getName(),
                "teacherAdminUserId", teacher.getId(),
                "teacherUsername", teacher.getUsername()
        ), ipAddress);
        ClassRoom savedRoom = classRoomMapper.selectById(room.getId());
        return buildDetail(savedRoom == null ? room : savedRoom);
    }

    @Override
    public PageResult<AdminClassRoomListItemVO> pageClassRooms(AdminClassRoomQueryDTO query, CurrentUser admin) {
        requirePermission(admin, "admin:classrooms:read");
        int page = query.getPage() == null ? 1 : query.getPage();
        int pageSize = query.getPageSize() == null ? 20 : query.getPageSize();
        LambdaQueryWrapper<ClassRoom> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(ClassRoom::getStatus, query.getStatus());
        } else {
            wrapper.ne(ClassRoom::getStatus, "deleted");
        }
        applyClassScope(wrapper, admin);
        applyKeyword(wrapper, query.getKeyword());
        boolean sorted = AdminSorts.apply(wrapper, query.getSortBy(), query.getSortDirection(), Map.of(
                "id", ClassRoom::getId,
                "name", ClassRoom::getName,
                "inviteCode", ClassRoom::getInviteCode,
                "status", ClassRoom::getStatus,
                "createdAt", ClassRoom::getCreatedAt,
                "updatedAt", ClassRoom::getUpdatedAt
        ));
        if (!sorted) {
            wrapper.orderByDesc(ClassRoom::getCreatedAt);
        }
        wrapper.orderByDesc(ClassRoom::getId);
        Page<ClassRoom> result = classRoomMapper.selectPage(Page.of(page, pageSize), wrapper);
        List<ClassRoom> rooms = result.getRecords();
        List<ClassMember> activeMembers = loadMembers(rooms.stream().map(ClassRoom::getId).toList(), List.of("active"));
        Map<Long, List<ClassMember>> activeMembersByClassId = groupMembers(activeMembers);
        Map<Long, AdminUser> teachersById = loadAdminUsers(rooms.stream().map(ClassRoom::getTeacherAdminUserId).toList());
        Map<Long, ClassStatsSnapshot> statsByClassId = buildClassStats(activeMembersByClassId);
        List<AdminClassRoomListItemVO> records = rooms.stream()
                .map(room -> toListItem(room, teachersById.get(room.getTeacherAdminUserId()), statsByClassId.get(room.getId())))
                .toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional
    public AdminClassRoomDetailVO updateClassRoom(
            Long classId,
            AdminUpdateClassRoomDTO request,
            CurrentUser admin,
            String ipAddress
    ) {
        requirePermission(admin, "admin:classrooms:update");
        ClassRoom room = requireClassRoom(classId);
        requireClassAccess(room, admin);
        if ("deleted".equals(room.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "已删除班级不能修改");
        }

        String beforeName = room.getName();
        String beforeDescription = room.getDescription();
        room.setName(request.name().trim());
        room.setDescription(StringUtils.hasText(request.description()) ? request.description().trim() : null);
        room.setUpdatedAt(OffsetDateTime.now());
        classRoomMapper.updateById(room);

        writeOperationLog(admin.id(), "classroom.update", "classroom", classId, Map.of(
                "beforeName", beforeName == null ? "" : beforeName,
                "afterName", room.getName(),
                "beforeDescription", beforeDescription == null ? "" : beforeDescription,
                "afterDescription", room.getDescription() == null ? "" : room.getDescription()
        ), ipAddress);
        return buildDetail(classRoomMapper.selectById(classId));
    }

    @Override
    public AdminClassRoomDetailVO getClassRoomDetail(Long classId, CurrentUser admin) {
        requirePermission(admin, "admin:classrooms:read");
        ClassRoom room = requireClassRoom(classId);
        requireClassAccess(room, admin);
        return buildDetail(room);
    }

    @Override
    public List<AdminClassMemberVO> listMembers(Long classId, CurrentUser admin) {
        requirePermission(admin, "admin:classrooms:read");
        requireClassAccess(requireClassRoom(classId), admin);
        return toMemberVOs(classMemberMapper.selectList(new LambdaQueryWrapper<ClassMember>()
                .eq(ClassMember::getClassId, classId)
                .ne(ClassMember::getMemberRole, "teacher")
                .orderByAsc(ClassMember::getMemberRole)
                .orderByDesc(ClassMember::getCreatedAt)));
    }

    @Override
    public List<AdminClassMemberStatsVO> listStats(Long classId, CurrentUser admin) {
        requirePermission(admin, "admin:classrooms:read");
        requireClassAccess(requireClassRoom(classId), admin);
        List<ClassMember> members = classMemberMapper.selectList(new LambdaQueryWrapper<ClassMember>()
                .eq(ClassMember::getClassId, classId)
                .eq(ClassMember::getStatus, "active")
                .ne(ClassMember::getMemberRole, "teacher")
                .orderByAsc(ClassMember::getUserId));
        return buildMemberStats(members);
    }

    @Override
    @Transactional
    public AdminClassMemberVO addMember(
            Long classId,
            AdminAddClassMemberDTO request,
            CurrentUser admin,
            String ipAddress
    ) {
        requirePermission(admin, "admin:classrooms:update");
        ClassRoom room = requireClassRoom(classId);
        requireClassAccess(room, admin);
        if (!"active".equals(room.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "只能向正常班级添加成员");
        }
        User target = resolveTargetUser(request);
        ClassMember exists = classMemberMapper.selectOne(new LambdaQueryWrapper<ClassMember>()
                .eq(ClassMember::getClassId, classId)
                .eq(ClassMember::getUserId, target.getId())
                .last("limit 1"));
        if (exists != null && "active".equals(exists.getStatus())) {
            throw BusinessException.conflict("成员已在班级中");
        }

        ClassMember member = exists == null ? new ClassMember() : exists;
        String beforeStatus = member.getStatus();
        OffsetDateTime now = OffsetDateTime.now();
        member.setClassId(classId);
        member.setUserId(target.getId());
        member.setMemberRole("member");
        member.setStatus("active");
        member.setInvitedByUserId(null);
        member.setReviewedByUserId(null);
        member.setReviewedAt(now);
        member.setJoinedAt(now);
        member.setRemovedAt(null);
        member.setUpdatedAt(now);
        if (member.getId() == null) {
            member.setCreatedAt(now);
            classMemberMapper.insert(member);
        } else {
            classMemberMapper.updateById(member);
        }
        writeOperationLog(admin.id(), "classroom.member.add", "classroom", classId, Map.of(
                "userId", target.getId(),
                "email", target.getEmail() == null ? "" : target.getEmail(),
                "beforeStatus", beforeStatus == null ? "" : beforeStatus,
                "afterStatus", "active"
        ), ipAddress);
        return toMemberVO(member, target);
    }

    @Override
    @Transactional
    public AdminClassMemberVO reviewMember(
            Long classId,
            Long userId,
            AdminReviewClassMemberDTO request,
            CurrentUser admin,
            String ipAddress
    ) {
        requirePermission(admin, "admin:classrooms:update");
        requireClassAccess(requireClassRoom(classId), admin);
        ClassMember member = classMemberMapper.selectOne(new LambdaQueryWrapper<ClassMember>()
                .eq(ClassMember::getClassId, classId)
                .eq(ClassMember::getUserId, userId)
                .last("limit 1"));
        if (member == null) {
            throw BusinessException.notFound("成员不存在");
        }
        if (!"pending_teacher_review".equals(member.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "当前成员不处于待审核状态");
        }
        OffsetDateTime now = OffsetDateTime.now();
        String nextStatus = Boolean.TRUE.equals(request.approved()) ? "active" : "rejected";
        member.setStatus(nextStatus);
        member.setReviewedByUserId(null);
        member.setReviewedAt(now);
        member.setJoinedAt("active".equals(nextStatus) ? now : null);
        member.setUpdatedAt(now);
        classMemberMapper.updateById(member);
        writeOperationLog(admin.id(), "classroom.member.review", "classroom", classId, Map.of(
                "userId", userId,
                "afterStatus", nextStatus,
                "approved", request.approved(),
                "reason", request.reason() == null ? "" : request.reason()
        ), ipAddress);
        return toMemberVO(member, userMapper.selectById(userId));
    }

    @Override
    @Transactional
    public AdminClassRoomDetailVO updateClassRoomStatus(
            Long classId,
            AdminUpdateClassRoomStatusDTO request,
            CurrentUser admin,
            String ipAddress
    ) {
        requirePermission(admin, "admin:classrooms:update");
        ClassRoom room = requireClassRoom(classId);
        requireClassAccess(room, admin);
        String beforeStatus = room.getStatus();
        if ("deleted".equals(beforeStatus)) {
            throw new BusinessException(ErrorCode.CONFLICT, "已删除班级不能调整状态");
        }
        room.setStatus(request.status());
        room.setUpdatedAt(OffsetDateTime.now());
        classRoomMapper.updateById(room);
        writeOperationLog(admin.id(), "classroom.status.update", "classroom", classId, Map.of(
                "beforeStatus", beforeStatus,
                "afterStatus", request.status(),
                "reason", request.reason() == null ? "" : request.reason()
        ), ipAddress);
        return buildDetail(classRoomMapper.selectById(classId));
    }

    @Override
    @Transactional
    public AdminClassMemberVO removeMember(
            Long classId,
            Long userId,
            AdminRemoveClassMemberDTO request,
            CurrentUser admin,
            String ipAddress
    ) {
        requirePermission(admin, "admin:classrooms:update");
        requireClassAccess(requireClassRoom(classId), admin);
        ClassMember member = classMemberMapper.selectOne(new LambdaQueryWrapper<ClassMember>()
                .eq(ClassMember::getClassId, classId)
                .eq(ClassMember::getUserId, userId)
                .last("limit 1"));
        if (member == null) {
            throw BusinessException.notFound("成员不存在");
        }
        if ("teacher".equals(member.getMemberRole())) {
            throw new BusinessException(ErrorCode.CONFLICT, "不能移除班级老师");
        }
        String beforeStatus = member.getStatus();
        OffsetDateTime now = OffsetDateTime.now();
        member.setStatus("removed");
        member.setRemovedAt(now);
        member.setUpdatedAt(now);
        classMemberMapper.updateById(member);
        writeOperationLog(admin.id(), "classroom.member.remove", "classroom", classId, Map.of(
                "userId", userId,
                "beforeStatus", beforeStatus,
                "afterStatus", "removed",
                "reason", request.reason() == null ? "" : request.reason()
        ), ipAddress);
        return toMemberVO(member, userMapper.selectById(userId));
    }

    private void applyKeyword(LambdaQueryWrapper<ClassRoom> wrapper, String keywordValue) {
        if (!StringUtils.hasText(keywordValue)) {
            return;
        }
        String keyword = keywordValue.trim();
        List<Long> teacherIds = adminUserMapper.selectList(new LambdaQueryWrapper<AdminUser>()
                        .like(AdminUser::getUsername, keyword)
                        .or()
                        .like(AdminUser::getDisplayName, keyword))
                .stream()
                .map(AdminUser::getId)
                .toList();
        wrapper.and(item -> {
            item.like(ClassRoom::getName, keyword)
                    .or()
                    .like(ClassRoom::getInviteCode, keyword);
            if (!teacherIds.isEmpty()) {
                item.or().in(ClassRoom::getTeacherAdminUserId, teacherIds);
            }
        });
    }

    private AdminClassRoomDetailVO buildDetail(ClassRoom room) {
        AdminUser teacher = adminUserMapper.selectById(room.getTeacherAdminUserId());
        List<ClassMember> members = classMemberMapper.selectList(new LambdaQueryWrapper<ClassMember>()
                .eq(ClassMember::getClassId, room.getId())
                .ne(ClassMember::getMemberRole, "teacher")
                .orderByAsc(ClassMember::getMemberRole)
                .orderByDesc(ClassMember::getCreatedAt));
        List<ClassMember> activeMembers = members.stream()
                .filter(member -> "active".equals(member.getStatus()))
                .toList();
        List<AdminClassMemberStatsVO> stats = buildMemberStats(activeMembers);
        ClassStatsSnapshot snapshot = summarizeStats(stats);
        return new AdminClassRoomDetailVO(
                room.getId(),
                room.getName(),
                room.getDescription(),
                room.getInviteCode(),
                room.getStatus(),
                room.getTeacherAdminUserId(),
                teacher == null ? null : teacher.getUsername(),
                teacher == null ? null : teacher.getDisplayName(),
                countMembers(members, "active"),
                countMembers(members, "pending_teacher_review"),
                countMembers(members, "removed"),
                snapshot.studySeconds(),
                snapshot.exerciseCount(),
                snapshot.correctCount(),
                snapshot.accuracyRate(),
                snapshot.lastStudyAt(),
                room.getCreatedAt(),
                room.getUpdatedAt(),
                toMemberVOs(members),
                stats
        );
    }

    private AdminClassRoomListItemVO toListItem(ClassRoom room, AdminUser teacher, ClassStatsSnapshot stats) {
        ClassStatsSnapshot snapshot = stats == null ? ClassStatsSnapshot.empty() : stats;
        return new AdminClassRoomListItemVO(
                room.getId(),
                room.getName(),
                room.getDescription(),
                room.getInviteCode(),
                room.getStatus(),
                room.getTeacherAdminUserId(),
                teacher == null ? null : teacher.getUsername(),
                teacher == null ? null : teacher.getDisplayName(),
                countMembers(room.getId(), "active"),
                countMembers(room.getId(), "pending_teacher_review"),
                snapshot.studySeconds(),
                snapshot.exerciseCount(),
                snapshot.correctCount(),
                snapshot.accuracyRate(),
                snapshot.lastStudyAt(),
                room.getCreatedAt(),
                room.getUpdatedAt()
        );
    }

    private List<ClassMember> loadMembers(List<Long> classIds, List<String> statuses) {
        if (classIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<ClassMember> wrapper = new LambdaQueryWrapper<ClassMember>()
                .in(ClassMember::getClassId, classIds)
                .ne(ClassMember::getMemberRole, "teacher");
        if (!statuses.isEmpty()) {
            wrapper.in(ClassMember::getStatus, statuses);
        }
        return classMemberMapper.selectList(wrapper);
    }

    private Map<Long, List<ClassMember>> groupMembers(List<ClassMember> members) {
        return members.stream().collect(Collectors.groupingBy(ClassMember::getClassId));
    }

    private Map<Long, ClassStatsSnapshot> buildClassStats(Map<Long, List<ClassMember>> activeMembersByClassId) {
        Set<Long> userIds = activeMembersByClassId.values()
                .stream()
                .flatMap(Collection::stream)
                .map(ClassMember::getUserId)
                .collect(Collectors.toSet());
        Map<Long, List<StudyEvent>> eventsByUserId = loadEventsByUserId(userIds);
        Map<Long, ClassStatsSnapshot> statsByClassId = new HashMap<>();
        activeMembersByClassId.forEach((classId, members) -> {
            List<AdminClassMemberStatsVO> stats = members.stream()
                    .map(member -> toStatsVO(member, null, eventsByUserId.getOrDefault(member.getUserId(), List.of())))
                    .toList();
            statsByClassId.put(classId, summarizeStats(stats));
        });
        return statsByClassId;
    }

    private List<AdminClassMemberStatsVO> buildMemberStats(List<ClassMember> members) {
        if (members.isEmpty()) {
            return List.of();
        }
        Map<Long, User> usersById = loadUsers(members.stream().map(ClassMember::getUserId).toList());
        Map<Long, List<StudyEvent>> eventsByUserId = loadEventsByUserId(usersById.keySet());
        return members.stream()
                .map(member -> toStatsVO(member, usersById.get(member.getUserId()), eventsByUserId.getOrDefault(member.getUserId(), List.of())))
                .sorted(Comparator.comparing(AdminClassMemberStatsVO::lastStudyAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private Map<Long, List<StudyEvent>> loadEventsByUserId(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return studyEventMapper.selectList(new LambdaQueryWrapper<StudyEvent>()
                        .in(StudyEvent::getUserId, userIds)
                        .orderByDesc(StudyEvent::getOccurredAt))
                .stream()
                .collect(Collectors.groupingBy(StudyEvent::getUserId));
    }

    private AdminClassMemberStatsVO toStatsVO(ClassMember member, User user, List<StudyEvent> events) {
        int studySeconds = events.stream().map(StudyEvent::getDurationSeconds).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
        int exerciseCount = countEvents(events, "exercise");
        int correctCount = (int) events.stream()
                .filter(event -> "exercise".equals(event.getEventType()))
                .filter(event -> "correct".equals(event.getResult()))
                .count();
        int vocabCount = countEvents(events, "vocab");
        int dialogueCount = countEvents(events, "dialogue");
        int matchingGameCount = countEvents(events, "matching_game");
        OffsetDateTime lastStudyAt = events.stream()
                .map(StudyEvent::getOccurredAt)
                .filter(Objects::nonNull)
                .max(OffsetDateTime::compareTo)
                .orElse(null);
        return new AdminClassMemberStatsVO(
                member.getUserId(),
                user == null ? null : user.getEmail(),
                user == null ? null : user.getNickname(),
                member.getMemberRole(),
                studySeconds,
                exerciseCount,
                correctCount,
                vocabCount,
                dialogueCount,
                matchingGameCount,
                accuracy(correctCount, exerciseCount),
                lastStudyAt
        );
    }

    private ClassStatsSnapshot summarizeStats(List<AdminClassMemberStatsVO> stats) {
        int studySeconds = stats.stream().map(AdminClassMemberStatsVO::studySeconds).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
        int exerciseCount = stats.stream().map(AdminClassMemberStatsVO::exerciseCount).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
        int correctCount = stats.stream().map(AdminClassMemberStatsVO::correctCount).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
        OffsetDateTime lastStudyAt = stats.stream()
                .map(AdminClassMemberStatsVO::lastStudyAt)
                .filter(Objects::nonNull)
                .max(OffsetDateTime::compareTo)
                .orElse(null);
        return new ClassStatsSnapshot(studySeconds, exerciseCount, correctCount, accuracy(correctCount, exerciseCount), lastStudyAt);
    }

    private int countEvents(List<StudyEvent> events, String eventType) {
        return (int) events.stream()
                .filter(event -> eventType.equals(event.getEventType()))
                .count();
    }

    private BigDecimal accuracy(int correctCount, int exerciseCount) {
        if (exerciseCount == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(correctCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(exerciseCount), 2, RoundingMode.HALF_UP);
    }

    private long countMembers(Long classId, String status) {
        return classMemberMapper.selectCount(new LambdaQueryWrapper<ClassMember>()
                .eq(ClassMember::getClassId, classId)
                .ne(ClassMember::getMemberRole, "teacher")
                .eq(ClassMember::getStatus, status));
    }

    private long countMembers(List<ClassMember> members, String status) {
        return members.stream()
                .filter(member -> !"teacher".equals(member.getMemberRole()))
                .filter(member -> status.equals(member.getStatus()))
                .count();
    }

    private List<AdminClassMemberVO> toMemberVOs(List<ClassMember> members) {
        if (members.isEmpty()) {
            return List.of();
        }
        Map<Long, User> usersById = loadUsers(members.stream().map(ClassMember::getUserId).toList());
        return members.stream()
                .map(member -> toMemberVO(member, usersById.get(member.getUserId())))
                .toList();
    }

    private AdminClassMemberVO toMemberVO(ClassMember member, User user) {
        return new AdminClassMemberVO(
                member.getId(),
                member.getClassId(),
                member.getUserId(),
                user == null ? null : user.getEmail(),
                user == null ? null : user.getNickname(),
                user == null ? null : user.getStatus(),
                member.getMemberRole(),
                member.getStatus(),
                member.getInvitedByUserId(),
                member.getReviewedByUserId(),
                member.getReviewedAt(),
                member.getJoinedAt(),
                member.getRemovedAt(),
                member.getCreatedAt()
        );
    }

    private Map<Long, User> loadUsers(Collection<Long> userIds) {
        List<Long> ids = userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(ids)
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private Map<Long, AdminUser> loadAdminUsers(Collection<Long> adminUserIds) {
        List<Long> ids = adminUserIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return adminUserMapper.selectBatchIds(ids)
                .stream()
                .collect(Collectors.toMap(AdminUser::getId, Function.identity()));
    }

    private ClassRoom requireClassRoom(Long classId) {
        ClassRoom room = classRoomMapper.selectById(classId);
        if (room == null) {
            throw BusinessException.notFound("班级不存在");
        }
        return room;
    }

    private User resolveTargetUser(AdminAddClassMemberDTO request) {
        User user;
        if (request.userId() != null) {
            user = userMapper.selectById(request.userId());
        } else {
            String email = request.email().trim().toLowerCase(Locale.ROOT);
            user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getEmail, email)
                    .last("limit 1"));
        }
        if (user == null || !"active".equals(user.getStatus())) {
            throw BusinessException.notFound("用户不存在或已禁用");
        }
        return user;
    }

    private AdminUser resolveTeacher(AdminCreateClassRoomDTO request, CurrentUser admin) {
        if (isScopedTeacherAdmin(admin)) {
            AdminUser teacher = adminUserMapper.selectById(admin.id());
            if (teacher == null || !"active".equals(teacher.getStatus())) {
                throw BusinessException.notFound("老师后台账号不存在或已禁用");
            }
            requireTeacherAdminRole(teacher);
            return teacher;
        }
        AdminUser teacher;
        if (request.teacherAdminUserId() != null) {
            teacher = adminUserMapper.selectById(request.teacherAdminUserId());
        } else {
            String username = request.teacherAdminUsername().trim().toLowerCase(Locale.ROOT);
            teacher = adminUserMapper.selectOne(new LambdaQueryWrapper<AdminUser>()
                    .eq(AdminUser::getUsername, username)
                    .last("limit 1"));
        }
        if (teacher == null || !"active".equals(teacher.getStatus())) {
            throw BusinessException.notFound("老师后台账号不存在或已禁用");
        }
        requireTeacherAdminRole(teacher);
        return teacher;
    }

    private void requireTeacherAdminRole(AdminUser teacher) {
        List<Long> roleIds = adminUserRoleMapper.selectList(new LambdaQueryWrapper<AdminUserRole>()
                        .eq(AdminUserRole::getAdminUserId, teacher.getId()))
                .stream()
                .map(AdminUserRole::getRoleId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (roleIds.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择具有老师角色的后台账号");
        }
        boolean hasTeacherRole = adminRoleMapper.selectBatchIds(roleIds)
                .stream()
                .anyMatch(role -> ROLE_TEACHER_ADMIN.equals(role.getRoleCode()));
        if (!hasTeacherRole) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择具有老师角色的后台账号");
        }
    }

    private void applyClassScope(LambdaQueryWrapper<ClassRoom> wrapper, CurrentUser admin) {
        if (isScopedTeacherAdmin(admin)) {
            wrapper.eq(ClassRoom::getTeacherAdminUserId, admin.id());
        }
    }

    private void requireClassAccess(ClassRoom room, CurrentUser admin) {
        if (!isScopedTeacherAdmin(admin)) {
            return;
        }
        if (Objects.equals(room.getTeacherAdminUserId(), admin.id())) {
            return;
        }
        throw BusinessException.forbidden(ErrorCode.FORBIDDEN, "只能维护自己负责的班级");
    }

    private boolean isScopedTeacherAdmin(CurrentUser admin) {
        return admin.roles().contains(ROLE_TEACHER_ADMIN) && !admin.permissions().contains(PERMISSION_ALL);
    }

    private String generateUniqueInviteCode() {
        String inviteCode;
        do {
            inviteCode = randomInviteCode();
        } while (classRoomMapper.selectCount(new LambdaQueryWrapper<ClassRoom>().eq(ClassRoom::getInviteCode, inviteCode)) > 0);
        return inviteCode;
    }

    private String randomInviteCode() {
        StringBuilder builder = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            builder.append(INVITE_ALPHABET.charAt(RANDOM.nextInt(INVITE_ALPHABET.length())));
        }
        return builder.toString();
    }

    private void requirePermission(CurrentUser admin, String permission) {
        if (admin.permissions().contains(PERMISSION_ALL) || admin.permissions().contains(permission)) {
            return;
        }
        throw BusinessException.forbidden(ErrorCode.FORBIDDEN, "缺少后台权限：" + permission);
    }

    private void writeOperationLog(
            Long adminUserId,
            String action,
            String targetType,
            Long targetId,
            Map<String, Object> detail,
            String ipAddress
    ) {
        OffsetDateTime now = OffsetDateTime.now();
        AdminOperationLog log = new AdminOperationLog();
        log.setAdminUserId(adminUserId);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(toJson(detail));
        log.setIpAddress(ipAddress);
        log.setCreatedAt(now);
        log.setUpdatedAt(now);
        adminOperationLogMapper.insertLog(log);
    }

    private String toJson(Map<String, Object> detail) {
        try {
            return objectMapper.writeValueAsString(new LinkedHashMap<>(detail));
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }

    private record ClassStatsSnapshot(
            Integer studySeconds,
            Integer exerciseCount,
            Integer correctCount,
            BigDecimal accuracyRate,
            OffsetDateTime lastStudyAt
    ) {
        static ClassStatsSnapshot empty() {
            return new ClassStatsSnapshot(0, 0, 0, BigDecimal.ZERO, null);
        }
    }
}
