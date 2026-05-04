package com.xc.study.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xc.study.module.admin.entity.AdminPermission;
import com.xc.study.module.admin.entity.AdminRole;
import com.xc.study.module.admin.entity.AdminRolePermission;
import com.xc.study.module.admin.entity.AdminUser;
import com.xc.study.module.admin.entity.AdminUserRole;
import com.xc.study.module.admin.mapper.AdminPermissionMapper;
import com.xc.study.module.admin.mapper.AdminRoleMapper;
import com.xc.study.module.admin.mapper.AdminRolePermissionMapper;
import com.xc.study.module.admin.mapper.AdminUserMapper;
import com.xc.study.module.admin.mapper.AdminUserRoleMapper;
import com.xc.study.module.classroom.entity.ClassMember;
import com.xc.study.module.classroom.entity.ClassRoom;
import com.xc.study.module.classroom.mapper.ClassMemberMapper;
import com.xc.study.module.classroom.mapper.ClassRoomMapper;
import com.xc.study.module.membership.entity.MembershipPlan;
import com.xc.study.module.membership.entity.UserMembership;
import com.xc.study.module.membership.mapper.MembershipPlanMapper;
import com.xc.study.module.membership.mapper.UserMembershipMapper;
import com.xc.study.module.payment.entity.PaymentOrder;
import com.xc.study.module.payment.mapper.PaymentOrderMapper;
import com.xc.study.module.payment.service.PaymentService;
import com.xc.study.module.user.entity.User;
import com.xc.study.module.user.entity.UserPreference;
import com.xc.study.module.user.mapper.UserMapper;
import com.xc.study.module.user.mapper.UserPreferenceMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
@Profile("dev")
public class DevDemoFlowBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDemoFlowBootstrapRunner.class);
    private static final String SUPER_ADMIN_ROLE_CODE = "super_admin";
    private static final String TEACHER_ADMIN_ROLE_CODE = "teacher_admin";
    private static final List<PermissionSeed> TEACHER_ADMIN_PERMISSIONS = List.of(
            new PermissionSeed("admin:classrooms:read", "班级查看", "classroom"),
            new PermissionSeed("admin:classrooms:update", "班级维护", "classroom")
    );

    private final JdbcTemplate jdbcTemplate;
    private final AdminUserMapper adminUserMapper;
    private final AdminRoleMapper adminRoleMapper;
    private final AdminPermissionMapper adminPermissionMapper;
    private final AdminRolePermissionMapper adminRolePermissionMapper;
    private final AdminUserRoleMapper adminUserRoleMapper;
    private final UserMapper userMapper;
    private final UserPreferenceMapper userPreferenceMapper;
    private final ClassRoomMapper classRoomMapper;
    private final ClassMemberMapper classMemberMapper;
    private final MembershipPlanMapper membershipPlanMapper;
    private final PaymentOrderMapper paymentOrderMapper;
    private final UserMembershipMapper userMembershipMapper;
    private final PaymentService paymentService;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;
    private final boolean resetUsers;
    private final boolean standardFlowEnabled;
    private final String teacherAdminUsername;
    private final String teacherAdminPassword;
    private final String teacherAdminDisplayName;
    private final String studentEmail;
    private final String studentPassword;
    private final String studentNickname;
    private final String className;
    private final String classDescription;
    private final String classInviteCode;
    private final String offlineTradeNo;

    public DevDemoFlowBootstrapRunner(
            JdbcTemplate jdbcTemplate,
            AdminUserMapper adminUserMapper,
            AdminRoleMapper adminRoleMapper,
            AdminPermissionMapper adminPermissionMapper,
            AdminRolePermissionMapper adminRolePermissionMapper,
            AdminUserRoleMapper adminUserRoleMapper,
            UserMapper userMapper,
            UserPreferenceMapper userPreferenceMapper,
            ClassRoomMapper classRoomMapper,
            ClassMemberMapper classMemberMapper,
            MembershipPlanMapper membershipPlanMapper,
            PaymentOrderMapper paymentOrderMapper,
            UserMembershipMapper userMembershipMapper,
            PaymentService paymentService,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap.demo-flow.enabled:false}") boolean enabled,
            @Value("${app.bootstrap.demo-flow.reset-users:false}") boolean resetUsers,
            @Value("${app.bootstrap.demo-flow.standard-flow-enabled:false}") boolean standardFlowEnabled,
            @Value("${app.bootstrap.demo-flow.teacher-admin.username:}") String teacherAdminUsername,
            @Value("${app.bootstrap.demo-flow.teacher-admin.password:}") String teacherAdminPassword,
            @Value("${app.bootstrap.demo-flow.teacher-admin.display-name:教师后台管理人员}") String teacherAdminDisplayName,
            @Value("${app.bootstrap.demo-flow.student.email:flow.student@example.com}") String studentEmail,
            @Value("${app.bootstrap.demo-flow.student.password:}") String studentPassword,
            @Value("${app.bootstrap.demo-flow.student.nickname:FlowStudent}") String studentNickname,
            @Value("${app.bootstrap.demo-flow.classroom.name:Demo HSK1 班级}") String className,
            @Value("${app.bootstrap.demo-flow.classroom.description:标准演示班级}") String classDescription,
            @Value("${app.bootstrap.demo-flow.classroom.invite-code:DEMO2026}") String classInviteCode,
            @Value("${app.bootstrap.demo-flow.offline-payment.trade-no:OFFLINE-DEMO-FLOW-001}") String offlineTradeNo
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.adminUserMapper = adminUserMapper;
        this.adminRoleMapper = adminRoleMapper;
        this.adminPermissionMapper = adminPermissionMapper;
        this.adminRolePermissionMapper = adminRolePermissionMapper;
        this.adminUserRoleMapper = adminUserRoleMapper;
        this.userMapper = userMapper;
        this.userPreferenceMapper = userPreferenceMapper;
        this.classRoomMapper = classRoomMapper;
        this.classMemberMapper = classMemberMapper;
        this.membershipPlanMapper = membershipPlanMapper;
        this.paymentOrderMapper = paymentOrderMapper;
        this.userMembershipMapper = userMembershipMapper;
        this.paymentService = paymentService;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.resetUsers = resetUsers;
        this.standardFlowEnabled = standardFlowEnabled;
        this.teacherAdminUsername = teacherAdminUsername;
        this.teacherAdminPassword = teacherAdminPassword;
        this.teacherAdminDisplayName = normalizeDisplayConfig(teacherAdminDisplayName);
        this.studentEmail = studentEmail;
        this.studentPassword = studentPassword;
        this.studentNickname = normalizeDisplayConfig(studentNickname);
        this.className = normalizeDisplayConfig(className);
        this.classDescription = normalizeDisplayConfig(classDescription);
        this.classInviteCode = classInviteCode;
        this.offlineTradeNo = offlineTradeNo;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        if (resetUsers) {
            resetUsersExceptSuperAdmin();
        }
        validateStandardFlowConfig();
        AdminRole teacherRole = ensureTeacherAdminRole(OffsetDateTime.now());
        AdminUser teacherAdmin = ensureTeacherAdminUser(teacherRole.getId(), OffsetDateTime.now());
        if (standardFlowEnabled) {
            ensureStandardFlow(teacherAdmin, OffsetDateTime.now());
        }
        log.info("Development demo flow bootstrap completed.");
    }

    private void resetUsersExceptSuperAdmin() {
        clearStudentUsers();
        Long superAdminCount = jdbcTemplate.queryForObject("""
                select count(*)
                from admin_users admin_user
                where exists (
                    select 1
                    from admin_user_roles user_role
                    join admin_roles role on role.id = user_role.role_id
                    where user_role.admin_user_id = admin_user.id
                      and role.role_code = ?
                )
                """, Long.class, SUPER_ADMIN_ROLE_CODE);
        if (superAdminCount == null || superAdminCount == 0) {
            log.warn("Demo flow reset skipped admin cleanup because no super admin user exists.");
            return;
        }
        jdbcTemplate.update("""
                update system_configs
                set updated_by = null,
                    updated_at = now()
                where updated_by is not null
                  and not exists (
                      select 1
                      from admin_user_roles user_role
                      join admin_roles role on role.id = user_role.role_id
                      where user_role.admin_user_id = system_configs.updated_by
                        and role.role_code = ?
                  )
                """, SUPER_ADMIN_ROLE_CODE);
        jdbcTemplate.update("""
                delete from admin_operation_logs
                where admin_user_id is not null
                  and not exists (
                      select 1
                      from admin_user_roles user_role
                      join admin_roles role on role.id = user_role.role_id
                      where user_role.admin_user_id = admin_operation_logs.admin_user_id
                        and role.role_code = ?
                  )
                """, SUPER_ADMIN_ROLE_CODE);
        jdbcTemplate.update("""
                delete from admin_user_roles
                where not exists (
                    select 1
                    from admin_user_roles super_user_role
                    join admin_roles role on role.id = super_user_role.role_id
                    where super_user_role.admin_user_id = admin_user_roles.admin_user_id
                      and role.role_code = ?
                )
                """, SUPER_ADMIN_ROLE_CODE);
        jdbcTemplate.update("""
                delete from admin_users
                where not exists (
                    select 1
                    from admin_user_roles user_role
                    join admin_roles role on role.id = user_role.role_id
                    where user_role.admin_user_id = admin_users.id
                      and role.role_code = ?
                )
                """, SUPER_ADMIN_ROLE_CODE);
        log.info("Development demo flow reset kept super admin users and removed other admin users.");
    }

    private void clearStudentUsers() {
        jdbcTemplate.update("delete from speech_records");
        jdbcTemplate.update("delete from asr_jobs");
        jdbcTemplate.update("delete from matching_game_sessions");
        jdbcTemplate.update("delete from user_sentence_favorites");
        jdbcTemplate.update("delete from user_sentence_progress");
        jdbcTemplate.update("delete from exercise_attempts");
        jdbcTemplate.update("delete from user_vocab_favorites");
        jdbcTemplate.update("delete from user_vocab_item_progress");
        jdbcTemplate.update("delete from user_vocab_progress");
        jdbcTemplate.update("delete from study_events");
        jdbcTemplate.update("delete from user_daily_stats");
        jdbcTemplate.update("delete from user_learning_summary");
        jdbcTemplate.update("delete from leaderboard_entries");
        jdbcTemplate.update("delete from class_members");
        jdbcTemplate.update("delete from classes");
        jdbcTemplate.update("delete from user_memberships");
        jdbcTemplate.update("delete from payment_notifications");
        jdbcTemplate.update("delete from payment_orders");
        jdbcTemplate.update("delete from user_preferences");
        jdbcTemplate.update("delete from users");
        log.info("Development demo flow reset removed all student users and user-owned records.");
    }

    private AdminRole ensureTeacherAdminRole(OffsetDateTime now) {
        AdminRole role = adminRoleMapper.selectOne(new LambdaQueryWrapper<AdminRole>()
                .eq(AdminRole::getRoleCode, TEACHER_ADMIN_ROLE_CODE)
                .last("limit 1"));
        if (role == null) {
            role = new AdminRole();
            role.setRoleCode(TEACHER_ADMIN_ROLE_CODE);
            role.setCreatedAt(now);
        }
        role.setRoleName("教师后台管理人员");
        role.setDescription("维护自己负责的班级并查看班级学习情况");
        role.setUpdatedAt(now);
        if (role.getId() == null) {
            adminRoleMapper.insert(role);
        } else {
            adminRoleMapper.updateById(role);
        }
        replaceRolePermissions(role.getId(), now);
        return role;
    }

    private void replaceRolePermissions(Long roleId, OffsetDateTime now) {
        adminRolePermissionMapper.delete(new LambdaQueryWrapper<AdminRolePermission>()
                .eq(AdminRolePermission::getRoleId, roleId));
        for (PermissionSeed seed : TEACHER_ADMIN_PERMISSIONS) {
            AdminPermission permission = ensurePermission(seed, now);
            AdminRolePermission relation = new AdminRolePermission();
            relation.setRoleId(roleId);
            relation.setPermissionId(permission.getId());
            relation.setCreatedAt(now);
            relation.setUpdatedAt(now);
            adminRolePermissionMapper.insert(relation);
        }
    }

    private AdminPermission ensurePermission(PermissionSeed seed, OffsetDateTime now) {
        AdminPermission permission = adminPermissionMapper.selectOne(new LambdaQueryWrapper<AdminPermission>()
                .eq(AdminPermission::getPermissionCode, seed.permissionCode())
                .last("limit 1"));
        if (permission == null) {
            permission = new AdminPermission();
            permission.setPermissionCode(seed.permissionCode());
            permission.setCreatedAt(now);
        }
        permission.setPermissionName(seed.permissionName());
        permission.setModuleName(seed.moduleName());
        permission.setUpdatedAt(now);
        if (permission.getId() == null) {
            adminPermissionMapper.insert(permission);
        } else {
            adminPermissionMapper.updateById(permission);
        }
        return permission;
    }

    private void validateStandardFlowConfig() {
        if (!standardFlowEnabled) {
            return;
        }
        requireConfigured(teacherAdminUsername, "APP_BOOTSTRAP_DEMO_FLOW_TEACHER_ADMIN_USERNAME");
        requireConfigured(teacherAdminPassword, "APP_BOOTSTRAP_DEMO_FLOW_TEACHER_ADMIN_PASSWORD");
        requireConfigured(studentEmail, "APP_BOOTSTRAP_DEMO_FLOW_STUDENT_EMAIL");
        requireConfigured(studentPassword, "APP_BOOTSTRAP_DEMO_FLOW_STUDENT_PASSWORD");
        requireConfigured(className, "APP_BOOTSTRAP_DEMO_FLOW_CLASS_NAME");
        requireConfigured(classInviteCode, "APP_BOOTSTRAP_DEMO_FLOW_CLASS_INVITE_CODE");
        requireConfigured(offlineTradeNo, "APP_BOOTSTRAP_DEMO_FLOW_OFFLINE_TRADE_NO");
    }

    private void requireConfigured(String value, String configName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(configName + " is required when APP_BOOTSTRAP_DEMO_FLOW_STANDARD_ENABLED=true");
        }
    }

    private AdminUser ensureTeacherAdminUser(Long roleId, OffsetDateTime now) {
        if (!StringUtils.hasText(teacherAdminUsername) || !StringUtils.hasText(teacherAdminPassword)) {
            log.warn("Demo flow teacher admin username or password is blank, skipped teacher admin user bootstrap.");
            return null;
        }
        if (teacherAdminPassword.length() < 8) {
            log.warn("Demo flow teacher admin password must be at least 8 characters.");
            return null;
        }
        String username = teacherAdminUsername.trim().toLowerCase(Locale.ROOT);
        AdminUser admin = adminUserMapper.selectOne(new LambdaQueryWrapper<AdminUser>()
                .eq(AdminUser::getUsername, username)
                .last("limit 1"));
        if (admin == null) {
            admin = new AdminUser();
            admin.setUsername(username);
            admin.setCreatedAt(now);
        }
        admin.setPasswordHash(passwordEncoder.encode(teacherAdminPassword));
        admin.setDisplayName(StringUtils.hasText(teacherAdminDisplayName) ? teacherAdminDisplayName.trim() : username);
        admin.setStatus("active");
        admin.setUpdatedAt(now);
        if (admin.getId() == null) {
            adminUserMapper.insert(admin);
        } else {
            adminUserMapper.updateById(admin);
        }
        ensureAdminUserRole(admin.getId(), roleId, now);
        return admin;
    }

    private void ensureStandardFlow(AdminUser teacherAdmin, OffsetDateTime now) {
        if (teacherAdmin == null) {
            throw new IllegalStateException("Teacher admin account was not initialized.");
        }
        User studentUser = ensureUser(studentEmail, studentPassword, studentNickname, now);
        ClassRoom classRoom = ensureDemoClassRoom(teacherAdmin, now);
        ensureStudentClassMember(classRoom.getId(), studentUser.getId(), now);
        MembershipPlan plan = ensureDemoPlan(now);
        ensureOfflinePaidMembership(studentUser, plan, now);
        log.info("Standard demo flow is ready: teacherAdmin={}, student={}, class={}",
                teacherAdmin.getUsername(), studentUser.getEmail(), classRoom.getName());
    }

    private User ensureUser(String emailValue, String password, String nickname, OffsetDateTime now) {
        if (password.length() < 8) {
            throw new IllegalStateException("Demo flow user password must be at least 8 characters.");
        }
        String email = emailValue.trim().toLowerCase(Locale.ROOT);
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email)
                .last("limit 1"));
        if (user == null) {
            user = new User();
            user.setEmail(email);
            user.setTrialStartedAt(now);
            user.setTrialEndsAt(now.plusDays(7));
            user.setCreatedAt(now);
        }
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setNickname(StringUtils.hasText(nickname) ? nickname.trim() : email);
        user.setStatus("active");
        user.setUpdatedAt(now);
        if (user.getId() == null) {
            userMapper.insert(user);
        } else {
            userMapper.updateById(user);
        }
        ensurePreference(user.getId(), now);
        return user;
    }

    private void ensurePreference(Long userId, OffsetDateTime now) {
        UserPreference preference = userPreferenceMapper.selectOne(new LambdaQueryWrapper<UserPreference>()
                .eq(UserPreference::getUserId, userId)
                .last("limit 1"));
        if (preference == null) {
            preference = new UserPreference();
            preference.setUserId(userId);
            preference.setCreatedAt(now);
        }
        preference.setUiLanguage("zh");
        preference.setTranslationLanguage("ru");
        preference.setVocabMeaningLanguage("ru");
        preference.setMatchingMeaningLanguage("ru");
        preference.setSoundEnabled(true);
        preference.setUpdatedAt(now);
        if (preference.getId() == null) {
            userPreferenceMapper.insert(preference);
        } else {
            userPreferenceMapper.updateById(preference);
        }
    }

    private ClassRoom ensureDemoClassRoom(AdminUser teacherAdmin, OffsetDateTime now) {
        String inviteCode = classInviteCode.trim().toUpperCase(Locale.ROOT);
        ClassRoom room = classRoomMapper.selectOne(new LambdaQueryWrapper<ClassRoom>()
                .eq(ClassRoom::getInviteCode, inviteCode)
                .last("limit 1"));
        if (room == null) {
            room = new ClassRoom();
            room.setInviteCode(inviteCode);
            room.setCreatedAt(now);
        }
        room.setName(className.trim());
        room.setDescription(StringUtils.hasText(classDescription) ? classDescription.trim() : null);
        room.setTeacherAdminUserId(teacherAdmin.getId());
        room.setOwnerUserId(null);
        room.setStatus("active");
        room.setUpdatedAt(now);
        if (room.getId() == null) {
            classRoomMapper.insert(room);
        } else {
            classRoomMapper.updateById(room);
        }
        return room;
    }

    private void ensureStudentClassMember(Long classId, Long userId, OffsetDateTime now) {
        ClassMember member = classMemberMapper.selectOne(new LambdaQueryWrapper<ClassMember>()
                .eq(ClassMember::getClassId, classId)
                .eq(ClassMember::getUserId, userId)
                .last("limit 1"));
        if (member == null) {
            member = new ClassMember();
            member.setClassId(classId);
            member.setUserId(userId);
            member.setCreatedAt(now);
        }
        member.setMemberRole("member");
        member.setStatus("active");
        member.setInvitedByUserId(null);
        member.setReviewedByUserId(null);
        member.setReviewedAt(null);
        member.setJoinedAt(now);
        member.setRemovedAt(null);
        member.setUpdatedAt(now);
        if (member.getId() == null) {
            classMemberMapper.insert(member);
        } else {
            classMemberMapper.updateById(member);
        }
    }

    private MembershipPlan ensureDemoPlan(OffsetDateTime now) {
        MembershipPlan plan = membershipPlanMapper.selectOne(new LambdaQueryWrapper<MembershipPlan>()
                .eq(MembershipPlan::getName, "月度会员")
                .eq(MembershipPlan::getStatus, "active")
                .last("limit 1"));
        if (plan == null) {
            plan = membershipPlanMapper.selectOne(new LambdaQueryWrapper<MembershipPlan>()
                    .eq(MembershipPlan::getStatus, "active")
                    .orderByAsc(MembershipPlan::getId)
                    .last("limit 1"));
            if (plan != null) {
                return plan;
            }
        }
        if (plan == null) {
            plan = new MembershipPlan();
            plan.setName("月度会员");
            plan.setCreatedAt(now);
        }
        plan.setDurationUnit("month");
        plan.setDurationValue(1);
        plan.setDurationDays(30);
        plan.setPrice(new BigDecimal("29.00"));
        plan.setCurrency("CNY");
        plan.setStatus("active");
        plan.setUpdatedAt(now);
        if (plan.getId() == null) {
            membershipPlanMapper.insert(plan);
        } else {
            membershipPlanMapper.updateById(plan);
        }
        return plan;
    }

    private void ensureOfflinePaidMembership(User studentUser, MembershipPlan plan, OffsetDateTime now) {
        String tradeNo = offlineTradeNo.trim();
        PaymentOrder existing = paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getUserId, studentUser.getId())
                .eq(PaymentOrder::getProvider, "offline")
                .eq(PaymentOrder::getProviderTradeNo, tradeNo)
                .last("limit 1"));
        if (existing == null) {
            paymentService.createOfflinePaidOrder(
                    studentUser.getId(),
                    plan.getId(),
                    plan.getPrice(),
                    plan.getCurrency(),
                    now,
                    tradeNo
            );
            return;
        }
        if (!"paid".equals(existing.getStatus())) {
            existing.setStatus("paid");
            existing.setPaidAt(now);
            existing.setProviderTradeNo(tradeNo);
            existing.setUpdatedAt(now);
            paymentOrderMapper.updateById(existing);
        }
        ensureMembershipForOrder(existing, plan, existing.getPaidAt() == null ? now : existing.getPaidAt());
    }

    private void ensureMembershipForOrder(PaymentOrder order, MembershipPlan plan, OffsetDateTime paidAt) {
        UserMembership existing = userMembershipMapper.selectOne(new LambdaQueryWrapper<UserMembership>()
                .eq(UserMembership::getPaymentOrderId, order.getId())
                .last("limit 1"));
        if (existing != null) {
            if (!"active".equals(existing.getStatus()) || existing.getEndsAt() == null || !existing.getEndsAt().isAfter(paidAt)) {
                existing.setStatus("active");
                existing.setEndsAt(paidAt.plusDays(plan.getDurationDays()));
                existing.setUpdatedAt(OffsetDateTime.now());
                userMembershipMapper.updateById(existing);
            }
            return;
        }
        UserMembership membership = new UserMembership();
        membership.setUserId(order.getUserId());
        membership.setPlanId(plan.getId());
        membership.setPaymentOrderId(order.getId());
        membership.setStartedAt(paidAt);
        membership.setEndsAt(paidAt.plusDays(plan.getDurationDays()));
        membership.setSource("payment");
        membership.setStatus("active");
        membership.setCreatedAt(paidAt);
        membership.setUpdatedAt(paidAt);
        userMembershipMapper.insert(membership);
    }

    private void ensureAdminUserRole(Long adminUserId, Long roleId, OffsetDateTime now) {
        AdminUserRole existing = adminUserRoleMapper.selectOne(new LambdaQueryWrapper<AdminUserRole>()
                .eq(AdminUserRole::getAdminUserId, adminUserId)
                .eq(AdminUserRole::getRoleId, roleId)
                .last("limit 1"));
        if (existing != null) {
            return;
        }
        AdminUserRole relation = new AdminUserRole();
        relation.setAdminUserId(adminUserId);
        relation.setRoleId(roleId);
        relation.setCreatedAt(now);
        relation.setUpdatedAt(now);
        adminUserRoleMapper.insert(relation);
    }

    private record PermissionSeed(String permissionCode, String permissionName, String moduleName) {
    }

    private String normalizeDisplayConfig(String value) {
        if (value == null) {
            return null;
        }
        return repairUtf8ReadAsLatin1(decodeUnicodeEscapes(value));
    }

    private String decodeUnicodeEscapes(String value) {
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current == '\\' && i + 5 < value.length() && value.charAt(i + 1) == 'u') {
                String hex = value.substring(i + 2, i + 6);
                try {
                    result.append((char) Integer.parseInt(hex, 16));
                    i += 5;
                    continue;
                } catch (NumberFormatException ignored) {
                    // Keep the original characters when the sequence is not a unicode escape.
                }
            }
            result.append(current);
        }
        return result.toString();
    }

    private String repairUtf8ReadAsLatin1(String value) {
        if (value.chars().noneMatch(this::isC1Control)) {
            return value;
        }
        return new String(value.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
    }

    private boolean isC1Control(int codePoint) {
        return codePoint >= 0x80 && codePoint <= 0x9F;
    }
}
