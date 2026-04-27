import { deleteJson, getJson, postJson, putJson } from './http'
import type { ClassMember, ClassMemberStats, ClassRoom, ClassRoomDetail } from '../types/api'

export interface CreateClassRoomPayload {
  name: string
  description?: string
}

export interface JoinClassRoomPayload {
  inviteCode: string
}

export interface AddClassMemberPayload {
  userId?: number
  email?: string
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

export function addClassMember(id: number, payload: AddClassMemberPayload) {
  return postJson<ClassMember>(`/classrooms/${id}/members`, payload)
}

export function reviewClassMember(id: number, userId: number, approved: boolean) {
  return putJson<ClassMember>(`/classrooms/${id}/members/${userId}/review`, { approved })
}

export function removeClassMember(id: number, userId: number) {
  return deleteJson<ClassMember>(`/classrooms/${id}/members/${userId}`)
}

export function fetchClassStats(id: number) {
  return getJson<ClassMemberStats[]>(`/classrooms/${id}/stats`)
}
