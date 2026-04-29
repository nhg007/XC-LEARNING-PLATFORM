import type { AdminSortDirection, AdminTableSortQuery } from '@/types/api'

export interface TableSortChange {
  prop?: string
  order?: 'ascending' | 'descending' | null
}

export function applyTableSort(query: AdminTableSortQuery & { page: number }, event: TableSortChange) {
  query.sortBy = event.order && event.prop ? event.prop : ''
  query.sortDirection = sortDirection(event.order)
  query.page = 1
}

function sortDirection(order?: TableSortChange['order']): AdminSortDirection | '' {
  if (order === 'ascending') {
    return 'asc'
  }
  if (order === 'descending') {
    return 'desc'
  }
  return ''
}
