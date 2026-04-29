package com.xc.study.module.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xc.study.common.BusinessException;
import com.xc.study.common.ErrorCode;
import com.xc.study.common.PageResult;
import com.xc.study.module.admin.dto.AdminLeaderboardQueryDTO;
import com.xc.study.module.admin.dto.AdminLearningReportQueryDTO;
import com.xc.study.module.admin.service.AdminReportService;
import com.xc.study.module.admin.service.support.AdminSorts;
import com.xc.study.module.admin.vo.AdminDailyLearningReportVO;
import com.xc.study.module.admin.vo.AdminLeaderboardEntryVO;
import com.xc.study.module.admin.vo.AdminLearningReportSummaryVO;
import com.xc.study.module.admin.vo.AdminLearningReportVO;
import com.xc.study.module.admin.vo.AdminUserLearningReportVO;
import com.xc.study.module.stats.entity.LeaderboardEntry;
import com.xc.study.module.stats.entity.UserDailyStat;
import com.xc.study.module.stats.mapper.LeaderboardEntryMapper;
import com.xc.study.module.stats.mapper.UserDailyStatMapper;
import com.xc.study.module.user.entity.User;
import com.xc.study.module.user.mapper.UserMapper;
import com.xc.study.security.CurrentUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminReportServiceImpl implements AdminReportService {

    private static final int MAX_REPORT_DAYS = 366;

    private final UserDailyStatMapper userDailyStatMapper;
    private final LeaderboardEntryMapper leaderboardEntryMapper;
    private final UserMapper userMapper;

    public AdminReportServiceImpl(
            UserDailyStatMapper userDailyStatMapper,
            LeaderboardEntryMapper leaderboardEntryMapper,
            UserMapper userMapper
    ) {
        this.userDailyStatMapper = userDailyStatMapper;
        this.leaderboardEntryMapper = leaderboardEntryMapper;
        this.userMapper = userMapper;
    }

    @Override
    public AdminLearningReportVO learningReport(AdminLearningReportQueryDTO query, CurrentUser admin) {
        requirePermission(admin, "admin:report:read");
        DateRange range = resolveDateRange(query.getDateFrom(), query.getDateTo());
        List<UserDailyStat> stats = userDailyStatMapper.selectList(new LambdaQueryWrapper<UserDailyStat>()
                .ge(UserDailyStat::getStatDate, range.from())
                .le(UserDailyStat::getStatDate, range.to())
                .orderByAsc(UserDailyStat::getStatDate)
                .orderByAsc(UserDailyStat::getUserId));
        Set<Long> matchedUserIds = resolveMatchedUserIds(query.getKeyword());
        if (matchedUserIds != null) {
            stats = stats.stream()
                    .filter(item -> matchedUserIds.contains(item.getUserId()))
                    .toList();
        }
        AdminLearningReportSummaryVO summary = buildSummary(range, stats);
        List<AdminDailyLearningReportVO> dailyStats = buildDailyStats(range, stats);
        PageResult<AdminUserLearningReportVO> users = buildUserPage(stats, query);
        return new AdminLearningReportVO(summary, dailyStats, users);
    }

    @Override
    public PageResult<AdminLeaderboardEntryVO> pageLeaderboards(AdminLeaderboardQueryDTO query, CurrentUser admin) {
        requirePermission(admin, "admin:report:read");
        int page = query.getPage() == null ? 1 : query.getPage();
        int pageSize = query.getPageSize() == null ? 20 : query.getPageSize();
        LambdaQueryWrapper<LeaderboardEntry> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getPeriodType())) {
            wrapper.eq(LeaderboardEntry::getPeriodType, query.getPeriodType());
        }
        if (query.getPeriodStart() != null) {
            wrapper.eq(LeaderboardEntry::getPeriodStart, query.getPeriodStart());
        }
        if (StringUtils.hasText(query.getMetricType())) {
            wrapper.eq(LeaderboardEntry::getMetricType, query.getMetricType());
        }
        boolean sorted = AdminSorts.apply(wrapper, query.getSortBy(), query.getSortDirection(), Map.of(
                "id", LeaderboardEntry::getId,
                "periodType", LeaderboardEntry::getPeriodType,
                "periodStart", LeaderboardEntry::getPeriodStart,
                "metricType", LeaderboardEntry::getMetricType,
                "scoreValue", LeaderboardEntry::getScoreValue,
                "rankNo", LeaderboardEntry::getRankNo,
                "generatedAt", LeaderboardEntry::getGeneratedAt
        ));
        if (!sorted) {
            wrapper.orderByDesc(LeaderboardEntry::getPeriodStart)
                    .orderByAsc(LeaderboardEntry::getMetricType)
                    .orderByAsc(LeaderboardEntry::getRankNo);
        }
        wrapper.orderByAsc(LeaderboardEntry::getId);
        Page<LeaderboardEntry> result = leaderboardEntryMapper.selectPage(Page.of(page, pageSize), wrapper);
        return PageResult.of(toLeaderboardVOs(result.getRecords()), result.getTotal(), result.getCurrent(), result.getSize());
    }

    private AdminLearningReportSummaryVO buildSummary(DateRange range, List<UserDailyStat> stats) {
        SummaryAccumulator accumulator = new SummaryAccumulator();
        Set<Long> userIds = new HashSet<>();
        stats.forEach(stat -> {
            accumulator.add(stat);
            userIds.add(stat.getUserId());
        });
        return new AdminLearningReportSummaryVO(
                range.from(),
                range.to(),
                userIds.size(),
                accumulator.studySeconds,
                accumulator.exerciseCount,
                accumulator.correctCount,
                accumulator.vocabReviewCount,
                accumulator.dialogueCount,
                accumulator.matchingGameCount,
                accuracy(accumulator.correctCount, accumulator.exerciseCount)
        );
    }

    private List<AdminDailyLearningReportVO> buildDailyStats(DateRange range, List<UserDailyStat> stats) {
        Map<LocalDate, List<UserDailyStat>> statsByDate = stats.stream()
                .collect(Collectors.groupingBy(UserDailyStat::getStatDate, LinkedHashMap::new, Collectors.toList()));
        List<AdminDailyLearningReportVO> result = new ArrayList<>();
        LocalDate cursor = range.from();
        while (!cursor.isAfter(range.to())) {
            List<UserDailyStat> dayStats = statsByDate.getOrDefault(cursor, List.of());
            SummaryAccumulator accumulator = new SummaryAccumulator();
            Set<Long> userIds = new HashSet<>();
            dayStats.forEach(stat -> {
                accumulator.add(stat);
                userIds.add(stat.getUserId());
            });
            result.add(new AdminDailyLearningReportVO(
                    cursor,
                    userIds.size(),
                    accumulator.studySeconds,
                    accumulator.exerciseCount,
                    accumulator.correctCount,
                    accumulator.vocabReviewCount,
                    accumulator.dialogueCount,
                    accumulator.matchingGameCount,
                    accuracy(accumulator.correctCount, accumulator.exerciseCount)
            ));
            cursor = cursor.plusDays(1);
        }
        return result;
    }

    private PageResult<AdminUserLearningReportVO> buildUserPage(List<UserDailyStat> stats, AdminLearningReportQueryDTO query) {
        int page = query.getPage() == null ? 1 : query.getPage();
        int pageSize = query.getPageSize() == null ? 20 : query.getPageSize();
        Map<Long, UserAccumulator> byUser = new LinkedHashMap<>();
        stats.forEach(stat -> byUser.computeIfAbsent(stat.getUserId(), UserAccumulator::new).add(stat));
        List<UserAccumulator> sorted = byUser.values().stream()
                .sorted(userReportComparator(query))
                .toList();
        int fromIndex = Math.min((page - 1) * pageSize, sorted.size());
        int toIndex = Math.min(fromIndex + pageSize, sorted.size());
        List<UserAccumulator> pageItems = sorted.subList(fromIndex, toIndex);
        Map<Long, User> users = loadUsers(pageItems.stream().map(UserAccumulator::userId).toList());
        List<AdminUserLearningReportVO> records = pageItems.stream()
                .map(item -> {
                    User user = users.get(item.userId());
                    return new AdminUserLearningReportVO(
                            item.userId(),
                            user == null ? null : user.getEmail(),
                            user == null ? null : user.getNickname(),
                            user == null ? null : user.getStatus(),
                            item.studySeconds(),
                            item.exerciseCount(),
                            item.correctCount(),
                            item.vocabReviewCount(),
                            item.dialogueCount(),
                            item.matchingGameCount(),
                            accuracy(item.correctCount(), item.exerciseCount()),
                            item.lastStudyDate()
                    );
                })
                .toList();
        return PageResult.of(records, sorted.size(), page, pageSize);
    }

    private List<AdminLeaderboardEntryVO> toLeaderboardVOs(List<LeaderboardEntry> entries) {
        if (entries.isEmpty()) {
            return List.of();
        }
        Map<Long, User> users = loadUsers(entries.stream().map(LeaderboardEntry::getUserId).toList());
        return entries.stream()
                .map(entry -> {
                    User user = users.get(entry.getUserId());
                    return new AdminLeaderboardEntryVO(
                            entry.getId(),
                            entry.getPeriodType(),
                            entry.getPeriodStart(),
                            entry.getMetricType(),
                            entry.getUserId(),
                            user == null ? null : user.getEmail(),
                            user == null ? null : user.getNickname(),
                            entry.getScoreValue(),
                            entry.getRankNo(),
                            entry.getGeneratedAt()
                    );
                })
                .toList();
    }

    private Set<Long> resolveMatchedUserIds(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String trimmed = keyword.trim();
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .like(User::getEmail, trimmed)
                .or()
                .like(User::getNickname, trimmed));
        return users.stream().map(User::getId).collect(Collectors.toSet());
    }

    private Map<Long, User> loadUsers(List<Long> ids) {
        List<Long> userIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(userIds)
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private DateRange resolveDateRange(LocalDate from, LocalDate to) {
        LocalDate safeTo = to == null ? LocalDate.now() : to;
        LocalDate safeFrom = from == null ? safeTo.minusDays(29) : from;
        if (safeFrom.isAfter(safeTo)) {
            throw new BusinessException("开始日期不能晚于结束日期");
        }
        if (safeFrom.plusDays(MAX_REPORT_DAYS - 1L).isBefore(safeTo)) {
            throw new BusinessException("报表日期范围最多 366 天");
        }
        return new DateRange(safeFrom, safeTo);
    }

    private BigDecimal accuracy(int correctCount, int exerciseCount) {
        if (exerciseCount == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(correctCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(exerciseCount), 2, RoundingMode.HALF_UP);
    }

    private Comparator<UserAccumulator> userReportComparator(AdminLearningReportQueryDTO query) {
        if (!StringUtils.hasText(query.getSortBy()) || !StringUtils.hasText(query.getSortDirection())) {
            return defaultUserReportComparator();
        }
        Comparator<UserAccumulator> comparator = switch (query.getSortBy().trim()) {
            case "userId" -> Comparator.comparing(UserAccumulator::userId);
            case "studySeconds" -> Comparator.comparingInt(UserAccumulator::studySeconds);
            case "exerciseCount" -> Comparator.comparingInt(UserAccumulator::exerciseCount);
            case "correctCount" -> Comparator.comparingInt(UserAccumulator::correctCount);
            case "accuracyRate" -> Comparator.comparingDouble(UserAccumulator::accuracyRate);
            case "vocabReviewCount" -> Comparator.comparingInt(UserAccumulator::vocabReviewCount);
            case "dialogueCount" -> Comparator.comparingInt(UserAccumulator::dialogueCount);
            case "matchingGameCount" -> Comparator.comparingInt(UserAccumulator::matchingGameCount);
            case "lastStudyDate" -> Comparator.comparing(UserAccumulator::lastStudyDate, Comparator.nullsLast(Comparator.naturalOrder()));
            default -> null;
        };
        if (comparator == null) {
            return defaultUserReportComparator();
        }
        if ("desc".equals(query.getSortDirection())) {
            comparator = comparator.reversed();
        } else if (!"asc".equals(query.getSortDirection())) {
            return defaultUserReportComparator();
        }
        return comparator.thenComparing(UserAccumulator::userId);
    }

    private Comparator<UserAccumulator> defaultUserReportComparator() {
        return Comparator.comparingInt(UserAccumulator::studySeconds).reversed()
                .thenComparing(UserAccumulator::userId);
    }

    private void requirePermission(CurrentUser admin, String permission) {
        if (admin.permissions().contains("admin:*") || admin.permissions().contains(permission)) {
            return;
        }
        throw BusinessException.forbidden(ErrorCode.FORBIDDEN, "缺少后台权限：" + permission);
    }

    private record DateRange(LocalDate from, LocalDate to) {
    }

    private static class SummaryAccumulator {
        protected int studySeconds;
        protected int exerciseCount;
        protected int correctCount;
        protected int vocabReviewCount;
        protected int dialogueCount;
        protected int matchingGameCount;

        protected void add(UserDailyStat stat) {
            studySeconds += safeInt(stat.getStudySeconds());
            exerciseCount += safeInt(stat.getExerciseCount());
            correctCount += safeInt(stat.getCorrectCount());
            vocabReviewCount += safeInt(stat.getVocabReviewCount());
            dialogueCount += safeInt(stat.getDialogueCount());
            matchingGameCount += safeInt(stat.getMatchingGameCount());
        }

        private static int safeInt(Integer value) {
            return value == null ? 0 : value;
        }
    }

    private static class UserAccumulator extends SummaryAccumulator {
        private final Long userId;
        private LocalDate lastStudyDate;

        private UserAccumulator(Long userId) {
            this.userId = userId;
        }

        @Override
        protected void add(UserDailyStat stat) {
            super.add(stat);
            if (lastStudyDate == null || stat.getStatDate().isAfter(lastStudyDate)) {
                lastStudyDate = stat.getStatDate();
            }
        }

        private Long userId() {
            return userId;
        }

        private int studySeconds() {
            return studySeconds;
        }

        private int exerciseCount() {
            return exerciseCount;
        }

        private int correctCount() {
            return correctCount;
        }

        private int vocabReviewCount() {
            return vocabReviewCount;
        }

        private int dialogueCount() {
            return dialogueCount;
        }

        private int matchingGameCount() {
            return matchingGameCount;
        }

        private double accuracyRate() {
            if (exerciseCount == 0) {
                return 0;
            }
            return (double) correctCount / exerciseCount;
        }

        private LocalDate lastStudyDate() {
            return lastStudyDate;
        }
    }
}
