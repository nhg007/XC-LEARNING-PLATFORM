package com.xc.study.module.dialogue.asr;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xc.study.module.dialogue.entity.AsrJob;
import com.xc.study.module.dialogue.entity.DialogueLine;
import com.xc.study.module.dialogue.entity.SpeechRecord;
import com.xc.study.module.dialogue.mapper.AsrJobMapper;
import com.xc.study.module.dialogue.mapper.DialogueLineMapper;
import com.xc.study.module.dialogue.mapper.SpeechRecordMapper;
import com.xc.study.module.media.entity.MediaAsset;
import com.xc.study.module.media.mapper.MediaAssetMapper;
import com.xc.study.module.media.storage.MediaResource;
import com.xc.study.module.media.storage.MediaStorageService;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class AsrJobProcessorTests {

    @Test
    void processPendingBatchRecognizesAudioAndUpdatesSpeechRecord() {
        AsrJobMapper asrJobMapper = mock(AsrJobMapper.class);
        SpeechRecordMapper speechRecordMapper = mock(SpeechRecordMapper.class);
        MediaAssetMapper mediaAssetMapper = mock(MediaAssetMapper.class);
        DialogueLineMapper dialogueLineMapper = mock(DialogueLineMapper.class);
        MediaStorageService mediaStorageService = mock(MediaStorageService.class);
        AsrClient asrClient = mock(AsrClient.class);
        AsrProperties properties = new AsrProperties();
        AsrJobProcessor processor = new AsrJobProcessor(
                asrJobMapper,
                speechRecordMapper,
                mediaAssetMapper,
                dialogueLineMapper,
                mediaStorageService,
                asrClient,
                properties,
                noOpTransactionTemplate()
        );

        AsrJob pendingJob = job("pending");
        Page<AsrJob> page = Page.of(1, 5);
        page.setRecords(List.of(pendingJob));
        when(asrJobMapper.selectPage(any(), any())).thenReturn(page);
        when(asrJobMapper.update(any(), any())).thenReturn(1);
        when(asrJobMapper.selectById(10L)).thenReturn(job("processing"));
        when(speechRecordMapper.selectOne(any())).thenReturn(speechRecord());
        when(dialogueLineMapper.selectById(30L)).thenReturn(line());
        when(mediaAssetMapper.selectById(20L)).thenReturn(audioAsset());
        when(mediaStorageService.load("speech-records/100/demo.wav"))
                .thenReturn(new MediaResource(new ByteArrayInputStream(new byte[]{1, 2, 3}), "audio/wav", 3));
        when(asrClient.recognize(any())).thenReturn(new AsrRecognitionResult("中 国"));

        int processed = processor.processPendingBatch();

        assertEquals(1, processed);
        ArgumentCaptor<AsrJob> jobCaptor = ArgumentCaptor.forClass(AsrJob.class);
        verify(asrJobMapper, times(2)).update(jobCaptor.capture(), any());
        assertEquals("processing", jobCaptor.getAllValues().get(0).getStatus());
        assertEquals("succeeded", jobCaptor.getAllValues().get(1).getStatus());
        verify(speechRecordMapper).updateRecognitionResult(
                eq(40L),
                eq("中 国"),
                any(),
                eq(new BigDecimal("100.00")),
                any(OffsetDateTime.class)
        );
    }

    @Test
    void processJobMarksJobFailedWhenAsrClientFails() {
        AsrJobMapper asrJobMapper = mock(AsrJobMapper.class);
        SpeechRecordMapper speechRecordMapper = mock(SpeechRecordMapper.class);
        MediaAssetMapper mediaAssetMapper = mock(MediaAssetMapper.class);
        DialogueLineMapper dialogueLineMapper = mock(DialogueLineMapper.class);
        MediaStorageService mediaStorageService = mock(MediaStorageService.class);
        AsrClient asrClient = mock(AsrClient.class);
        AsrJobProcessor processor = new AsrJobProcessor(
                asrJobMapper,
                speechRecordMapper,
                mediaAssetMapper,
                dialogueLineMapper,
                mediaStorageService,
                asrClient,
                new AsrProperties(),
                noOpTransactionTemplate()
        );

        when(asrJobMapper.update(any(), any())).thenReturn(1);
        when(asrJobMapper.selectById(10L)).thenReturn(job("processing"));
        when(speechRecordMapper.selectOne(any())).thenReturn(speechRecord());
        when(dialogueLineMapper.selectById(30L)).thenReturn(line());
        when(mediaAssetMapper.selectById(20L)).thenReturn(audioAsset());
        when(mediaStorageService.load("speech-records/100/demo.wav"))
                .thenReturn(new MediaResource(new ByteArrayInputStream(new byte[]{1, 2, 3}), "audio/wav", 3));
        when(asrClient.recognize(any())).thenThrow(new AsrException("service down"));

        boolean processed = processor.processJob(10L);

        assertTrue(processed);
        ArgumentCaptor<AsrJob> jobCaptor = ArgumentCaptor.forClass(AsrJob.class);
        verify(asrJobMapper, times(2)).update(jobCaptor.capture(), any());
        assertEquals("processing", jobCaptor.getAllValues().get(0).getStatus());
        assertEquals("failed", jobCaptor.getAllValues().get(1).getStatus());
        assertEquals("service down", jobCaptor.getAllValues().get(1).getErrorMessage());
        verify(speechRecordMapper, never()).updateRecognitionResult(any(), any(), any(), any(), any());
    }

    private AsrJob job(String status) {
        OffsetDateTime now = OffsetDateTime.now();
        AsrJob job = new AsrJob();
        job.setId(10L);
        job.setUserId(100L);
        job.setAudioAssetId(20L);
        job.setEngineName("local-asr");
        job.setStatus(status);
        job.setCreatedAt(now);
        job.setUpdatedAt(now);
        return job;
    }

    private SpeechRecord speechRecord() {
        SpeechRecord record = new SpeechRecord();
        record.setId(40L);
        record.setUserId(100L);
        record.setDialogueLineId(30L);
        record.setAudioAssetId(20L);
        record.setAsrJobId(10L);
        return record;
    }

    private DialogueLine line() {
        DialogueLine line = new DialogueLine();
        line.setId(30L);
        line.setHanziText("中国");
        return line;
    }

    private MediaAsset audioAsset() {
        MediaAsset asset = new MediaAsset();
        asset.setId(20L);
        asset.setObjectKey("speech-records/100/demo.wav");
        asset.setMediaType("audio");
        asset.setStatus("active");
        return asset;
    }

    private TransactionTemplate noOpTransactionTemplate() {
        return new TransactionTemplate(new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) {
            }

            @Override
            public void rollback(TransactionStatus status) {
            }
        });
    }
}
