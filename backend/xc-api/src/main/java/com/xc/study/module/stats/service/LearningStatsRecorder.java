package com.xc.study.module.stats.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xc.study.module.stats.entity.LeaderboardEntry;
import com.xc.study.module.stats.entity.StudyEvent;
import com.xc.study.module.stats.entity.UserDailyStat;
import com.xc.study.module.stats.entity.UserLearningSummary;
import com.xc.study.module.stats.mapper.LeaderboardEntryMapper;
import com.xc.study.module.stats.mapper.StudyEventMapper;
import com.xc.study.module.stats.mapper.UserDailyStatMapper;
import com.xc.study.module.stats.mapper.UserLearningSummaryMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LearningStatsRecorder {

    private static final LocalDate ALL_TIME_PERIOD_START = LocalDate.of(1970, 1, 1);
    private static final int LEADERBOARD_LIMIT = 100;

    private final StudyEventMapper studyEventMapper;
    private final UserDailyStatMapper userDailyStatMapper;
    private final UserLearningSummaryMapper userLearningSummaryMapper;
    private final LeaderboardEntryMapper leaderboardEntryMapper;

    public LearningStatsRecorder(
            StudyEventMapper studyEventMapper,
            UserDailyStatMapper userDailyStatMapper,
            UserLearningSummaryMapper userLearningSummaryMapper,
            LeaderboardEntryMapper leaderboardEntryMapper
    ) {
        this.studyEventMapper = studyEventMapper;
        this.userDailyStatMapper = userDailyStatMapper;
        this.userLearningSummaryMapper = userLearningSummaryMapper;
        this.leaderboardEntryMapper = leaderboardEntryMapper;
    }

    @Transactional
    public StudyEvent recordEvent(
            Long userId,
            String eventType,
            Long targetId,
            String result,
            Integer durationSeconds,
            OffsetDateTime occurredAt
    ) {
        OffsetDateTime safeOccurredAt = occurredAt == null ? OffsetDateTime.now() : occurredAt;
        StudyEvent event = new StudyEvent();
        event.setUserId(userId);
        event.setEventType(eventType);
        event.setTargetId(targetId);
        event.setResult(result);
        event.setDurationSeconds(Math.max(0, durationSeconds == null ? 0 : durationSeconds));
        event.setOccurredAt(safeOccurredAt);
        studyEventMapper.insert(event);
        rebuildUserStats(userId);
        refreshLeaderboards(safeOccurredAt.toLocalDate());
        return event;
    }

    @Transactional
    public void rebuildUserStats(Long userId) {
        List<StudyEvent> events = studyEventMapper.selectList(new LambdaQueryWrapper<StudyEvent>()
                .eq(StudyEvent::getUserId, userId)
                .orderByAsc(StudyEvent::getOccurredAt)
                .orderByAsc(StudyEvent::getId));
        userDailyStatMapper.delete(new LambdaQueryWrapper<UserDailyStat>()
                .eq(UserDailyStat::getUserId, userId));

        Map<LocalDate, DailyAccumulator> dailyByDate = new LinkedHashMap<>();
        for (StudyEvent event : events) {
            if (event.getOccurredAt() == null) {
                continue;
            }
            dailyByDate.computeIfAbsent(event.getOccurredAt().toLocalDate(), ignored -> new DailyAccumulator())
                    .add(event);
        }

        OffsetDateTime now = OffsetDateTime.now();
        for (Map.Entry<LocalDate, DailyAccumulator> entry : dailyByDate.entrySet()) {
            UserDailyStat stat = entry.getValue().toDailyStat(userId, entry.getKey(), now);
            userDailyStatMapper.insert(stat);
        }
        upsertSummary(userId, dailyByDate, now);
    }

    @Transactional
    public void refreshLeaderboards(LocalDate date) {
        LocalDate safeDate = date == null ? LocalDate.now() : date;
        refreshPeriod(new PeriodScope("daily", safeDate, safeDate, safeDate));
        LocalDate weekStart = safeDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        refreshPeriod(new PeriodScope("weekly", weekStart, weekStart, weekStart.plusDays(6)));
        LocalDate monthStart = safeDate.withDayOfMonth(1);
        refreshPeriod(new PeriodScope("monthly", monthStart, monthStart, monthStart.plusMonths(1).minusDays(1)));
        refreshPeriod(new PeriodScope("all", ALL_TIME_PERIOD_START, null, null));
    }

    private void upsertSummary(Long userId, Map<LocalDate, DailyAccumulator> dailyByDate, OffsetDateTime now) {
        SummaryAccumulator summary = new SummaryAccumulator();
        dailyByDate.forEach((date, daily) -> summary.add(date, daily));
        UserLearningSummary entity = userLearningSummaryMapper.selectOne(new LambdaQueryWrapper<UserLearningSummary>()
                .eq(UserLearningSummary::getUserId, userId)
                .last("limit 1"));
        if (entity == null) {
            entity = new UserLearningSummary();
            entity.setUserId(userId);
            entity.setCreatedAt(now);
        }
        entity.setTotalStudySeconds(summary.studySeconds);
        entity.setTotalExerciseCount(summary.exerciseCount);
        entity.setTotalCorrectCount(summary.correctCount);
        entity.setTotalVocabReviewCount(summary.vocabReviewCount);
        entity.setCurrentStreakDays(currentStreakDays(summary.activeDates, summary.lastStudyDate));
        entity.setLongestStreakDays(longestStreakDays(summary.activeDates));
        entity.setOverallAccuracyRate(accuracy(summary.correctCount, summary.exerciseCount));
        entity.setLastStudyDate(summary.lastStudyDate);
        entity.setUpdatedAt(now);
        if (entity.getId() == null) {
            userLearningSummaryMapper.insert(entity);
        } else {
            userLearningSummaryMapper.updateById(entity);
        }
    }

    private void refreshPeriod(PeriodScope scope) {
        for (String metricType : List.of("streak", "accuracy", "vocab_count", "game_score")) {
            leaderboardEntryMapper.delete(new LambdaQueryWrapper<LeaderboardEntry>()
                    .eq(LeaderboardEntry::getPeriodType, scope.periodType())
                    .eq(LeaderboardEntry::getPeriodStart, scope.periodStart())
                    .eq(LeaderboardEntry::getMetricType, metricType));
            List<ScoreRow> scores = "all".equals(scope.periodType()) && "streak".equals(metricType)
                    ? allTimeStreakScores()
                    : aggregatePeriodScores(scope, metricType);
            insertLeaderboardRows(scope, metricType, scores);
        }
    }

    private List<ScoreRow> allTimeStreakScores() {
        return userLearningSummaryMapper.selectList(new LambdaQueryWrapper<UserLearningSummary>()
                        .gt(UserLearningSummary::getCurrentStreakDays, 0))
                .stream()
                .map(summary -> new ScoreRow(summary.getUserId(), BigDecimal.valueOf(safeInt(summary.getCurrentStreakDays()))))
                .sorted(scoreComparator())
                .limit(LEADERBOARD_LIMIT)
                .toList();
    }

    private List<ScoreRow> aggregatePeriodScores(PeriodScope scope, String metricType) {
        LambdaQueryWrapper<UserDailyStat> wrapper = new LambdaQueryWrapper<>();
        if (scope.from() != null) {
            wrapper.ge(UserDailyStat::getStatDate, scope.from());
        }
        if (scope.to() != null) {
            wrapper.le(UserDailyStat::getStatDate, scope.to());
        }
        Map<Long, PeriodAccumulator> byUser = new LinkedHashMap<>();
        userDailyStatMapper.selectList(wrapper.orderByAsc(UserDailyStat::getUserId))
                .forEach(stat -> byUser.computeIfAbsent(stat.getUserId(), ignored -> new PeriodAccumulator()).add(stat));
        return byUser.entrySet()
                .stream()
                .map(entry -> new ScoreRow(entry.getKey(), entry.getValue().score(metricType)))
                .filter(row -> row.score().compareTo(BigDecimal.ZERO) > 0)
                .sorted(scoreComparator())
                .limit(LEADERBOARD_LIMIT)
                .toList();
    }

    private void insertLeaderboardRows(PeriodScope scope, String metricType, List<ScoreRow> scores) {
        OffsetDateTime now = OffsetDateTime.now();
        int rank = 1;
        for (ScoreRow score : scores) {
            LeaderboardEntry entry = new LeaderboardEntry();
            entry.setPeriodType(scope.periodType());
            entry.setPeriodStart(scope.periodStart());
            entry.setMetricType(metricType);
            entry.setUserId(score.userId());
            entry.setScoreValue(score.score());
            entry.setRankNo(rank++);
            entry.setGeneratedAt(now);
            entry.setCreatedAt(now);
            entry.setUpdatedAt(now);
            leaderboardEntryMapper.insert(entry);
        }
    }

    private Comparator<ScoreRow> scoreComparator() {
        return Comparator.comparing(ScoreRow::score, Comparator.reverseOrder())
                .thenComparing(ScoreRow::userId);
    }

    private BigDecimal accuracy(int correctCount, int exerciseCount) {
        if (exerciseCount == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(correctCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(exerciseCount), 2, RoundingMode.HALF_UP);
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

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
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

        private boolean active() {
            return studySeconds > 0 || exerciseCount > 0 || vocabReviewCount > 0 || dialogueCount > 0 || matchingGameCount > 0;
        }

        private UserDailyStat toDailyStat(Long userId, LocalDate date, OffsetDateTime now) {
            UserDailyStat stat = new UserDailyStat();
            stat.setUserId(userId);
            stat.setStatDate(date);
            stat.setStudySeconds(studySeconds);
            stat.setExerciseCount(exerciseCount);
            stat.setCorrectCount(correctCount);
            stat.setVocabReviewCount(vocabReviewCount);
            stat.setDialogueCount(dialogueCount);
            stat.setMatchingGameCount(matchingGameCount);
            stat.setAccuracyRate(exerciseCount == 0
                    ? BigDecimal.ZERO
                    : BigDecimal.valueOf(correctCount).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(exerciseCount), 2, RoundingMode.HALF_UP));
            stat.setCreatedAt(now);
            stat.setUpdatedAt(now);
            return stat;
        }
    }

    private static class SummaryAccumulator {
        private int studySeconds;
        private int exerciseCount;
        private int correctCount;
        private int vocabReviewCount;
        private final Set<LocalDate> activeDates = new java.util.HashSet<>();
        private LocalDate lastStudyDate;

        private void add(LocalDate date, DailyAccumulator daily) {
            studySeconds += daily.studySeconds;
            exerciseCount += daily.exerciseCount;
            correctCount += daily.correctCount;
            vocabReviewCount += daily.vocabReviewCount;
            if (daily.active()) {
                activeDates.add(date);
                if (lastStudyDate == null || date.isAfter(lastStudyDate)) {
                    lastStudyDate = date;
                }
            }
        }
    }

    private class PeriodAccumulator {
        private int activeDayCount;
        private int exerciseCount;
        private int correctCount;
        private int vocabReviewCount;
        private int matchingGameCount;

        private void add(UserDailyStat stat) {
            if (safeInt(stat.getStudySeconds()) > 0
                    || safeInt(stat.getExerciseCount()) > 0
                    || safeInt(stat.getVocabReviewCount()) > 0
                    || safeInt(stat.getDialogueCount()) > 0
                    || safeInt(stat.getMatchingGameCount()) > 0) {
                activeDayCount += 1;
            }
            exerciseCount += safeInt(stat.getExerciseCount());
            correctCount += safeInt(stat.getCorrectCount());
            vocabReviewCount += safeInt(stat.getVocabReviewCount());
            matchingGameCount += safeInt(stat.getMatchingGameCount());
        }

        private BigDecimal score(String metricType) {
            return switch (metricType) {
                case "streak" -> BigDecimal.valueOf(activeDayCount);
                case "accuracy" -> accuracy(correctCount, exerciseCount);
                case "vocab_count" -> BigDecimal.valueOf(vocabReviewCount);
                case "game_score" -> BigDecimal.valueOf(matchingGameCount);
                default -> BigDecimal.ZERO;
            };
        }
    }

    private record PeriodScope(String periodType, LocalDate periodStart, LocalDate from, LocalDate to) {
    }

    private record ScoreRow(Long userId, BigDecimal score) {
    }
}
