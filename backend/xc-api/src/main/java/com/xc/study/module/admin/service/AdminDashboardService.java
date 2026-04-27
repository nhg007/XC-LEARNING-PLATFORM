package com.xc.study.module.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xc.study.module.admin.vo.AdminDashboardSummaryVO;
import com.xc.study.module.classroom.entity.ClassRoom;
import com.xc.study.module.classroom.mapper.ClassRoomMapper;
import com.xc.study.module.exercise.entity.ExerciseSet;
import com.xc.study.module.exercise.mapper.ExerciseSetMapper;
import com.xc.study.module.membership.entity.UserMembership;
import com.xc.study.module.membership.mapper.UserMembershipMapper;
import com.xc.study.module.stats.entity.StudyEvent;
import com.xc.study.module.stats.mapper.StudyEventMapper;
import com.xc.study.module.user.entity.User;
import com.xc.study.module.user.mapper.UserMapper;
import com.xc.study.module.vocab.entity.VocabList;
import com.xc.study.module.vocab.mapper.VocabListMapper;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;

@Service
public class AdminDashboardService {

    private final UserMapper userMapper;
    private final UserMembershipMapper userMembershipMapper;
    private final ClassRoomMapper classRoomMapper;
    private final StudyEventMapper studyEventMapper;
    private final VocabListMapper vocabListMapper;
    private final ExerciseSetMapper exerciseSetMapper;

    public AdminDashboardService(
            UserMapper userMapper,
            UserMembershipMapper userMembershipMapper,
            ClassRoomMapper classRoomMapper,
            StudyEventMapper studyEventMapper,
            VocabListMapper vocabListMapper,
            ExerciseSetMapper exerciseSetMapper
    ) {
        this.userMapper = userMapper;
        this.userMembershipMapper = userMembershipMapper;
        this.classRoomMapper = classRoomMapper;
        this.studyEventMapper = studyEventMapper;
        this.vocabListMapper = vocabListMapper;
        this.exerciseSetMapper = exerciseSetMapper;
    }

    public AdminDashboardSummaryVO summary() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime todayStart = LocalDate.now().atStartOfDay().atOffset(ZoneOffset.systemDefault().getRules().getOffset(now.toInstant()));
        return new AdminDashboardSummaryVO(
                userMapper.selectCount(new LambdaQueryWrapper<User>().ne(User::getStatus, "deleted")),
                userMembershipMapper.selectCount(new LambdaQueryWrapper<UserMembership>()
                        .eq(UserMembership::getStatus, "active")
                        .le(UserMembership::getStartedAt, now)
                        .gt(UserMembership::getEndsAt, now)),
                classRoomMapper.selectCount(new LambdaQueryWrapper<ClassRoom>().eq(ClassRoom::getStatus, "active")),
                studyEventMapper.selectCount(new LambdaQueryWrapper<StudyEvent>().ge(StudyEvent::getOccurredAt, todayStart)),
                vocabListMapper.selectCount(new LambdaQueryWrapper<VocabList>().eq(VocabList::getStatus, "active")),
                exerciseSetMapper.selectCount(new LambdaQueryWrapper<ExerciseSet>().eq(ExerciseSet::getStatus, "active"))
        );
    }
}
