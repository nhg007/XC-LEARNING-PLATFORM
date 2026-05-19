package com.xc.study.module.admin.vo;

import java.time.OffsetDateTime;
import java.util.List;
import com.xc.study.module.vocab.vo.VocabStrokeOrderAssetVO;

public record AdminVocabItemVO(
        Long id,
        Long vocabListId,
        String vocabListName,
        String listType,
        String level,
        String vocabListStatus,
        String hanzi,
        String pinyin,
        String meaningEn,
        String meaningRu,
        String exampleSentence,
        Long audioAssetId,
        String audioUrl,
        Long strokeOrderAssetId,
        String strokeOrderUrl,
        List<VocabStrokeOrderAssetVO> strokeOrderAssets,
        Integer sortOrder,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<Long> vocabListIds,
        List<String> vocabListNames
) {
}
