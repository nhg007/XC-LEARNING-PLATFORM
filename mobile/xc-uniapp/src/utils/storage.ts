import type { UserProfile } from '../types/api'

const TOKEN_KEY = 'xc_mobile_token'
const PROFILE_KEY = 'xc_mobile_profile'
const LATEST_PAYMENT_ORDER_KEY = 'xc_mobile_latest_payment_order'
const LATEST_PAYMENT_ORDER_TTL_MS = 24 * 60 * 60 * 1000

interface LatestPaymentOrderCache {
  orderNo: string
  expiresAt: number
}

export function getToken() {
  return uni.getStorageSync(TOKEN_KEY) as string
}

export function setToken(token: string) {
  uni.setStorageSync(TOKEN_KEY, token)
}

export function clearToken() {
  uni.removeStorageSync(TOKEN_KEY)
}

export function getProfile() {
  return (uni.getStorageSync(PROFILE_KEY) || null) as UserProfile | null
}

export function setProfile(profile: UserProfile) {
  uni.setStorageSync(PROFILE_KEY, profile)
}

export function clearProfile() {
  uni.removeStorageSync(PROFILE_KEY)
}

export function getLatestPaymentOrderNo() {
  const cache = (uni.getStorageSync(LATEST_PAYMENT_ORDER_KEY) || null) as LatestPaymentOrderCache | null
  if (!cache?.orderNo || !cache.expiresAt || cache.expiresAt <= Date.now()) {
    clearLatestPaymentOrderNo()
    return ''
  }
  return cache.orderNo
}

export function setLatestPaymentOrderNo(orderNo: string) {
  uni.setStorageSync(LATEST_PAYMENT_ORDER_KEY, {
    orderNo,
    expiresAt: Date.now() + LATEST_PAYMENT_ORDER_TTL_MS
  } satisfies LatestPaymentOrderCache)
}

export function clearLatestPaymentOrderNo() {
  uni.removeStorageSync(LATEST_PAYMENT_ORDER_KEY)
}

export function clearSession() {
  clearToken()
  clearProfile()
  clearLatestPaymentOrderNo()
}
