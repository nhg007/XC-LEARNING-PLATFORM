import { getJson, postJson, putJson } from '@/api/http'
import type {
  AdminMembershipPlan,
  AdminMembershipPlanPayload,
  AdminMembershipPlanQuery,
  AdminPaymentOrder,
  AdminPaymentOrderQuery,
  AdminUpdateMembershipPlanStatusPayload,
  PageResult
} from '@/types/api'

export function fetchAdminMembershipPlans(query: AdminMembershipPlanQuery) {
  const params = new URLSearchParams()
  params.set('page', String(query.page))
  params.set('pageSize', String(query.pageSize))
  if (query.keyword?.trim()) {
    params.set('keyword', query.keyword.trim())
  }
  if (query.status) {
    params.set('status', query.status)
  }
  return getJson<PageResult<AdminMembershipPlan>>(`/admin/membership/plans?${params.toString()}`)
}

export function createAdminMembershipPlan(payload: AdminMembershipPlanPayload) {
  return postJson<AdminMembershipPlan>('/admin/membership/plans', payload)
}

export function updateAdminMembershipPlan(planId: number, payload: AdminMembershipPlanPayload) {
  return putJson<AdminMembershipPlan>(`/admin/membership/plans/${planId}`, payload)
}

export function updateAdminMembershipPlanStatus(planId: number, payload: AdminUpdateMembershipPlanStatusPayload) {
  return putJson<AdminMembershipPlan>(`/admin/membership/plans/${planId}/status`, payload)
}

export function fetchAdminPaymentOrders(query: AdminPaymentOrderQuery) {
  const params = new URLSearchParams()
  params.set('page', String(query.page))
  params.set('pageSize', String(query.pageSize))
  if (query.keyword?.trim()) {
    params.set('keyword', query.keyword.trim())
  }
  if (query.status) {
    params.set('status', query.status)
  }
  if (query.provider) {
    params.set('provider', query.provider)
  }
  if (query.clientType) {
    params.set('clientType', query.clientType)
  }
  if (query.createdFrom) {
    params.set('createdFrom', query.createdFrom)
  }
  if (query.createdTo) {
    params.set('createdTo', query.createdTo)
  }
  return getJson<PageResult<AdminPaymentOrder>>(`/admin/orders?${params.toString()}`)
}

export function fetchAdminPaymentOrderDetail(orderId: number) {
  return getJson<AdminPaymentOrder>(`/admin/orders/${orderId}`)
}
