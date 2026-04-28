package com.xc.study.module.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xc.study.module.admin.entity.AdminOperationLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminOperationLogMapper extends BaseMapper<AdminOperationLog> {

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
