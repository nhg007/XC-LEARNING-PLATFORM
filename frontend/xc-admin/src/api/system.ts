import { getJson, postJson, putJson } from '@/api/http'
import { appendSortParams } from '@/api/query'
import type {
  AdminAccount,
  AdminAccountQuery,
  AdminCreateAccountPayload,
  AdminMatchingStage,
  AdminOperationLog,
  AdminOperationLogQuery,
  AdminPermission,
  AdminRuntimeSettings,
  AdminResetAccountPasswordPayload,
  AdminRole,
  AdminRolePayload,
  AdminSystemConfig,
  AdminSystemConfigQuery,
  AdminUpdateAccountPayload,
  AdminUpdateAccountRolesPayload,
  AdminUpdateMatchingStagesPayload,
  AdminUpdateRuntimeSettingsPayload,
  AdminUpdateRolePermissionsPayload,
  AdminUpdateSystemConfigPayload,
  PageResult
} from '@/types/api'

export function fetchAdminRoles() {
  return getJson<AdminRole[]>('/admin/roles')
}

export function createAdminRole(payload: AdminRolePayload) {
  return postJson<AdminRole>('/admin/roles', payload)
}

export function updateAdminRole(roleId: number, payload: AdminRolePayload) {
  return putJson<AdminRole>(`/admin/roles/${roleId}`, payload)
}

export function deleteAdminRole(roleId: number) {
  return postJson<void>(`/admin/roles/${roleId}/delete`)
}

export function fetchAdminAccounts(query: AdminAccountQuery) {
  const params = new URLSearchParams()
  params.set('page', String(query.page))
  params.set('pageSize', String(query.pageSize))
  appendSortParams(params, query)
  if (query.keyword?.trim()) {
    params.set('keyword', query.keyword.trim())
  }
  if (query.status) {
    params.set('status', query.status)
  }
  return getJson<PageResult<AdminAccount>>(`/admin/admin-users?${params.toString()}`)
}

export function createAdminAccount(payload: AdminCreateAccountPayload) {
  return postJson<AdminAccount>('/admin/admin-users', payload)
}

export function updateAdminAccount(adminUserId: number, payload: AdminUpdateAccountPayload) {
  return putJson<AdminAccount>(`/admin/admin-users/${adminUserId}`, payload)
}

export function updateAdminAccountRoles(adminUserId: number, payload: AdminUpdateAccountRolesPayload) {
  return putJson<AdminAccount>(`/admin/admin-users/${adminUserId}/roles`, payload)
}

export function resetAdminAccountPassword(adminUserId: number, payload: AdminResetAccountPasswordPayload) {
  return putJson<AdminAccount>(`/admin/admin-users/${adminUserId}/password`, payload)
}

export function fetchAdminPermissions() {
  return getJson<AdminPermission[]>('/admin/permissions')
}

export function updateAdminRolePermissions(roleId: number, payload: AdminUpdateRolePermissionsPayload) {
  return putJson<AdminRole>(`/admin/roles/${roleId}/permissions`, payload)
}

export function fetchAdminSystemConfigs(query: AdminSystemConfigQuery) {
  const params = new URLSearchParams()
  params.set('page', String(query.page))
  params.set('pageSize', String(query.pageSize))
  appendSortParams(params, query)
  if (query.configGroup) {
    params.set('configGroup', query.configGroup)
  }
  if (query.keyword?.trim()) {
    params.set('keyword', query.keyword.trim())
  }
  return getJson<PageResult<AdminSystemConfig>>(`/admin/system-configs?${params.toString()}`)
}

export function updateAdminSystemConfig(configKey: string, payload: AdminUpdateSystemConfigPayload) {
  return putJson<AdminSystemConfig>(`/admin/system-configs/${encodeURIComponent(configKey)}`, payload)
}

export function fetchAdminMatchingStages() {
  return getJson<AdminMatchingStage[]>('/admin/system-configs/matching-stages')
}

export function updateAdminMatchingStages(payload: AdminUpdateMatchingStagesPayload) {
  return putJson<AdminMatchingStage[]>('/admin/system-configs/matching-stages', payload)
}

export function fetchAdminRuntimeSettings() {
  return getJson<AdminRuntimeSettings>('/admin/system-configs/runtime-settings')
}

export function updateAdminRuntimeSettings(payload: AdminUpdateRuntimeSettingsPayload) {
  return putJson<AdminRuntimeSettings>('/admin/system-configs/runtime-settings', payload)
}

export function fetchAdminOperationLogs(query: AdminOperationLogQuery) {
  const params = new URLSearchParams()
  params.set('page', String(query.page))
  params.set('pageSize', String(query.pageSize))
  appendSortParams(params, query)
  if (query.adminUserId) {
    params.set('adminUserId', String(query.adminUserId))
  }
  if (query.action?.trim()) {
    params.set('action', query.action.trim())
  }
  if (query.targetType?.trim()) {
    params.set('targetType', query.targetType.trim())
  }
  if (query.targetId) {
    params.set('targetId', String(query.targetId))
  }
  if (query.createdFrom) {
    params.set('createdFrom', query.createdFrom)
  }
  if (query.createdTo) {
    params.set('createdTo', query.createdTo)
  }
  if (query.keyword?.trim()) {
    params.set('keyword', query.keyword.trim())
  }
  return getJson<PageResult<AdminOperationLog>>(`/admin/operation-logs?${params.toString()}`)
}
