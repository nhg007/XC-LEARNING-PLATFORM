import { request } from './http'
import type { ExerciseAnswer, ExerciseCheckResult, ExerciseSet, FavoriteSentenceExercise, PageResult, SentenceExercise, SentenceFavoriteStatus } from '../types/api'

export interface CheckExercisePayload {
  answerText?: string
  orderedWords?: string[]
  showedAnswer?: boolean
  translationLanguage?: 'ru' | 'en'
  durationSeconds?: number
  exerciseType?: string
}

export interface ExerciseSetQuery {
  page?: number
  pageSize?: number
  parentId?: number | null
  exerciseType?: string
  level?: string
}

export interface ExerciseQuestionQuery {
  page?: number
  pageSize?: number
  exerciseType?: string
}

export function fetchExerciseSets(query: ExerciseSetQuery = {}) {
  const params = new URLSearchParams()
  params.set('page', String(query.page ?? 1))
  params.set('pageSize', String(query.pageSize ?? 100))
  if (query.parentId) {
    params.set('parentId', String(query.parentId))
  }
  if (query.exerciseType) {
    params.set('exerciseType', query.exerciseType)
  }
  if (query.level) {
    params.set('level', query.level)
  }
  return request<PageResult<ExerciseSet>>(`/exercises/sets?${params.toString()}`)
}

export function fetchExerciseQuestions(setId: number, query: ExerciseQuestionQuery | string = {}) {
  const normalizedQuery = typeof query === 'string' ? { exerciseType: query } : query
  const params = new URLSearchParams()
  params.set('page', String(normalizedQuery.page ?? 1))
  params.set('pageSize', String(normalizedQuery.pageSize ?? 100))
  if (normalizedQuery.exerciseType) {
    params.set('exerciseType', normalizedQuery.exerciseType)
  }
  return request<PageResult<SentenceExercise>>(`/exercises/sets/${setId}/questions?${params.toString()}`)
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

export function fetchFavoriteSentenceExercises(page = 1, pageSize = 20) {
  return request<PageResult<FavoriteSentenceExercise>>(`/exercises/favorites?page=${page}&pageSize=${pageSize}`)
}

export function favoriteSentenceExercise(exerciseId: number) {
  return request<SentenceFavoriteStatus>(`/exercises/${exerciseId}/favorite`, {
    method: 'POST'
  })
}

export function unfavoriteSentenceExercise(exerciseId: number) {
  return request<SentenceFavoriteStatus>(`/exercises/${exerciseId}/favorite`, {
    method: 'DELETE'
  })
}
