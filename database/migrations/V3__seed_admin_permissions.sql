insert into admin_permissions (permission_code, permission_name, module_name)
values
  ('admin:*', '后台全部权限', 'system'),
  ('admin:users:read', '用户查看', 'users'),
  ('admin:users:update', '用户维护', 'users'),
  ('admin:memberships:read', '会员查看', 'membership'),
  ('admin:memberships:update', '会员套餐维护', 'membership'),
  ('admin:memberships:adjust', '用户会员调整', 'membership'),
  ('admin:orders:read', '订单查看', 'orders'),
  ('admin:classrooms:read', '班级查看', 'classroom'),
  ('admin:classrooms:update', '班级维护', 'classroom'),
  ('admin:content:read', '内容查看', 'content'),
  ('admin:content:update', '内容维护', 'content'),
  ('admin:report:read', '报表查看', 'reports'),
  ('admin:system:read', '系统配置查看', 'system'),
  ('admin:system:update', '系统配置维护', 'system'),
  ('admin:audit:read', '操作日志查看', 'system')
on conflict (permission_code) do update
set
  permission_name = excluded.permission_name,
  module_name = excluded.module_name,
  updated_at = now();

insert into admin_roles (role_code, role_name, description)
values ('super_admin', '超级管理员', '拥有后台全部权限')
on conflict (role_code) do update
set
  role_name = excluded.role_name,
  description = excluded.description,
  updated_at = now();

insert into admin_role_permissions (role_id, permission_id)
select role.id, permission.id
from admin_roles role
cross join admin_permissions permission
where role.role_code = 'super_admin'
on conflict (role_id, permission_id) do nothing;

insert into admin_user_roles (admin_user_id, role_id)
select admin_user.id, role.id
from admin_users admin_user
cross join admin_roles role
where role.role_code = 'super_admin'
on conflict (admin_user_id, role_id) do nothing;
