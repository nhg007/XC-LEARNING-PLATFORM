import { request } from './http'
import type { AuthToken, UserProfile } from '../types/api'

export interface LoginPayload {
  account: string
  password: string
}

export interface RegisterPayload {
  email: string
  password: string
  nickname?: string
}

export function login(payload: LoginPayload) {
  return request<AuthToken<UserProfile>>('/auth/login', {
    method: 'POST',
    data: payload,
    skipAuth: true
  })
}

export function register(payload: RegisterPayload) {
  return request<AuthToken<UserProfile>>('/auth/register', {
    method: 'POST',
    data: payload,
    skipAuth: true
  })
}
