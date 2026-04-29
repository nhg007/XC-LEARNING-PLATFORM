update admin_roles
set
  role_name = '教师后台管理人员',
  description = '维护自己负责的班级并查看班级学习情况',
  updated_at = now()
where role_code = 'teacher_admin';

delete from admin_role_permissions role_permission
using admin_roles role, admin_permissions permission
where role_permission.role_id = role.id
  and role_permission.permission_id = permission.id
  and role.role_code = 'teacher_admin'
  and permission.permission_code in (
    'admin:users:read',
    'admin:report:read'
  );
