insert into admin_permissions (permission_code, permission_name, module_name)
values ('admin:orders:update', '订单异常处理', 'orders')
on conflict (permission_code) do update
set
  permission_name = excluded.permission_name,
  module_name = excluded.module_name,
  updated_at = now();

insert into admin_role_permissions (role_id, permission_id)
select role.id, permission.id
from admin_roles role
cross join admin_permissions permission
where role.role_code = 'super_admin'
  and permission.permission_code = 'admin:orders:update'
on conflict (role_id, permission_id) do nothing;
