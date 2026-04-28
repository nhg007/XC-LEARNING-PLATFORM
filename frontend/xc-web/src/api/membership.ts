import { getJson, postJson } from './http'
import type { CreatePaymentOrderPayload, MembershipPlan, MembershipStatus, PaymentOrder } from '../types/api'

export function fetchMembershipStatus() {
  return getJson<MembershipStatus>('/membership/status')
}

export function fetchMembershipPlans() {
  return getJson<MembershipPlan[]>('/membership/plans')
}

export function createPaymentOrder(payload: CreatePaymentOrderPayload) {
  return postJson<PaymentOrder>('/payment/orders', payload)
}

export function fetchPaymentOrder(orderNo: string) {
  return getJson<PaymentOrder>(`/payment/orders/${encodeURIComponent(orderNo)}`)
}

export function simulatePayment(orderNo: string) {
  return postJson<PaymentOrder>(`/payment/orders/${encodeURIComponent(orderNo)}/simulate-paid`)
}
