package com.xc.study.module.classroom.controller;

import com.xc.study.common.ApiResponse;
import com.xc.study.module.classroom.dto.AddClassMemberRequest;
import com.xc.study.module.classroom.dto.CreateClassRoomRequest;
import com.xc.study.module.classroom.dto.JoinClassRoomRequest;
import com.xc.study.module.classroom.dto.ReviewClassMemberRequest;
import com.xc.study.module.classroom.service.ClassRoomService;
import com.xc.study.module.classroom.vo.ClassMemberStatsVO;
import com.xc.study.module.classroom.vo.ClassMemberVO;
import com.xc.study.module.classroom.vo.ClassRoomDetailVO;
import com.xc.study.module.classroom.vo.ClassRoomVO;
import com.xc.study.security.CurrentUser;
import com.xc.study.security.CurrentUserProvider;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/classrooms")
public class ClassRoomController {

    private final ClassRoomService classRoomService;
    private final CurrentUserProvider currentUserProvider;

    public ClassRoomController(ClassRoomService classRoomService, CurrentUserProvider currentUserProvider) {
        this.classRoomService = classRoomService;
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

    @PostMapping("/{id}/members")
    public ApiResponse<ClassMemberVO> addMember(
            @PathVariable Long id,
            @Valid @RequestBody AddClassMemberRequest request
    ) {
        return ApiResponse.ok(classRoomService.addMember(currentUserProvider.requireStudent().id(), id, request));
    }

    @PutMapping("/{id}/members/{userId}/review")
    public ApiResponse<ClassMemberVO> reviewMember(
            @PathVariable Long id,
            @PathVariable Long userId,
            @Valid @RequestBody ReviewClassMemberRequest request
    ) {
        return ApiResponse.ok(classRoomService.reviewMember(currentUserProvider.requireStudent().id(), id, userId, request));
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ApiResponse<ClassMemberVO> removeMember(@PathVariable Long id, @PathVariable Long userId) {
        return ApiResponse.ok(classRoomService.removeMember(currentUserProvider.requireStudent().id(), id, userId));
    }

    @GetMapping("/{id}/stats")
    public ApiResponse<List<ClassMemberStatsVO>> stats(@PathVariable Long id) {
        return ApiResponse.ok(classRoomService.classStats(currentUserProvider.requireStudent().id(), id));
    }

    @GetMapping("/{id}/members/{userId}/stats")
    public ApiResponse<ClassMemberStatsVO> memberStats(@PathVariable Long id, @PathVariable Long userId) {
        return ApiResponse.ok(classRoomService.memberStats(currentUserProvider.requireStudent().id(), id, userId));
    }
}
