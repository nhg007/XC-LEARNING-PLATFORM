import { request } from './http'
import type { AuthToken, UserProfile } from '../types/api'

export interface LoginPayload {
  account: string
  password: string
}

export function login(payload: LoginPayload) {
  return request<AuthToken<UserProfile>>('/auth/login', {
    method: 'POST',
    data: payload,
    skipAuth: true
  })
}
