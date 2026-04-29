import { getJson, postJson } from './http'
import type { ClassMember, ClassRoom, ClassRoomDetail } from '../types/api'

export interface JoinClassRoomPayload {
  inviteCode: string
}

export function fetchClassRooms() {
  return getJson<ClassRoom[]>('/classrooms')
}

export function joinClassRoom(payload: JoinClassRoomPayload) {
  return postJson<ClassRoom>('/classrooms/join', payload)
}

export function fetchClassRoomDetail(id: number) {
  return getJson<ClassRoomDetail>(`/classrooms/${id}`)
}

export function fetchClassMembers(id: number) {
  return getJson<ClassMember[]>(`/classrooms/${id}/members`)
}
