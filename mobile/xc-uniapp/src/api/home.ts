import { request } from './http'
import type { LearningSummary, MembershipStatus } from '../types/api'

export function fetchMembershipStatus() {
  return request<MembershipStatus>('/membership/status')
}

export function fetchLearningSummary() {
  return request<LearningSummary>('/stats/summary')
}
