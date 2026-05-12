drop index if exists idx_classes_teacher_admin;

alter table classes
  drop constraint if exists fk_classes_teacher_admin_user;

alter table classes
  drop column if exists teacher_admin_user_id;

alter table class_members
  drop constraint if exists ck_class_members_role;

alter table class_members
  drop constraint if exists ck_class_members_status;

update class_members
set status = 'active',
    joined_at = coalesce(joined_at, updated_at, created_at, now()),
    updated_at = now()
where status in ('invited', 'pending_teacher_review');

update class_members
set status = 'removed',
    removed_at = coalesce(removed_at, updated_at, created_at, now()),
    updated_at = now()
where status = 'rejected';

drop index if exists idx_class_members_invited_by;
drop index if exists idx_class_members_reviewed_by;

alter table class_members
  drop constraint if exists fk_class_members_invited_by;

alter table class_members
  drop constraint if exists fk_class_members_reviewed_by;

alter table class_members
  drop column if exists member_role,
  drop column if exists invited_by_user_id,
  drop column if exists reviewed_by_user_id,
  drop column if exists reviewed_at;

alter table class_members
  add constraint ck_class_members_status check (status in ('active', 'left', 'removed'));

comment on column classes.owner_user_id is '班级创建者学生用户 ID';
comment on column class_members.status is '成员状态：active/left/removed';
