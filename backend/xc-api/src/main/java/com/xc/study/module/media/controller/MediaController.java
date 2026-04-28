package com.xc.study.module.media.controller;

import com.xc.study.module.media.storage.MediaObjectNotFoundException;
import com.xc.study.module.media.storage.MediaResource;
import com.xc.study.module.media.storage.MediaStorageException;
import com.xc.study.module.media.storage.MediaStorageService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/media")
public class MediaController {

    private final MediaStorageService mediaStorageService;

    public MediaController(MediaStorageService mediaStorageService) {
        this.mediaStorageService = mediaStorageService;
    }

    @GetMapping("/**")
    public ResponseEntity<InputStreamResource> load(HttpServletRequest request) {
        String objectKey = extractObjectKey(request);
        try {
            MediaResource resource = mediaStorageService.load(objectKey);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(resource.contentType()))
                    .contentLength(resource.contentLength())
                    .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                    .body(new InputStreamResource(resource.inputStream()));
        } catch (MediaObjectNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "媒体文件不存在");
        } catch (MediaStorageException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "媒体文件读取失败");
        }
    }

    private String extractObjectKey(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String prefix = (StringUtils.hasText(contextPath) ? contextPath : "") + "/media/";
        int prefixIndex = requestUri.indexOf(prefix);
        if (prefixIndex < 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "媒体文件不存在");
        }
        String objectKey = requestUri.substring(prefixIndex + prefix.length());
        objectKey = URLDecoder.decode(objectKey, StandardCharsets.UTF_8);
        if (!StringUtils.hasText(objectKey) || objectKey.contains("..")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "媒体文件不存在");
        }
        return objectKey;
    }
}
