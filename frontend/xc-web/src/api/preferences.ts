import { getJson, putJson } from './http'
import type { UpdateUserPreferencePayload, UserPreference } from '../types/api'

export function fetchUserPreference() {
  return getJson<UserPreference>('/preferences')
}

export function updateUserPreference(payload: UpdateUserPreferencePayload) {
  return putJson<UserPreference>('/preferences', payload)
}
