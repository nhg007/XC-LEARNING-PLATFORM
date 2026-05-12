delete from admin_role_permissions
where permission_id in (
  select id
  from admin_permissions
  where permission_code in ('admin:classrooms:read', 'admin:classrooms:update')
);

delete from admin_permissions
where permission_code in ('admin:classrooms:read', 'admin:classrooms:update');

update admin_roles
set description = '历史演示角色，不再授予班级后台管理权限',
    updated_at = now()
where role_code = 'teacher_admin';
