import type { UserProfile } from '../types/api'

const TOKEN_KEY = 'xc_mobile_token'
const PROFILE_KEY = 'xc_mobile_profile'

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
