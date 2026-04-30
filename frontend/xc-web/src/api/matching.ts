import { getJson, postJson, putJson } from './http'
import type { CreateMatchingGamePayload, MatchingGameSession, MatchingGameType, MatchingSourceType, MatchingStage, MatchingStageGroup, UpdateMatchingGamePayload } from '../types/api'

export function createMatchingGame(payload: CreateMatchingGamePayload) {
  return postJson<MatchingGameSession>('/matching-games', payload)
}

export function fetchMatchingStages() {
  return getJson<MatchingStage[]>('/matching-games/stages')
}

export interface MatchingStageGroupQuery {
  gameType?: MatchingGameType
  sourceType?: MatchingSourceType
  vocabListId?: number | null
  meaningLanguage?: 'ru' | 'en'
}

export function fetchMatchingStageGroups(query: MatchingStageGroupQuery = {}) {
  const params = new URLSearchParams()
  if (query.sourceType) {
    params.set('sourceType', query.sourceType)
  }
  if (query.gameType) {
    params.set('gameType', query.gameType)
  }
  if (query.vocabListId) {
    params.set('vocabListId', String(query.vocabListId))
  }
  if (query.meaningLanguage) {
    params.set('meaningLanguage', query.meaningLanguage)
  }
  const suffix = params.toString()
  return getJson<MatchingStageGroup[]>(`/matching-games/stage-groups${suffix ? `?${suffix}` : ''}`)
}

export function fetchMatchingGame(sessionId: number) {
  return getJson<MatchingGameSession>(`/matching-games/${sessionId}`)
}

export function updateMatchingGame(sessionId: number, payload: UpdateMatchingGamePayload) {
  return putJson<MatchingGameSession>(`/matching-games/${sessionId}`, payload)
}
