package com.xc.study.module.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xc.study.module.admin.dto.AdminOperationLogQueryDTO;
import com.xc.study.module.admin.entity.AdminOperationLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AdminOperationLogMapper extends BaseMapper<AdminOperationLog> {

    @Select("""
            <script>
            select
              id,
              admin_user_id,
              action,
              target_type,
              target_id,
              detail::text as detail,
              ip_address,
              created_at,
              updated_at
            from admin_operation_logs
            <where>
              <if test="query.adminUserId != null">
                and admin_user_id = #{query.adminUserId}
              </if>
              <if test="query.action != null and query.action != ''">
                and action = #{query.action}
              </if>
              <if test="query.targetType != null and query.targetType != ''">
                and target_type = #{query.targetType}
              </if>
              <if test="query.targetId != null">
                and target_id = #{query.targetId}
              </if>
              <if test="query.createdFrom != null">
                and created_at &gt;= #{query.createdFrom}
              </if>
              <if test="query.createdTo != null">
                and created_at &lt;= #{query.createdTo}
              </if>
              <if test="query.keyword != null and query.keyword != ''">
                and (
                  action like concat('%', #{query.keyword}, '%')
                  or target_type like concat('%', #{query.keyword}, '%')
                  or ip_address like concat('%', #{query.keyword}, '%')
                )
              </if>
            </where>
            <choose>
              <when test="query.sortBy == 'id' and query.sortDirection == 'asc'">order by id asc</when>
              <when test="query.sortBy == 'id' and query.sortDirection == 'desc'">order by id desc</when>
              <when test="query.sortBy == 'adminUserId' and query.sortDirection == 'asc'">order by admin_user_id asc, id desc</when>
              <when test="query.sortBy == 'adminUserId' and query.sortDirection == 'desc'">order by admin_user_id desc, id desc</when>
              <when test="query.sortBy == 'action' and query.sortDirection == 'asc'">order by action asc, id desc</when>
              <when test="query.sortBy == 'action' and query.sortDirection == 'desc'">order by action desc, id desc</when>
              <when test="query.sortBy == 'targetType' and query.sortDirection == 'asc'">order by target_type asc, id desc</when>
              <when test="query.sortBy == 'targetType' and query.sortDirection == 'desc'">order by target_type desc, id desc</when>
              <when test="query.sortBy == 'targetId' and query.sortDirection == 'asc'">order by target_id asc, id desc</when>
              <when test="query.sortBy == 'targetId' and query.sortDirection == 'desc'">order by target_id desc, id desc</when>
              <when test="query.sortBy == 'ipAddress' and query.sortDirection == 'asc'">order by ip_address asc, id desc</when>
              <when test="query.sortBy == 'ipAddress' and query.sortDirection == 'desc'">order by ip_address desc, id desc</when>
              <when test="query.sortBy == 'createdAt' and query.sortDirection == 'asc'">order by created_at asc, id desc</when>
              <when test="query.sortBy == 'createdAt' and query.sortDirection == 'desc'">order by created_at desc, id desc</when>
              <otherwise>order by created_at desc, id desc</otherwise>
            </choose>
            </script>
            """)
    Page<AdminOperationLog> selectLogPage(Page<AdminOperationLog> page, @Param("query") AdminOperationLogQueryDTO query);

    @Select("""
            select
              id,
              admin_user_id,
              action,
              target_type,
              target_id,
              detail::text as detail,
              ip_address,
              created_at,
              updated_at
            from admin_operation_logs
            where target_type = #{targetType}
              and target_id = #{targetId}
            order by created_at desc, id desc
            """)
    Page<AdminOperationLog> selectTargetLogPage(
            Page<AdminOperationLog> page,
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId
    );

    @Insert("""
            insert into admin_operation_logs (
              admin_user_id, action, target_type, target_id, detail, ip_address, created_at, updated_at
            ) values (
              #{adminUserId}, #{action}, #{targetType}, #{targetId}, cast(#{detail} as jsonb),
              #{ipAddress}, #{createdAt}, #{updatedAt}
            )
            """)
    int insertLog(AdminOperationLog log);
}
