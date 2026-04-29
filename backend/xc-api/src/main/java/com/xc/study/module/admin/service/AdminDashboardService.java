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

    private static final String ACTIVE_MEMBERSHIP_EXISTS_SQL = """
            select 1
            from user_memberships membership
            where membership.user_id = users.id
              and membership.status = 'active'
              and membership.started_at <= {0}
              and membership.ends_at > {0}
            """;
    private static final String TEACHER_MEMBER_EXISTS_SQL = """
            select 1
            from class_members member
            where member.user_id = users.id
              and member.member_role = 'teacher'
            """;
    private static final String ADMIN_USER_EXISTS_SQL = """
            select 1
            from admin_users admin_user
            where lower(admin_user.username) = lower(users.email)
            """;

    private final UserMapper userMapper;
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
                userMapper.selectCount(studentUserWrapper().ne(User::getStatus, "deleted")),
                userMapper.selectCount(studentUserWrapper().eq(User::getStatus, "active")),
                userMapper.selectCount(studentUserWrapper().eq(User::getStatus, "disabled")),
                userMapper.selectCount(studentUserWrapper()
                        .eq(User::getStatus, "active")
                        .gt(User::getTrialEndsAt, now)
                        .notExists(ACTIVE_MEMBERSHIP_EXISTS_SQL, now)),
                userMapper.selectCount(studentUserWrapper()
                        .ne(User::getStatus, "deleted")
                        .ge(User::getCreatedAt, todayStart)),
                userMapper.selectCount(studentUserWrapper()
                        .ne(User::getStatus, "deleted")
                        .exists(ACTIVE_MEMBERSHIP_EXISTS_SQL, now)),
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

    private LambdaQueryWrapper<User> studentUserWrapper() {
        return new LambdaQueryWrapper<User>()
                .notExists(TEACHER_MEMBER_EXISTS_SQL)
                .notExists(ADMIN_USER_EXISTS_SQL);
    }
}
