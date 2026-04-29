import { request } from './http'
import type { CreatePaymentOrderPayload, MembershipPlan, MembershipStatus, PaymentOrder } from '../types/api'

export function fetchMembershipStatus() {
  return request<MembershipStatus>('/membership/status')
}

export function fetchMembershipPlans() {
  return request<MembershipPlan[]>('/membership/plans')
}

export function createPaymentOrder(data: CreatePaymentOrderPayload) {
  return request<PaymentOrder>('/payment/orders', {
    method: 'POST',
    data
  })
}

export function fetchPaymentOrder(orderNo: string, options: { silent?: boolean } = {}) {
  return request<PaymentOrder>(`/payment/orders/${encodeURIComponent(orderNo)}`, {
    silent: options.silent
  })
}

export function simulatePayment(orderNo: string) {
  return request<PaymentOrder>(`/payment/orders/${encodeURIComponent(orderNo)}/simulate-paid`, {
    method: 'POST'
  })
}
