package com.xc.study.module.stats.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xc.study.module.stats.entity.StudyEvent;
import java.time.OffsetDateTime;
import java.util.Collection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface StudyEventMapper extends BaseMapper<StudyEvent> {

    @Select("""
            <script>
            select count(*)
            from study_events se
            where se.occurred_at >= #{from}
              and exists (
                select 1
                from class_members cm
                where cm.user_id = se.user_id
                  and cm.status = 'active'
                  and cm.class_id in
                  <foreach collection="classIds" item="classId" open="(" separator="," close=")">
                    #{classId}
                  </foreach>
              )
            </script>
            """)
    long countFromByClassIds(
            @Param("from") OffsetDateTime from,
            @Param("classIds") Collection<Long> classIds
    );
}
