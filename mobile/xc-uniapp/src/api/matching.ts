import { request } from './http'
import type { MatchingDifficulty, MatchingGameSession, MatchingSourceType, MatchingStage, MatchingStatus } from '../types/api'

export interface CreateMatchingGamePayload {
  sourceType: MatchingSourceType
  vocabListId?: number | null
  meaningLanguage: 'ru' | 'en'
  difficulty: MatchingDifficulty
}

export interface UpdateMatchingGamePayload {
  matchedPairs: number
  wrongCount: number
  elapsedSeconds: number
  status: MatchingStatus
}

export function createMatchingGame(data: CreateMatchingGamePayload) {
  return request<MatchingGameSession>('/matching-games', {
    method: 'POST',
    data
  })
}

export function fetchMatchingStages() {
  return request<MatchingStage[]>('/matching-games/stages')
}

export function fetchMatchingGame(sessionId: number) {
  return request<MatchingGameSession>(`/matching-games/${sessionId}`)
}

export function updateMatchingGame(sessionId: number, data: UpdateMatchingGamePayload) {
  return request<MatchingGameSession>(`/matching-games/${sessionId}`, {
    method: 'PUT',
    data
  })
}
