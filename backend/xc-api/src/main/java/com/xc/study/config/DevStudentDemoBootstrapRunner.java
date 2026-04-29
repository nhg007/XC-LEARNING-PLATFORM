package com.xc.study.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xc.study.module.classroom.entity.ClassMember;
import com.xc.study.module.classroom.entity.ClassRoom;
import com.xc.study.module.classroom.mapper.ClassMemberMapper;
import com.xc.study.module.classroom.mapper.ClassRoomMapper;
import com.xc.study.module.dialogue.entity.DialogueLine;
import com.xc.study.module.exercise.entity.SentenceExercise;
import com.xc.study.module.membership.entity.UserMembership;
import com.xc.study.module.membership.mapper.UserMembershipMapper;
import com.xc.study.module.stats.entity.StudyEvent;
import com.xc.study.module.stats.mapper.StudyEventMapper;
import com.xc.study.module.stats.service.LearningStatsRecorder;
import com.xc.study.module.user.entity.User;
import com.xc.study.module.user.entity.UserPreference;
import com.xc.study.module.user.mapper.UserMapper;
import com.xc.study.module.user.mapper.UserPreferenceMapper;
import com.xc.study.module.vocab.entity.UserVocabFavorite;
import com.xc.study.module.vocab.entity.UserVocabProgress;
import com.xc.study.module.vocab.entity.VocabItem;
import com.xc.study.module.vocab.mapper.UserVocabFavoriteMapper;
import com.xc.study.module.vocab.mapper.UserVocabProgressMapper;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
@Profile("dev")
public class DevStudentDemoBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevStudentDemoBootstrapRunner.class);
    private static final String DEMO_CLASS_INVITE_CODE = "DEMO2026";

    private final UserMapper userMapper;
    private final UserPreferenceMapper userPreferenceMapper;
    private final UserMembershipMapper userMembershipMapper;
    private final UserVocabProgressMapper userVocabProgressMapper;
    private final UserVocabFavoriteMapper userVocabFavoriteMapper;
    private final ClassRoomMapper classRoomMapper;
    private final ClassMemberMapper classMemberMapper;
    private final StudyEventMapper studyEventMapper;
    private final LearningStatsRecorder learningStatsRecorder;
    private final PasswordEncoder passwordEncoder;
    private final DevDemoContentSeeder demoContentSeeder;
    private final boolean enabled;
    private final String email;
    private final String password;
    private final String nickname;

    public DevStudentDemoBootstrapRunner(
            UserMapper userMapper,
            UserPreferenceMapper userPreferenceMapper,
            UserMembershipMapper userMembershipMapper,
            UserVocabProgressMapper userVocabProgressMapper,
            UserVocabFavoriteMapper userVocabFavoriteMapper,
            ClassRoomMapper classRoomMapper,
            ClassMemberMapper classMemberMapper,
            StudyEventMapper studyEventMapper,
            LearningStatsRecorder learningStatsRecorder,
            PasswordEncoder passwordEncoder,
            DevDemoContentSeeder demoContentSeeder,
            @Value("${app.bootstrap.student-demo.enabled:false}") boolean enabled,
            @Value("${app.bootstrap.student-demo.email:}") String email,
            @Value("${app.bootstrap.student-demo.password:}") String password,
            @Value("${app.bootstrap.student-demo.nickname:Demo Student}") String nickname
    ) {
        this.userMapper = userMapper;
        this.userPreferenceMapper = userPreferenceMapper;
        this.userMembershipMapper = userMembershipMapper;
        this.userVocabProgressMapper = userVocabProgressMapper;
        this.userVocabFavoriteMapper = userVocabFavoriteMapper;
        this.classRoomMapper = classRoomMapper;
        this.classMemberMapper = classMemberMapper;
        this.studyEventMapper = studyEventMapper;
        this.learningStatsRecorder = learningStatsRecorder;
        this.passwordEncoder = passwordEncoder;
        this.demoContentSeeder = demoContentSeeder;
        this.enabled = enabled;
        this.email = email;
        this.password = password;
        this.nickname = nickname;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            log.warn("Development student demo bootstrap is enabled but email or password is blank.");
            return;
        }
        if (password.length() < 8) {
            log.warn("Development student demo password must be at least 8 characters.");
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        User demoUser = ensureStudent(now);
        ensurePreference(demoUser.getId());
        ensureMembership(demoUser.getId(), now);

        DevDemoContentSeeder.DemoContent demoContent = demoContentSeeder.ensureContent();
        List<VocabItem> vocabItems = demoContent.vocabItems();
        ensureVocabProgress(demoUser.getId(), demoContent.vocabList().getId(), vocabItems);
        ensureFavorites(demoUser.getId(), vocabItems);

        ensureClassRoom(demoUser.getId());
        ensureStudyEvents(demoUser.getId(), vocabItems, demoContent.exercises(), demoContent.dialogueLines());
        learningStatsRecorder.rebuildUserStats(demoUser.getId());
        learningStatsRecorder.refreshLeaderboards(LocalDate.now());

        log.info("Development student demo data is ready for '{}'.", email);
    }

    private User ensureStudent(OffsetDateTime now) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email)
                .last("limit 1"));
        if (user == null) {
            user = new User();
            user.setEmail(email);
            user.setCreatedAt(now);
        }
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setNickname(StringUtils.hasText(nickname) ? nickname : "Demo Student");
        user.setStatus("active");
        user.setTrialStartedAt(now.minusDays(3));
        user.setTrialEndsAt(now.plusDays(30));
        user.setUpdatedAt(now);
        if (user.getId() == null) {
            userMapper.insert(user);
        } else {
            userMapper.updateById(user);
        }
        return user;
    }

    private void ensurePreference(Long userId) {
        UserPreference preference = userPreferenceMapper.selectOne(new LambdaQueryWrapper<UserPreference>()
                .eq(UserPreference::getUserId, userId)
                .last("limit 1"));
        if (preference != null) {
            return;
        }
        preference = new UserPreference();
        preference.setUserId(userId);
        preference.setUiLanguage("zh");
        preference.setTranslationLanguage("ru");
        preference.setVocabMeaningLanguage("ru");
        preference.setMatchingMeaningLanguage("ru");
        preference.setSoundEnabled(true);
        userPreferenceMapper.insert(preference);
    }

    private void ensureMembership(Long userId, OffsetDateTime now) {
        UserMembership membership = userMembershipMapper.selectOne(new LambdaQueryWrapper<UserMembership>()
                .eq(UserMembership::getUserId, userId)
                .eq(UserMembership::getStatus, "active")
                .last("limit 1"));
        if (membership == null) {
            membership = new UserMembership();
            membership.setUserId(userId);
            membership.setStartedAt(now.minusDays(1));
            membership.setEndsAt(now.plusDays(90));
            membership.setSource("admin_adjustment");
            membership.setAdjustReason("Dev demo student access");
            membership.setStatus("active");
            userMembershipMapper.insert(membership);
            return;
        }
        if (membership.getEndsAt() == null || membership.getEndsAt().isBefore(now.plusDays(30))) {
            membership.setEndsAt(now.plusDays(90));
            membership.setUpdatedAt(now);
            userMembershipMapper.updateById(membership);
        }
    }

    private void ensureVocabProgress(Long userId, Long vocabListId, List<VocabItem> items) {
        if (items.isEmpty()) {
            return;
        }
        UserVocabProgress progress = userVocabProgressMapper.selectOne(new LambdaQueryWrapper<UserVocabProgress>()
                .eq(UserVocabProgress::getUserId, userId)
                .eq(UserVocabProgress::getVocabListId, vocabListId)
                .last("limit 1"));
        if (progress != null) {
            return;
        }
        progress = new UserVocabProgress();
        progress.setUserId(userId);
        progress.setVocabListId(vocabListId);
        progress.setCurrentIndex(Math.min(3, items.size()));
        progress.setLastVocabItemId(items.get(Math.min(2, items.size() - 1)).getId());
        progress.setReviewedCount(Math.min(3, items.size()));
        userVocabProgressMapper.insert(progress);
    }

    private void ensureFavorites(Long userId, List<VocabItem> items) {
        for (int i = 0; i < Math.min(2, items.size()); i++) {
            Long itemId = items.get(i).getId();
            UserVocabFavorite favorite = userVocabFavoriteMapper.selectOne(new LambdaQueryWrapper<UserVocabFavorite>()
                    .eq(UserVocabFavorite::getUserId, userId)
                    .eq(UserVocabFavorite::getVocabItemId, itemId)
                    .last("limit 1"));
            if (favorite == null) {
                favorite = new UserVocabFavorite();
                favorite.setUserId(userId);
                favorite.setVocabItemId(itemId);
                userVocabFavoriteMapper.insert(favorite);
            }
        }
    }

    private void ensureClassRoom(Long userId) {
        ClassRoom room = classRoomMapper.selectOne(new LambdaQueryWrapper<ClassRoom>()
                .eq(ClassRoom::getInviteCode, DEMO_CLASS_INVITE_CODE)
                .last("limit 1"));
        if (room == null) {
            room = new ClassRoom();
            room.setName("Demo HSK1 学习班");
            room.setOwnerUserId(userId);
            room.setDescription("开发环境演示班级，包含成员和学习统计。");
            room.setInviteCode(DEMO_CLASS_INVITE_CODE);
            room.setStatus("active");
            classRoomMapper.insert(room);
        }

        ClassMember member = classMemberMapper.selectOne(new LambdaQueryWrapper<ClassMember>()
                .eq(ClassMember::getClassId, room.getId())
                .eq(ClassMember::getUserId, userId)
                .last("limit 1"));
        if (member != null) {
            return;
        }
        OffsetDateTime now = OffsetDateTime.now();
        member = new ClassMember();
        member.setClassId(room.getId());
        member.setUserId(userId);
        member.setMemberRole("teacher");
        member.setStatus("active");
        member.setInvitedByUserId(userId);
        member.setReviewedByUserId(userId);
        member.setReviewedAt(now);
        member.setJoinedAt(now);
        classMemberMapper.insert(member);
    }

    private void ensureStudyEvents(Long userId, List<VocabItem> vocabItems, List<SentenceExercise> exercises, List<DialogueLine> dialogueLines) {
        Long existingCount = studyEventMapper.selectCount(new LambdaQueryWrapper<StudyEvent>()
                .eq(StudyEvent::getUserId, userId));
        if (existingCount > 0) {
            learningStatsRecorder.rebuildUserStats(userId);
            learningStatsRecorder.refreshLeaderboards(LocalDate.now());
            return;
        }
        if (vocabItems.isEmpty() || exercises.isEmpty() || dialogueLines.isEmpty()) {
            return;
        }

        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            OffsetDateTime baseTime = today.minusDays(i).atTime(19, 30).atZone(ZoneId.systemDefault()).toOffsetDateTime();
            createStudyEvent(userId, "vocab", vocabItems.get(i % vocabItems.size()).getId(), "completed", 180 + i * 10, baseTime);
            createStudyEvent(userId, "exercise", exercises.get(i % exercises.size()).getId(), i % 4 == 0 ? "wrong" : "correct", 240 + i * 12, baseTime.plusMinutes(8));
            createStudyEvent(userId, "dialogue", dialogueLines.get(i % dialogueLines.size()).getId(), i % 5 == 0 ? "wrong" : "correct", 160 + i * 8, baseTime.plusMinutes(16));
        }
    }

    private void createStudyEvent(Long userId, String eventType, Long targetId, String result, int durationSeconds, OffsetDateTime occurredAt) {
        learningStatsRecorder.recordEvent(userId, eventType, targetId, result, durationSeconds, occurredAt);
    }
}
