package com.xc.study.module.exercise.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.xc.study.common.PageResult;
import com.xc.study.common.cache.MasterDataCache;
import com.xc.study.module.exercise.mapper.ExerciseAttemptMapper;
import com.xc.study.module.exercise.mapper.ExerciseSetItemMapper;
import com.xc.study.module.exercise.mapper.ExerciseSetMapper;
import com.xc.study.module.exercise.mapper.SentenceExerciseMapper;
import com.xc.study.module.exercise.mapper.SentenceWordOptionMapper;
import com.xc.study.module.exercise.mapper.UserSentenceFavoriteMapper;
import com.xc.study.module.exercise.mapper.UserSentenceProgressMapper;
import com.xc.study.module.exercise.vo.ExerciseSetVO;
import com.xc.study.module.media.mapper.MediaAssetMapper;
import com.xc.study.module.stats.service.LearningStatsRecorder;
import java.util.List;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ExerciseServiceTests {

    @Test
    void listSetsWithoutExerciseTypeDoesNotDefaultToAudioOrder() {
        MasterDataCache masterDataCache = mock(MasterDataCache.class);
        when(masterDataCache.get(any(String.class), any(TypeReference.class), any(Supplier.class)))
                .thenReturn(PageResult.of(List.<ExerciseSetVO>of(), 0, 1, 20));
        ExerciseService service = new ExerciseService(
                mock(ExerciseSetMapper.class),
                mock(ExerciseSetItemMapper.class),
                mock(SentenceExerciseMapper.class),
                mock(SentenceWordOptionMapper.class),
                mock(ExerciseAttemptMapper.class),
                mock(UserSentenceFavoriteMapper.class),
                mock(UserSentenceProgressMapper.class),
                mock(MediaAssetMapper.class),
                mock(LearningStatsRecorder.class),
                masterDataCache
        );

        service.listSets(1, 20, null, null, null);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(masterDataCache).get(keyCaptor.capture(), any(TypeReference.class), any(Supplier.class));
        assertTrue(keyCaptor.getValue().contains("type:_"));
    }
}
