package com.xc.study.module.dialogue.asr;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xc.study.module.admin.service.RuntimeConfigService;
import com.xc.study.module.dialogue.entity.AsrJob;
import com.xc.study.module.dialogue.entity.DialogueLine;
import com.xc.study.module.dialogue.entity.SpeechRecord;
import com.xc.study.module.dialogue.mapper.AsrJobMapper;
import com.xc.study.module.dialogue.mapper.DialogueLineMapper;
import com.xc.study.module.dialogue.mapper.SpeechRecordMapper;
import com.xc.study.module.media.entity.MediaAsset;
import com.xc.study.module.media.mapper.MediaAssetMapper;
import com.xc.study.module.media.storage.MediaResource;
import com.xc.study.module.media.storage.MediaStorageException;
import com.xc.study.module.media.storage.MediaStorageService;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
public class AsrJobProcessor {

    private static final Logger log = LoggerFactory.getLogger(AsrJobProcessor.class);

    private final AsrJobMapper asrJobMapper;
    private final SpeechRecordMapper speechRecordMapper;
    private final MediaAssetMapper mediaAssetMapper;
    private final DialogueLineMapper dialogueLineMapper;
    private final MediaStorageService mediaStorageService;
    private final AsrClient asrClient;
    private final AsrProperties properties;
    private final RuntimeConfigService runtimeConfigService;
    private final TransactionTemplate transactionTemplate;

    public AsrJobProcessor(
            AsrJobMapper asrJobMapper,
            SpeechRecordMapper speechRecordMapper,
            MediaAssetMapper mediaAssetMapper,
            DialogueLineMapper dialogueLineMapper,
            MediaStorageService mediaStorageService,
            AsrClient asrClient,
            AsrProperties properties,
            RuntimeConfigService runtimeConfigService,
            TransactionTemplate transactionTemplate
    ) {
        this.asrJobMapper = asrJobMapper;
        this.speechRecordMapper = speechRecordMapper;
        this.mediaAssetMapper = mediaAssetMapper;
        this.dialogueLineMapper = dialogueLineMapper;
        this.mediaStorageService = mediaStorageService;
        this.asrClient = asrClient;
        this.properties = properties;
        this.runtimeConfigService = runtimeConfigService;
        this.transactionTemplate = transactionTemplate;
    }

    public int processPendingBatch() {
        int batchSize = Math.max(1, runtimeConfigService.getInt(RuntimeConfigService.ASR_BATCH_SIZE, properties.getBatchSize()));
        Page<AsrJob> page = asrJobMapper.selectPage(Page.of(1, batchSize), new LambdaQueryWrapper<AsrJob>()
                .eq(AsrJob::getStatus, "pending")
                .orderByAsc(AsrJob::getCreatedAt)
                .orderByAsc(AsrJob::getId));
        List<AsrJob> pendingJobs = page.getRecords();
        int processed = 0;
        for (AsrJob pendingJob : pendingJobs) {
            if (processJob(pendingJob.getId())) {
                processed++;
            }
        }
        return processed;
    }

    public boolean processJob(Long jobId) {
        AsrJob claimedJob = claimPendingJob(jobId);
        if (claimedJob == null) {
            return false;
        }
        try {
            AsrExecutionContext context = loadExecutionContext(claimedJob);
            AsrRecognitionResult result = asrClient.recognize(context.toRecognitionRequest());
            String recognizedText = result == null ? "" : result.recognizedText();
            if (!StringUtils.hasText(recognizedText)) {
                throw new AsrException("ASR 识别文本为空");
            }
            DialogueTextMatcher.MatchResult matchResult = DialogueTextMatcher.match(
                    recognizedText,
                    context.line().getHanziText()
            );
            markSucceeded(claimedJob.getId(), context.record().getId(), recognizedText.trim(), context.line().getHanziText(), matchResult);
            return true;
        } catch (RuntimeException ex) {
            markFailed(claimedJob.getId(), safeErrorMessage(ex));
            log.warn("ASR job {} failed: {}", claimedJob.getId(), ex.getMessage());
            return true;
        }
    }

    private AsrJob claimPendingJob(Long jobId) {
        return transactionTemplate.execute(status -> {
            OffsetDateTime now = OffsetDateTime.now();
            AsrJob update = new AsrJob();
            update.setStatus("processing");
            update.setStartedAt(now);
            update.setUpdatedAt(now);
            int updated = asrJobMapper.update(update, new LambdaUpdateWrapper<AsrJob>()
                    .eq(AsrJob::getId, jobId)
                    .eq(AsrJob::getStatus, "pending"));
            return updated == 1 ? asrJobMapper.selectById(jobId) : null;
        });
    }

    private AsrExecutionContext loadExecutionContext(AsrJob job) {
        SpeechRecord record = speechRecordMapper.selectOne(new LambdaQueryWrapper<SpeechRecord>()
                .eq(SpeechRecord::getAsrJobId, job.getId())
                .last("limit 1"));
        if (record == null) {
            throw new AsrException("录音记录不存在");
        }
        DialogueLine line = dialogueLineMapper.selectById(record.getDialogueLineId());
        if (line == null || !StringUtils.hasText(line.getHanziText())) {
            throw new AsrException("台词不存在");
        }
        MediaAsset audioAsset = mediaAssetMapper.selectById(job.getAudioAssetId());
        if (audioAsset == null || !StringUtils.hasText(audioAsset.getObjectKey())) {
            throw new AsrException("录音文件不存在");
        }
        MediaResource resource;
        try {
            resource = mediaStorageService.load(audioAsset.getObjectKey());
        } catch (MediaStorageException ex) {
            throw new AsrException("录音文件读取失败", ex);
        }
        byte[] audioBytes;
        try (InputStream inputStream = resource.inputStream()) {
            audioBytes = inputStream.readAllBytes();
        } catch (IOException ex) {
            throw new AsrException("录音文件读取失败", ex);
        }
        return new AsrExecutionContext(job, record, line, audioAsset, resource.contentType(), resource.contentLength(), audioBytes);
    }

    private void markSucceeded(
            Long jobId,
            Long recordId,
            String recognizedText,
            String standardText,
            DialogueTextMatcher.MatchResult matchResult
    ) {
        transactionTemplate.executeWithoutResult(status -> {
            OffsetDateTime now = OffsetDateTime.now();
            AsrJob jobUpdate = new AsrJob();
            jobUpdate.setRecognizedText(recognizedText);
            jobUpdate.setStatus("succeeded");
            jobUpdate.setFinishedAt(now);
            jobUpdate.setUpdatedAt(now);
            asrJobMapper.update(jobUpdate, new LambdaUpdateWrapper<AsrJob>()
                    .eq(AsrJob::getId, jobId)
                    .eq(AsrJob::getStatus, "processing"));

            speechRecordMapper.updateRecognitionResult(
                    recordId,
                    recognizedText,
                    compareResultJson(recognizedText, standardText, matchResult),
                    matchResult.score(),
                    now
            );
        });
    }

    private void markFailed(Long jobId, String errorMessage) {
        transactionTemplate.executeWithoutResult(status -> {
            OffsetDateTime now = OffsetDateTime.now();
            AsrJob jobUpdate = new AsrJob();
            jobUpdate.setStatus("failed");
            jobUpdate.setErrorMessage(errorMessage);
            jobUpdate.setFinishedAt(now);
            jobUpdate.setUpdatedAt(now);
            asrJobMapper.update(jobUpdate, new LambdaUpdateWrapper<AsrJob>()
                    .eq(AsrJob::getId, jobId)
                    .eq(AsrJob::getStatus, "processing"));
        });
    }

    private String compareResultJson(String recognizedText, String standardText, DialogueTextMatcher.MatchResult result) {
        return """
                {"correct":%s,"recognizedText":"%s","standardText":"%s","firstMismatchIndex":%s}
                """.formatted(
                result.correct(),
                jsonEscape(recognizedText),
                jsonEscape(standardText),
                result.firstMismatchIndex() == null ? "null" : result.firstMismatchIndex()
        ).trim();
    }

    private String jsonEscape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }

    private String safeErrorMessage(RuntimeException ex) {
        String message = ex.getMessage();
        if (!StringUtils.hasText(message)) {
            return "ASR 任务处理失败";
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }

    private record AsrExecutionContext(
            AsrJob job,
            SpeechRecord record,
            DialogueLine line,
            MediaAsset audioAsset,
            String contentType,
            long contentLength,
            byte[] audioBytes
    ) {

        private AsrRecognitionRequest toRecognitionRequest() {
            return new AsrRecognitionRequest(
                    job.getId(),
                    job.getUserId(),
                    job.getAudioAssetId(),
                    audioAsset.getObjectKey(),
                    contentType,
                    contentLength,
                    audioBytes,
                    record.getDialogueLineId(),
                    line.getHanziText(),
                    job.getEngineName()
            );
        }
    }
}
