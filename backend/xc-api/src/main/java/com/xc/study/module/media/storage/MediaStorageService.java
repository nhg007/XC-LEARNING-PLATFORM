package com.xc.study.module.media.storage;

import java.io.InputStream;

public interface MediaStorageService {

    StoredMediaObject store(String objectKey, String contentType, long contentLength, InputStream inputStream);

    MediaResource load(String objectKey);
}
