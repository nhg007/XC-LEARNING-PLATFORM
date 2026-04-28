import { getJson, postJson, putJson } from './http'
import type { CreateMatchingGamePayload, MatchingGameSession, UpdateMatchingGamePayload } from '../types/api'

export function createMatchingGame(payload: CreateMatchingGamePayload) {
  return postJson<MatchingGameSession>('/matching-games', payload)
}

export function fetchMatchingGame(sessionId: number) {
  return getJson<MatchingGameSession>(`/matching-games/${sessionId}`)
}

export function updateMatchingGame(sessionId: number, payload: UpdateMatchingGamePayload) {
  return putJson<MatchingGameSession>(`/matching-games/${sessionId}`, payload)
}
