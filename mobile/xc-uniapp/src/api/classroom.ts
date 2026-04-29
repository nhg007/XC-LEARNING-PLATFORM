import { request } from './http'
import type { ClassMember, ClassMemberStats, ClassRoom, ClassRoomDetail } from '../types/api'

export interface JoinClassRoomPayload {
  inviteCode: string
}

export interface AddClassMemberPayload {
  userId?: number
  email?: string
}

export function fetchClassRooms() {
  return request<ClassRoom[]>('/classrooms')
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

export function addClassMember(id: number, payload: AddClassMemberPayload) {
  return request<ClassMember>(`/classrooms/${id}/members`, {
    method: 'POST',
    data: payload
  })
}

export function reviewClassMember(id: number, userId: number, approved: boolean) {
  return request<ClassMember>(`/classrooms/${id}/members/${userId}/review`, {
    method: 'PUT',
    data: { approved }
  })
}

export function removeClassMember(id: number, userId: number) {
  return request<ClassMember>(`/classrooms/${id}/members/${userId}`, {
    method: 'DELETE'
  })
}

export function fetchClassStats(id: number) {
  return request<ClassMemberStats[]>(`/classrooms/${id}/stats`)
}
