import { getJson, postJson, putJson } from '@/api/http'
import { appendSortParams } from '@/api/query'
import type {
  AdminCreateOfflinePaymentOrderPayload,
  AdminMembershipPlan,
  AdminMembershipPlanPayload,
  AdminMembershipPlanQuery,
  AdminFailPaymentOrderPayload,
  AdminOrderExceptionSummary,
  AdminOperationLog,
  AdminPaymentNotification,
  AdminPaymentNotificationQuery,
  AdminPaymentOrder,
  AdminPaymentOrderQuery,
  AdminUpdateMembershipPlanStatusPayload,
  PageResult
} from '@/types/api'

export function fetchAdminMembershipPlans(query: AdminMembershipPlanQuery) {
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
  appendSortParams(params, query)
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
  if (query.exceptionType) {
    params.set('exceptionType', query.exceptionType)
  }
  if (query.pendingTimeoutMinutes) {
    params.set('pendingTimeoutMinutes', String(query.pendingTimeoutMinutes))
  }
  if (query.createdFrom) {
    params.set('createdFrom', query.createdFrom)
  }
  if (query.createdTo) {
    params.set('createdTo', query.createdTo)
  }
  return getJson<PageResult<AdminPaymentOrder>>(`/admin/orders?${params.toString()}`)
}

export function fetchAdminOrderExceptionSummary(pendingTimeoutMinutes?: number) {
  const params = new URLSearchParams()
  if (pendingTimeoutMinutes) {
    params.set('pendingTimeoutMinutes', String(pendingTimeoutMinutes))
  }
  const query = params.toString()
  return getJson<AdminOrderExceptionSummary>(`/admin/orders/exception-summary${query ? `?${query}` : ''}`)
}

export function fetchAdminPaymentOrderDetail(orderId: number) {
  return getJson<AdminPaymentOrder>(`/admin/orders/${orderId}`)
}

export function createAdminOfflinePaymentOrder(payload: AdminCreateOfflinePaymentOrderPayload) {
  return postJson<AdminPaymentOrder>('/admin/orders/offline-payments', payload)
}

export function fetchAdminPaymentOrderOperationLogs(orderId: number, page = 1, pageSize = 20) {
  const params = new URLSearchParams()
  params.set('page', String(page))
  params.set('pageSize', String(pageSize))
  return getJson<PageResult<AdminOperationLog>>(`/admin/orders/${orderId}/operation-logs?${params.toString()}`)
}

export function markAdminPaymentOrderFailed(orderId: number, payload: AdminFailPaymentOrderPayload) {
  return putJson<AdminPaymentOrder>(`/admin/orders/${orderId}/failed`, payload)
}

export function fetchAdminPaymentNotifications(query: AdminPaymentNotificationQuery) {
  const params = new URLSearchParams()
  params.set('page', String(query.page))
  params.set('pageSize', String(query.pageSize))
  appendSortParams(params, query)
  if (query.keyword?.trim()) {
    params.set('keyword', query.keyword.trim())
  }
  if (query.provider) {
    params.set('provider', query.provider)
  }
  if (query.processStatus) {
    params.set('processStatus', query.processStatus)
  }
  if (query.resultCode?.trim()) {
    params.set('resultCode', query.resultCode.trim())
  }
  if (query.signatureValid !== undefined && query.signatureValid !== '') {
    params.set('signatureValid', String(query.signatureValid))
  }
  if (query.orderId) {
    params.set('orderId', String(query.orderId))
  }
  if (query.receivedFrom) {
    params.set('receivedFrom', query.receivedFrom)
  }
  if (query.receivedTo) {
    params.set('receivedTo', query.receivedTo)
  }
  return getJson<PageResult<AdminPaymentNotification>>(`/admin/payment-notifications?${params.toString()}`)
}
