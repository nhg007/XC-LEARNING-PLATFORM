package com.xc.study.module.dialogue.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.xc.study.common.BusinessException;
import com.xc.study.common.ErrorCode;
import com.xc.study.common.PageParams;
import com.xc.study.common.PageResult;
import com.xc.study.common.cache.MasterDataCache;
import com.xc.study.module.admin.service.RuntimeConfigService;
import com.xc.study.module.dialogue.asr.DialogueTextMatcher;
import com.xc.study.module.dialogue.dto.CheckDialogueLineRequest;
import com.xc.study.module.dialogue.entity.AsrJob;
import com.xc.study.module.dialogue.entity.DialogueLine;
import com.xc.study.module.dialogue.entity.DialogueLineVocab;
import com.xc.study.module.dialogue.entity.SpeechRecord;
import com.xc.study.module.dialogue.entity.VideoMaterial;
import com.xc.study.module.dialogue.mapper.AsrJobMapper;
import com.xc.study.module.dialogue.mapper.DialogueLineMapper;
import com.xc.study.module.dialogue.mapper.DialogueLineVocabMapper;
import com.xc.study.module.dialogue.mapper.SpeechRecordMapper;
import com.xc.study.module.dialogue.mapper.VideoMaterialMapper;
import com.xc.study.module.dialogue.vo.DialogueLineAnalysisVO;
import com.xc.study.module.dialogue.vo.DialogueLineCheckResultVO;
import com.xc.study.module.dialogue.vo.DialogueLineVO;
import com.xc.study.module.dialogue.vo.DialogueLineVocabVO;
import com.xc.study.module.dialogue.vo.SpeechRecordVO;
import com.xc.study.module.dialogue.vo.VideoMaterialVO;
import com.xc.study.module.media.entity.MediaAsset;
import com.xc.study.module.media.mapper.MediaAssetMapper;
import com.xc.study.module.media.storage.MediaStorageException;
import com.xc.study.module.media.storage.MediaStorageService;
import com.xc.study.module.media.storage.StoredMediaObject;
import com.xc.study.module.stats.entity.StudyEvent;
import com.xc.study.module.stats.service.LearningStatsRecorder;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DialogueService {

    private static final TypeReference<PageResult<VideoMaterialVO>> VIDEO_MATERIAL_PAGE_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<DialogueLineVO>> DIALOGUE_LINE_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<DialogueLineAnalysisVO> DIALOGUE_LINE_ANALYSIS_TYPE = new TypeReference<>() {
    };

    private final VideoMaterialMapper videoMaterialMapper;
    private final DialogueLineMapper dialogueLineMapper;
    private final DialogueLineVocabMapper dialogueLineVocabMapper;
    private final AsrJobMapper asrJobMapper;
    private final SpeechRecordMapper speechRecordMapper;
    private final MediaAssetMapper mediaAssetMapper;
    private final MediaStorageService mediaStorageService;
    private final LearningStatsRecorder learningStatsRecorder;
    private final String asrEngineName;
    private final MasterDataCache masterDataCache;
    private final RuntimeConfigService runtimeConfigService;

    public DialogueService(
            VideoMaterialMapper videoMaterialMapper,
            DialogueLineMapper dialogueLineMapper,
            DialogueLineVocabMapper dialogueLineVocabMapper,
            AsrJobMapper asrJobMapper,
            SpeechRecordMapper speechRecordMapper,
            MediaAssetMapper mediaAssetMapper,
            MediaStorageService mediaStorageService,
            LearningStatsRecorder learningStatsRecorder,
            MasterDataCache masterDataCache,
            RuntimeConfigService runtimeConfigService,
            @Value("${app.asr.engine-name:local-asr}") String asrEngineName
    ) {
        this.videoMaterialMapper = videoMaterialMapper;
        this.dialogueLineMapper = dialogueLineMapper;
        this.dialogueLineVocabMapper = dialogueLineVocabMapper;
        this.asrJobMapper = asrJobMapper;
        this.speechRecordMapper = speechRecordMapper;
        this.mediaAssetMapper = mediaAssetMapper;
        this.mediaStorageService = mediaStorageService;
        this.learningStatsRecorder = learningStatsRecorder;
        this.masterDataCache = masterDataCache;
        this.runtimeConfigService = runtimeConfigService;
        this.asrEngineName = StringUtils.hasText(asrEngineName) ? asrEngineName.trim() : "local-asr";
    }

    public PageResult<VideoMaterialVO> listMaterials(long page, long pageSize, String materialType, Long parentId) {
        PageParams params = PageParams.normalize(page, pageSize);
        return masterDataCache.get(
                materialCacheKey(params.page(), params.pageSize(), materialType, parentId),
                VIDEO_MATERIAL_PAGE_TYPE,
                () -> loadMaterials(params.page(), params.pageSize(), materialType, parentId)
        );
    }

    private PageResult<VideoMaterialVO> loadMaterials(long page, long pageSize, String materialType, Long parentId) {
        if (parentId != null) {
            ensureActiveMaterialHierarchy(parentId);
        }
        Page<VideoMaterial> result = videoMaterialMapper.selectPage(Page.of(page, pageSize), new LambdaQueryWrapper<VideoMaterial>()
                .eq(VideoMaterial::getStatus, "active")
                .isNull(parentId == null, VideoMaterial::getParentId)
                .eq(parentId != null, VideoMaterial::getParentId, parentId)
                .eq(StringUtils.hasText(materialType), VideoMaterial::getMaterialType, materialType)
                .orderByDesc(VideoMaterial::getUpdatedAt)
                .orderByDesc(VideoMaterial::getId));
        return PageResult.of(toMaterialVOs(result.getRecords()), result.getTotal(), page, pageSize);
    }

    public List<DialogueLineVO> listLines(Long materialId) {
        return masterDataCache.get(
                linesCacheKey(materialId),
                DIALOGUE_LINE_LIST_TYPE,
                () -> loadLines(materialId)
        );
    }

    private List<DialogueLineVO> loadLines(Long materialId) {
        List<VideoMaterial> scopeMaterials = resolveActiveMaterialScope(materialId);
        List<Long> scopeIds = scopeMaterials.stream().map(VideoMaterial::getId).toList();
        if (scopeIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Integer> materialOrder = new HashMap<>();
        for (int i = 0; i < scopeIds.size(); i++) {
            materialOrder.put(scopeIds.get(i), i);
        }
        List<DialogueLine> lines = dialogueLineMapper.selectList(new LambdaQueryWrapper<DialogueLine>()
                        .in(DialogueLine::getMaterialId, scopeIds))
                .stream()
                .sorted(Comparator
                        .comparing((DialogueLine line) -> materialOrder.getOrDefault(line.getMaterialId(), Integer.MAX_VALUE))
                        .thenComparing(line -> line.getLineNo() == null ? Integer.MAX_VALUE : line.getLineNo())
                        .thenComparing(DialogueLine::getId))
                .toList();
        return toLineVOs(lines);
    }

    public DialogueLineAnalysisVO getAnalysis(Long lineId) {
        return masterDataCache.get(
                analysisCacheKey(lineId),
                DIALOGUE_LINE_ANALYSIS_TYPE,
                () -> loadAnalysis(lineId)
        );
    }

    private DialogueLineAnalysisVO loadAnalysis(Long lineId) {
        DialogueLine line = requireActiveLine(lineId);
        List<DialogueLineVocabVO> vocabItems = dialogueLineVocabMapper.selectList(new LambdaQueryWrapper<DialogueLineVocab>()
                        .eq(DialogueLineVocab::getDialogueLineId, line.getId())
                        .orderByAsc(DialogueLineVocab::getId))
                .stream()
                .map(vocab -> new DialogueLineVocabVO(
                        vocab.getId(),
                        vocab.getVocabItemId(),
                        vocab.getWordText(),
                        vocab.getPinyin(),
                        vocab.getMeaningEn(),
                        vocab.getMeaningRu(),
                        vocab.getExplanation()
                ))
                .toList();
        return new DialogueLineAnalysisVO(
                line.getId(),
                line.getHanziText(),
                line.getPinyinText(),
                line.getTranslationEn(),
                line.getTranslationRu(),
                vocabItems
        );
    }

    @Transactional
    public DialogueLineCheckResultVO checkLine(Long userId, Long lineId, CheckDialogueLineRequest request) {
        DialogueLine line = requireActiveLine(lineId);
        String submittedAnswer = resolveSubmittedAnswer(request);
        if (!StringUtils.hasText(submittedAnswer)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "答案不能为空");
        }
        String standardAnswer = line.getHanziText();
        DialogueTextMatcher.MatchResult matchResult = DialogueTextMatcher.match(submittedAnswer, standardAnswer);
        boolean correct = matchResult.correct();

        StudyEvent event = learningStatsRecorder.recordEvent(
                userId,
                "dialogue",
                line.getId(),
                correct ? "correct" : "wrong",
                request.durationSeconds(),
                OffsetDateTime.now()
        );
        return new DialogueLineCheckResultVO(
                event.getId(),
                line.getId(),
                correct,
                submittedAnswer,
                standardAnswer,
                matchResult.firstMismatchIndex(),
                correct ? "回答正确" : "回答有误，请继续修改"
        );
    }

    @Transactional
    public SpeechRecordVO uploadSpeechRecord(Long userId, Long lineId, MultipartFile file, Integer durationMs) {
        DialogueLine line = requireActiveLine(lineId);
        MediaAsset audioAsset = storeSpeechAudio(userId, file, durationMs);
        OffsetDateTime now = OffsetDateTime.now();

        AsrJob asrJob = new AsrJob();
        asrJob.setUserId(userId);
        asrJob.setAudioAssetId(audioAsset.getId());
        asrJob.setEngineName(runtimeConfigService.getString(RuntimeConfigService.ASR_ENGINE_NAME, asrEngineName));
        asrJob.setStatus("pending");
        asrJob.setCreatedAt(now);
        asrJob.setUpdatedAt(now);
        asrJobMapper.insert(asrJob);

        SpeechRecord record = new SpeechRecord();
        record.setUserId(userId);
        record.setDialogueLineId(line.getId());
        record.setAudioAssetId(audioAsset.getId());
        record.setAsrJobId(asrJob.getId());
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        speechRecordMapper.insert(record);

        learningStatsRecorder.recordEvent(userId, "dialogue", line.getId(), "completed", durationSeconds(durationMs), now);
        return toSpeechRecordVO(record, asrJob, audioAsset);
    }

    public SpeechRecordVO getSpeechRecord(Long userId, Long recordId) {
        SpeechRecord record = speechRecordMapper.selectById(recordId);
        if (record == null || !userId.equals(record.getUserId())) {
            throw BusinessException.notFound("录音记录不存在");
        }
        AsrJob asrJob = record.getAsrJobId() == null ? null : asrJobMapper.selectById(record.getAsrJobId());
        MediaAsset audioAsset = mediaAssetMapper.selectById(record.getAudioAssetId());
        return toSpeechRecordVO(record, asrJob, audioAsset);
    }

    @Transactional
    public SpeechRecordVO retrySpeechRecord(Long userId, Long recordId) {
        SpeechRecord record = speechRecordMapper.selectById(recordId);
        if (record == null || !userId.equals(record.getUserId())) {
            throw BusinessException.notFound("录音记录不存在");
        }
        if (record.getAsrJobId() == null) {
            throw BusinessException.notFound("ASR 任务不存在");
        }
        AsrJob asrJob = asrJobMapper.selectById(record.getAsrJobId());
        if (asrJob == null) {
            throw BusinessException.notFound("ASR 任务不存在");
        }
        if ("pending".equals(asrJob.getStatus()) || "processing".equals(asrJob.getStatus())) {
            MediaAsset audioAsset = mediaAssetMapper.selectById(record.getAudioAssetId());
            return toSpeechRecordVO(record, asrJob, audioAsset);
        }
        if (!"failed".equals(asrJob.getStatus())) {
            throw BusinessException.conflict("当前录音不需要重新识别");
        }
        OffsetDateTime now = OffsetDateTime.now();
        int updated = asrJobMapper.resetFailedJobForRetry(asrJob.getId(), now);
        if (updated == 1) {
            speechRecordMapper.clearRecognitionResult(record.getId(), now);
        }
        SpeechRecord refreshedRecord = speechRecordMapper.selectById(recordId);
        AsrJob refreshedJob = asrJobMapper.selectById(record.getAsrJobId());
        MediaAsset audioAsset = mediaAssetMapper.selectById(record.getAudioAssetId());
        return toSpeechRecordVO(refreshedRecord, refreshedJob, audioAsset);
    }

    private List<VideoMaterialVO> toMaterialVOs(List<VideoMaterial> materials) {
        if (materials.isEmpty()) {
            return List.of();
        }
        Map<Long, MediaAsset> covers = loadMediaAssets(materials.stream().map(VideoMaterial::getCoverAssetId).toList());
        return materials.stream()
                .map(material -> {
                    MediaAsset cover = covers.get(material.getCoverAssetId());
                    return new VideoMaterialVO(
                            material.getId(),
                            material.getTitle(),
                            material.getParentId(),
                            parentMaterialTitle(material.getParentId()),
                            material.getMaterialType(),
                            material.getDescription(),
                            material.getCoverAssetId(),
                            cover == null ? null : cover.getUrl(),
                            countActiveChildren(material.getId()),
                            countDirectActiveLines(material.getId()),
                            countActiveLines(material.getId())
                    );
                })
                .toList();
    }

    private List<DialogueLineVO> toLineVOs(List<DialogueLine> lines) {
        if (lines.isEmpty()) {
            return List.of();
        }
        Map<Long, MediaAsset> audioAssets = loadMediaAssets(lines.stream().map(DialogueLine::getAudioAssetId).toList());
        return lines.stream()
                .map(line -> {
                    MediaAsset audio = audioAssets.get(line.getAudioAssetId());
                    return new DialogueLineVO(
                            line.getId(),
                            line.getMaterialId(),
                            line.getLineNo(),
                            line.getHanziText(),
                            line.getPinyinText(),
                            line.getTranslationEn(),
                            line.getTranslationRu(),
                            line.getAudioAssetId(),
                            audio == null ? null : audio.getUrl(),
                            line.getStartMs(),
                            line.getEndMs(),
                            wordOptions(line.getHanziText())
                    );
                })
                .toList();
    }

    private Map<Long, MediaAsset> loadMediaAssets(List<Long> ids) {
        List<Long> assetIds = ids.stream().filter(id -> id != null).distinct().toList();
        if (assetIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return mediaAssetMapper.selectList(new LambdaQueryWrapper<MediaAsset>()
                        .in(MediaAsset::getId, assetIds)
                        .eq(MediaAsset::getStatus, "active"))
                .stream()
                .collect(Collectors.toMap(MediaAsset::getId, Function.identity()));
    }

    private MediaAsset storeSpeechAudio(Long userId, MultipartFile file, Integer durationMs) {
        validateSpeechAudio(file);
        String extension = extension(file.getOriginalFilename());
        if (!StringUtils.hasText(extension)) {
            extension = extensionFromContentType(file.getContentType());
        }
        String objectKey = "speech-records/" + userId + "/" + UUID.randomUUID() + "." + extension;
        StoredMediaObject storedObject;
        try {
            try (InputStream inputStream = file.getInputStream()) {
                storedObject = mediaStorageService.store(objectKey, file.getContentType(), file.getSize(), inputStream);
            }
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "录音文件保存失败");
        } catch (MediaStorageException ex) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, "录音文件保存失败");
        }

        OffsetDateTime now = OffsetDateTime.now();
        MediaAsset asset = new MediaAsset();
        asset.setMediaType("audio");
        asset.setObjectKey(storedObject.objectKey());
        asset.setOriginalFilename(originalFilename(file));
        asset.setUrl(storedObject.url());
        asset.setLanguage("zh");
        asset.setDurationMs(durationMs == null || durationMs < 0 ? null : durationMs);
        asset.setFileSize(file.getSize());
        asset.setStatus("active");
        asset.setCreatedAt(now);
        asset.setUpdatedAt(now);
        mediaAssetMapper.insert(asset);
        return asset;
    }

    private String originalFilename(MultipartFile file) {
        if (file == null || !StringUtils.hasText(file.getOriginalFilename())) {
            return null;
        }
        String value = file.getOriginalFilename().trim().replace('\\', '/');
        int index = value.lastIndexOf('/');
        String filename = index >= 0 ? value.substring(index + 1) : value;
        filename = filename.replaceAll("[\\r\\n\\t]", " ").trim();
        if (!StringUtils.hasText(filename) || ".".equals(filename) || "..".equals(filename)) {
            return null;
        }
        return filename.length() > 255 ? filename.substring(0, 255) : filename;
    }

    private void validateSpeechAudio(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "请选择录音文件");
        }
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.toLowerCase(Locale.ROOT).startsWith("audio/")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "录音 MIME 类型不匹配");
        }
        String extension = extension(file.getOriginalFilename());
        if (!StringUtils.hasText(extension)) {
            extension = extensionFromContentType(contentType);
        }
        Set<String> allowedExtensions = parseExtensions(runtimeConfigService.getString(
                RuntimeConfigService.UPLOAD_AUDIO_EXTENSIONS,
                String.join(",", ALLOWED_SPEECH_AUDIO_EXTENSIONS)
        ));
        if (!allowedExtensions.contains(extension)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "录音文件后缀不支持");
        }
    }

    private Set<String> parseExtensions(String value) {
        if (!StringUtils.hasText(value)) {
            return Set.of();
        }
        return java.util.Arrays.stream(value.split(","))
                .map(item -> item.trim().toLowerCase(Locale.ROOT))
                .filter(StringUtils::hasText)
                .collect(java.util.stream.Collectors.toSet());
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

    private String extensionFromContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return "";
        }
        String normalized = contentType.toLowerCase(Locale.ROOT);
        if (normalized.contains("mpeg") || normalized.contains("mp3")) {
            return "mp3";
        }
        if (normalized.contains("mp4") || normalized.contains("m4a")) {
            return "m4a";
        }
        if (normalized.contains("wav")) {
            return "wav";
        }
        if (normalized.contains("ogg")) {
            return "ogg";
        }
        if (normalized.contains("webm")) {
            return "webm";
        }
        if (normalized.contains("aac")) {
            return "aac";
        }
        return "";
    }

    private Integer durationSeconds(Integer durationMs) {
        if (durationMs == null || durationMs <= 0) {
            return 0;
        }
        return Math.min(86400, Math.max(1, (int) Math.round(durationMs / 1000.0)));
    }

    private SpeechRecordVO toSpeechRecordVO(SpeechRecord record, AsrJob asrJob, MediaAsset audioAsset) {
        return new SpeechRecordVO(
                record.getId(),
                record.getDialogueLineId(),
                record.getAudioAssetId(),
                audioAsset == null ? null : audioAsset.getUrl(),
                record.getAsrJobId(),
                asrJob == null ? null : asrJob.getStatus(),
                StringUtils.hasText(record.getRecognizedText()) ? record.getRecognizedText() : asrJob == null ? null : asrJob.getRecognizedText(),
                record.getCompareResult(),
                record.getScore(),
                asrJob == null ? null : asrJob.getErrorMessage(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }

    private long countDirectActiveLines(Long materialId) {
        return dialogueLineMapper.selectCount(new LambdaQueryWrapper<DialogueLine>()
                .eq(DialogueLine::getMaterialId, materialId));
    }

    private long countActiveLines(Long materialId) {
        List<Long> scopeIds = resolveActiveMaterialScope(materialId).stream().map(VideoMaterial::getId).toList();
        if (scopeIds.isEmpty()) {
            return 0;
        }
        return dialogueLineMapper.selectCount(new LambdaQueryWrapper<DialogueLine>()
                .in(DialogueLine::getMaterialId, scopeIds));
    }

    private long countActiveChildren(Long materialId) {
        return videoMaterialMapper.selectCount(new LambdaQueryWrapper<VideoMaterial>()
                .eq(VideoMaterial::getParentId, materialId)
                .eq(VideoMaterial::getStatus, "active"));
    }

    private String parentMaterialTitle(Long parentId) {
        if (parentId == null) {
            return null;
        }
        VideoMaterial parent = videoMaterialMapper.selectById(parentId);
        return parent == null ? null : parent.getTitle();
    }

    private String materialCacheKey(long page, long pageSize, String materialType, Long parentId) {
        return "dialogue:materials:page:%d:size:%d:type:%s:parent:%s".formatted(
                page,
                pageSize,
                cachePart(materialType),
                parentId == null ? "_" : parentId
        );
    }

    private String linesCacheKey(Long materialId) {
        return "dialogue:lines:material:%d".formatted(materialId);
    }

    private String analysisCacheKey(Long lineId) {
        return "dialogue:line-analysis:id:%d".formatted(lineId);
    }

    private String cachePart(String value) {
        return StringUtils.hasText(value) ? value.trim().replace(":", "%3A") : "_";
    }

    private VideoMaterial requireActiveMaterial(Long materialId) {
        return ensureActiveMaterialHierarchy(materialId);
    }

    private DialogueLine requireActiveLine(Long lineId) {
        DialogueLine line = dialogueLineMapper.selectById(lineId);
        if (line == null) {
            throw BusinessException.notFound("台词不存在");
        }
        requireActiveMaterial(line.getMaterialId());
        return line;
    }

    private VideoMaterial ensureActiveMaterialHierarchy(Long materialId) {
        VideoMaterial material = videoMaterialMapper.selectById(materialId);
        if (material == null || !"active".equals(material.getStatus())) {
            throw BusinessException.notFound("台词材料不存在");
        }
        Set<Long> visited = new HashSet<>();
        VideoMaterial current = material;
        while (current.getParentId() != null) {
            if (!visited.add(current.getId())) {
                throw BusinessException.conflict("台词材料层级存在循环引用");
            }
            VideoMaterial parent = videoMaterialMapper.selectById(current.getParentId());
            if (parent == null || !"active".equals(parent.getStatus())) {
                throw BusinessException.notFound("台词材料不存在");
            }
            current = parent;
        }
        return material;
    }

    private List<VideoMaterial> resolveActiveMaterialScope(Long materialId) {
        VideoMaterial root = ensureActiveMaterialHierarchy(materialId);
        List<VideoMaterial> scope = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        ArrayDeque<VideoMaterial> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            VideoMaterial current = queue.removeFirst();
            if (!visited.add(current.getId())) {
                continue;
            }
            scope.add(current);
            List<VideoMaterial> children = videoMaterialMapper.selectList(new LambdaQueryWrapper<VideoMaterial>()
                    .eq(VideoMaterial::getParentId, current.getId())
                    .eq(VideoMaterial::getStatus, "active")
                    .orderByDesc(VideoMaterial::getUpdatedAt)
                    .orderByDesc(VideoMaterial::getId));
            queue.addAll(children);
        }
        return scope;
    }

    private List<String> wordOptions(String answer) {
        if (!StringUtils.hasText(answer)) {
            return List.of();
        }
        return answer.codePoints()
                .mapToObj(Character::toString)
                .filter(StringUtils::hasText)
                .filter(text -> !text.matches("[\\s\\p{Punct}，。！？、；：“”‘’（）《》【】]+"))
                .toList();
    }

    private String resolveSubmittedAnswer(CheckDialogueLineRequest request) {
        if (request.orderedWords() != null && !request.orderedWords().isEmpty()) {
            return request.orderedWords().stream()
                    .filter(StringUtils::hasText)
                    .collect(Collectors.joining(""));
        }
        return request.answerText();
    }

    private static final Set<String> ALLOWED_SPEECH_AUDIO_EXTENSIONS = Set.of("mp3", "wav", "m4a", "ogg", "aac", "webm");
}
