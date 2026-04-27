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
  if (options.body !== undefined) {
    headers.set('Content-Type', 'application/json')
  }
  const token = getToken()
  if (!options.skipAuth && token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(`${baseUrl}${path}`, {
    ...options,
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body)
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
