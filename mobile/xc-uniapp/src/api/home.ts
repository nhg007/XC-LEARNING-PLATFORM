import { request } from './http'
import type { LearningSummary, MembershipStatus, PageResult, VocabList } from '../types/api'

export function fetchMembershipStatus() {
  return request<MembershipStatus>('/membership/status')
}

export function fetchLearningSummary() {
  return request<LearningSummary>('/stats/summary')
}

export function fetchVocabLists() {
  return request<PageResult<VocabList>>('/vocab/lists?page=1&pageSize=4')
}
