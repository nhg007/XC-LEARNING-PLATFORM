package com.xc.study.module.dialogue.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xc.study.module.dialogue.entity.AsrJob;
import java.time.OffsetDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AsrJobMapper extends BaseMapper<AsrJob> {

    @Update("""
            update asr_jobs
            set status = 'pending',
                recognized_text = null,
                error_message = null,
                started_at = null,
                finished_at = null,
                updated_at = #{updatedAt}
            where id = #{jobId}
              and status = 'failed'
            """)
    int resetFailedJobForRetry(
            @Param("jobId") Long jobId,
            @Param("updatedAt") OffsetDateTime updatedAt
    );
}
