package com.xc.study.module.admin.vo;

import java.util.List;

public record AdminContentImportResultVO(
        String importType,
        int requestedCount,
        int successCount,
        List<String> errors
) {
}
