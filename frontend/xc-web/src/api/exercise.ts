import { getJson, postJson } from './http'
import type { ExerciseAnswer, ExerciseAttempt, ExerciseCheckResult, ExerciseSet, PageResult, SentenceExercise } from '../types/api'

export interface CheckExercisePayload {
  answerText?: string
  orderedWords?: string[]
  showedAnswer?: boolean
  translationLanguage?: 'ru' | 'en'
  durationSeconds?: number
}

export function fetchExerciseSets() {
  return getJson<PageResult<ExerciseSet>>('/exercises/sets?page=1&pageSize=20')
}

export function fetchExerciseQuestions(setId: number) {
  return getJson<PageResult<SentenceExercise>>(`/exercises/sets/${setId}/questions?page=1&pageSize=50`)
}

export function checkExercise(exerciseId: number, payload: CheckExercisePayload) {
  return postJson<ExerciseCheckResult>(`/exercises/${exerciseId}/check`, payload)
}

export function fetchExerciseAnswer(exerciseId: number) {
  return getJson<ExerciseAnswer>(`/exercises/${exerciseId}/answer`)
}

export function fetchExerciseAttempts(page = 1, pageSize = 20) {
  return getJson<PageResult<ExerciseAttempt>>(`/exercise-attempts?page=${page}&pageSize=${pageSize}`)
}
