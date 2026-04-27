import { getJson, postJson } from './http'
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
  return postJson<AuthToken<UserProfile>>('/auth/login', payload, { skipAuth: true })
}

export function register(payload: RegisterPayload) {
  return postJson<AuthToken<UserProfile>>('/auth/register', payload, { skipAuth: true })
}

export function fetchCurrentUser() {
  return getJson<UserProfile>('/auth/me')
}
