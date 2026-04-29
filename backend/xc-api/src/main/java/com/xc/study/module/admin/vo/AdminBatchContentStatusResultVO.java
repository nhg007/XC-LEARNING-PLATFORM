package com.xc.study.module.admin.vo;

import java.util.List;

public record AdminBatchContentStatusResultVO(
        int requestedCount,
        int successCount,
        List<String> errors
) {
}
