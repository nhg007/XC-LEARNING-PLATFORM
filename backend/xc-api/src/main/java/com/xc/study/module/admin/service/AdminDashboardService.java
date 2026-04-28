package com.xc.study.module.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xc.study.module.admin.vo.AdminDashboardSummaryVO;
import com.xc.study.module.classroom.entity.ClassMember;
import com.xc.study.module.classroom.entity.ClassRoom;
import com.xc.study.module.classroom.mapper.ClassMemberMapper;
import com.xc.study.module.classroom.mapper.ClassRoomMapper;
import com.xc.study.module.dialogue.entity.DialogueLine;
import com.xc.study.module.dialogue.entity.VideoMaterial;
import com.xc.study.module.dialogue.mapper.DialogueLineMapper;
import com.xc.study.module.dialogue.mapper.VideoMaterialMapper;
import com.xc.study.module.exercise.entity.ExerciseSet;
import com.xc.study.module.exercise.entity.SentenceExercise;
import com.xc.study.module.exercise.mapper.ExerciseSetMapper;
import com.xc.study.module.exercise.mapper.SentenceExerciseMapper;
import com.xc.study.module.membership.entity.MembershipPlan;
import com.xc.study.module.membership.mapper.MembershipPlanMapper;
import com.xc.study.module.membership.entity.UserMembership;
import com.xc.study.module.membership.mapper.UserMembershipMapper;
import com.xc.study.module.payment.entity.PaymentOrder;
import com.xc.study.module.payment.mapper.PaymentOrderMapper;
import com.xc.study.module.stats.entity.StudyEvent;
import com.xc.study.module.stats.mapper.StudyEventMapper;
import com.xc.study.module.user.entity.User;
import com.xc.study.module.user.mapper.UserMapper;
import com.xc.study.module.vocab.entity.VocabList;
import com.xc.study.module.vocab.entity.VocabItem;
import com.xc.study.module.vocab.mapper.VocabItemMapper;
import com.xc.study.module.vocab.mapper.VocabListMapper;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Service;

@Service
public class AdminDashboardService {

    private final UserMapper userMapper;
    private final UserMembershipMapper userMembershipMapper;
    private final MembershipPlanMapper membershipPlanMapper;
    private final PaymentOrderMapper paymentOrderMapper;
    private final ClassRoomMapper classRoomMapper;
    private final ClassMemberMapper classMemberMapper;
    private final StudyEventMapper studyEventMapper;
    private final VocabListMapper vocabListMapper;
    private final VocabItemMapper vocabItemMapper;
    private final ExerciseSetMapper exerciseSetMapper;
    private final SentenceExerciseMapper sentenceExerciseMapper;
    private final VideoMaterialMapper videoMaterialMapper;
    private final DialogueLineMapper dialogueLineMapper;

    public AdminDashboardService(
            UserMapper userMapper,
            UserMembershipMapper userMembershipMapper,
            MembershipPlanMapper membershipPlanMapper,
            PaymentOrderMapper paymentOrderMapper,
            ClassRoomMapper classRoomMapper,
            ClassMemberMapper classMemberMapper,
            StudyEventMapper studyEventMapper,
            VocabListMapper vocabListMapper,
            VocabItemMapper vocabItemMapper,
            ExerciseSetMapper exerciseSetMapper,
            SentenceExerciseMapper sentenceExerciseMapper,
            VideoMaterialMapper videoMaterialMapper,
            DialogueLineMapper dialogueLineMapper
    ) {
        this.userMapper = userMapper;
        this.userMembershipMapper = userMembershipMapper;
        this.membershipPlanMapper = membershipPlanMapper;
        this.paymentOrderMapper = paymentOrderMapper;
        this.classRoomMapper = classRoomMapper;
        this.classMemberMapper = classMemberMapper;
        this.studyEventMapper = studyEventMapper;
        this.vocabListMapper = vocabListMapper;
        this.vocabItemMapper = vocabItemMapper;
        this.exerciseSetMapper = exerciseSetMapper;
        this.sentenceExerciseMapper = sentenceExerciseMapper;
        this.videoMaterialMapper = videoMaterialMapper;
        this.dialogueLineMapper = dialogueLineMapper;
    }

    public AdminDashboardSummaryVO summary() {
        OffsetDateTime now = OffsetDateTime.now();
        ZoneId zoneId = ZoneId.systemDefault();
        OffsetDateTime todayStart = LocalDate.now(zoneId).atStartOfDay(zoneId).toOffsetDateTime();
        return new AdminDashboardSummaryVO(
                userMapper.selectCount(new LambdaQueryWrapper<User>().ne(User::getStatus, "deleted")),
                userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getStatus, "active")),
                userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getStatus, "disabled")),
                userMapper.selectCount(new LambdaQueryWrapper<User>()
                        .eq(User::getStatus, "active")
                        .gt(User::getTrialEndsAt, now)),
                userMapper.selectCount(new LambdaQueryWrapper<User>()
                        .ne(User::getStatus, "deleted")
                        .ge(User::getCreatedAt, todayStart)),
                userMembershipMapper.selectCount(new LambdaQueryWrapper<UserMembership>()
                        .eq(UserMembership::getStatus, "active")
                        .le(UserMembership::getStartedAt, now)
                        .gt(UserMembership::getEndsAt, now)),
                membershipPlanMapper.selectCount(new LambdaQueryWrapper<MembershipPlan>().eq(MembershipPlan::getStatus, "active")),
                paymentOrderMapper.selectCount(new LambdaQueryWrapper<PaymentOrder>().ge(PaymentOrder::getCreatedAt, todayStart)),
                paymentOrderMapper.selectCount(new LambdaQueryWrapper<PaymentOrder>().eq(PaymentOrder::getStatus, "pending")),
                paymentOrderMapper.selectCount(new LambdaQueryWrapper<PaymentOrder>().eq(PaymentOrder::getStatus, "paid")),
                paymentOrderMapper.sumPaidAmountFrom(todayStart),
                classRoomMapper.selectCount(new LambdaQueryWrapper<ClassRoom>().eq(ClassRoom::getStatus, "active")),
                classMemberMapper.selectCount(new LambdaQueryWrapper<ClassMember>().eq(ClassMember::getStatus, "active")),
                classMemberMapper.selectCount(new LambdaQueryWrapper<ClassMember>().eq(ClassMember::getStatus, "pending_teacher_review")),
                classMemberMapper.countActiveClassesFrom(todayStart),
                studyEventMapper.selectCount(new LambdaQueryWrapper<StudyEvent>().ge(StudyEvent::getOccurredAt, todayStart)),
                vocabListMapper.selectCount(new LambdaQueryWrapper<VocabList>().eq(VocabList::getStatus, "active")),
                vocabListMapper.selectCount(new LambdaQueryWrapper<VocabList>().eq(VocabList::getStatus, "inactive")),
                vocabItemMapper.selectCount(new LambdaQueryWrapper<VocabItem>().eq(VocabItem::getStatus, "active")),
                exerciseSetMapper.selectCount(new LambdaQueryWrapper<ExerciseSet>().eq(ExerciseSet::getStatus, "active")),
                exerciseSetMapper.selectCount(new LambdaQueryWrapper<ExerciseSet>().eq(ExerciseSet::getStatus, "inactive")),
                sentenceExerciseMapper.selectCount(new LambdaQueryWrapper<SentenceExercise>().eq(SentenceExercise::getStatus, "active")),
                videoMaterialMapper.selectCount(new LambdaQueryWrapper<VideoMaterial>().eq(VideoMaterial::getStatus, "active")),
                videoMaterialMapper.selectCount(new LambdaQueryWrapper<VideoMaterial>().eq(VideoMaterial::getStatus, "inactive")),
                dialogueLineMapper.selectCount(new LambdaQueryWrapper<DialogueLine>())
        );
    }
}
