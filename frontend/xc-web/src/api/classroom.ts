import { getJson, postJson } from './http'
import type { ClassMember, ClassMemberStats, ClassRoom, ClassRoomDetail } from '../types/api'

export interface CreateClassRoomPayload {
  name: string
  description?: string
}

export interface JoinClassRoomPayload {
  inviteCode: string
}

export function fetchClassRooms() {
  return getJson<ClassRoom[]>('/classrooms')
}

export function createClassRoom(payload: CreateClassRoomPayload) {
  return postJson<ClassRoom>('/classrooms', payload)
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

export function fetchClassStats(id: number) {
  return getJson<ClassMemberStats[]>(`/classrooms/${id}/stats`)
}
