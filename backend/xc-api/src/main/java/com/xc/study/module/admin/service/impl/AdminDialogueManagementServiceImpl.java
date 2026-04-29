package com.xc.study.module.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xc.study.common.BusinessException;
import com.xc.study.common.ErrorCode;
import com.xc.study.common.PageResult;
import com.xc.study.common.cache.MasterDataCache;
import com.xc.study.module.admin.dto.AdminBatchBindMediaAssetDTO;
import com.xc.study.module.admin.dto.AdminBatchUpdateContentStatusDTO;
import com.xc.study.module.admin.dto.AdminDialogueLineQueryDTO;
import com.xc.study.module.admin.dto.AdminDialogueLineVocabQueryDTO;
import com.xc.study.module.admin.dto.AdminUpdateContentStatusDTO;
import com.xc.study.module.admin.dto.AdminUpsertDialogueLineDTO;
import com.xc.study.module.admin.dto.AdminUpsertDialogueLineVocabDTO;
import com.xc.study.module.admin.dto.AdminUpsertVideoMaterialDTO;
import com.xc.study.module.admin.dto.AdminVideoMaterialQueryDTO;
import com.xc.study.module.admin.entity.AdminOperationLog;
import com.xc.study.module.admin.mapper.AdminOperationLogMapper;
import com.xc.study.module.admin.service.AdminDialogueManagementService;
import com.xc.study.module.admin.service.support.AdminSorts;
import com.xc.study.module.admin.vo.AdminBatchBindMediaAssetResultVO;
import com.xc.study.module.admin.vo.AdminBatchContentStatusResultVO;
import com.xc.study.module.admin.vo.AdminDialogueLineVO;
import com.xc.study.module.admin.vo.AdminDialogueLineVocabVO;
import com.xc.study.module.admin.vo.AdminVideoMaterialVO;
import com.xc.study.module.dialogue.entity.DialogueLine;
import com.xc.study.module.dialogue.entity.DialogueLineVocab;
import com.xc.study.module.dialogue.entity.VideoMaterial;
import com.xc.study.module.dialogue.mapper.DialogueLineMapper;
import com.xc.study.module.dialogue.mapper.DialogueLineVocabMapper;
import com.xc.study.module.dialogue.mapper.VideoMaterialMapper;
import com.xc.study.module.media.entity.MediaAsset;
import com.xc.study.module.media.mapper.MediaAssetMapper;
import com.xc.study.module.vocab.entity.VocabItem;
import com.xc.study.module.vocab.mapper.VocabItemMapper;
import com.xc.study.security.CurrentUser;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminDialogueManagementServiceImpl implements AdminDialogueManagementService {

    private final VideoMaterialMapper videoMaterialMapper;
    private final DialogueLineMapper dialogueLineMapper;
    private final DialogueLineVocabMapper dialogueLineVocabMapper;
    private final VocabItemMapper vocabItemMapper;
    private final MediaAssetMapper mediaAssetMapper;
    private final AdminOperationLogMapper adminOperationLogMapper;
    private final ObjectMapper objectMapper;
    private final MasterDataCache masterDataCache;

    public AdminDialogueManagementServiceImpl(
            VideoMaterialMapper videoMaterialMapper,
            DialogueLineMapper dialogueLineMapper,
            DialogueLineVocabMapper dialogueLineVocabMapper,
            VocabItemMapper vocabItemMapper,
            MediaAssetMapper mediaAssetMapper,
            AdminOperationLogMapper adminOperationLogMapper,
            ObjectMapper objectMapper,
            MasterDataCache masterDataCache
    ) {
        this.videoMaterialMapper = videoMaterialMapper;
        this.dialogueLineMapper = dialogueLineMapper;
        this.dialogueLineVocabMapper = dialogueLineVocabMapper;
        this.vocabItemMapper = vocabItemMapper;
        this.mediaAssetMapper = mediaAssetMapper;
        this.adminOperationLogMapper = adminOperationLogMapper;
        this.objectMapper = objectMapper;
        this.masterDataCache = masterDataCache;
    }

    @Override
    public PageResult<AdminVideoMaterialVO> pageMaterials(AdminVideoMaterialQueryDTO query, CurrentUser admin) {
        requirePermission(admin, "admin:content:read");
        int page = query.getPage() == null ? 1 : query.getPage();
        int pageSize = query.getPageSize() == null ? 20 : query.getPageSize();
        LambdaQueryWrapper<VideoMaterial> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getMaterialType())) {
            wrapper.eq(VideoMaterial::getMaterialType, query.getMaterialType());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(VideoMaterial::getStatus, query.getStatus());
        }
        if (query.getHasCover() != null) {
            if (query.getHasCover()) {
                wrapper.isNotNull(VideoMaterial::getCoverAssetId);
            } else {
                wrapper.isNull(VideoMaterial::getCoverAssetId);
            }
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(item -> item.like(VideoMaterial::getTitle, keyword)
                    .or()
                    .like(VideoMaterial::getDescription, keyword));
        }
        boolean sorted = AdminSorts.apply(wrapper, query.getSortBy(), query.getSortDirection(), Map.of(
                "id", VideoMaterial::getId,
                "title", VideoMaterial::getTitle,
                "materialType", VideoMaterial::getMaterialType,
                "status", VideoMaterial::getStatus,
                "createdAt", VideoMaterial::getCreatedAt,
                "updatedAt", VideoMaterial::getUpdatedAt
        ));
        if (!sorted) {
            wrapper.orderByDesc(VideoMaterial::getUpdatedAt);
        }
        wrapper.orderByDesc(VideoMaterial::getId);
        Page<VideoMaterial> result = videoMaterialMapper.selectPage(Page.of(page, pageSize), wrapper);
        return PageResult.of(toMaterialVOs(result.getRecords()), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional
    public AdminVideoMaterialVO createMaterial(AdminUpsertVideoMaterialDTO request, CurrentUser admin, String ipAddress) {
        requirePermission(admin, "admin:content:update");
        validateCoverAsset(request.coverAssetId());
        OffsetDateTime now = OffsetDateTime.now();
        VideoMaterial material = new VideoMaterial();
        fillMaterial(material, request);
        material.setStatus(StringUtils.hasText(request.status()) ? request.status() : "active");
        material.setCreatedAt(now);
        material.setUpdatedAt(now);
        videoMaterialMapper.insert(material);
        writeOperationLog(admin.id(), "content.video.material.create", "video_material", material.getId(), materialSnapshot(material), ipAddress);
        evictVideoMaterialCache();
        return toMaterialVOs(List.of(material)).get(0);
    }

    @Override
    @Transactional
    public AdminVideoMaterialVO updateMaterial(Long materialId, AdminUpsertVideoMaterialDTO request, CurrentUser admin, String ipAddress) {
        requirePermission(admin, "admin:content:update");
        validateCoverAsset(request.coverAssetId());
        VideoMaterial material = requireMaterial(materialId);
        Map<String, Object> before = materialSnapshot(material);
        fillMaterial(material, request);
        if (StringUtils.hasText(request.status())) {
            material.setStatus(request.status());
        }
        material.setUpdatedAt(OffsetDateTime.now());
        videoMaterialMapper.updateById(material);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("before", before);
        detail.put("after", materialSnapshot(material));
        writeOperationLog(admin.id(), "content.video.material.update", "video_material", materialId, detail, ipAddress);
        evictVideoMaterialCache();
        return toMaterialVOs(List.of(material)).get(0);
    }

    @Override
    @Transactional
    public AdminVideoMaterialVO updateMaterialStatus(Long materialId, AdminUpdateContentStatusDTO request, CurrentUser admin, String ipAddress) {
        requirePermission(admin, "admin:content:update");
        VideoMaterial material = requireMaterial(materialId);
        String beforeStatus = material.getStatus();
        material.setStatus(request.status());
        material.setUpdatedAt(OffsetDateTime.now());
        videoMaterialMapper.updateById(material);
        writeOperationLog(admin.id(), "content.video.material.status.update", "video_material", materialId, Map.of(
                "beforeStatus", beforeStatus,
                "afterStatus", request.status(),
                "reason", request.reason() == null ? "" : request.reason()
        ), ipAddress);
        evictVideoMaterialCache();
        return toMaterialVOs(List.of(material)).get(0);
    }

    @Override
    @Transactional
    public AdminBatchContentStatusResultVO updateMaterialStatuses(
            AdminBatchUpdateContentStatusDTO request,
            CurrentUser admin,
            String ipAddress
    ) {
        requirePermission(admin, "admin:content:update");
        OffsetDateTime now = OffsetDateTime.now();
        List<String> errors = new ArrayList<>();
        List<Map<String, Object>> changes = new ArrayList<>();
        for (Long materialId : request.ids()) {
            VideoMaterial material = videoMaterialMapper.selectById(materialId);
            if (material == null) {
                errors.add("台词材料 ID " + materialId + " 不存在");
                continue;
            }
            String beforeStatus = material.getStatus();
            material.setStatus(request.status());
            material.setUpdatedAt(now);
            videoMaterialMapper.updateById(material);
            changes.add(Map.of(
                    "id", materialId,
                    "beforeStatus", beforeStatus == null ? "" : beforeStatus,
                    "afterStatus", request.status()
            ));
        }
        writeOperationLog(admin.id(), "content.video.material.status.batch_update", "video_material", null, Map.of(
                "requestedCount", request.ids().size(),
                "successCount", changes.size(),
                "afterStatus", request.status(),
                "reason", request.reason() == null ? "" : request.reason(),
                "errors", errors,
                "changes", changes
        ), ipAddress);
        evictVideoMaterialCache();
        return new AdminBatchContentStatusResultVO(request.ids().size(), changes.size(), errors);
    }

    @Override
    public PageResult<AdminDialogueLineVO> pageLines(AdminDialogueLineQueryDTO query, CurrentUser admin) {
        requirePermission(admin, "admin:content:read");
        int page = query.getPage() == null ? 1 : query.getPage();
        int pageSize = query.getPageSize() == null ? 20 : query.getPageSize();
        LambdaQueryWrapper<DialogueLine> wrapper = new LambdaQueryWrapper<>();
        if (query.getMaterialId() != null) {
            wrapper.eq(DialogueLine::getMaterialId, query.getMaterialId());
        }
        if (query.getHasAudio() != null) {
            if (query.getHasAudio()) {
                wrapper.isNotNull(DialogueLine::getAudioAssetId);
            } else {
                wrapper.isNull(DialogueLine::getAudioAssetId);
            }
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(item -> item.like(DialogueLine::getHanziText, keyword)
                    .or()
                    .like(DialogueLine::getPinyinText, keyword)
                    .or()
                    .like(DialogueLine::getTranslationEn, keyword)
                    .or()
                    .like(DialogueLine::getTranslationRu, keyword));
        }
        boolean sorted = AdminSorts.apply(wrapper, query.getSortBy(), query.getSortDirection(), Map.of(
                "id", DialogueLine::getId,
                "materialId", DialogueLine::getMaterialId,
                "lineNo", DialogueLine::getLineNo,
                "hanziText", DialogueLine::getHanziText,
                "startMs", DialogueLine::getStartMs,
                "endMs", DialogueLine::getEndMs,
                "createdAt", DialogueLine::getCreatedAt,
                "updatedAt", DialogueLine::getUpdatedAt
        ));
        if (!sorted) {
            wrapper.orderByAsc(DialogueLine::getMaterialId).orderByAsc(DialogueLine::getLineNo);
        }
        wrapper.orderByAsc(DialogueLine::getId);
        Page<DialogueLine> result = dialogueLineMapper.selectPage(Page.of(page, pageSize), wrapper);
        return PageResult.of(toLineVOs(result.getRecords()), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional
    public AdminDialogueLineVO createLine(AdminUpsertDialogueLineDTO request, CurrentUser admin, String ipAddress) {
        requirePermission(admin, "admin:content:update");
        validateLineRequest(request, null);
        OffsetDateTime now = OffsetDateTime.now();
        DialogueLine line = new DialogueLine();
        fillLine(line, request);
        line.setCreatedAt(now);
        line.setUpdatedAt(now);
        dialogueLineMapper.insert(line);
        writeOperationLog(admin.id(), "content.dialogue.line.create", "dialogue_line", line.getId(), lineSnapshot(line), ipAddress);
        evictVideoMaterialCache();
        return toLineVOs(List.of(line)).get(0);
    }

    @Override
    @Transactional
    public AdminDialogueLineVO updateLine(Long lineId, AdminUpsertDialogueLineDTO request, CurrentUser admin, String ipAddress) {
        requirePermission(admin, "admin:content:update");
        DialogueLine line = requireLine(lineId);
        validateLineRequest(request, lineId);
        Map<String, Object> before = lineSnapshot(line);
        fillLine(line, request);
        line.setUpdatedAt(OffsetDateTime.now());
        dialogueLineMapper.updateById(line);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("before", before);
        detail.put("after", lineSnapshot(line));
        writeOperationLog(admin.id(), "content.dialogue.line.update", "dialogue_line", lineId, detail, ipAddress);
        evictVideoMaterialCache();
        return toLineVOs(List.of(line)).get(0);
    }

    @Override
    @Transactional
    public AdminBatchBindMediaAssetResultVO bindDialogueLineAudio(
            AdminBatchBindMediaAssetDTO request,
            CurrentUser admin,
            String ipAddress
    ) {
        requirePermission(admin, "admin:content:update");
        OffsetDateTime now = OffsetDateTime.now();
        List<String> errors = new ArrayList<>();
        List<Map<String, Object>> successfulBindings = new ArrayList<>();
        for (var binding : request.bindings()) {
            Long lineId = binding.targetId();
            Long mediaAssetId = binding.mediaAssetId();
            DialogueLine line = dialogueLineMapper.selectById(lineId);
            if (line == null) {
                errors.add("台词行 ID " + lineId + " 不存在");
                continue;
            }
            MediaAsset asset = mediaAssetMapper.selectById(mediaAssetId);
            if (!isActiveMediaAsset(asset, "audio")) {
                errors.add("台词行 ID " + lineId + " 的音频资源 ID " + mediaAssetId + " 不存在、已停用或类型不正确");
                continue;
            }
            Long beforeAudioAssetId = line.getAudioAssetId();
            line.setAudioAssetId(mediaAssetId);
            line.setUpdatedAt(now);
            dialogueLineMapper.updateById(line);
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("targetId", lineId);
            detail.put("beforeAudioAssetId", beforeAudioAssetId);
            detail.put("afterAudioAssetId", mediaAssetId);
            successfulBindings.add(detail);
        }
        writeOperationLog(admin.id(), "content.dialogue.line.audio.batch_bind", "dialogue_line", null, Map.of(
                "requestedCount", request.bindings().size(),
                "successCount", successfulBindings.size(),
                "errors", errors,
                "bindings", successfulBindings
        ), ipAddress);
        evictDialogueContentCache();
        return new AdminBatchBindMediaAssetResultVO(request.bindings().size(), successfulBindings.size(), errors);
    }

    @Override
    @Transactional
    public AdminBatchBindMediaAssetResultVO bindMaterialCover(
            AdminBatchBindMediaAssetDTO request,
            CurrentUser admin,
            String ipAddress
    ) {
        requirePermission(admin, "admin:content:update");
        OffsetDateTime now = OffsetDateTime.now();
        List<String> errors = new ArrayList<>();
        List<Map<String, Object>> successfulBindings = new ArrayList<>();
        for (var binding : request.bindings()) {
            Long materialId = binding.targetId();
            Long mediaAssetId = binding.mediaAssetId();
            VideoMaterial material = videoMaterialMapper.selectById(materialId);
            if (material == null) {
                errors.add("台词材料 ID " + materialId + " 不存在");
                continue;
            }
            MediaAsset asset = mediaAssetMapper.selectById(mediaAssetId);
            if (!isActiveMediaAsset(asset, "image")) {
                errors.add("台词材料 ID " + materialId + " 的封面资源 ID " + mediaAssetId + " 不存在、已停用或类型不正确");
                continue;
            }
            Long beforeCoverAssetId = material.getCoverAssetId();
            material.setCoverAssetId(mediaAssetId);
            material.setUpdatedAt(now);
            videoMaterialMapper.updateById(material);
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("targetId", materialId);
            detail.put("beforeCoverAssetId", beforeCoverAssetId);
            detail.put("afterCoverAssetId", mediaAssetId);
            successfulBindings.add(detail);
        }
        writeOperationLog(admin.id(), "content.video.material.cover.batch_bind", "video_material", null, Map.of(
                "requestedCount", request.bindings().size(),
                "successCount", successfulBindings.size(),
                "errors", errors,
                "bindings", successfulBindings
        ), ipAddress);
        evictVideoMaterialCache();
        return new AdminBatchBindMediaAssetResultVO(request.bindings().size(), successfulBindings.size(), errors);
    }

    @Override
    public PageResult<AdminDialogueLineVocabVO> pageLineVocab(AdminDialogueLineVocabQueryDTO query, CurrentUser admin) {
        requirePermission(admin, "admin:content:read");
        int page = query.getPage() == null ? 1 : query.getPage();
        int pageSize = query.getPageSize() == null ? 20 : query.getPageSize();
        LambdaQueryWrapper<DialogueLineVocab> wrapper = new LambdaQueryWrapper<>();
        if (query.getDialogueLineId() != null) {
            wrapper.eq(DialogueLineVocab::getDialogueLineId, query.getDialogueLineId());
        }
        if (query.getMaterialId() != null) {
            List<Long> lineIds = dialogueLineMapper.selectList(new LambdaQueryWrapper<DialogueLine>()
                            .eq(DialogueLine::getMaterialId, query.getMaterialId()))
                    .stream()
                    .map(DialogueLine::getId)
                    .toList();
            if (lineIds.isEmpty()) {
                return PageResult.of(List.of(), 0, page, pageSize);
            }
            wrapper.in(DialogueLineVocab::getDialogueLineId, lineIds);
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(item -> item.like(DialogueLineVocab::getWordText, keyword)
                    .or()
                    .like(DialogueLineVocab::getPinyin, keyword)
                    .or()
                    .like(DialogueLineVocab::getMeaningEn, keyword)
                    .or()
                    .like(DialogueLineVocab::getMeaningRu, keyword)
                    .or()
                    .like(DialogueLineVocab::getExplanation, keyword));
        }
        boolean sorted = AdminSorts.apply(wrapper, query.getSortBy(), query.getSortDirection(), Map.of(
                "id", DialogueLineVocab::getId,
                "dialogueLineId", DialogueLineVocab::getDialogueLineId,
                "wordText", DialogueLineVocab::getWordText,
                "pinyin", DialogueLineVocab::getPinyin,
                "createdAt", DialogueLineVocab::getCreatedAt,
                "updatedAt", DialogueLineVocab::getUpdatedAt
        ));
        if (!sorted) {
            wrapper.orderByAsc(DialogueLineVocab::getDialogueLineId);
        }
        wrapper.orderByAsc(DialogueLineVocab::getId);
        Page<DialogueLineVocab> result = dialogueLineVocabMapper.selectPage(Page.of(page, pageSize), wrapper);
        return PageResult.of(toLineVocabVOs(result.getRecords()), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional
    public AdminDialogueLineVocabVO createLineVocab(AdminUpsertDialogueLineVocabDTO request, CurrentUser admin, String ipAddress) {
        requirePermission(admin, "admin:content:update");
        requireLine(request.dialogueLineId());
        VocabItem vocabItem = resolveVocabItem(request.vocabItemId());
        OffsetDateTime now = OffsetDateTime.now();
        DialogueLineVocab vocab = new DialogueLineVocab();
        fillLineVocab(vocab, request, vocabItem);
        vocab.setCreatedAt(now);
        vocab.setUpdatedAt(now);
        dialogueLineVocabMapper.insert(vocab);
        writeOperationLog(admin.id(), "content.dialogue.line.vocab.create", "dialogue_line_vocab", vocab.getId(), lineVocabSnapshot(vocab), ipAddress);
        evictDialogueLineAnalysisCache();
        return toLineVocabVOs(List.of(vocab)).get(0);
    }

    @Override
    @Transactional
    public AdminDialogueLineVocabVO updateLineVocab(Long vocabId, AdminUpsertDialogueLineVocabDTO request, CurrentUser admin, String ipAddress) {
        requirePermission(admin, "admin:content:update");
        requireLine(request.dialogueLineId());
        VocabItem vocabItem = resolveVocabItem(request.vocabItemId());
        DialogueLineVocab vocab = requireLineVocab(vocabId);
        Map<String, Object> before = lineVocabSnapshot(vocab);
        fillLineVocab(vocab, request, vocabItem);
        vocab.setUpdatedAt(OffsetDateTime.now());
        dialogueLineVocabMapper.updateById(vocab);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("before", before);
        detail.put("after", lineVocabSnapshot(vocab));
        writeOperationLog(admin.id(), "content.dialogue.line.vocab.update", "dialogue_line_vocab", vocabId, detail, ipAddress);
        evictDialogueLineAnalysisCache();
        return toLineVocabVOs(List.of(vocab)).get(0);
    }

    @Override
    @Transactional
    public void deleteLineVocab(Long vocabId, CurrentUser admin, String ipAddress) {
        requirePermission(admin, "admin:content:update");
        DialogueLineVocab vocab = requireLineVocab(vocabId);
        dialogueLineVocabMapper.deleteById(vocabId);
        writeOperationLog(admin.id(), "content.dialogue.line.vocab.delete", "dialogue_line_vocab", vocabId, lineVocabSnapshot(vocab), ipAddress);
        evictDialogueLineAnalysisCache();
    }

    private void fillMaterial(VideoMaterial material, AdminUpsertVideoMaterialDTO request) {
        material.setTitle(request.title().trim());
        material.setMaterialType(request.materialType());
        material.setDescription(blankToNull(request.description()));
        material.setCoverAssetId(request.coverAssetId());
    }

    private void fillLine(DialogueLine line, AdminUpsertDialogueLineDTO request) {
        line.setMaterialId(request.materialId());
        line.setLineNo(request.lineNo());
        line.setHanziText(request.hanziText().trim());
        line.setPinyinText(blankToNull(request.pinyinText()));
        line.setTranslationEn(blankToNull(request.translationEn()));
        line.setTranslationRu(blankToNull(request.translationRu()));
        line.setAudioAssetId(request.audioAssetId());
        line.setStartMs(request.startMs());
        line.setEndMs(request.endMs());
    }

    private void fillLineVocab(DialogueLineVocab vocab, AdminUpsertDialogueLineVocabDTO request, VocabItem vocabItem) {
        vocab.setDialogueLineId(request.dialogueLineId());
        vocab.setVocabItemId(request.vocabItemId());
        vocab.setWordText(request.wordText().trim());
        vocab.setPinyin(defaultText(request.pinyin(), vocabItem == null ? null : vocabItem.getPinyin()));
        vocab.setMeaningEn(defaultText(request.meaningEn(), vocabItem == null ? null : vocabItem.getMeaningEn()));
        vocab.setMeaningRu(defaultText(request.meaningRu(), vocabItem == null ? null : vocabItem.getMeaningRu()));
        vocab.setExplanation(blankToNull(request.explanation()));
    }

    private void validateLineRequest(AdminUpsertDialogueLineDTO request, Long currentLineId) {
        requireMaterial(request.materialId());
        if (request.startMs() != null && request.endMs() != null && request.endMs() < request.startMs()) {
            throw new BusinessException("结束时间不能早于开始时间");
        }
        if (request.audioAssetId() != null) {
            MediaAsset asset = mediaAssetMapper.selectById(request.audioAssetId());
            if (asset == null || !"audio".equals(asset.getMediaType()) || !"active".equals(asset.getStatus())) {
                throw BusinessException.notFound("音频资源不存在或已停用");
            }
        }
        DialogueLine existing = dialogueLineMapper.selectOne(new LambdaQueryWrapper<DialogueLine>()
                .eq(DialogueLine::getMaterialId, request.materialId())
                .eq(DialogueLine::getLineNo, request.lineNo())
                .last("limit 1"));
        if (existing != null && !Objects.equals(existing.getId(), currentLineId)) {
            throw BusinessException.conflict("同一材料下台词顺序不能重复");
        }
    }

    private void validateCoverAsset(Long coverAssetId) {
        if (coverAssetId == null) {
            return;
        }
        MediaAsset asset = mediaAssetMapper.selectById(coverAssetId);
        if (asset == null || !"image".equals(asset.getMediaType()) || !"active".equals(asset.getStatus())) {
            throw BusinessException.notFound("封面图片不存在或已停用");
        }
    }

    private boolean isActiveMediaAsset(MediaAsset asset, String mediaType) {
        return asset != null && mediaType.equals(asset.getMediaType()) && "active".equals(asset.getStatus());
    }

    private List<AdminVideoMaterialVO> toMaterialVOs(List<VideoMaterial> materials) {
        if (materials.isEmpty()) {
            return List.of();
        }
        Map<Long, MediaAsset> covers = loadMediaAssets(materials.stream().map(VideoMaterial::getCoverAssetId).toList());
        return materials.stream()
                .map(material -> {
                    MediaAsset cover = covers.get(material.getCoverAssetId());
                    return new AdminVideoMaterialVO(
                            material.getId(),
                            material.getTitle(),
                            material.getMaterialType(),
                            material.getDescription(),
                            material.getCoverAssetId(),
                            cover == null ? null : cover.getUrl(),
                            material.getStatus(),
                            countLines(material.getId()),
                            material.getCreatedAt(),
                            material.getUpdatedAt()
                    );
                })
                .toList();
    }

    private List<AdminDialogueLineVO> toLineVOs(List<DialogueLine> lines) {
        if (lines.isEmpty()) {
            return List.of();
        }
        Map<Long, VideoMaterial> materials = loadMaterials(lines.stream().map(DialogueLine::getMaterialId).toList());
        Map<Long, MediaAsset> audioAssets = loadMediaAssets(lines.stream().map(DialogueLine::getAudioAssetId).toList());
        return lines.stream()
                .map(line -> {
                    VideoMaterial material = materials.get(line.getMaterialId());
                    MediaAsset audio = audioAssets.get(line.getAudioAssetId());
                    return new AdminDialogueLineVO(
                            line.getId(),
                            line.getMaterialId(),
                            material == null ? null : material.getTitle(),
                            line.getLineNo(),
                            line.getHanziText(),
                            line.getPinyinText(),
                            line.getTranslationEn(),
                            line.getTranslationRu(),
                            line.getAudioAssetId(),
                            audio == null ? null : audio.getUrl(),
                            line.getStartMs(),
                            line.getEndMs(),
                            line.getCreatedAt(),
                            line.getUpdatedAt()
                    );
                })
                .toList();
    }

    private List<AdminDialogueLineVocabVO> toLineVocabVOs(List<DialogueLineVocab> vocabRecords) {
        if (vocabRecords.isEmpty()) {
            return List.of();
        }
        Map<Long, DialogueLine> lines = loadLines(vocabRecords.stream().map(DialogueLineVocab::getDialogueLineId).toList());
        Map<Long, VideoMaterial> materials = loadMaterials(lines.values().stream().map(DialogueLine::getMaterialId).toList());
        Map<Long, VocabItem> vocabItems = loadVocabItems(vocabRecords.stream().map(DialogueLineVocab::getVocabItemId).toList());
        return vocabRecords.stream()
                .map(vocab -> {
                    DialogueLine line = lines.get(vocab.getDialogueLineId());
                    VideoMaterial material = line == null ? null : materials.get(line.getMaterialId());
                    VocabItem vocabItem = vocabItems.get(vocab.getVocabItemId());
                    return new AdminDialogueLineVocabVO(
                            vocab.getId(),
                            vocab.getDialogueLineId(),
                            line == null ? null : line.getMaterialId(),
                            material == null ? null : material.getTitle(),
                            line == null ? null : line.getLineNo(),
                            line == null ? null : line.getHanziText(),
                            vocab.getVocabItemId(),
                            vocabItem == null ? null : vocabItem.getHanzi(),
                            vocab.getWordText(),
                            vocab.getPinyin(),
                            vocab.getMeaningEn(),
                            vocab.getMeaningRu(),
                            vocab.getExplanation(),
                            vocab.getCreatedAt(),
                            vocab.getUpdatedAt()
                    );
                })
                .toList();
    }

    private Map<Long, DialogueLine> loadLines(List<Long> ids) {
        List<Long> lineIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (lineIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return dialogueLineMapper.selectBatchIds(lineIds)
                .stream()
                .collect(Collectors.toMap(DialogueLine::getId, Function.identity()));
    }

    private Map<Long, VideoMaterial> loadMaterials(List<Long> ids) {
        List<Long> materialIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (materialIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return videoMaterialMapper.selectBatchIds(materialIds)
                .stream()
                .collect(Collectors.toMap(VideoMaterial::getId, Function.identity()));
    }

    private Map<Long, MediaAsset> loadMediaAssets(List<Long> ids) {
        List<Long> assetIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (assetIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return mediaAssetMapper.selectBatchIds(assetIds)
                .stream()
                .collect(Collectors.toMap(MediaAsset::getId, Function.identity()));
    }

    private Map<Long, VocabItem> loadVocabItems(List<Long> ids) {
        List<Long> itemIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (itemIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return vocabItemMapper.selectBatchIds(itemIds)
                .stream()
                .collect(Collectors.toMap(VocabItem::getId, Function.identity()));
    }

    private long countLines(Long materialId) {
        return dialogueLineMapper.selectCount(new LambdaQueryWrapper<DialogueLine>()
                .eq(DialogueLine::getMaterialId, materialId));
    }

    private VideoMaterial requireMaterial(Long materialId) {
        VideoMaterial material = videoMaterialMapper.selectById(materialId);
        if (material == null) {
            throw BusinessException.notFound("台词材料不存在");
        }
        return material;
    }

    private DialogueLine requireLine(Long lineId) {
        DialogueLine line = dialogueLineMapper.selectById(lineId);
        if (line == null) {
            throw BusinessException.notFound("台词不存在");
        }
        return line;
    }

    private DialogueLineVocab requireLineVocab(Long vocabId) {
        DialogueLineVocab vocab = dialogueLineVocabMapper.selectById(vocabId);
        if (vocab == null) {
            throw BusinessException.notFound("台词词汇解析不存在");
        }
        return vocab;
    }

    private VocabItem resolveVocabItem(Long vocabItemId) {
        if (vocabItemId == null) {
            return null;
        }
        VocabItem item = vocabItemMapper.selectById(vocabItemId);
        if (item == null) {
            throw BusinessException.notFound("词汇不存在");
        }
        return item;
    }

    private Map<String, Object> materialSnapshot(VideoMaterial material) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("title", material.getTitle());
        snapshot.put("materialType", material.getMaterialType());
        snapshot.put("description", material.getDescription());
        snapshot.put("coverAssetId", material.getCoverAssetId());
        snapshot.put("status", material.getStatus());
        return snapshot;
    }

    private Map<String, Object> lineSnapshot(DialogueLine line) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("materialId", line.getMaterialId());
        snapshot.put("lineNo", line.getLineNo());
        snapshot.put("hanziText", line.getHanziText());
        snapshot.put("pinyinText", line.getPinyinText());
        snapshot.put("translationEn", line.getTranslationEn());
        snapshot.put("translationRu", line.getTranslationRu());
        snapshot.put("audioAssetId", line.getAudioAssetId());
        snapshot.put("startMs", line.getStartMs());
        snapshot.put("endMs", line.getEndMs());
        return snapshot;
    }

    private Map<String, Object> lineVocabSnapshot(DialogueLineVocab vocab) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("dialogueLineId", vocab.getDialogueLineId());
        snapshot.put("vocabItemId", vocab.getVocabItemId());
        snapshot.put("wordText", vocab.getWordText());
        snapshot.put("pinyin", vocab.getPinyin());
        snapshot.put("meaningEn", vocab.getMeaningEn());
        snapshot.put("meaningRu", vocab.getMeaningRu());
        snapshot.put("explanation", vocab.getExplanation());
        return snapshot;
    }

    private void requirePermission(CurrentUser admin, String permission) {
        if (admin.permissions().contains("admin:*") || admin.permissions().contains(permission)) {
            return;
        }
        throw BusinessException.forbidden(ErrorCode.FORBIDDEN, "缺少后台权限：" + permission);
    }

    private void evictVideoMaterialCache() {
        masterDataCache.evictByPrefix("dialogue:materials:");
        evictDialogueContentCache();
    }

    private void evictDialogueContentCache() {
        masterDataCache.evictByPrefix("dialogue:lines:");
        evictDialogueLineAnalysisCache();
    }

    private void evictDialogueLineAnalysisCache() {
        masterDataCache.evictByPrefix("dialogue:line-analysis:");
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

    private String defaultText(String value, String fallback) {
        if (StringUtils.hasText(value)) {
            return value.trim();
        }
        return blankToNull(fallback);
    }
}
