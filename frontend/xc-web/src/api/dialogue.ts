import { getJson, postJson } from './http'
import type {
  CheckDialogueLinePayload,
  DialogueLine,
  DialogueLineAnalysis,
  DialogueLineCheckResult,
  PageResult,
  VideoMaterial,
  VideoMaterialType
} from '../types/api'

export function fetchVideoMaterials(materialType?: VideoMaterialType | '', parentId?: number | null) {
  const params = new URLSearchParams({ page: '1', pageSize: '50' })
  if (materialType) {
    params.set('materialType', materialType)
  }
  if (parentId) {
    params.set('parentId', String(parentId))
  }
  return getJson<PageResult<VideoMaterial>>(`/video-materials?${params.toString()}`)
}

export function fetchDialogueLines(materialId: number) {
  return getJson<DialogueLine[]>(`/video-materials/${materialId}/lines`)
}

export function fetchDialogueLineAnalysis(lineId: number) {
  return getJson<DialogueLineAnalysis>(`/dialogue-lines/${lineId}/analysis`)
}

export function checkDialogueLine(lineId: number, payload: CheckDialogueLinePayload) {
  return postJson<DialogueLineCheckResult>(`/dialogue-lines/${lineId}/check`, payload)
}
