package com.xc.study.module.classroom.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xc.study.common.BusinessException;
import com.xc.study.common.ErrorCode;
import com.xc.study.module.classroom.dto.AddClassMemberRequest;
import com.xc.study.module.classroom.dto.CreateClassRoomRequest;
import com.xc.study.module.classroom.dto.JoinClassRoomRequest;
import com.xc.study.module.classroom.dto.ReviewClassMemberRequest;
import com.xc.study.module.classroom.entity.ClassMember;
import com.xc.study.module.classroom.entity.ClassRoom;
import com.xc.study.module.classroom.mapper.ClassMemberMapper;
import com.xc.study.module.classroom.mapper.ClassRoomMapper;
import com.xc.study.module.classroom.vo.ClassMemberStatsVO;
import com.xc.study.module.classroom.vo.ClassMemberVO;
import com.xc.study.module.classroom.vo.ClassRoomDetailVO;
import com.xc.study.module.classroom.vo.ClassRoomVO;
import com.xc.study.module.stats.entity.StudyEvent;
import com.xc.study.module.stats.mapper.StudyEventMapper;
import com.xc.study.module.user.entity.User;
import com.xc.study.module.user.mapper.UserMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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
public class ClassRoomService {

    private static final String INVITE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ClassRoomMapper classRoomMapper;
    private final ClassMemberMapper classMemberMapper;
    private final UserMapper userMapper;
    private final StudyEventMapper studyEventMapper;

    public ClassRoomService(
            ClassRoomMapper classRoomMapper,
            ClassMemberMapper classMemberMapper,
            UserMapper userMapper,
            StudyEventMapper studyEventMapper
    ) {
        this.classRoomMapper = classRoomMapper;
        this.classMemberMapper = classMemberMapper;
        this.userMapper = userMapper;
        this.studyEventMapper = studyEventMapper;
    }

    public List<ClassRoomVO> myClasses(Long userId) {
        List<ClassMember> memberships = classMemberMapper.selectList(new LambdaQueryWrapper<ClassMember>()
                .eq(ClassMember::getUserId, userId)
                .in(ClassMember::getStatus, List.of("active", "pending_teacher_review", "invited"))
                .orderByDesc(ClassMember::getCreatedAt));
        if (memberships.isEmpty()) {
            return List.of();
        }
        Map<Long, ClassMember> membershipByClassId = memberships.stream()
                .collect(Collectors.toMap(ClassMember::getClassId, Function.identity(), (left, right) -> left));
        List<ClassRoom> rooms = classRoomMapper.selectBatchIds(membershipByClassId.keySet());
        return rooms.stream()
                .filter(room -> !"deleted".equals(room.getStatus()))
                .map(room -> toVO(room, membershipByClassId.get(room.getId())))
                .toList();
    }

    @Transactional
    public ClassRoomVO create(Long userId, CreateClassRoomRequest request) {
        ClassRoom room = new ClassRoom();
        room.setName(request.name());
        room.setDescription(request.description());
        room.setOwnerUserId(userId);
        room.setInviteCode(generateUniqueInviteCode());
        room.setStatus("active");
        classRoomMapper.insert(room);

        ClassMember teacher = new ClassMember();
        teacher.setClassId(room.getId());
        teacher.setUserId(userId);
        teacher.setMemberRole("teacher");
        teacher.setStatus("active");
        teacher.setJoinedAt(OffsetDateTime.now());
        classMemberMapper.insert(teacher);
        return toVO(room, teacher);
    }

    @Transactional
    public ClassRoomVO join(Long userId, JoinClassRoomRequest request) {
        ClassRoom room = classRoomMapper.selectOne(new LambdaQueryWrapper<ClassRoom>()
                .eq(ClassRoom::getInviteCode, request.inviteCode().trim().toUpperCase())
                .eq(ClassRoom::getStatus, "active")
                .last("limit 1"));
        if (room == null) {
            throw BusinessException.notFound("邀请码无效");
        }
        ClassMember exists = classMemberMapper.selectOne(new LambdaQueryWrapper<ClassMember>()
                .eq(ClassMember::getClassId, room.getId())
                .eq(ClassMember::getUserId, userId)
                .last("limit 1"));
        if (exists != null && "active".equals(exists.getStatus())) {
            return toVO(room, exists);
        }
        if (exists != null && "pending_teacher_review".equals(exists.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "加入申请正在审核");
        }

        ClassMember member = exists == null ? new ClassMember() : exists;
        member.setClassId(room.getId());
        member.setUserId(userId);
        member.setMemberRole("member");
        member.setStatus("active");
        member.setJoinedAt(OffsetDateTime.now());
        if (member.getId() == null) {
            classMemberMapper.insert(member);
        } else {
            classMemberMapper.updateById(member);
        }
        return toVO(room, member);
    }

    public ClassRoomDetailVO detail(Long userId, Long classId) {
        ClassRoom room = requireActiveRoom(classId);
        ClassMember member = requireKnownMember(classId, userId);
        return toDetailVO(room, member);
    }

    public List<ClassMemberVO> members(Long userId, Long classId) {
        requireActiveRoom(classId);
        ClassMember currentMember = requireActiveMember(classId, userId);
        List<ClassMember> members;
        if ("teacher".equals(currentMember.getMemberRole())) {
            members = classMemberMapper.selectList(new LambdaQueryWrapper<ClassMember>()
                    .eq(ClassMember::getClassId, classId)
                    .notIn(ClassMember::getStatus, List.of("left", "removed"))
                    .orderByDesc(ClassMember::getCreatedAt));
        } else {
            members = List.of(currentMember);
        }
        return toMemberVOs(members);
    }

    @Transactional
    public ClassMemberVO addMember(Long userId, Long classId, AddClassMemberRequest request) {
        requireActiveRoom(classId);
        ClassMember operator = requireActiveMember(classId, userId);
        User target = resolveTargetUser(request);
        if (target.getId().equals(userId)) {
            throw new BusinessException("不能添加自己");
        }
        ClassMember exists = classMemberMapper.selectOne(new LambdaQueryWrapper<ClassMember>()
                .eq(ClassMember::getClassId, classId)
                .eq(ClassMember::getUserId, target.getId())
                .last("limit 1"));
        String nextStatus = "teacher".equals(operator.getMemberRole()) ? "active" : "pending_teacher_review";
        if (exists != null && "active".equals(exists.getStatus())) {
            throw BusinessException.conflict("成员已在班级中");
        }
        if (exists != null && "pending_teacher_review".equals(exists.getStatus())) {
            throw BusinessException.conflict("成员申请正在审核");
        }

        ClassMember member = exists == null ? new ClassMember() : exists;
        member.setClassId(classId);
        member.setUserId(target.getId());
        member.setMemberRole("member");
        member.setStatus(nextStatus);
        member.setInvitedByUserId(userId);
        member.setReviewedByUserId("active".equals(nextStatus) ? userId : null);
        member.setReviewedAt("active".equals(nextStatus) ? OffsetDateTime.now() : null);
        member.setJoinedAt("active".equals(nextStatus) ? OffsetDateTime.now() : null);
        member.setRemovedAt(null);
        if (member.getId() == null) {
            classMemberMapper.insert(member);
        } else {
            classMemberMapper.updateById(member);
        }
        return toMemberVO(member, target);
    }

    @Transactional
    public ClassMemberVO reviewMember(Long userId, Long classId, Long targetUserId, ReviewClassMemberRequest request) {
        requireActiveRoom(classId);
        requireTeacher(classId, userId);
        ClassMember member = classMemberMapper.selectOne(new LambdaQueryWrapper<ClassMember>()
                .eq(ClassMember::getClassId, classId)
                .eq(ClassMember::getUserId, targetUserId)
                .last("limit 1"));
        if (member == null) {
            throw BusinessException.notFound("成员不存在");
        }
        if (!"pending_teacher_review".equals(member.getStatus())) {
            throw new BusinessException("当前成员不处于待审核状态");
        }
        OffsetDateTime now = OffsetDateTime.now();
        member.setStatus(Boolean.TRUE.equals(request.approved()) ? "active" : "rejected");
        member.setReviewedByUserId(userId);
        member.setReviewedAt(now);
        member.setJoinedAt(Boolean.TRUE.equals(request.approved()) ? now : null);
        classMemberMapper.updateById(member);
        return toMemberVO(member, userMapper.selectById(targetUserId));
    }

    @Transactional
    public ClassMemberVO removeMember(Long userId, Long classId, Long targetUserId) {
        requireActiveRoom(classId);
        ClassMember operator = requireActiveMember(classId, userId);
        ClassMember target = classMemberMapper.selectOne(new LambdaQueryWrapper<ClassMember>()
                .eq(ClassMember::getClassId, classId)
                .eq(ClassMember::getUserId, targetUserId)
                .last("limit 1"));
        if (target == null) {
            throw BusinessException.notFound("成员不存在");
        }
        if (targetUserId.equals(userId)) {
            if ("teacher".equals(target.getMemberRole())) {
                throw new BusinessException("老师暂不能直接退出班级");
            }
            target.setStatus("left");
        } else {
            if (!"teacher".equals(operator.getMemberRole())) {
                throw BusinessException.forbidden(ErrorCode.CLASSROOM_PERMISSION_DENIED, "需要班级老师权限");
            }
            if ("teacher".equals(target.getMemberRole())) {
                throw new BusinessException("不能移除班级老师");
            }
            target.setStatus("removed");
        }
        target.setRemovedAt(OffsetDateTime.now());
        classMemberMapper.updateById(target);
        return toMemberVO(target, userMapper.selectById(targetUserId));
    }

    public List<ClassMemberStatsVO> classStats(Long userId, Long classId) {
        requireActiveRoom(classId);
        ClassMember operator = requireActiveMember(classId, userId);
        List<ClassMember> members;
        if ("teacher".equals(operator.getMemberRole())) {
            members = classMemberMapper.selectList(new LambdaQueryWrapper<ClassMember>()
                    .eq(ClassMember::getClassId, classId)
                    .eq(ClassMember::getStatus, "active")
                    .orderByAsc(ClassMember::getUserId));
        } else {
            members = List.of(operator);
        }
        return buildStats(members);
    }

    public ClassMemberStatsVO memberStats(Long userId, Long classId, Long targetUserId) {
        requireActiveRoom(classId);
        ClassMember operator = requireActiveMember(classId, userId);
        if (!targetUserId.equals(userId) && !"teacher".equals(operator.getMemberRole())) {
            throw BusinessException.forbidden(ErrorCode.CLASSROOM_PERMISSION_DENIED, "只能查看自己的班级统计");
        }
        ClassMember target = classMemberMapper.selectOne(new LambdaQueryWrapper<ClassMember>()
                .eq(ClassMember::getClassId, classId)
                .eq(ClassMember::getUserId, targetUserId)
                .eq(ClassMember::getStatus, "active")
                .last("limit 1"));
        if (target == null) {
            throw BusinessException.notFound("成员不存在");
        }
        return buildStats(List.of(target)).get(0);
    }

    public void requireTeacher(Long classId, Long userId) {
        ClassMember member = classMemberMapper.selectOne(new LambdaQueryWrapper<ClassMember>()
                .eq(ClassMember::getClassId, classId)
                .eq(ClassMember::getUserId, userId)
                .eq(ClassMember::getMemberRole, "teacher")
                .eq(ClassMember::getStatus, "active")
                .last("limit 1"));
        if (member == null) {
            throw BusinessException.forbidden(ErrorCode.CLASSROOM_PERMISSION_DENIED, "需要班级老师权限");
        }
    }

    private ClassRoom requireActiveRoom(Long classId) {
        ClassRoom room = classRoomMapper.selectById(classId);
        if (room == null || !"active".equals(room.getStatus())) {
            throw BusinessException.notFound("班级不存在");
        }
        return room;
    }

    private ClassMember requireKnownMember(Long classId, Long userId) {
        ClassMember member = classMemberMapper.selectOne(new LambdaQueryWrapper<ClassMember>()
                .eq(ClassMember::getClassId, classId)
                .eq(ClassMember::getUserId, userId)
                .in(ClassMember::getStatus, List.of("active", "pending_teacher_review", "invited"))
                .last("limit 1"));
        if (member == null) {
            throw BusinessException.forbidden(ErrorCode.CLASSROOM_PERMISSION_DENIED, "不是班级成员");
        }
        return member;
    }

    private ClassMember requireActiveMember(Long classId, Long userId) {
        ClassMember member = classMemberMapper.selectOne(new LambdaQueryWrapper<ClassMember>()
                .eq(ClassMember::getClassId, classId)
                .eq(ClassMember::getUserId, userId)
                .eq(ClassMember::getStatus, "active")
                .last("limit 1"));
        if (member == null) {
            throw BusinessException.forbidden(ErrorCode.CLASSROOM_PERMISSION_DENIED, "需要班级正式成员权限");
        }
        return member;
    }

    private ClassRoomDetailVO toDetailVO(ClassRoom room, ClassMember member) {
        long activeCount = classMemberMapper.selectCount(new LambdaQueryWrapper<ClassMember>()
                .eq(ClassMember::getClassId, room.getId())
                .eq(ClassMember::getStatus, "active"));
        long pendingCount = classMemberMapper.selectCount(new LambdaQueryWrapper<ClassMember>()
                .eq(ClassMember::getClassId, room.getId())
                .eq(ClassMember::getStatus, "pending_teacher_review"));
        return new ClassRoomDetailVO(
                room.getId(),
                room.getName(),
                room.getDescription(),
                room.getInviteCode(),
                room.getStatus(),
                member.getMemberRole(),
                member.getStatus(),
                activeCount,
                pendingCount
        );
    }

    private ClassRoomVO toVO(ClassRoom room, ClassMember member) {
        return new ClassRoomVO(
                room.getId(),
                room.getName(),
                room.getDescription(),
                room.getInviteCode(),
                room.getStatus(),
                member.getMemberRole(),
                member.getStatus()
        );
    }

    private List<ClassMemberVO> toMemberVOs(List<ClassMember> members) {
        if (members.isEmpty()) {
            return List.of();
        }
        Map<Long, User> usersById = userMapper.selectBatchIds(members.stream().map(ClassMember::getUserId).toList())
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return members.stream()
                .map(member -> toMemberVO(member, usersById.get(member.getUserId())))
                .toList();
    }

    private ClassMemberVO toMemberVO(ClassMember member, User user) {
        return new ClassMemberVO(
                member.getId(),
                member.getClassId(),
                member.getUserId(),
                user == null ? null : user.getEmail(),
                user == null ? null : user.getNickname(),
                member.getMemberRole(),
                member.getStatus(),
                member.getInvitedByUserId(),
                member.getReviewedByUserId(),
                member.getReviewedAt(),
                member.getJoinedAt(),
                member.getRemovedAt()
        );
    }

    private User resolveTargetUser(AddClassMemberRequest request) {
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

    private List<ClassMemberStatsVO> buildStats(List<ClassMember> members) {
        if (members.isEmpty()) {
            return List.of();
        }
        Set<Long> userIds = members.stream().map(ClassMember::getUserId).collect(Collectors.toSet());
        Map<Long, User> usersById = userMapper.selectBatchIds(userIds)
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, List<StudyEvent>> eventsByUserId = studyEventMapper.selectList(new LambdaQueryWrapper<StudyEvent>()
                        .in(StudyEvent::getUserId, userIds)
                        .orderByDesc(StudyEvent::getOccurredAt))
                .stream()
                .collect(Collectors.groupingBy(StudyEvent::getUserId));
        List<ClassMemberStatsVO> stats = new ArrayList<>();
        for (ClassMember member : members) {
            User user = usersById.get(member.getUserId());
            stats.add(toStatsVO(member, user, eventsByUserId.getOrDefault(member.getUserId(), List.of())));
        }
        return stats.stream()
                .sorted(Comparator.comparing(ClassMemberStatsVO::lastStudyAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private ClassMemberStatsVO toStatsVO(ClassMember member, User user, List<StudyEvent> events) {
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
        return new ClassMemberStatsVO(
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
}
