import { request } from './http'
import type { MatchingDifficulty, MatchingGameSession, MatchingGameType, MatchingSourceType, MatchingStage, MatchingStageGroup, MatchingStatus } from '../types/api'

export interface CreateMatchingGamePayload {
  gameType?: MatchingGameType
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

export interface MatchingStageGroupQuery {
  gameType?: MatchingGameType
  sourceType?: MatchingSourceType
  vocabListId?: number | null
  meaningLanguage?: 'ru' | 'en'
}

export function fetchMatchingStageGroups(query: MatchingStageGroupQuery = {}) {
  const params: Record<string, string | number> = {}
  if (query.sourceType) {
    params.sourceType = query.sourceType
  }
  if (query.gameType) {
    params.gameType = query.gameType
  }
  if (query.vocabListId) {
    params.vocabListId = query.vocabListId
  }
  if (query.meaningLanguage) {
    params.meaningLanguage = query.meaningLanguage
  }
  return request<MatchingStageGroup[]>('/matching-games/stage-groups', {
    data: params
  })
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
