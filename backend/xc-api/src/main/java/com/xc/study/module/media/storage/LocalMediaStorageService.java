package com.xc.study.module.media.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@ConditionalOnProperty(prefix = "app.media", name = "type", havingValue = "local", matchIfMissing = true)
public class LocalMediaStorageService implements MediaStorageService {

    private final Path storageRoot;
    private final String publicUrlPrefix;

    public LocalMediaStorageService(MediaStorageProperties properties) {
        this.storageRoot = Paths.get(properties.getStorageRoot()).toAbsolutePath().normalize();
        this.publicUrlPrefix = stripTrailingSlash(properties.getPublicUrlPrefix());
    }

    @Override
    public StoredMediaObject store(String objectKey, String contentType, long contentLength, InputStream inputStream) {
        Path target = resolveObjectKey(objectKey);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            return new StoredMediaObject(objectKey, publicUrlPrefix + "/" + objectKey);
        } catch (IOException ex) {
            throw new MediaStorageException("媒体文件保存失败", ex);
        }
    }

    @Override
    public MediaResource load(String objectKey) {
        Path target = resolveObjectKey(objectKey);
        if (!Files.isRegularFile(target)) {
            throw new MediaObjectNotFoundException("媒体文件不存在");
        }
        try {
            String contentType = Files.probeContentType(target);
            if (!StringUtils.hasText(contentType)) {
                contentType = "application/octet-stream";
            }
            return new MediaResource(Files.newInputStream(target), contentType, Files.size(target));
        } catch (IOException ex) {
            throw new MediaStorageException("媒体文件读取失败", ex);
        }
    }

    @Override
    public void delete(String objectKey) {
        Path target = resolveObjectKey(objectKey);
        try {
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            throw new MediaStorageException("媒体文件删除失败", ex);
        }
    }

    private Path resolveObjectKey(String objectKey) {
        String normalizedKey = normalizeObjectKey(objectKey);
        Path target = storageRoot.resolve(normalizedKey).normalize();
        if (!target.startsWith(storageRoot)) {
            throw new MediaStorageException("媒体对象路径不合法");
        }
        return target;
    }

    private String normalizeObjectKey(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            throw new MediaStorageException("媒体对象路径不能为空");
        }
        String normalized = objectKey.trim().replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (!StringUtils.hasText(normalized) || normalized.contains("..")) {
            throw new MediaStorageException("媒体对象路径不合法");
        }
        return normalized;
    }

    private String stripTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return "/api/media";
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
