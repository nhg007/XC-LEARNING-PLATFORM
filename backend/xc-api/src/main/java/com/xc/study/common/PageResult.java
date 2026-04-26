package com.xc.study.common;

import java.util.List;

public record PageResult<T>(
        List<T> records,
        long total,
        long page,
        long pageSize
) {
    public static <T> PageResult<T> of(List<T> records, long total, long page, long pageSize) {
        return new PageResult<>(records, total, page, pageSize);
    }
}
