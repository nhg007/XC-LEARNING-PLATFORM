package com.xc.study.module.classroom.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xc.study.common.BusinessException;
import com.xc.study.common.ErrorCode;
import com.xc.study.module.classroom.dto.CreateClassRoomRequest;
import com.xc.study.module.classroom.dto.JoinClassRoomRequest;
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
import com.xc.study.security.CurrentUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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
                .eq(ClassMember::getStatus, "active")
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
    public ClassRoomVO create(CurrentUser currentUser, CreateClassRoomRequest request) {
        User owner = userMapper.selectById(currentUser.id());
        if (owner == null || !"active".equals(owner.getStatus())) {
            throw BusinessException.forbidden(ErrorCode.CLASSROOM_PERMISSION_DENIED, "账号不可创建班级");
        }
        OffsetDateTime now = OffsetDateTime.now();
        ClassRoom room = new ClassRoom();
        room.setName(request.name().trim());
        room.setOwnerUserId(currentUser.id());
        room.setDescription(StringUtils.hasText(request.description()) ? request.description().trim() : null);
        room.setInviteCode(generateUniqueInviteCode());
        room.setStatus("active");
        classRoomMapper.insert(room);

        ClassMember member = new ClassMember();
        member.setClassId(room.getId());
        member.setUserId(currentUser.id());
        member.setStatus("active");
        member.setJoinedAt(now);
        classMemberMapper.insert(member);
        return toVO(room, member);
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
        ClassMember member = exists == null ? new ClassMember() : exists;
        member.setClassId(room.getId());
        member.setUserId(userId);
        member.setStatus("active");
        member.setJoinedAt(OffsetDateTime.now());
        member.setRemovedAt(null);
        if (member.getId() == null) {
            classMemberMapper.insert(member);
        } else {
            classMemberMapper.updateById(member);
        }
        return toVO(room, member);
    }

    public ClassRoomDetailVO detail(Long userId, Long classId) {
        ClassRoom room = requireActiveRoom(classId);
        ClassMember member = requireActiveMember(classId, userId);
        return toDetailVO(room, member);
    }

    public List<ClassMemberVO> members(Long userId, Long classId) {
        requireActiveRoom(classId);
        requireActiveMember(classId, userId);
        List<ClassMember> members = classMemberMapper.selectList(new LambdaQueryWrapper<ClassMember>()
                .eq(ClassMember::getClassId, classId)
                .eq(ClassMember::getStatus, "active")
                .orderByAsc(ClassMember::getUserId));
        return toMemberVOs(members);
    }

    public List<ClassMemberStatsVO> classStats(Long userId, Long classId) {
        ClassRoom room = requireActiveRoom(classId);
        requireActiveMember(classId, userId);
        requireClassOwner(room, userId);
        List<ClassMember> members = classMemberMapper.selectList(new LambdaQueryWrapper<ClassMember>()
                .eq(ClassMember::getClassId, classId)
                .eq(ClassMember::getStatus, "active")
                .orderByAsc(ClassMember::getUserId));
        return buildStats(members);
    }

    public ClassMemberStatsVO memberStats(Long userId, Long classId, Long targetUserId) {
        ClassRoom room = requireActiveRoom(classId);
        requireActiveMember(classId, userId);
        requireClassOwner(room, userId);
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

    private ClassRoom requireActiveRoom(Long classId) {
        ClassRoom room = classRoomMapper.selectById(classId);
        if (room == null || !"active".equals(room.getStatus())) {
            throw BusinessException.notFound("班级不存在");
        }
        return room;
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

    private void requireClassOwner(ClassRoom room, Long userId) {
        if (!Objects.equals(room.getOwnerUserId(), userId)) {
            throw BusinessException.forbidden(ErrorCode.CLASSROOM_PERMISSION_DENIED, "只有班级创建者可以查看成员学习情况");
        }
    }

    private ClassRoomDetailVO toDetailVO(ClassRoom room, ClassMember member) {
        User owner = ownerUser(room);
        long activeCount = classMemberMapper.selectCount(new LambdaQueryWrapper<ClassMember>()
                .eq(ClassMember::getClassId, room.getId())
                .eq(ClassMember::getStatus, "active"));
        return new ClassRoomDetailVO(
                room.getId(),
                room.getName(),
                room.getDescription(),
                room.getInviteCode(),
                ownerName(owner),
                ownerContact(owner),
                room.getOwnerUserId(),
                Objects.equals(room.getOwnerUserId(), member.getUserId()),
                room.getStatus(),
                member.getStatus(),
                activeCount
        );
    }

    private ClassRoomVO toVO(ClassRoom room, ClassMember member) {
        User owner = ownerUser(room);
        return new ClassRoomVO(
                room.getId(),
                room.getName(),
                room.getDescription(),
                room.getInviteCode(),
                ownerName(owner),
                ownerContact(owner),
                room.getOwnerUserId(),
                Objects.equals(room.getOwnerUserId(), member.getUserId()),
                room.getStatus(),
                member.getStatus()
        );
    }

    private User ownerUser(ClassRoom room) {
        if (room.getOwnerUserId() == null) {
            return null;
        }
        return userMapper.selectById(room.getOwnerUserId());
    }

    private String ownerName(User owner) {
        if (owner == null) {
            return null;
        }
        return StringUtils.hasText(owner.getNickname()) ? owner.getNickname() : owner.getEmail();
    }

    private String ownerContact(User owner) {
        return owner == null ? null : owner.getEmail();
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
                member.getStatus(),
                member.getJoinedAt(),
                member.getRemovedAt()
        );
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
