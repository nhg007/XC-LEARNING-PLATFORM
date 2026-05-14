package com.xc.study.module.classroom.controller;

import com.xc.study.common.ApiResponse;
import com.xc.study.common.PageResult;
import com.xc.study.module.classroom.dto.CreateClassRoomRequest;
import com.xc.study.module.classroom.dto.JoinClassRoomRequest;
import com.xc.study.module.classroom.service.ClassRoomService;
import com.xc.study.module.classroom.vo.ClassMemberStatsVO;
import com.xc.study.module.classroom.vo.ClassMemberVO;
import com.xc.study.module.classroom.vo.ClassRoomDetailVO;
import com.xc.study.module.classroom.vo.ClassRoomVO;
import com.xc.study.module.exercise.service.ExerciseService;
import com.xc.study.module.exercise.vo.ExerciseAttemptVO;
import com.xc.study.module.stats.service.StatsService;
import com.xc.study.module.stats.vo.DailyStatVO;
import com.xc.study.module.stats.vo.LearningSummaryVO;
import com.xc.study.module.stats.vo.StudyEventVO;
import com.xc.study.security.CurrentUser;
import com.xc.study.security.CurrentUserProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/classrooms")
public class ClassRoomController {

    private final ClassRoomService classRoomService;
    private final StatsService statsService;
    private final ExerciseService exerciseService;
    private final CurrentUserProvider currentUserProvider;

    public ClassRoomController(
            ClassRoomService classRoomService,
            StatsService statsService,
            ExerciseService exerciseService,
            CurrentUserProvider currentUserProvider
    ) {
        this.classRoomService = classRoomService;
        this.statsService = statsService;
        this.exerciseService = exerciseService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public ApiResponse<List<ClassRoomVO>> myClasses() {
        return ApiResponse.ok(classRoomService.myClasses(currentUserProvider.requireStudent().id()));
    }

    @PostMapping
    public ApiResponse<ClassRoomVO> create(@Valid @RequestBody CreateClassRoomRequest request) {
        CurrentUser currentUser = currentUserProvider.requireStudent();
        return ApiResponse.ok(classRoomService.create(currentUser, request));
    }

    @PostMapping("/join")
    public ApiResponse<ClassRoomVO> join(@Valid @RequestBody JoinClassRoomRequest request) {
        return ApiResponse.ok(classRoomService.join(currentUserProvider.requireStudent().id(), request));
    }

    @GetMapping("/{id}")
    public ApiResponse<ClassRoomDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(classRoomService.detail(currentUserProvider.requireStudent().id(), id));
    }

    @GetMapping("/{id}/members")
    public ApiResponse<List<ClassMemberVO>> members(@PathVariable Long id) {
        return ApiResponse.ok(classRoomService.members(currentUserProvider.requireStudent().id(), id));
    }

    @GetMapping("/{id}/stats")
    public ApiResponse<List<ClassMemberStatsVO>> stats(@PathVariable Long id) {
        return ApiResponse.ok(classRoomService.classStats(currentUserProvider.requireStudent().id(), id));
    }

    @GetMapping("/{id}/members/{userId}/stats")
    public ApiResponse<ClassMemberStatsVO> memberStats(@PathVariable Long id, @PathVariable Long userId) {
        return ApiResponse.ok(classRoomService.memberStats(currentUserProvider.requireStudent().id(), id, userId));
    }

    @GetMapping("/{id}/members/{userId}/records/summary")
    public ApiResponse<LearningSummaryVO> memberLearningSummary(@PathVariable Long id, @PathVariable Long userId) {
        classRoomService.requireMemberRecordAccess(currentUserProvider.requireStudent().id(), id, userId);
        return ApiResponse.ok(statsService.getSummary(userId));
    }

    @GetMapping("/{id}/members/{userId}/records/daily")
    public ApiResponse<List<DailyStatVO>> memberDailyStats(
            @PathVariable Long id,
            @PathVariable Long userId,
            @RequestParam(defaultValue = "30") @Min(1) @Max(366) int days
    ) {
        classRoomService.requireMemberRecordAccess(currentUserProvider.requireStudent().id(), id, userId);
        return ApiResponse.ok(statsService.listDailyStats(userId, days));
    }

    @GetMapping("/{id}/members/{userId}/records/events")
    public ApiResponse<PageResult<StudyEventVO>> memberStudyEvents(
            @PathVariable Long id,
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize,
            @RequestParam(required = false) String eventType
    ) {
        classRoomService.requireMemberRecordAccess(currentUserProvider.requireStudent().id(), id, userId);
        return ApiResponse.ok(statsService.listEvents(userId, page, pageSize, eventType));
    }

    @GetMapping("/{id}/members/{userId}/records/attempts")
    public ApiResponse<PageResult<ExerciseAttemptVO>> memberExerciseAttempts(
            @PathVariable Long id,
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize
    ) {
        classRoomService.requireMemberRecordAccess(currentUserProvider.requireStudent().id(), id, userId);
        return ApiResponse.ok(exerciseService.listAttempts(userId, page, pageSize));
    }
}
