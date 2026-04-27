import { deleteJson, getJson, postJson, putJson } from './http'
import type { FavoriteStatus, PageResult, VocabItem, VocabList, VocabProgress } from '../types/api'

export interface UpdateVocabProgressPayload {
  currentIndex: number
  lastVocabItemId?: number | null
  reviewedCount?: number
}

export function fetchVocabLists(pageSize = 20) {
  return getJson<PageResult<VocabList>>(`/vocab/lists?page=1&pageSize=${pageSize}`)
}

export function fetchVocabItems(vocabListId: number, pageSize = 100) {
  return getJson<PageResult<VocabItem>>(`/vocab/lists/${vocabListId}/items?page=1&pageSize=${pageSize}`)
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
