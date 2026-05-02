import { request } from './http'
import type { ExerciseAnswer, ExerciseCheckResult, ExerciseSet, PageResult, SentenceExercise } from '../types/api'

export interface CheckExercisePayload {
  answerText?: string
  orderedWords?: string[]
  showedAnswer?: boolean
  translationLanguage?: 'ru' | 'en'
  durationSeconds?: number
}

export function fetchExerciseSets() {
  return request<PageResult<ExerciseSet>>('/exercises/sets?page=1&pageSize=100')
}

export function fetchExerciseQuestions(setId: number) {
  return request<PageResult<SentenceExercise>>(`/exercises/sets/${setId}/questions`)
}

export function checkExercise(exerciseId: number, payload: CheckExercisePayload) {
  return request<ExerciseCheckResult>(`/exercises/${exerciseId}/check`, {
    method: 'POST',
    data: payload
  })
}

export function fetchExerciseAnswer(exerciseId: number) {
  return request<ExerciseAnswer>(`/exercises/${exerciseId}/answer`)
}
