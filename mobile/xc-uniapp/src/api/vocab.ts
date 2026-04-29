import { request } from './http'
import type { FavoriteStatus, PageResult, VocabItem, VocabList, VocabProgress } from '../types/api'

export interface UpdateVocabProgressPayload {
  currentIndex: number
  lastVocabItemId?: number | null
  reviewedCount?: number
}

export function fetchVocabLists(page = 1, pageSize = 20) {
  return request<PageResult<VocabList>>(`/vocab/lists?page=${page}&pageSize=${pageSize}`)
}

export function fetchVocabItems(vocabListId: number) {
  return request<PageResult<VocabItem>>(`/vocab/lists/${vocabListId}/items?page=1&pageSize=100`)
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
