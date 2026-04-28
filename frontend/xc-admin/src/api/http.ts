import { ElMessage } from 'element-plus'
import router from '../router'
import type { ApiResponse } from '../types/api'
import { clearToken, getToken } from '../utils/storage'

const baseUrl = import.meta.env.VITE_API_BASE_URL

interface RequestOptions extends Omit<RequestInit, 'body'> {
  body?: unknown
  skipAuth?: boolean
}

export class ApiError extends Error {
  code: string
  traceId: string
  status: number

  constructor(message: string, code: string, traceId: string, status: number) {
    super(message)
    this.code = code
    this.traceId = traceId
    this.status = status
  }
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  if (!baseUrl) {
    throw new ApiError('VITE_API_BASE_URL 未配置', 'CONFIG_ERROR', '', 0)
  }
  const headers = new Headers(options.headers)
  headers.set('Accept', 'application/json')
  const isFormData = options.body instanceof FormData
  if (options.body !== undefined && !isFormData) {
    headers.set('Content-Type', 'application/json')
  }
  const token = getToken()
  if (!options.skipAuth && token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const body = options.body === undefined ? undefined : isFormData ? options.body : JSON.stringify(options.body)
  const response = await fetch(`${baseUrl}${path}`, {
    ...options,
    headers,
    body: body as BodyInit | undefined
  })
  const payload = (await response.json()) as ApiResponse<T>
  if (!response.ok || !payload.success) {
    const error = new ApiError(payload.message || '请求失败', payload.code, payload.traceId, response.status)
    if (response.status === 401) {
      clearToken()
      void router.push('/login')
    }
    if (response.status === 401 || response.status === 403) {
      ElMessage.warning(error.message)
    }
    throw error
  }
  return payload.data
}

export function getJson<T>(path: string, options?: RequestOptions) {
  return request<T>(path, { ...options, method: 'GET' })
}

export function postJson<T>(path: string, body?: unknown, options?: RequestOptions) {
  return request<T>(path, { ...options, method: 'POST', body })
}

export function postForm<T>(path: string, body: FormData, options?: RequestOptions) {
  return request<T>(path, { ...options, method: 'POST', body })
}

export function putJson<T>(path: string, body?: unknown, options?: RequestOptions) {
  return request<T>(path, { ...options, method: 'PUT', body })
}

export function deleteJson<T>(path: string, options?: RequestOptions) {
  return request<T>(path, { ...options, method: 'DELETE' })
}
