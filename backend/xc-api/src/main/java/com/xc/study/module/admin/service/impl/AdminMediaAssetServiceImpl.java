package com.xc.study.module.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xc.study.common.BusinessException;
import com.xc.study.common.ErrorCode;
import com.xc.study.common.PageResult;
import com.xc.study.module.admin.dto.AdminMediaAssetQueryDTO;
import com.xc.study.module.admin.dto.AdminUploadMediaAssetDTO;
import com.xc.study.module.admin.entity.AdminOperationLog;
import com.xc.study.module.admin.mapper.AdminOperationLogMapper;
import com.xc.study.module.admin.service.AdminMediaAssetService;
import com.xc.study.module.admin.vo.AdminMediaAssetVO;
import com.xc.study.module.media.entity.MediaAsset;
import com.xc.study.module.media.mapper.MediaAssetMapper;
import com.xc.study.module.media.storage.MediaStorageException;
import com.xc.study.module.media.storage.MediaStorageService;
import com.xc.study.module.media.storage.StoredMediaObject;
import com.xc.study.security.CurrentUser;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AdminMediaAssetServiceImpl implements AdminMediaAssetService {

    private static final Set<String> AUDIO_EXTENSIONS = Set.of("mp3", "wav", "m4a", "ogg", "aac", "webm");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "webm", "mov");

    private final MediaAssetMapper mediaAssetMapper;
    private final AdminOperationLogMapper adminOperationLogMapper;
    private final ObjectMapper objectMapper;
    private final MediaStorageService mediaStorageService;

    public AdminMediaAssetServiceImpl(
            MediaAssetMapper mediaAssetMapper,
            AdminOperationLogMapper adminOperationLogMapper,
            ObjectMapper objectMapper,
            MediaStorageService mediaStorageService
    ) {
        this.mediaAssetMapper = mediaAssetMapper;
        this.adminOperationLogMapper = adminOperationLogMapper;
        this.objectMapper = objectMapper;
        this.mediaStorageService = mediaStorageService;
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
        asset.setUrl(storedObject.url());
        asset.setLanguage(blankToNull(request.getLanguage()));
        asset.setDurationMs(request.getDurationMs());
        asset.setFileSize(file.getSize());
        asset.setCreatedAt(now);
        asset.setUpdatedAt(now);
        mediaAssetMapper.insert(asset);

        writeOperationLog(admin.id(), "content.media.upload", "media_asset", asset.getId(), Map.of(
                "mediaType", asset.getMediaType(),
                "url", asset.getUrl(),
                "language", asset.getLanguage() == null ? "" : asset.getLanguage(),
                "fileSize", asset.getFileSize()
        ), ipAddress);
        return toVO(asset);
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
}
