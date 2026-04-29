import { request } from './http'
import type { UpdateUserPreferencePayload, UserPreference } from '../types/api'

export function fetchUserPreference() {
  return request<UserPreference>('/preferences')
}

export function updateUserPreference(payload: UpdateUserPreferencePayload) {
  return request<UserPreference>('/preferences', {
    method: 'PUT',
    data: payload
  })
}
