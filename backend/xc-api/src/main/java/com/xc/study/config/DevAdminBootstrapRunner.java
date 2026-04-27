package com.xc.study.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xc.study.module.admin.entity.AdminUser;
import com.xc.study.module.admin.mapper.AdminUserMapper;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Profile("dev")
public class DevAdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevAdminBootstrapRunner.class);

    private final AdminUserMapper adminUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;
    private final String username;
    private final String password;
    private final String displayName;

    public DevAdminBootstrapRunner(
            AdminUserMapper adminUserMapper,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap.admin.enabled:false}") boolean enabled,
            @Value("${app.bootstrap.admin.username:}") String username,
            @Value("${app.bootstrap.admin.password:}") String password,
            @Value("${app.bootstrap.admin.display-name:Local Admin}") String displayName
    ) {
        this.adminUserMapper = adminUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.username = username;
        this.password = password;
        this.displayName = displayName;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            log.warn("Development admin bootstrap is enabled but username or password is blank.");
            return;
        }
        if (password.length() < 8) {
            log.warn("Development admin bootstrap password must be at least 8 characters.");
            return;
        }

        AdminUser existing = adminUserMapper.selectOne(new LambdaQueryWrapper<AdminUser>()
                .eq(AdminUser::getUsername, username));
        if (existing != null) {
            log.info("Development admin user '{}' already exists, skipped bootstrap.", username);
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        AdminUser admin = new AdminUser();
        admin.setUsername(username);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setDisplayName(StringUtils.hasText(displayName) ? displayName : username);
        admin.setStatus("active");
        admin.setCreatedAt(now);
        admin.setUpdatedAt(now);
        adminUserMapper.insert(admin);
        log.info("Created development admin user '{}'.", username);
    }
}
