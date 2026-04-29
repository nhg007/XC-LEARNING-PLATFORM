package com.xc.study.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DevDemoContentBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDemoContentBootstrapRunner.class);

    private final DevDemoContentSeeder contentSeeder;
    private final boolean enabled;

    public DevDemoContentBootstrapRunner(
            DevDemoContentSeeder contentSeeder,
            @Value("${app.bootstrap.demo-content.enabled:false}") boolean enabled
    ) {
        this.contentSeeder = contentSeeder;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        contentSeeder.ensureContent();
        log.info("Development demo content bootstrap completed.");
    }
}
