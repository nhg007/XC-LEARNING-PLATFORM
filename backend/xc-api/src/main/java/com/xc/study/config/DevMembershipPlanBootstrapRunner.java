package com.xc.study.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xc.study.module.membership.entity.MembershipPlan;
import com.xc.study.module.membership.mapper.MembershipPlanMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DevMembershipPlanBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevMembershipPlanBootstrapRunner.class);

    private final MembershipPlanMapper membershipPlanMapper;
    private final boolean enabled;

    public DevMembershipPlanBootstrapRunner(
            MembershipPlanMapper membershipPlanMapper,
            @Value("${app.bootstrap.membership-plans.enabled:true}") boolean enabled
    ) {
        this.membershipPlanMapper = membershipPlanMapper;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        OffsetDateTime now = OffsetDateTime.now();
        ensurePlan("月度会员", "month", 1, new BigDecimal("29.00"), now);
        ensurePlan("季度会员", "month", 3, new BigDecimal("79.00"), now);
        ensurePlan("年度会员", "month", 12, new BigDecimal("268.00"), now);
        log.info("Development membership plans are ready.");
    }

    private void ensurePlan(String name, String durationUnit, int durationValue, BigDecimal price, OffsetDateTime now) {
        MembershipPlan plan = membershipPlanMapper.selectOne(new LambdaQueryWrapper<MembershipPlan>()
                .eq(MembershipPlan::getName, name)
                .last("limit 1"));
        if (plan == null) {
            plan = new MembershipPlan();
            plan.setName(name);
            plan.setCreatedAt(now);
        }
        plan.setDurationUnit(durationUnit);
        plan.setDurationValue(durationValue);
        plan.setDurationDays(resolveDurationDays(durationUnit, durationValue));
        plan.setPrice(price);
        plan.setCurrency("CNY");
        plan.setStatus("active");
        plan.setUpdatedAt(now);
        if (plan.getId() == null) {
            membershipPlanMapper.insert(plan);
        } else {
            membershipPlanMapper.updateById(plan);
        }
    }

    private int resolveDurationDays(String durationUnit, int durationValue) {
        if ("month".equals(durationUnit)) {
            return durationValue * 30;
        }
        return durationValue;
    }
}
