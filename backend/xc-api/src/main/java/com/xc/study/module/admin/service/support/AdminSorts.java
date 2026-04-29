package com.xc.study.module.admin.service.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import java.util.Map;
import org.springframework.util.StringUtils;

public final class AdminSorts {

    private AdminSorts() {
    }

    public static <T> boolean apply(
            LambdaQueryWrapper<T> wrapper,
            String sortBy,
            String sortDirection,
            Map<String, SFunction<T, ?>> allowedFields
    ) {
        if (!StringUtils.hasText(sortBy) || !StringUtils.hasText(sortDirection)) {
            return false;
        }
        SFunction<T, ?> field = allowedFields.get(sortBy.trim());
        if (field == null) {
            return false;
        }
        boolean ascending = "asc".equals(sortDirection);
        boolean descending = "desc".equals(sortDirection);
        if (!ascending && !descending) {
            return false;
        }
        wrapper.orderBy(true, ascending, field);
        return true;
    }
}
