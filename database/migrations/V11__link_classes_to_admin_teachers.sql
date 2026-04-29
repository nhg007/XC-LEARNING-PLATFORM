alter table classes
  add column if not exists teacher_admin_user_id bigint;

update classes c
set teacher_admin_user_id = au.id
from users u
join admin_users au on lower(au.username) = lower(u.email)
where c.owner_user_id = u.id
  and c.teacher_admin_user_id is null;

update classes c
set teacher_admin_user_id = teacher_admin.id
from (
  select au.id
  from admin_users au
  join admin_user_roles aur on aur.admin_user_id = au.id
  join admin_roles ar on ar.id = aur.role_id
  where au.status = 'active'
    and ar.role_code = 'teacher_admin'
  order by au.id
  limit 1
) teacher_admin
where c.teacher_admin_user_id is null;

update classes c
set teacher_admin_user_id = admin_user.id
from (
  select au.id
  from admin_users au
  where au.status = 'active'
  order by au.id
  limit 1
) admin_user
where c.teacher_admin_user_id is null;

alter table classes
  alter column owner_user_id drop not null;

do $$
begin
  if not exists (
    select 1
    from information_schema.table_constraints
    where constraint_name = 'fk_classes_teacher_admin_user'
      and table_name = 'classes'
  ) then
    alter table classes
      add constraint fk_classes_teacher_admin_user
      foreign key (teacher_admin_user_id) references admin_users(id);
  end if;
end $$;

create index if not exists idx_classes_teacher_admin on classes(teacher_admin_user_id);

comment on column classes.teacher_admin_user_id is '班级负责老师后台账号 ID';
comment on column classes.owner_user_id is '历史字段：早期前台老师用户 ID，后续不再使用';
