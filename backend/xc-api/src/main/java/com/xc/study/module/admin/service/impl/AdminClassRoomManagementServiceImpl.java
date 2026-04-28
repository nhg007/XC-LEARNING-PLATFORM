package com.xc.study.module.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xc.study.common.BusinessException;
import com.xc.study.common.ErrorCode;
import com.xc.study.common.PageResult;
import com.xc.study.module.admin.dto.AdminClassRoomQueryDTO;
import com.xc.study.module.admin.dto.AdminRemoveClassMemberDTO;
import com.xc.study.module.admin.dto.AdminUpdateClassRoomStatusDTO;
import com.xc.study.module.admin.entity.AdminOperationLog;
import com.xc.study.module.admin.mapper.AdminOperationLogMapper;
import com.xc.study.module.admin.service.AdminClassRoomManagementService;
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
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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

    private final ClassRoomMapper classRoomMapper;
    private final ClassMemberMapper classMemberMapper;
    private final UserMapper userMapper;
    private final StudyEventMapper studyEventMapper;
    private final AdminOperationLogMapper adminOperationLogMapper;
    private final ObjectMapper objectMapper;

    public AdminClassRoomManagementServiceImpl(
            ClassRoomMapper classRoomMapper,
            ClassMemberMapper classMemberMapper,
            UserMapper userMapper,
            StudyEventMapper studyEventMapper,
            AdminOperationLogMapper adminOperationLogMapper,
            ObjectMapper objectMapper
    ) {
        this.classRoomMapper = classRoomMapper;
        this.classMemberMapper = classMemberMapper;
        this.userMapper = userMapper;
        this.studyEventMapper = studyEventMapper;
        this.adminOperationLogMapper = adminOperationLogMapper;
        this.objectMapper = objectMapper;
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
        applyKeyword(wrapper, query.getKeyword());
        wrapper.orderByDesc(ClassRoom::getCreatedAt);
        Page<ClassRoom> result = classRoomMapper.selectPage(Page.of(page, pageSize), wrapper);
        List<ClassRoom> rooms = result.getRecords();
        List<ClassMember> activeMembers = loadMembers(rooms.stream().map(ClassRoom::getId).toList(), List.of("active"));
        Map<Long, List<ClassMember>> activeMembersByClassId = groupMembers(activeMembers);
        Map<Long, User> ownersById = loadUsers(rooms.stream().map(ClassRoom::getOwnerUserId).toList());
        Map<Long, ClassStatsSnapshot> statsByClassId = buildClassStats(activeMembersByClassId);
        List<AdminClassRoomListItemVO> records = rooms.stream()
                .map(room -> toListItem(room, ownersById.get(room.getOwnerUserId()), statsByClassId.get(room.getId())))
                .toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public AdminClassRoomDetailVO getClassRoomDetail(Long classId, CurrentUser admin) {
        requirePermission(admin, "admin:classrooms:read");
        return buildDetail(requireClassRoom(classId));
    }

    @Override
    public List<AdminClassMemberVO> listMembers(Long classId, CurrentUser admin) {
        requirePermission(admin, "admin:classrooms:read");
        requireClassRoom(classId);
        return toMemberVOs(classMemberMapper.selectList(new LambdaQueryWrapper<ClassMember>()
                .eq(ClassMember::getClassId, classId)
                .orderByAsc(ClassMember::getMemberRole)
                .orderByDesc(ClassMember::getCreatedAt)));
    }

    @Override
    public List<AdminClassMemberStatsVO> listStats(Long classId, CurrentUser admin) {
        requirePermission(admin, "admin:classrooms:read");
        requireClassRoom(classId);
        List<ClassMember> members = classMemberMapper.selectList(new LambdaQueryWrapper<ClassMember>()
                .eq(ClassMember::getClassId, classId)
                .eq(ClassMember::getStatus, "active")
                .orderByAsc(ClassMember::getUserId));
        return buildMemberStats(members);
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
        requireClassRoom(classId);
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
        List<Long> ownerIds = userMapper.selectList(new LambdaQueryWrapper<User>()
                        .like(User::getEmail, keyword)
                        .or()
                        .like(User::getNickname, keyword))
                .stream()
                .map(User::getId)
                .toList();
        wrapper.and(item -> {
            item.like(ClassRoom::getName, keyword)
                    .or()
                    .like(ClassRoom::getInviteCode, keyword);
            if (!ownerIds.isEmpty()) {
                item.or().in(ClassRoom::getOwnerUserId, ownerIds);
            }
        });
    }

    private AdminClassRoomDetailVO buildDetail(ClassRoom room) {
        User owner = userMapper.selectById(room.getOwnerUserId());
        List<ClassMember> members = classMemberMapper.selectList(new LambdaQueryWrapper<ClassMember>()
                .eq(ClassMember::getClassId, room.getId())
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
                room.getOwnerUserId(),
                owner == null ? null : owner.getEmail(),
                owner == null ? null : owner.getNickname(),
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

    private AdminClassRoomListItemVO toListItem(ClassRoom room, User owner, ClassStatsSnapshot stats) {
        ClassStatsSnapshot snapshot = stats == null ? ClassStatsSnapshot.empty() : stats;
        return new AdminClassRoomListItemVO(
                room.getId(),
                room.getName(),
                room.getDescription(),
                room.getInviteCode(),
                room.getStatus(),
                room.getOwnerUserId(),
                owner == null ? null : owner.getEmail(),
                owner == null ? null : owner.getNickname(),
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
                .in(ClassMember::getClassId, classIds);
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
                .eq(ClassMember::getStatus, status));
    }

    private long countMembers(List<ClassMember> members, String status) {
        return members.stream().filter(member -> status.equals(member.getStatus())).count();
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

    private ClassRoom requireClassRoom(Long classId) {
        ClassRoom room = classRoomMapper.selectById(classId);
        if (room == null) {
            throw BusinessException.notFound("班级不存在");
        }
        return room;
    }

    private void requirePermission(CurrentUser admin, String permission) {
        if (admin.permissions().contains("admin:*") || admin.permissions().contains(permission)) {
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
