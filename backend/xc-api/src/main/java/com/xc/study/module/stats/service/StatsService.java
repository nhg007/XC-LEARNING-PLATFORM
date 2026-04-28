package com.xc.study.module.stats.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xc.study.common.PageResult;
import com.xc.study.module.stats.entity.LeaderboardEntry;
import com.xc.study.module.stats.entity.StudyEvent;
import com.xc.study.module.stats.entity.UserDailyStat;
import com.xc.study.module.stats.entity.UserLearningSummary;
import com.xc.study.module.stats.mapper.LeaderboardEntryMapper;
import com.xc.study.module.stats.mapper.StudyEventMapper;
import com.xc.study.module.stats.mapper.UserDailyStatMapper;
import com.xc.study.module.stats.mapper.UserLearningSummaryMapper;
import com.xc.study.module.stats.vo.DailyStatVO;
import com.xc.study.module.stats.vo.LeaderboardEntryVO;
import com.xc.study.module.stats.vo.LearningSummaryVO;
import com.xc.study.module.stats.vo.StudyEventVO;
import com.xc.study.module.user.entity.User;
import com.xc.study.module.user.mapper.UserMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

@Service
public class StatsService {

    private final UserLearningSummaryMapper userLearningSummaryMapper;
    private final UserDailyStatMapper userDailyStatMapper;
    private final StudyEventMapper studyEventMapper;
    private final LeaderboardEntryMapper leaderboardEntryMapper;
    private final UserMapper userMapper;

    public StatsService(
            UserLearningSummaryMapper userLearningSummaryMapper,
            UserDailyStatMapper userDailyStatMapper,
            StudyEventMapper studyEventMapper,
            LeaderboardEntryMapper leaderboardEntryMapper,
            UserMapper userMapper
    ) {
        this.userLearningSummaryMapper = userLearningSummaryMapper;
        this.userDailyStatMapper = userDailyStatMapper;
        this.studyEventMapper = studyEventMapper;
        this.leaderboardEntryMapper = leaderboardEntryMapper;
        this.userMapper = userMapper;
    }

    public LearningSummaryVO getSummary(Long userId) {
        UserLearningSummary summary = userLearningSummaryMapper.selectOne(new LambdaQueryWrapper<UserLearningSummary>()
                .eq(UserLearningSummary::getUserId, userId)
                .last("limit 1"));
        if (summary != null) {
            return new LearningSummaryVO(
                    summary.getTotalStudySeconds(),
                    summary.getTotalExerciseCount(),
                    summary.getTotalCorrectCount(),
                    summary.getTotalVocabReviewCount(),
                    summary.getCurrentStreakDays(),
                    summary.getLongestStreakDays(),
                    summary.getOverallAccuracyRate(),
                    summary.getLastStudyDate()
            );
        }
        List<StudyEvent> events = studyEventMapper.selectList(new LambdaQueryWrapper<StudyEvent>()
                .eq(StudyEvent::getUserId, userId)
                .orderByAsc(StudyEvent::getOccurredAt));
        if (!events.isEmpty()) {
            return buildSummary(events);
        }
        return new LearningSummaryVO(0, 0, 0, 0, 0, 0, BigDecimal.ZERO, null);
    }

    public PageResult<StudyEventVO> listEvents(Long userId, long page, long pageSize, String eventType) {
        Page<StudyEvent> result = studyEventMapper.selectPage(Page.of(page, pageSize), new LambdaQueryWrapper<StudyEvent>()
                .eq(StudyEvent::getUserId, userId)
                .eq(eventType != null && !eventType.isBlank(), StudyEvent::getEventType, eventType)
                .orderByDesc(StudyEvent::getOccurredAt)
                .orderByDesc(StudyEvent::getId));
        return PageResult.of(result.getRecords().stream().map(this::toEventVO).toList(), result.getTotal(), page, pageSize);
    }

    public List<DailyStatVO> listDailyStats(Long userId, int days) {
        int safeDays = Math.max(1, Math.min(days, 366));
        LocalDate startDate = LocalDate.now().minusDays(safeDays - 1L);
        List<UserDailyStat> stats = userDailyStatMapper.selectList(new LambdaQueryWrapper<UserDailyStat>()
                .eq(UserDailyStat::getUserId, userId)
                .ge(UserDailyStat::getStatDate, startDate)
                .orderByAsc(UserDailyStat::getStatDate));
        if (!stats.isEmpty()) {
            Map<LocalDate, UserDailyStat> statsByDate = stats.stream()
                    .collect(Collectors.toMap(UserDailyStat::getStatDate, Function.identity(), (left, right) -> left, TreeMap::new));
            Map<LocalDate, DailyStatVO> byDate = new TreeMap<>();
            for (int i = 0; i < safeDays; i++) {
                LocalDate date = startDate.plusDays(i);
                UserDailyStat stat = statsByDate.get(date);
                byDate.put(date, stat == null ? emptyDailyStat(date) : toDailyStatVO(stat));
            }
            return byDate.values().stream().toList();
        }
        List<StudyEvent> events = studyEventMapper.selectList(new LambdaQueryWrapper<StudyEvent>()
                .eq(StudyEvent::getUserId, userId)
                .ge(StudyEvent::getOccurredAt, startDate.atStartOfDay().atOffset(OffsetDateTime.now().getOffset()))
                .orderByAsc(StudyEvent::getOccurredAt));
        Map<LocalDate, DailyAccumulator> byDate = new TreeMap<>();
        for (int i = 0; i < safeDays; i++) {
            byDate.put(startDate.plusDays(i), new DailyAccumulator());
        }
        for (StudyEvent event : events) {
            LocalDate date = event.getOccurredAt().toLocalDate();
            DailyAccumulator accumulator = byDate.computeIfAbsent(date, ignored -> new DailyAccumulator());
            accumulator.add(event);
        }
        return byDate.entrySet()
                .stream()
                .map(entry -> entry.getValue().toVO(entry.getKey()))
                .toList();
    }

    public PageResult<LeaderboardEntryVO> listLeaderboards(
            long page,
            long pageSize,
            String periodType,
            LocalDate periodStart,
            String metricType
    ) {
        LambdaQueryWrapper<LeaderboardEntry> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(periodType)) {
            wrapper.eq(LeaderboardEntry::getPeriodType, periodType);
        }
        if (periodStart != null) {
            wrapper.eq(LeaderboardEntry::getPeriodStart, periodStart);
        }
        if (StringUtils.hasText(metricType)) {
            wrapper.eq(LeaderboardEntry::getMetricType, metricType);
        }
        wrapper.orderByDesc(LeaderboardEntry::getPeriodStart)
                .orderByAsc(LeaderboardEntry::getMetricType)
                .orderByAsc(LeaderboardEntry::getRankNo)
                .orderByAsc(LeaderboardEntry::getId);
        Page<LeaderboardEntry> result = leaderboardEntryMapper.selectPage(Page.of(page, pageSize), wrapper);
        Map<Long, User> usersById = loadUsers(result.getRecords().stream().map(LeaderboardEntry::getUserId).toList());
        List<LeaderboardEntryVO> records = result.getRecords().stream()
                .map(entry -> {
                    User user = usersById.get(entry.getUserId());
                    return new LeaderboardEntryVO(
                            entry.getId(),
                            entry.getPeriodType(),
                            entry.getPeriodStart(),
                            entry.getMetricType(),
                            entry.getUserId(),
                            user == null ? null : user.getNickname(),
                            entry.getScoreValue(),
                            entry.getRankNo(),
                            entry.getGeneratedAt()
                    );
                })
                .toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private Map<Long, User> loadUsers(List<Long> userIds) {
        List<Long> ids = userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(ids)
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private DailyStatVO emptyDailyStat(LocalDate date) {
        return new DailyStatVO(date, 0, 0, 0, 0, 0, 0, BigDecimal.ZERO);
    }

    private DailyStatVO toDailyStatVO(UserDailyStat stat) {
        return new DailyStatVO(
                stat.getStatDate(),
                stat.getStudySeconds(),
                stat.getExerciseCount(),
                stat.getCorrectCount(),
                stat.getVocabReviewCount(),
                stat.getDialogueCount(),
                stat.getMatchingGameCount(),
                stat.getAccuracyRate()
        );
    }

    private LearningSummaryVO buildSummary(List<StudyEvent> events) {
        int totalSeconds = events.stream().map(StudyEvent::getDurationSeconds).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
        int exerciseCount = countEvents(events, "exercise");
        int correctCount = (int) events.stream()
                .filter(event -> "exercise".equals(event.getEventType()))
                .filter(event -> "correct".equals(event.getResult()))
                .count();
        int vocabCount = countEvents(events, "vocab");
        Set<LocalDate> dates = events.stream()
                .map(StudyEvent::getOccurredAt)
                .filter(Objects::nonNull)
                .map(OffsetDateTime::toLocalDate)
                .collect(Collectors.toSet());
        LocalDate lastDate = dates.stream().max(Comparator.naturalOrder()).orElse(null);
        return new LearningSummaryVO(
                totalSeconds,
                exerciseCount,
                correctCount,
                vocabCount,
                currentStreakDays(dates, lastDate),
                longestStreakDays(dates),
                accuracy(correctCount, exerciseCount),
                lastDate
        );
    }

    private StudyEventVO toEventVO(StudyEvent event) {
        return new StudyEventVO(
                event.getId(),
                event.getEventType(),
                event.getTargetId(),
                event.getResult(),
                event.getDurationSeconds(),
                event.getOccurredAt()
        );
    }

    private int countEvents(List<StudyEvent> events, String eventType) {
        return (int) events.stream()
                .filter(event -> eventType.equals(event.getEventType()))
                .count();
    }

    private int currentStreakDays(Set<LocalDate> dates, LocalDate lastDate) {
        if (lastDate == null) {
            return 0;
        }
        int streak = 0;
        LocalDate cursor = lastDate;
        while (dates.contains(cursor)) {
            streak += 1;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private int longestStreakDays(Set<LocalDate> dates) {
        int longest = 0;
        int current = 0;
        LocalDate previous = null;
        for (LocalDate date : dates.stream().sorted().toList()) {
            current = previous != null && date.equals(previous.plusDays(1)) ? current + 1 : 1;
            longest = Math.max(longest, current);
            previous = date;
        }
        return longest;
    }

    private BigDecimal accuracy(int correctCount, int exerciseCount) {
        if (exerciseCount == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(correctCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(exerciseCount), 2, RoundingMode.HALF_UP);
    }

    private static class DailyAccumulator {
        private int studySeconds;
        private int exerciseCount;
        private int correctCount;
        private int vocabReviewCount;
        private int dialogueCount;
        private int matchingGameCount;

        private void add(StudyEvent event) {
            studySeconds += event.getDurationSeconds() == null ? 0 : event.getDurationSeconds();
            if ("exercise".equals(event.getEventType())) {
                exerciseCount += 1;
                if ("correct".equals(event.getResult())) {
                    correctCount += 1;
                }
            } else if ("vocab".equals(event.getEventType())) {
                vocabReviewCount += 1;
            } else if ("dialogue".equals(event.getEventType())) {
                dialogueCount += 1;
            } else if ("matching_game".equals(event.getEventType())) {
                matchingGameCount += 1;
            }
        }

        private DailyStatVO toVO(LocalDate date) {
            BigDecimal accuracy = exerciseCount == 0
                    ? BigDecimal.ZERO
                    : BigDecimal.valueOf(correctCount).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(exerciseCount), 2, RoundingMode.HALF_UP);
            return new DailyStatVO(date, studySeconds, exerciseCount, correctCount, vocabReviewCount, dialogueCount, matchingGameCount, accuracy);
        }
    }
}
