import { deleteJson, getJson, postJson, putJson } from './http'
import type { FavoriteStatus, PageResult, VocabItem, VocabItemProgressStatus, VocabList, VocabProgress } from '../types/api'

export interface UpdateVocabProgressPayload {
  currentIndex: number
  lastVocabItemId?: number | null
  reviewedCount?: number
  itemStatus?: VocabItemProgressStatus
  durationSeconds?: number
}

export interface VocabListQuery {
  pageSize?: number
  parentId?: number | null
  listType?: string
  level?: string
}

export function fetchVocabLists(pageSizeOrQuery: number | VocabListQuery = 20) {
  const query = typeof pageSizeOrQuery === 'number' ? { pageSize: pageSizeOrQuery } : pageSizeOrQuery
  const params = new URLSearchParams()
  params.set('page', '1')
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
  return getJson<PageResult<VocabList>>(`/vocab/lists?${params.toString()}`)
}

export function fetchVocabList(vocabListId: number) {
  return getJson<VocabList>(`/vocab/lists/${vocabListId}`)
}

export function fetchVocabItems(vocabListId: number, pageSize = 100) {
  return getJson<PageResult<VocabItem>>(`/vocab/lists/${vocabListId}/items?page=1&pageSize=${pageSize}`)
}

export function fetchFavoriteVocabItems(page = 1, pageSize = 20) {
  return getJson<PageResult<VocabItem>>(`/vocab/favorites?page=${page}&pageSize=${pageSize}`)
}

export function fetchVocabItem(vocabItemId: number) {
  return getJson<VocabItem>(`/vocab/items/${vocabItemId}`)
}

export function fetchVocabProgress(vocabListId: number) {
  return getJson<VocabProgress>(`/vocab/lists/${vocabListId}/progress`)
}

export function updateVocabProgress(vocabListId: number, payload: UpdateVocabProgressPayload) {
  return putJson<VocabProgress>(`/vocab/lists/${vocabListId}/progress`, payload)
}

export function favoriteVocabItem(vocabItemId: number) {
  return postJson<FavoriteStatus>(`/vocab/items/${vocabItemId}/favorite`)
}

export function unfavoriteVocabItem(vocabItemId: number) {
  return deleteJson<FavoriteStatus>(`/vocab/items/${vocabItemId}/favorite`)
}
