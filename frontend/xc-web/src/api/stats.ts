import { getJson } from './http'
import type { DailyStat, LeaderboardEntry, LeaderboardQuery, LearningSummary, PageResult, StudyEvent } from '../types/api'

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

export function fetchLeaderboards(query: LeaderboardQuery) {
  const params = new URLSearchParams({
    page: String(query.page),
    pageSize: String(query.pageSize)
  })
  if (query.periodType) {
    params.set('periodType', query.periodType)
  }
  if (query.periodStart) {
    params.set('periodStart', query.periodStart)
  }
  if (query.metricType) {
    params.set('metricType', query.metricType)
  }
  return getJson<PageResult<LeaderboardEntry>>(`/leaderboards?${params.toString()}`)
}
