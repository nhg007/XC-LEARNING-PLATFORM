import { request } from './http'
import type { FavoriteStatus, PageResult, VocabItem, VocabItemProgressStatus, VocabList, VocabProgress } from '../types/api'

export interface UpdateVocabProgressPayload {
  currentIndex: number
  lastVocabItemId?: number | null
  reviewedCount?: number
  itemStatus?: VocabItemProgressStatus
  durationSeconds?: number
}

export interface VocabListQuery {
  page?: number
  pageSize?: number
  parentId?: number | null
  listType?: string
  level?: string
}

export function fetchVocabLists(pageOrQuery: number | VocabListQuery = 1, pageSize = 20) {
  const query = typeof pageOrQuery === 'number' ? { page: pageOrQuery, pageSize } : pageOrQuery
  const params = new URLSearchParams()
  params.set('page', String(query.page ?? 1))
  params.set('pageSize', String(query.pageSize ?? 20))
  if (query.parentId) {
    params.set('parentId', String(query.parentId))
  }
  if (query.listType) {
    params.set('listType', query.listType)
  }
  if (query.level) {
    params.set('level', query.level)
  }
  return request<PageResult<VocabList>>(`/vocab/lists?${params.toString()}`)
}

export function fetchVocabList(vocabListId: number) {
  return request<VocabList>(`/vocab/lists/${vocabListId}`)
}

export function fetchVocabItems(vocabListId: number) {
  return request<PageResult<VocabItem>>(`/vocab/lists/${vocabListId}/items?page=1&pageSize=100`)
}

export function fetchFavoriteVocabItems(page = 1, pageSize = 20) {
  return request<PageResult<VocabItem>>(`/vocab/favorites?page=${page}&pageSize=${pageSize}`)
}

export function fetchVocabItem(vocabItemId: number) {
  return request<VocabItem>(`/vocab/items/${vocabItemId}`)
}

export function fetchVocabProgress(vocabListId: number) {
  return request<VocabProgress>(`/vocab/lists/${vocabListId}/progress`)
}

export function updateVocabProgress(vocabListId: number, data: UpdateVocabProgressPayload) {
  return request<VocabProgress>(`/vocab/lists/${vocabListId}/progress`, {
    method: 'PUT',
    data
  })
}

export function favoriteVocabItem(vocabItemId: number) {
  return request<FavoriteStatus>(`/vocab/items/${vocabItemId}/favorite`, {
    method: 'POST'
  })
}

export function unfavoriteVocabItem(vocabItemId: number) {
  return request<FavoriteStatus>(`/vocab/items/${vocabItemId}/favorite`, {
    method: 'DELETE'
  })
}
