package com.xc.study.module.dialogue.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.xc.study.common.BusinessException;
import com.xc.study.common.ErrorCode;
import com.xc.study.common.PageResult;
import com.xc.study.common.cache.MasterDataCache;
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
import java.util.Collections;
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
        this.asrEngineName = StringUtils.hasText(asrEngineName) ? asrEngineName.trim() : "local-asr";
    }

    public PageResult<VideoMaterialVO> listMaterials(long page, long pageSize, String materialType) {
        return masterDataCache.get(
                materialCacheKey(page, pageSize, materialType),
                VIDEO_MATERIAL_PAGE_TYPE,
                () -> loadMaterials(page, pageSize, materialType)
        );
    }

    private PageResult<VideoMaterialVO> loadMaterials(long page, long pageSize, String materialType) {
        Page<VideoMaterial> result = videoMaterialMapper.selectPage(Page.of(page, pageSize), new LambdaQueryWrapper<VideoMaterial>()
                .eq(VideoMaterial::getStatus, "active")
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
        VideoMaterial material = requireActiveMaterial(materialId);
        List<DialogueLine> lines = dialogueLineMapper.selectList(new LambdaQueryWrapper<DialogueLine>()
                .eq(DialogueLine::getMaterialId, material.getId())
                .orderByAsc(DialogueLine::getLineNo)
                .orderByAsc(DialogueLine::getId));
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
        asrJob.setEngineName(asrEngineName);
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
                            material.getMaterialType(),
                            material.getDescription(),
                            material.getCoverAssetId(),
                            cover == null ? null : cover.getUrl(),
                            countLines(material.getId())
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
        if (!ALLOWED_SPEECH_AUDIO_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "录音文件后缀不支持");
        }
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

    private long countLines(Long materialId) {
        return dialogueLineMapper.selectCount(new LambdaQueryWrapper<DialogueLine>()
                .eq(DialogueLine::getMaterialId, materialId));
    }

    private String materialCacheKey(long page, long pageSize, String materialType) {
        return "dialogue:materials:page:%d:size:%d:type:%s".formatted(
                page,
                pageSize,
                cachePart(materialType)
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
        VideoMaterial material = videoMaterialMapper.selectById(materialId);
        if (material == null || !"active".equals(material.getStatus())) {
            throw BusinessException.notFound("台词材料不存在");
        }
        return material;
    }

    private DialogueLine requireActiveLine(Long lineId) {
        DialogueLine line = dialogueLineMapper.selectById(lineId);
        if (line == null) {
            throw BusinessException.notFound("台词不存在");
        }
        requireActiveMaterial(line.getMaterialId());
        return line;
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
