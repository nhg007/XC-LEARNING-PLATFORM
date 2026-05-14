import { request } from './http'
import type { DailyStat, LearningSummary, PageResult, StudyEvent } from '../types/api'

export interface LearningRecordScope {
  classId?: number
  userId?: number
}

function recordBase(scope?: LearningRecordScope) {
  if (scope?.classId && scope.userId) {
    return `/classrooms/${scope.classId}/members/${scope.userId}/records`
  }
  return '/stats'
}

export function fetchLearningSummary(scope?: LearningRecordScope) {
  return request<LearningSummary>(`${recordBase(scope)}/summary`)
}

export function fetchDailyStats(days = 14, scope?: LearningRecordScope) {
  const params = new URLSearchParams()
  params.set('days', String(days))
  return request<DailyStat[]>(`${recordBase(scope)}/daily?${params.toString()}`)
}

export function fetchStudyEvents(page = 1, pageSize = 20, eventType?: string, scope?: LearningRecordScope) {
  const params = new URLSearchParams()
  params.set('page', String(page))
  params.set('pageSize', String(pageSize))
  if (eventType) {
    params.set('eventType', eventType)
  }
  return request<PageResult<StudyEvent>>(`${recordBase(scope)}/events?${params.toString()}`)
}
