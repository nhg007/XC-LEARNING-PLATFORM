import { getJson } from '@/api/http'
import { appendSortParams } from '@/api/query'
import type {
  AdminLeaderboardEntry,
  AdminLeaderboardQuery,
  AdminLearningReport,
  AdminLearningReportQuery,
  PageResult
} from '@/types/api'

export function fetchAdminLearningReport(query: AdminLearningReportQuery) {
  const params = new URLSearchParams()
  params.set('page', String(query.page))
  params.set('pageSize', String(query.pageSize))
  appendSortParams(params, query)
  if (query.dateFrom) {
    params.set('dateFrom', query.dateFrom)
  }
  if (query.dateTo) {
    params.set('dateTo', query.dateTo)
  }
  if (query.keyword?.trim()) {
    params.set('keyword', query.keyword.trim())
  }
  return getJson<AdminLearningReport>(`/admin/reports/learning?${params.toString()}`)
}

export function fetchAdminLeaderboards(query: AdminLeaderboardQuery) {
  const params = new URLSearchParams()
  params.set('page', String(query.page))
  params.set('pageSize', String(query.pageSize))
  appendSortParams(params, query)
  if (query.periodType) {
    params.set('periodType', query.periodType)
  }
  if (query.periodStart) {
    params.set('periodStart', query.periodStart)
  }
  if (query.metricType) {
    params.set('metricType', query.metricType)
  }
  return getJson<PageResult<AdminLeaderboardEntry>>(`/admin/leaderboards?${params.toString()}`)
}
