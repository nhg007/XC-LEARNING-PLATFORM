package com.xc.study.module.dialogue.service;

import com.xc.study.common.BusinessException;
import com.xc.study.module.dialogue.entity.AsrJob;
import com.xc.study.module.dialogue.entity.SpeechRecord;
import com.xc.study.module.dialogue.mapper.AsrJobMapper;
import com.xc.study.module.dialogue.mapper.DialogueLineMapper;
import com.xc.study.module.dialogue.mapper.DialogueLineVocabMapper;
import com.xc.study.module.dialogue.mapper.SpeechRecordMapper;
import com.xc.study.module.dialogue.mapper.VideoMaterialMapper;
import com.xc.study.module.dialogue.vo.SpeechRecordVO;
import com.xc.study.module.media.entity.MediaAsset;
import com.xc.study.module.media.mapper.MediaAssetMapper;
import com.xc.study.module.media.storage.MediaStorageService;
import com.xc.study.module.stats.service.LearningStatsRecorder;
import java.time.OffsetDateTime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DialogueServiceTests {

    @Test
    void retrySpeechRecordResetsFailedAsrJobAndClearsResult() {
        AsrJobMapper asrJobMapper = mock(AsrJobMapper.class);
        SpeechRecordMapper speechRecordMapper = mock(SpeechRecordMapper.class);
        MediaAssetMapper mediaAssetMapper = mock(MediaAssetMapper.class);
        DialogueService service = service(asrJobMapper, speechRecordMapper, mediaAssetMapper);

        SpeechRecord record = speechRecord();
        when(speechRecordMapper.selectById(40L)).thenReturn(record);
        when(asrJobMapper.selectById(10L)).thenReturn(job("failed")).thenReturn(job("pending"));
        when(asrJobMapper.resetFailedJobForRetry(eq(10L), any(OffsetDateTime.class))).thenReturn(1);
        when(mediaAssetMapper.selectById(20L)).thenReturn(audioAsset());

        SpeechRecordVO result = service.retrySpeechRecord(100L, 40L);

        assertEquals("pending", result.asrStatus());
        verify(asrJobMapper).resetFailedJobForRetry(eq(10L), any(OffsetDateTime.class));
        verify(speechRecordMapper).clearRecognitionResult(eq(40L), any(OffsetDateTime.class));
    }

    @Test
    void retrySpeechRecordRejectsSucceededJob() {
        AsrJobMapper asrJobMapper = mock(AsrJobMapper.class);
        SpeechRecordMapper speechRecordMapper = mock(SpeechRecordMapper.class);
        DialogueService service = service(asrJobMapper, speechRecordMapper, mock(MediaAssetMapper.class));

        when(speechRecordMapper.selectById(40L)).thenReturn(speechRecord());
        when(asrJobMapper.selectById(10L)).thenReturn(job("succeeded"));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.retrySpeechRecord(100L, 40L));

        assertEquals("当前录音不需要重新识别", ex.getMessage());
        verify(asrJobMapper, never()).resetFailedJobForRetry(any(), any());
        verify(speechRecordMapper, never()).clearRecognitionResult(any(), any());
    }

    private DialogueService service(
            AsrJobMapper asrJobMapper,
            SpeechRecordMapper speechRecordMapper,
            MediaAssetMapper mediaAssetMapper
    ) {
        return new DialogueService(
                mock(VideoMaterialMapper.class),
                mock(DialogueLineMapper.class),
                mock(DialogueLineVocabMapper.class),
                asrJobMapper,
                speechRecordMapper,
                mediaAssetMapper,
                mock(MediaStorageService.class),
                mock(LearningStatsRecorder.class),
                "local-asr"
        );
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

    private AsrJob job(String status) {
        AsrJob job = new AsrJob();
        job.setId(10L);
        job.setUserId(100L);
        job.setAudioAssetId(20L);
        job.setStatus(status);
        job.setEngineName("local-asr");
        return job;
    }

    private MediaAsset audioAsset() {
        MediaAsset asset = new MediaAsset();
        asset.setId(20L);
        asset.setUrl("/api/media/speech-records/100/demo.wav");
        asset.setObjectKey("speech-records/100/demo.wav");
        return asset;
    }
}
