import { getJson, postForm, postJson, putJson } from '@/api/http'
import type {
  AdminDialogueLine,
  AdminDialogueLinePayload,
  AdminDialogueLineQuery,
  AdminExerciseSet,
  AdminExerciseSetPayload,
  AdminExerciseSetQuery,
  AdminMediaAsset,
  AdminMediaAssetQuery,
  AdminSentenceExercise,
  AdminSentenceExercisePayload,
  AdminSentenceExerciseQuery,
  AdminUpdateContentStatusPayload,
  AdminVocabItem,
  AdminVocabItemPayload,
  AdminVocabItemQuery,
  AdminVocabList,
  AdminVocabListPayload,
  AdminVocabListQuery,
  AdminVideoMaterial,
  AdminVideoMaterialPayload,
  AdminVideoMaterialQuery,
  PageResult
} from '@/types/api'

export function fetchAdminVocabLists(query: AdminVocabListQuery) {
  const params = new URLSearchParams()
  params.set('page', String(query.page))
  params.set('pageSize', String(query.pageSize))
  if (query.keyword?.trim()) {
    params.set('keyword', query.keyword.trim())
  }
  if (query.listType) {
    params.set('listType', query.listType)
  }
  if (query.level?.trim()) {
    params.set('level', query.level.trim())
  }
  if (query.status) {
    params.set('status', query.status)
  }
  return getJson<PageResult<AdminVocabList>>(`/admin/vocab-lists?${params.toString()}`)
}

export function createAdminVocabList(payload: AdminVocabListPayload) {
  return postJson<AdminVocabList>('/admin/vocab-lists', payload)
}

export function updateAdminVocabList(listId: number, payload: AdminVocabListPayload) {
  return putJson<AdminVocabList>(`/admin/vocab-lists/${listId}`, payload)
}

export function updateAdminVocabListStatus(listId: number, payload: AdminUpdateContentStatusPayload) {
  return putJson<AdminVocabList>(`/admin/vocab-lists/${listId}/status`, payload)
}

export function fetchAdminVocabItems(query: AdminVocabItemQuery) {
  const params = new URLSearchParams()
  params.set('page', String(query.page))
  params.set('pageSize', String(query.pageSize))
  if (query.vocabListId) {
    params.set('vocabListId', String(query.vocabListId))
  }
  if (query.keyword?.trim()) {
    params.set('keyword', query.keyword.trim())
  }
  if (query.status) {
    params.set('status', query.status)
  }
  return getJson<PageResult<AdminVocabItem>>(`/admin/vocab-items?${params.toString()}`)
}

export function createAdminVocabItem(payload: AdminVocabItemPayload) {
  return postJson<AdminVocabItem>('/admin/vocab-items', payload)
}

export function updateAdminVocabItem(itemId: number, payload: AdminVocabItemPayload) {
  return putJson<AdminVocabItem>(`/admin/vocab-items/${itemId}`, payload)
}

export function updateAdminVocabItemStatus(itemId: number, payload: AdminUpdateContentStatusPayload) {
  return putJson<AdminVocabItem>(`/admin/vocab-items/${itemId}/status`, payload)
}

export function fetchAdminMediaAssets(query: AdminMediaAssetQuery) {
  const params = new URLSearchParams()
  params.set('page', String(query.page))
  params.set('pageSize', String(query.pageSize))
  if (query.keyword?.trim()) {
    params.set('keyword', query.keyword.trim())
  }
  if (query.mediaType) {
    params.set('mediaType', query.mediaType)
  }
  if (query.language) {
    params.set('language', query.language)
  }
  return getJson<PageResult<AdminMediaAsset>>(`/admin/media-assets?${params.toString()}`)
}

export function uploadAdminMediaAsset(formData: FormData) {
  return postForm<AdminMediaAsset>('/admin/media-assets', formData)
}

export function fetchAdminExerciseSets(query: AdminExerciseSetQuery) {
  const params = new URLSearchParams()
  params.set('page', String(query.page))
  params.set('pageSize', String(query.pageSize))
  if (query.keyword?.trim()) {
    params.set('keyword', query.keyword.trim())
  }
  if (query.exerciseType) {
    params.set('exerciseType', query.exerciseType)
  }
  if (query.level?.trim()) {
    params.set('level', query.level.trim())
  }
  if (query.status) {
    params.set('status', query.status)
  }
  return getJson<PageResult<AdminExerciseSet>>(`/admin/exercise-sets?${params.toString()}`)
}

export function createAdminExerciseSet(payload: AdminExerciseSetPayload) {
  return postJson<AdminExerciseSet>('/admin/exercise-sets', payload)
}

export function updateAdminExerciseSet(setId: number, payload: AdminExerciseSetPayload) {
  return putJson<AdminExerciseSet>(`/admin/exercise-sets/${setId}`, payload)
}

export function updateAdminExerciseSetStatus(setId: number, payload: AdminUpdateContentStatusPayload) {
  return putJson<AdminExerciseSet>(`/admin/exercise-sets/${setId}/status`, payload)
}

export function fetchAdminSentenceExercises(query: AdminSentenceExerciseQuery) {
  const params = new URLSearchParams()
  params.set('page', String(query.page))
  params.set('pageSize', String(query.pageSize))
  if (query.exerciseSetId) {
    params.set('exerciseSetId', String(query.exerciseSetId))
  }
  if (query.keyword?.trim()) {
    params.set('keyword', query.keyword.trim())
  }
  if (query.exerciseType) {
    params.set('exerciseType', query.exerciseType)
  }
  if (query.status) {
    params.set('status', query.status)
  }
  return getJson<PageResult<AdminSentenceExercise>>(`/admin/sentence-exercises?${params.toString()}`)
}

export function createAdminSentenceExercise(payload: AdminSentenceExercisePayload) {
  return postJson<AdminSentenceExercise>('/admin/sentence-exercises', payload)
}

export function updateAdminSentenceExercise(exerciseId: number, payload: AdminSentenceExercisePayload) {
  return putJson<AdminSentenceExercise>(`/admin/sentence-exercises/${exerciseId}`, payload)
}

export function updateAdminSentenceExerciseStatus(exerciseId: number, payload: AdminUpdateContentStatusPayload) {
  return putJson<AdminSentenceExercise>(`/admin/sentence-exercises/${exerciseId}/status`, payload)
}

export function fetchAdminVideoMaterials(query: AdminVideoMaterialQuery) {
  const params = new URLSearchParams()
  params.set('page', String(query.page))
  params.set('pageSize', String(query.pageSize))
  if (query.keyword?.trim()) {
    params.set('keyword', query.keyword.trim())
  }
  if (query.materialType) {
    params.set('materialType', query.materialType)
  }
  if (query.status) {
    params.set('status', query.status)
  }
  return getJson<PageResult<AdminVideoMaterial>>(`/admin/video-materials?${params.toString()}`)
}

export function createAdminVideoMaterial(payload: AdminVideoMaterialPayload) {
  return postJson<AdminVideoMaterial>('/admin/video-materials', payload)
}

export function updateAdminVideoMaterial(materialId: number, payload: AdminVideoMaterialPayload) {
  return putJson<AdminVideoMaterial>(`/admin/video-materials/${materialId}`, payload)
}

export function updateAdminVideoMaterialStatus(materialId: number, payload: AdminUpdateContentStatusPayload) {
  return putJson<AdminVideoMaterial>(`/admin/video-materials/${materialId}/status`, payload)
}

export function fetchAdminDialogueLines(query: AdminDialogueLineQuery) {
  const params = new URLSearchParams()
  params.set('page', String(query.page))
  params.set('pageSize', String(query.pageSize))
  if (query.materialId) {
    params.set('materialId', String(query.materialId))
  }
  if (query.keyword?.trim()) {
    params.set('keyword', query.keyword.trim())
  }
  return getJson<PageResult<AdminDialogueLine>>(`/admin/dialogue-lines?${params.toString()}`)
}

export function createAdminDialogueLine(payload: AdminDialogueLinePayload) {
  return postJson<AdminDialogueLine>('/admin/dialogue-lines', payload)
}

export function updateAdminDialogueLine(lineId: number, payload: AdminDialogueLinePayload) {
  return putJson<AdminDialogueLine>(`/admin/dialogue-lines/${lineId}`, payload)
}
