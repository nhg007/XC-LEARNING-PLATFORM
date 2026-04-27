import { request } from './http'
import type { DailyStat, LearningSummary, PageResult, StudyEvent } from '../types/api'

export function fetchLearningSummary() {
  return request<LearningSummary>('/stats/summary')
}

export function fetchDailyStats(days = 14) {
  return request<DailyStat[]>(`/stats/daily?days=${days}`)
}

export function fetchStudyEvents(page = 1, pageSize = 20, eventType?: string) {
  const suffix = eventType ? `&eventType=${encodeURIComponent(eventType)}` : ''
  return request<PageResult<StudyEvent>>(`/stats/events?page=${page}&pageSize=${pageSize}${suffix}`)
}
