import { getJson } from './http'
import type { ClassRoom, LearningSummary, MembershipStatus, PageResult, VocabList } from '../types/api'

export function fetchMembershipStatus() {
  return getJson<MembershipStatus>('/membership/status')
}

export function fetchLearningSummary() {
  return getJson<LearningSummary>('/stats/summary')
}

export function fetchVocabLists() {
  return getJson<PageResult<VocabList>>('/vocab/lists?page=1&pageSize=10')
}

export function fetchClassRooms() {
  return getJson<ClassRoom[]>('/classrooms')
}
