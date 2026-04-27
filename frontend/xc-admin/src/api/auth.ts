import { getJson, postJson } from './http'
import type { AdminProfile, AuthToken } from '../types/api'

export interface LoginPayload {
  account: string
  password: string
}

export function login(payload: LoginPayload) {
  return postJson<AuthToken<AdminProfile>>('/admin/auth/login', payload, { skipAuth: true })
}

export function fetchCurrentAdmin() {
  return getJson<AdminProfile>('/admin/auth/me')
}
