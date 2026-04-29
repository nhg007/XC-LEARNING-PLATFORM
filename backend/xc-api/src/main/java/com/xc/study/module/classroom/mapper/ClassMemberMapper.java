package com.xc.study.module.classroom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xc.study.module.classroom.entity.ClassMember;
import java.time.OffsetDateTime;
import java.util.Collection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ClassMemberMapper extends BaseMapper<ClassMember> {

    @Select("""
            select count(distinct cm.class_id)
            from class_members cm
            join study_events se on se.user_id = cm.user_id
            where cm.status = 'active'
              and se.occurred_at >= #{from}
            """)
    long countActiveClassesFrom(@Param("from") OffsetDateTime from);

    @Select("""
            <script>
            select count(distinct cm.class_id)
            from class_members cm
            join study_events se on se.user_id = cm.user_id
            where cm.status = 'active'
              and se.occurred_at >= #{from}
              and cm.class_id in
              <foreach collection="classIds" item="classId" open="(" separator="," close=")">
                #{classId}
              </foreach>
            </script>
            """)
    long countActiveClassesFromInClasses(
            @Param("from") OffsetDateTime from,
            @Param("classIds") Collection<Long> classIds
    );
}
