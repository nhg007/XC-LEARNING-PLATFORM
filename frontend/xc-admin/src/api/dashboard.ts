import { getJson } from './http'
import type { AdminDashboardSummary } from '../types/api'

export function fetchDashboardSummary() {
  return getJson<AdminDashboardSummary>('/admin/dashboard/summary')
}
