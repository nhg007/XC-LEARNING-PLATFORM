package com.xc.study.module.admin.vo;

import java.util.List;

public record AdminBatchBindMediaAssetResultVO(
        int requestedCount,
        int successCount,
        List<String> errors
) {
}
