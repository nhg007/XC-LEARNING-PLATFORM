package com.xc.study.module.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xc.study.common.BusinessException;
import com.xc.study.common.ErrorCode;
import com.xc.study.common.PageResult;
import com.xc.study.module.admin.dto.AdminBatchUpdateContentStatusDTO;
import com.xc.study.module.admin.dto.AdminMediaAssetQueryDTO;
import com.xc.study.module.admin.dto.AdminUpdateContentStatusDTO;
import com.xc.study.module.admin.dto.AdminUploadMediaAssetDTO;
import com.xc.study.module.admin.entity.AdminOperationLog;
import com.xc.study.module.admin.mapper.AdminOperationLogMapper;
import com.xc.study.module.admin.service.AdminMediaAssetService;
import com.xc.study.module.admin.vo.AdminBatchContentStatusResultVO;
import com.xc.study.module.admin.vo.AdminMediaAssetVO;
import com.xc.study.module.media.entity.MediaAsset;
import com.xc.study.module.media.mapper.MediaAssetMapper;
import com.xc.study.module.media.storage.MediaStorageException;
import com.xc.study.module.media.storage.MediaStorageProperties;
import com.xc.study.module.media.storage.MediaStorageService;
import com.xc.study.module.media.storage.StoredMediaObject;
import com.xc.study.security.CurrentUser;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AdminMediaAssetServiceImpl implements AdminMediaAssetService {

    private static final Set<String> AUDIO_EXTENSIONS = Set.of("mp3", "wav", "m4a", "ogg", "aac", "webm");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "webm", "mov");
    private static final List<MediaReferenceTarget> MEDIA_REFERENCE_TARGETS = List.of(
            new MediaReferenceTarget("vocab_items", "audio_asset_id", "词汇音频"),
            new MediaReferenceTarget("sentence_exercises", "audio_zh_asset_id", "句子练习音频"),
            new MediaReferenceTarget("video_materials", "cover_asset_id", "台词材料封面"),
            new MediaReferenceTarget("dialogue_lines", "audio_asset_id", "台词音频"),
            new MediaReferenceTarget("asr_jobs", "audio_asset_id", "ASR 任务"),
            new MediaReferenceTarget("speech_records", "audio_asset_id", "口语记录")
    );

    private final MediaAssetMapper mediaAssetMapper;
    private final AdminOperationLogMapper adminOperationLogMapper;
    private final ObjectMapper objectMapper;
    private final MediaStorageService mediaStorageService;
    private final MediaStorageProperties mediaStorageProperties;
    private final JdbcTemplate jdbcTemplate;

    public AdminMediaAssetServiceImpl(
            MediaAssetMapper mediaAssetMapper,
            AdminOperationLogMapper adminOperationLogMapper,
            ObjectMapper objectMapper,
            MediaStorageService mediaStorageService,
            MediaStorageProperties mediaStorageProperties,
            JdbcTemplate jdbcTemplate
    ) {
        this.mediaAssetMapper = mediaAssetMapper;
        this.adminOperationLogMapper = adminOperationLogMapper;
        this.objectMapper = objectMapper;
        this.mediaStorageService = mediaStorageService;
        this.mediaStorageProperties = mediaStorageProperties;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PageResult<AdminMediaAssetVO> pageAssets(AdminMediaAssetQueryDTO query, CurrentUser admin) {
        requirePermission(admin, "admin:content:read");
        int page = query.getPage() == null ? 1 : query.getPage();
        int pageSize = query.getPageSize() == null ? 20 : query.getPageSize();
        LambdaQueryWrapper<MediaAsset> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getMediaType())) {
            wrapper.eq(MediaAsset::getMediaType, query.getMediaType());
        }
        if (StringUtils.hasText(query.getLanguage())) {
            wrapper.eq(MediaAsset::getLanguage, query.getLanguage());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(MediaAsset::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.like(MediaAsset::getUrl, keyword);
        }
        wrapper.orderByDesc(MediaAsset::getCreatedAt).orderByDesc(MediaAsset::getId);
        Page<MediaAsset> result = mediaAssetMapper.selectPage(Page.of(page, pageSize), wrapper);
        return PageResult.of(
                result.getRecords().stream().map(this::toVO).toList(),
                result.getTotal(),
                result.getCurrent(),
                result.getSize()
        );
    }

    @Override
    public AdminMediaAssetVO upload(AdminUploadMediaAssetDTO request, CurrentUser admin, String ipAddress) {
        requirePermission(admin, "admin:content:update");
        MultipartFile file = request.getFile();
        validateFile(request.getMediaType(), file);

        String extension = extension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + "." + extension;
        String objectKey = request.getMediaType() + "/" + filename;
        StoredMediaObject storedObject;
        try {
            try (InputStream inputStream = file.getInputStream()) {
                storedObject = mediaStorageService.store(objectKey, file.getContentType(), file.getSize(), inputStream);
            }
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "媒体文件保存失败");
        } catch (MediaStorageException ex) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, "媒体文件保存失败");
        }

        OffsetDateTime now = OffsetDateTime.now();
        MediaAsset asset = new MediaAsset();
        asset.setMediaType(request.getMediaType());
        asset.setObjectKey(storedObject.objectKey());
        asset.setUrl(storedObject.url());
        asset.setLanguage(blankToNull(request.getLanguage()));
        asset.setDurationMs(request.getDurationMs());
        asset.setFileSize(file.getSize());
        asset.setStatus("active");
        asset.setCreatedAt(now);
        asset.setUpdatedAt(now);
        mediaAssetMapper.insert(asset);

        writeOperationLog(admin.id(), "content.media.upload", "media_asset", asset.getId(), Map.of(
                "mediaType", asset.getMediaType(),
                "objectKey", asset.getObjectKey(),
                "url", asset.getUrl(),
                "language", asset.getLanguage() == null ? "" : asset.getLanguage(),
                "fileSize", asset.getFileSize()
        ), ipAddress);
        return toVO(asset);
    }

    @Override
    public void delete(Long assetId, CurrentUser admin, String ipAddress) {
        requirePermission(admin, "admin:content:update");
        MediaAsset asset = mediaAssetMapper.selectById(assetId);
        if (asset == null) {
            throw BusinessException.notFound("媒体资源不存在");
        }
        if (!"inactive".equals(asset.getStatus())) {
            throw BusinessException.conflict("请先停用媒体资源后再删除");
        }
        Map<String, Long> references = findReferences(assetId);
        if (!references.isEmpty()) {
            throw BusinessException.conflict("媒体资源仍被引用，请先解绑后再删除：" + formatReferences(references));
        }

        String objectKey = resolveObjectKey(asset);
        try {
            mediaStorageService.delete(objectKey);
        } catch (MediaStorageException ex) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, "媒体文件删除失败");
        }

        try {
            int deleted = mediaAssetMapper.deleteById(assetId);
            if (deleted == 0) {
                throw BusinessException.notFound("媒体资源不存在");
            }
        } catch (DataIntegrityViolationException ex) {
            throw BusinessException.conflict("媒体资源仍被引用，请先解绑后再删除");
        }

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("mediaType", asset.getMediaType());
        detail.put("objectKey", objectKey);
        detail.put("url", asset.getUrl());
        detail.put("fileSize", asset.getFileSize() == null ? 0 : asset.getFileSize());
        writeOperationLog(admin.id(), "content.media.delete", "media_asset", assetId, detail, ipAddress);
    }

    @Override
    public AdminMediaAssetVO updateStatus(Long assetId, AdminUpdateContentStatusDTO request, CurrentUser admin, String ipAddress) {
        requirePermission(admin, "admin:content:update");
        MediaAsset asset = mediaAssetMapper.selectById(assetId);
        if (asset == null) {
            throw BusinessException.notFound("媒体资源不存在");
        }
        String beforeStatus = asset.getStatus();
        asset.setStatus(request.status());
        asset.setUpdatedAt(OffsetDateTime.now());
        mediaAssetMapper.updateById(asset);
        writeOperationLog(admin.id(), "content.media.status.update", "media_asset", assetId, Map.of(
                "beforeStatus", beforeStatus == null ? "" : beforeStatus,
                "afterStatus", request.status(),
                "reason", request.reason() == null ? "" : request.reason()
        ), ipAddress);
        return toVO(asset);
    }

    @Override
    @Transactional
    public AdminBatchContentStatusResultVO updateStatuses(
            AdminBatchUpdateContentStatusDTO request,
            CurrentUser admin,
            String ipAddress
    ) {
        requirePermission(admin, "admin:content:update");
        OffsetDateTime now = OffsetDateTime.now();
        List<String> errors = new java.util.ArrayList<>();
        List<Map<String, Object>> changes = new java.util.ArrayList<>();
        for (Long assetId : request.ids()) {
            MediaAsset asset = mediaAssetMapper.selectById(assetId);
            if (asset == null) {
                errors.add("媒体资源 ID " + assetId + " 不存在");
                continue;
            }
            String beforeStatus = asset.getStatus();
            asset.setStatus(request.status());
            asset.setUpdatedAt(now);
            mediaAssetMapper.updateById(asset);
            changes.add(Map.of(
                    "id", assetId,
                    "beforeStatus", beforeStatus == null ? "" : beforeStatus,
                    "afterStatus", request.status()
            ));
        }
        writeOperationLog(admin.id(), "content.media.status.batch_update", "media_asset", null, Map.of(
                "requestedCount", request.ids().size(),
                "successCount", changes.size(),
                "afterStatus", request.status(),
                "reason", request.reason() == null ? "" : request.reason(),
                "errors", errors,
                "changes", changes
        ), ipAddress);
        return new AdminBatchContentStatusResultVO(request.ids().size(), changes.size(), errors);
    }

    private Map<String, Long> findReferences(Long assetId) {
        Map<String, Long> references = new LinkedHashMap<>();
        for (MediaReferenceTarget target : MEDIA_REFERENCE_TARGETS) {
            Long count = jdbcTemplate.queryForObject(
                    "select count(*) from " + target.table() + " where " + target.column() + " = ?",
                    Long.class,
                    assetId
            );
            if (count != null && count > 0) {
                references.put(target.label(), count);
            }
        }
        return references;
    }

    private String formatReferences(Map<String, Long> references) {
        return references.entrySet()
                .stream()
                .map(entry -> entry.getKey() + " " + entry.getValue())
                .toList()
                .toString();
    }

    private String resolveObjectKey(MediaAsset asset) {
        if (StringUtils.hasText(asset.getObjectKey())) {
            return asset.getObjectKey().trim();
        }
        String objectKey = objectKeyFromUrl(asset.getUrl());
        if (StringUtils.hasText(objectKey)) {
            return objectKey;
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "无法解析媒体对象路径，不能执行物理删除");
    }

    private String objectKeyFromUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        String trimmedUrl = url.trim();
        String publicPrefix = stripTrailingSlash(mediaStorageProperties.getPublicUrlPrefix());
        String objectKey = objectKeyAfterPrefix(trimmedUrl, publicPrefix);
        if (StringUtils.hasText(objectKey)) {
            return objectKey;
        }
        try {
            URI uri = URI.create(trimmedUrl);
            String path = uri.getPath();
            if (!StringUtils.hasText(path)) {
                return null;
            }
            String pathPrefix = publicPrefix;
            try {
                URI prefixUri = URI.create(publicPrefix);
                if (StringUtils.hasText(prefixUri.getPath())) {
                    pathPrefix = stripTrailingSlash(prefixUri.getPath());
                }
            } catch (IllegalArgumentException ignored) {
                pathPrefix = publicPrefix;
            }
            return objectKeyAfterPrefix(path, pathPrefix);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String objectKeyAfterPrefix(String value, String prefix) {
        if (!StringUtils.hasText(value) || !StringUtils.hasText(prefix)) {
            return null;
        }
        String normalizedPrefix = stripTrailingSlash(prefix);
        String expectedPrefix = normalizedPrefix + "/";
        if (!value.startsWith(expectedPrefix)) {
            return null;
        }
        return URLDecoder.decode(value.substring(expectedPrefix.length()), StandardCharsets.UTF_8);
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

    private void validateFile(String mediaType, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "请选择媒体文件");
        }
        String extension = extension(file.getOriginalFilename());
        if (!allowedExtensions(mediaType).contains(extension)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "文件后缀不支持");
        }
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.toLowerCase(Locale.ROOT).startsWith(mediaType + "/")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "文件 MIME 类型不匹配");
        }
    }

    private Set<String> allowedExtensions(String mediaType) {
        return switch (mediaType) {
            case "audio" -> AUDIO_EXTENSIONS;
            case "image" -> IMAGE_EXTENSIONS;
            case "video" -> VIDEO_EXTENSIONS;
            default -> Set.of();
        };
    }

    private String extension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            return "";
        }
        return filename.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private AdminMediaAssetVO toVO(MediaAsset asset) {
        return new AdminMediaAssetVO(
                asset.getId(),
                asset.getMediaType(),
                asset.getUrl(),
                asset.getLanguage(),
                asset.getDurationMs(),
                asset.getFileSize(),
                asset.getStatus(),
                asset.getCreatedAt(),
                asset.getUpdatedAt()
        );
    }

    private void requirePermission(CurrentUser admin, String permission) {
        if (admin.permissions().contains("admin:*") || admin.permissions().contains(permission)) {
            return;
        }
        throw BusinessException.forbidden(ErrorCode.FORBIDDEN, "缺少后台权限：" + permission);
    }

    private void writeOperationLog(
            Long adminUserId,
            String action,
            String targetType,
            Long targetId,
            Map<String, Object> detail,
            String ipAddress
    ) {
        OffsetDateTime now = OffsetDateTime.now();
        AdminOperationLog log = new AdminOperationLog();
        log.setAdminUserId(adminUserId);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(toJson(detail));
        log.setIpAddress(ipAddress);
        log.setCreatedAt(now);
        log.setUpdatedAt(now);
        adminOperationLogMapper.insertLog(log);
    }

    private String toJson(Map<String, Object> detail) {
        try {
            return objectMapper.writeValueAsString(new HashMap<>(detail));
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }

    private String blankToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private record MediaReferenceTarget(String table, String column, String label) {
    }
}
