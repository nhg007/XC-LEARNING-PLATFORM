package com.xc.study.module.media.storage;

import java.io.InputStream;

public record MediaResource(
        InputStream inputStream,
        String contentType,
        long contentLength
) {
}
