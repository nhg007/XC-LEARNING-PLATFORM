import { getJson, putJson } from '@/api/http'
import type {
  AdminClassMember,
  AdminClassMemberStats,
  AdminClassRoomDetail,
  AdminClassRoomListItem,
  AdminClassRoomQuery,
  AdminRemoveClassMemberPayload,
  AdminUpdateClassRoomStatusPayload,
  PageResult
} from '@/types/api'

export function fetchAdminClassRooms(query: AdminClassRoomQuery) {
  const params = new URLSearchParams()
  params.set('page', String(query.page))
  params.set('pageSize', String(query.pageSize))
  if (query.keyword?.trim()) {
    params.set('keyword', query.keyword.trim())
  }
  if (query.status) {
    params.set('status', query.status)
  }
  return getJson<PageResult<AdminClassRoomListItem>>(`/admin/classes?${params.toString()}`)
}

export function fetchAdminClassRoomDetail(classId: number) {
  return getJson<AdminClassRoomDetail>(`/admin/classes/${classId}`)
}

export function fetchAdminClassMembers(classId: number) {
  return getJson<AdminClassMember[]>(`/admin/classes/${classId}/members`)
}

export function fetchAdminClassStats(classId: number) {
  return getJson<AdminClassMemberStats[]>(`/admin/classes/${classId}/stats`)
}

export function updateAdminClassRoomStatus(classId: number, payload: AdminUpdateClassRoomStatusPayload) {
  return putJson<AdminClassRoomDetail>(`/admin/classes/${classId}/status`, payload)
}

export function removeAdminClassMember(classId: number, userId: number, payload: AdminRemoveClassMemberPayload) {
  return putJson<AdminClassMember>(`/admin/classes/${classId}/members/${userId}/remove`, payload)
}
