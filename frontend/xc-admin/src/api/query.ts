import type { AdminTableSortQuery } from '@/types/api'

export function appendSortParams(params: URLSearchParams, query: AdminTableSortQuery) {
  if (query.sortBy && query.sortDirection) {
    params.set('sortBy', query.sortBy)
    params.set('sortDirection', query.sortDirection)
  }
}
