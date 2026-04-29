import { getJson, putJson } from '@/api/http'
import { appendSortParams } from '@/api/query'
import type {
  AdminAdjustMembershipPayload,
  AdminUpdateUserStatusPayload,
  AdminUserDetail,
  AdminUserListItem,
  AdminUserQuery,
  PageResult
} from '@/types/api'

export function fetchAdminUsers(query: AdminUserQuery) {
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
  if (query.accessLevel) {
    params.set('accessLevel', query.accessLevel)
  }
  if (query.createdFrom) {
    params.set('createdFrom', query.createdFrom)
  }
  if (query.createdTo) {
    params.set('createdTo', query.createdTo)
  }
  return getJson<PageResult<AdminUserListItem>>(`/admin/users?${params.toString()}`)
}

export function fetchAdminUserDetail(userId: number) {
  return getJson<AdminUserDetail>(`/admin/users/${userId}`)
}

export function updateAdminUserStatus(userId: number, payload: AdminUpdateUserStatusPayload) {
  return putJson<AdminUserDetail>(`/admin/users/${userId}/status`, payload)
}

export function adjustAdminUserMembership(userId: number, payload: AdminAdjustMembershipPayload) {
  return putJson<AdminUserDetail>(`/admin/users/${userId}/membership`, payload)
}
