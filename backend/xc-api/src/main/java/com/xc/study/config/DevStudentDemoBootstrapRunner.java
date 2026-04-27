package com.xc.study.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xc.study.module.classroom.entity.ClassMember;
import com.xc.study.module.classroom.entity.ClassRoom;
import com.xc.study.module.classroom.mapper.ClassMemberMapper;
import com.xc.study.module.classroom.mapper.ClassRoomMapper;
import com.xc.study.module.exercise.entity.ExerciseSet;
import com.xc.study.module.exercise.entity.SentenceExercise;
import com.xc.study.module.exercise.entity.SentenceWordOption;
import com.xc.study.module.exercise.mapper.ExerciseSetMapper;
import com.xc.study.module.exercise.mapper.SentenceExerciseMapper;
import com.xc.study.module.exercise.mapper.SentenceWordOptionMapper;
import com.xc.study.module.membership.entity.UserMembership;
import com.xc.study.module.membership.mapper.UserMembershipMapper;
import com.xc.study.module.stats.entity.StudyEvent;
import com.xc.study.module.stats.mapper.StudyEventMapper;
import com.xc.study.module.user.entity.User;
import com.xc.study.module.user.entity.UserPreference;
import com.xc.study.module.user.mapper.UserMapper;
import com.xc.study.module.user.mapper.UserPreferenceMapper;
import com.xc.study.module.vocab.entity.UserVocabFavorite;
import com.xc.study.module.vocab.entity.UserVocabProgress;
import com.xc.study.module.vocab.entity.VocabItem;
import com.xc.study.module.vocab.entity.VocabList;
import com.xc.study.module.vocab.mapper.UserVocabFavoriteMapper;
import com.xc.study.module.vocab.mapper.UserVocabProgressMapper;
import com.xc.study.module.vocab.mapper.VocabItemMapper;
import com.xc.study.module.vocab.mapper.VocabListMapper;
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
    private final VocabListMapper vocabListMapper;
    private final VocabItemMapper vocabItemMapper;
    private final UserVocabProgressMapper userVocabProgressMapper;
    private final UserVocabFavoriteMapper userVocabFavoriteMapper;
    private final ExerciseSetMapper exerciseSetMapper;
    private final SentenceExerciseMapper sentenceExerciseMapper;
    private final SentenceWordOptionMapper sentenceWordOptionMapper;
    private final ClassRoomMapper classRoomMapper;
    private final ClassMemberMapper classMemberMapper;
    private final StudyEventMapper studyEventMapper;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;
    private final String email;
    private final String password;
    private final String nickname;

    public DevStudentDemoBootstrapRunner(
            UserMapper userMapper,
            UserPreferenceMapper userPreferenceMapper,
            UserMembershipMapper userMembershipMapper,
            VocabListMapper vocabListMapper,
            VocabItemMapper vocabItemMapper,
            UserVocabProgressMapper userVocabProgressMapper,
            UserVocabFavoriteMapper userVocabFavoriteMapper,
            ExerciseSetMapper exerciseSetMapper,
            SentenceExerciseMapper sentenceExerciseMapper,
            SentenceWordOptionMapper sentenceWordOptionMapper,
            ClassRoomMapper classRoomMapper,
            ClassMemberMapper classMemberMapper,
            StudyEventMapper studyEventMapper,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap.student-demo.enabled:false}") boolean enabled,
            @Value("${app.bootstrap.student-demo.email:}") String email,
            @Value("${app.bootstrap.student-demo.password:}") String password,
            @Value("${app.bootstrap.student-demo.nickname:Demo Student}") String nickname
    ) {
        this.userMapper = userMapper;
        this.userPreferenceMapper = userPreferenceMapper;
        this.userMembershipMapper = userMembershipMapper;
        this.vocabListMapper = vocabListMapper;
        this.vocabItemMapper = vocabItemMapper;
        this.userVocabProgressMapper = userVocabProgressMapper;
        this.userVocabFavoriteMapper = userVocabFavoriteMapper;
        this.exerciseSetMapper = exerciseSetMapper;
        this.sentenceExerciseMapper = sentenceExerciseMapper;
        this.sentenceWordOptionMapper = sentenceWordOptionMapper;
        this.classRoomMapper = classRoomMapper;
        this.classMemberMapper = classMemberMapper;
        this.studyEventMapper = studyEventMapper;
        this.passwordEncoder = passwordEncoder;
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

        VocabList vocabList = ensureVocabList();
        List<VocabItem> vocabItems = ensureVocabItems(vocabList.getId());
        ensureVocabProgress(demoUser.getId(), vocabList.getId(), vocabItems);
        ensureFavorites(demoUser.getId(), vocabItems);

        ExerciseSet exerciseSet = ensureExerciseSet();
        List<SentenceExercise> exercises = ensureExercises(exerciseSet.getId());
        ensureClassRoom(demoUser.getId());
        ensureStudyEvents(demoUser.getId(), vocabItems, exercises);

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

    private VocabList ensureVocabList() {
        VocabList list = vocabListMapper.selectOne(new LambdaQueryWrapper<VocabList>()
                .eq(VocabList::getName, "Demo HSK1 基础词汇")
                .eq(VocabList::getListType, "HSK")
                .eq(VocabList::getLevel, "HSK1")
                .last("limit 1"));
        if (list != null) {
            return list;
        }
        list = new VocabList();
        list.setName("Demo HSK1 基础词汇");
        list.setListType("HSK");
        list.setLevel("HSK1");
        list.setDescription("开发环境演示词表：问候、人物、日常表达。");
        list.setSortOrder(1);
        list.setStatus("active");
        vocabListMapper.insert(list);
        return list;
    }

    private List<VocabItem> ensureVocabItems(Long vocabListId) {
        return List.of(
                ensureVocabItem(vocabListId, "你好", "nǐ hǎo", "hello", "привет", "你好，我是安娜。", 1),
                ensureVocabItem(vocabListId, "谢谢", "xiè xie", "thank you", "спасибо", "谢谢你的帮助。", 2),
                ensureVocabItem(vocabListId, "学生", "xué sheng", "student", "студент", "我是汉语学生。", 3),
                ensureVocabItem(vocabListId, "老师", "lǎo shī", "teacher", "учитель", "王老师很好。", 4),
                ensureVocabItem(vocabListId, "学习", "xué xí", "to study", "учиться", "我每天学习汉语。", 5),
                ensureVocabItem(vocabListId, "朋友", "péng you", "friend", "друг", "他是我的朋友。", 6)
        );
    }

    private VocabItem ensureVocabItem(
            Long vocabListId,
            String hanzi,
            String pinyin,
            String meaningEn,
            String meaningRu,
            String exampleSentence,
            int sortOrder
    ) {
        VocabItem item = vocabItemMapper.selectOne(new LambdaQueryWrapper<VocabItem>()
                .eq(VocabItem::getVocabListId, vocabListId)
                .eq(VocabItem::getHanzi, hanzi)
                .last("limit 1"));
        if (item != null) {
            return item;
        }
        item = new VocabItem();
        item.setVocabListId(vocabListId);
        item.setHanzi(hanzi);
        item.setPinyin(pinyin);
        item.setMeaningEn(meaningEn);
        item.setMeaningRu(meaningRu);
        item.setExampleSentence(exampleSentence);
        item.setSortOrder(sortOrder);
        item.setStatus("active");
        vocabItemMapper.insert(item);
        return item;
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

    private ExerciseSet ensureExerciseSet() {
        ExerciseSet set = exerciseSetMapper.selectOne(new LambdaQueryWrapper<ExerciseSet>()
                .eq(ExerciseSet::getTitle, "Demo HSK1 句子排序")
                .eq(ExerciseSet::getExerciseType, "translation_order")
                .eq(ExerciseSet::getLevel, "HSK1")
                .last("limit 1"));
        if (set != null) {
            return set;
        }
        set = new ExerciseSet();
        set.setTitle("Demo HSK1 句子排序");
        set.setExerciseType("translation_order");
        set.setLevel("HSK1");
        set.setStatus("active");
        exerciseSetMapper.insert(set);
        return set;
    }

    private List<SentenceExercise> ensureExercises(Long exerciseSetId) {
        return List.of(
                ensureExercise(exerciseSetId, "我是学生", "wǒ shì xué sheng", "I am a student.", "Я студент.", 1, List.of("我", "是", "学生")),
                ensureExercise(exerciseSetId, "我学习汉语", "wǒ xué xí hàn yǔ", "I study Chinese.", "Я изучаю китайский.", 2, List.of("我", "学习", "汉语")),
                ensureExercise(exerciseSetId, "老师很好", "lǎo shī hěn hǎo", "The teacher is very good.", "Учитель очень хороший.", 3, List.of("老师", "很", "好"))
        );
    }

    private SentenceExercise ensureExercise(
            Long exerciseSetId,
            String answer,
            String pinyin,
            String translationEn,
            String translationRu,
            int sortOrder,
            List<String> words
    ) {
        SentenceExercise exercise = sentenceExerciseMapper.selectOne(new LambdaQueryWrapper<SentenceExercise>()
                .eq(SentenceExercise::getExerciseSetId, exerciseSetId)
                .eq(SentenceExercise::getHanziAnswer, answer)
                .last("limit 1"));
        if (exercise == null) {
            exercise = new SentenceExercise();
            exercise.setExerciseSetId(exerciseSetId);
            exercise.setExerciseType("translation_order");
            exercise.setHanziAnswer(answer);
            exercise.setPinyinPrompt(pinyin);
            exercise.setTranslationEn(translationEn);
            exercise.setTranslationRu(translationRu);
            exercise.setExplanation("按正确语序排列词块。");
            exercise.setSortOrder(sortOrder);
            exercise.setStatus("active");
            sentenceExerciseMapper.insert(exercise);
        }
        ensureWordOptions(exercise.getId(), words);
        return exercise;
    }

    private void ensureWordOptions(Long exerciseId, List<String> words) {
        Long count = sentenceWordOptionMapper.selectCount(new LambdaQueryWrapper<SentenceWordOption>()
                .eq(SentenceWordOption::getExerciseId, exerciseId));
        if (count > 0) {
            return;
        }
        for (int i = 0; i < words.size(); i++) {
            SentenceWordOption option = new SentenceWordOption();
            option.setExerciseId(exerciseId);
            option.setWordText(words.get(i));
            option.setCorrectOrder(i + 1);
            sentenceWordOptionMapper.insert(option);
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

    private void ensureStudyEvents(Long userId, List<VocabItem> vocabItems, List<SentenceExercise> exercises) {
        Long existingCount = studyEventMapper.selectCount(new LambdaQueryWrapper<StudyEvent>()
                .eq(StudyEvent::getUserId, userId));
        if (existingCount > 0 || vocabItems.isEmpty() || exercises.isEmpty()) {
            return;
        }

        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            OffsetDateTime baseTime = today.minusDays(i).atTime(19, 30).atZone(ZoneId.systemDefault()).toOffsetDateTime();
            createStudyEvent(userId, "vocab", vocabItems.get(i % vocabItems.size()).getId(), "completed", 180 + i * 10, baseTime);
            createStudyEvent(userId, "exercise", exercises.get(i % exercises.size()).getId(), i % 4 == 0 ? "wrong" : "correct", 240 + i * 12, baseTime.plusMinutes(8));
        }
    }

    private void createStudyEvent(Long userId, String eventType, Long targetId, String result, int durationSeconds, OffsetDateTime occurredAt) {
        StudyEvent event = new StudyEvent();
        event.setUserId(userId);
        event.setEventType(eventType);
        event.setTargetId(targetId);
        event.setResult(result);
        event.setDurationSeconds(durationSeconds);
        event.setOccurredAt(occurredAt);
        studyEventMapper.insert(event);
    }
}
