package com.xc.study.module.dialogue.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xc.study.module.dialogue.entity.SpeechRecord;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SpeechRecordMapper extends BaseMapper<SpeechRecord> {

    @Update("""
            update speech_records
            set recognized_text = #{recognizedText},
                compare_result = cast(#{compareResult} as jsonb),
                score = #{score},
                updated_at = #{updatedAt}
            where id = #{recordId}
            """)
    int updateRecognitionResult(
            @Param("recordId") Long recordId,
            @Param("recognizedText") String recognizedText,
            @Param("compareResult") String compareResult,
            @Param("score") BigDecimal score,
            @Param("updatedAt") OffsetDateTime updatedAt
    );

    @Update("""
            update speech_records
            set recognized_text = null,
                compare_result = null,
                score = null,
                updated_at = #{updatedAt}
            where id = #{recordId}
            """)
    int clearRecognitionResult(
            @Param("recordId") Long recordId,
            @Param("updatedAt") OffsetDateTime updatedAt
    );
}
