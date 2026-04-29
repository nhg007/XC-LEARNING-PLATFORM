insert into admin_roles (role_code, role_name, description)
values ('teacher_admin', '教师后台管理人员', '查看用户、维护班级并查看学习报表')
on conflict (role_code) do update
set
  role_name = excluded.role_name,
  description = excluded.description,
  updated_at = now();

insert into admin_role_permissions (role_id, permission_id)
select role.id, permission.id
from admin_roles role
join admin_permissions permission on permission.permission_code in (
  'admin:users:read',
  'admin:classrooms:read',
  'admin:classrooms:update',
  'admin:report:read'
)
where role.role_code = 'teacher_admin'
on conflict (role_id, permission_id) do nothing;
