import { getJson } from './http'
import type { DailyStat, LeaderboardEntry, LeaderboardQuery, LearningSummary, PageResult, StudyEvent } from '../types/api'

export interface LearningRecordScope {
  classroomId: number
  userId: number
}

function statsPath(path: string, scope?: LearningRecordScope | null) {
  return scope ? `/classrooms/${scope.classroomId}/members/${scope.userId}/records/${path}` : `/stats/${path}`
}

export function fetchLearningSummary(scope?: LearningRecordScope | null) {
  return getJson<LearningSummary>(statsPath('summary', scope))
}

export function fetchStudyEvents(page = 1, pageSize = 20, eventType?: string, scope?: LearningRecordScope | null) {
  const params = new URLSearchParams({
    page: String(page),
    pageSize: String(pageSize)
  })
  if (eventType) {
    params.set('eventType', eventType)
  }
  return getJson<PageResult<StudyEvent>>(`${statsPath('events', scope)}?${params.toString()}`)
}

export function fetchDailyStats(days = 30, scope?: LearningRecordScope | null) {
  return getJson<DailyStat[]>(`${statsPath('daily', scope)}?days=${days}`)
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
