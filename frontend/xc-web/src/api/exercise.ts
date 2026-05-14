import { deleteJson, getJson, postJson } from './http'
import type { ExerciseAnswer, ExerciseAttempt, ExerciseCheckResult, ExerciseSet, FavoriteSentenceExercise, PageResult, SentenceExercise, SentenceFavoriteStatus } from '../types/api'

export interface CheckExercisePayload {
  answerText?: string
  orderedWords?: string[]
  showedAnswer?: boolean
  translationLanguage?: 'ru' | 'en'
  durationSeconds?: number
  exerciseType?: string
}

export interface ExerciseSetQuery {
  pageSize?: number
  parentId?: number | null
  exerciseType?: string
  level?: string
}

export interface ExerciseAttemptScope {
  classroomId: number
  userId: number
}

export function fetchExerciseSets(query: ExerciseSetQuery = {}) {
  const params = new URLSearchParams()
  params.set('page', '1')
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
  return getJson<PageResult<ExerciseSet>>(`/exercises/sets?${params.toString()}`)
}

export function fetchExerciseQuestions(setId: number, exerciseType?: string) {
  const params = new URLSearchParams()
  params.set('page', '1')
  params.set('pageSize', '50')
  if (exerciseType) {
    params.set('exerciseType', exerciseType)
  }
  return getJson<PageResult<SentenceExercise>>(`/exercises/sets/${setId}/questions?${params.toString()}`)
}

export function checkExercise(exerciseId: number, payload: CheckExercisePayload) {
  return postJson<ExerciseCheckResult>(`/exercises/${exerciseId}/check`, payload)
}

export function fetchExerciseAnswer(exerciseId: number) {
  return getJson<ExerciseAnswer>(`/exercises/${exerciseId}/answer`)
}

export function fetchFavoriteSentenceExercises(page = 1, pageSize = 20) {
  return getJson<PageResult<FavoriteSentenceExercise>>(`/exercises/favorites?page=${page}&pageSize=${pageSize}`)
}

export function favoriteSentenceExercise(exerciseId: number) {
  return postJson<SentenceFavoriteStatus>(`/exercises/${exerciseId}/favorite`)
}

export function unfavoriteSentenceExercise(exerciseId: number) {
  return deleteJson<SentenceFavoriteStatus>(`/exercises/${exerciseId}/favorite`)
}

export function fetchExerciseAttempts(page = 1, pageSize = 20, scope?: ExerciseAttemptScope | null) {
  const basePath = scope ? `/classrooms/${scope.classroomId}/members/${scope.userId}/records/attempts` : '/exercise-attempts'
  return getJson<PageResult<ExerciseAttempt>>(`${basePath}?page=${page}&pageSize=${pageSize}`)
}
