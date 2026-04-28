package com.xc.study.module.media.storage;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import java.io.InputStream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@ConditionalOnProperty(prefix = "app.media", name = "type", havingValue = "minio")
public class MinioMediaStorageService implements MediaStorageService {

    private final MinioClient minioClient;
    private final MediaStorageProperties properties;
    private final String bucket;
    private final String publicUrlPrefix;
    private volatile boolean bucketReady;

    public MinioMediaStorageService(MediaStorageProperties properties) {
        MediaStorageProperties.Minio minio = properties.getMinio();
        requireText(minio.getEndpoint(), "MINIO_ENDPOINT");
        requireText(minio.getAccessKey(), "MINIO_ACCESS_KEY");
        requireText(minio.getSecretKey(), "MINIO_SECRET_KEY");
        requireText(minio.getBucket(), "MINIO_BUCKET");
        this.properties = properties;
        this.bucket = minio.getBucket();
        this.publicUrlPrefix = stripTrailingSlash(properties.getPublicUrlPrefix());
        this.minioClient = MinioClient.builder()
                .endpoint(minio.getEndpoint())
                .credentials(minio.getAccessKey(), minio.getSecretKey())
                .build();
    }

    @Override
    public StoredMediaObject store(String objectKey, String contentType, long contentLength, InputStream inputStream) {
        ensureBucket();
        String normalizedKey = normalizeObjectKey(objectKey);
        try {
            PutObjectArgs.Builder args = PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(normalizedKey)
                    .stream(inputStream, contentLength, -1);
            if (StringUtils.hasText(contentType)) {
                args.contentType(contentType);
            }
            minioClient.putObject(args.build());
            return new StoredMediaObject(normalizedKey, publicUrlPrefix + "/" + normalizedKey);
        } catch (Exception ex) {
            throw new MediaStorageException("媒体文件上传 MinIO 失败", ex);
        }
    }

    @Override
    public MediaResource load(String objectKey) {
        String normalizedKey = normalizeObjectKey(objectKey);
        try {
            StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(normalizedKey)
                    .build());
            GetObjectResponse object = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(normalizedKey)
                    .build());
            String contentType = StringUtils.hasText(stat.contentType())
                    ? stat.contentType()
                    : "application/octet-stream";
            return new MediaResource(object, contentType, stat.size());
        } catch (ErrorResponseException ex) {
            if ("NoSuchKey".equals(ex.errorResponse().code())) {
                throw new MediaObjectNotFoundException("媒体文件不存在", ex);
            }
            throw new MediaStorageException("媒体文件读取失败", ex);
        } catch (Exception ex) {
            throw new MediaStorageException("媒体文件读取失败", ex);
        }
    }

    private void ensureBucket() {
        if (bucketReady) {
            return;
        }
        synchronized (this) {
            if (bucketReady) {
                return;
            }
            try {
                boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
                if (!exists && properties.getMinio().isCreateBucket()) {
                    minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                } else if (!exists) {
                    throw new MediaStorageException("MinIO bucket 不存在：" + bucket);
                }
                bucketReady = true;
            } catch (Exception ex) {
                throw new MediaStorageException("MinIO bucket 初始化失败", ex);
            }
        }
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

    private void requireText(String value, String name) {
        if (!StringUtils.hasText(value)) {
            throw new MediaStorageException(name + " 未配置");
        }
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
