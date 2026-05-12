import { request } from './http'
import type { ClassMember, ClassMemberStats, ClassRoom, ClassRoomDetail } from '../types/api'

export interface JoinClassRoomPayload {
  inviteCode: string
}

export interface CreateClassRoomPayload {
  name: string
  description?: string
}

export function fetchClassRooms() {
  return request<ClassRoom[]>('/classrooms')
}

export function createClassRoom(payload: CreateClassRoomPayload) {
  return request<ClassRoom>('/classrooms', {
    method: 'POST',
    data: payload
  })
}

export function joinClassRoom(payload: JoinClassRoomPayload) {
  return request<ClassRoom>('/classrooms/join', {
    method: 'POST',
    data: payload
  })
}

export function fetchClassRoomDetail(id: number) {
  return request<ClassRoomDetail>(`/classrooms/${id}`)
}

export function fetchClassMembers(id: number) {
  return request<ClassMember[]>(`/classrooms/${id}/members`)
}

export function fetchClassStats(id: number) {
  return request<ClassMemberStats[]>(`/classrooms/${id}/stats`)
}
