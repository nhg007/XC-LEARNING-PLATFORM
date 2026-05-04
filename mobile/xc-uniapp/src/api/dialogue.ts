import { request, uploadFile } from './http'
import type {
  DialogueLine,
  DialogueLineAnalysis,
  DialogueLineCheckResult,
  PageResult,
  SpeechRecord,
  VideoMaterial,
  VideoMaterialType
} from '../types/api'

export interface CheckDialogueLinePayload {
  answerText?: string
  orderedWords?: string[]
  showedAnswer?: boolean
  translationLanguage?: 'ru' | 'en'
  durationSeconds?: number
}

export function fetchVideoMaterials(materialType?: VideoMaterialType | '', parentId?: number | null) {
  const params = new URLSearchParams({ page: '1', pageSize: '50' })
  if (materialType) {
    params.set('materialType', materialType)
  }
  if (parentId) {
    params.set('parentId', String(parentId))
  }
  return request<PageResult<VideoMaterial>>(`/video-materials?${params.toString()}`)
}

export function fetchDialogueLines(materialId: number) {
  return request<DialogueLine[]>(`/video-materials/${materialId}/lines`)
}

export function fetchDialogueLineAnalysis(lineId: number) {
  return request<DialogueLineAnalysis>(`/dialogue-lines/${lineId}/analysis`)
}

export function checkDialogueLine(lineId: number, payload: CheckDialogueLinePayload) {
  return request<DialogueLineCheckResult>(`/dialogue-lines/${lineId}/check`, {
    method: 'POST',
    data: payload
  })
}

export function uploadSpeechRecord(lineId: number, filePath: string, durationMs?: number) {
  const formData: Record<string, string | number> = {}
  if (durationMs !== undefined) {
    formData.durationMs = durationMs
  }
  return uploadFile<SpeechRecord>(`/dialogue-lines/${lineId}/speech-records`, {
    filePath,
    formData
  })
}

export function fetchSpeechRecord(recordId: number) {
  return request<SpeechRecord>(`/speech-records/${recordId}`)
}

export function retrySpeechRecord(recordId: number) {
  return request<SpeechRecord>(`/speech-records/${recordId}/retry`, {
    method: 'POST'
  })
}
