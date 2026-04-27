import { getJson } from './http'
import type { DailyStat, LearningSummary, PageResult, StudyEvent } from '../types/api'

export function fetchLearningSummary() {
  return getJson<LearningSummary>('/stats/summary')
}

export function fetchStudyEvents(page = 1, pageSize = 20, eventType?: string) {
  const params = new URLSearchParams({
    page: String(page),
    pageSize: String(pageSize)
  })
  if (eventType) {
    params.set('eventType', eventType)
  }
  return getJson<PageResult<StudyEvent>>(`/stats/events?${params.toString()}`)
}

export function fetchDailyStats(days = 30) {
  return getJson<DailyStat[]>(`/stats/daily?days=${days}`)
}
