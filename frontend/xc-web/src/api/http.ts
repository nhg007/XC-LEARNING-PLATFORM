import router from '../router'
import { t } from '../plugins/i18n'
import type { ApiResponse } from '../types/api'
import { notifyWarning } from '../utils/notify'
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
    throw new ApiError(t('api.baseUrlMissing'), 'CONFIG_ERROR', '', 0)
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
    const error = new ApiError(payload.message || t('api.requestFailed'), payload.code, payload.traceId, response.status)
    if (response.status === 401) {
      clearToken()
      void router.push('/login')
    }
    if (error.code === 'MEMBERSHIP_REQUIRED') {
      notifyWarning(t('api.membershipRequired'))
      void router.push('/membership')
      throw error
    }
    if (response.status === 401 || response.status === 403) {
      notifyWarning(error.message)
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

export function putJson<T>(path: string, body?: unknown, options?: RequestOptions) {
  return request<T>(path, { ...options, method: 'PUT', body })
}

export function deleteJson<T>(path: string, options?: RequestOptions) {
  return request<T>(path, { ...options, method: 'DELETE' })
}

export function resolveApiResourceUrl(url?: string | null) {
  const value = url?.trim()
  if (!value) {
    return ''
  }
  if (/^(https?:|blob:|data:|file:)/i.test(value)) {
    return value
  }
  if (!baseUrl) {
    return value
  }
  const normalizedBaseUrl = normalizeBaseUrl(baseUrl)
  const apiOrigin = normalizedBaseUrl.endsWith('/api')
    ? normalizedBaseUrl.slice(0, -4)
    : normalizedBaseUrl
  const withApiOrigin = (path: string) => apiOrigin ? `${apiOrigin}${path}` : path
  if (value.startsWith('/api/')) {
    return withApiOrigin(value)
  }
  if (value.startsWith('api/')) {
    return withApiOrigin(`/${value}`)
  }
  if (value.startsWith('/media/')) {
    return normalizedBaseUrl.endsWith('/api') ? `${normalizedBaseUrl}${value}` : withApiOrigin(value)
  }
  if (value.startsWith('media/')) {
    return normalizedBaseUrl.endsWith('/api') ? `${normalizedBaseUrl}/${value}` : withApiOrigin(`/${value}`)
  }
  if (/^(audio|image|video)\//i.test(value)) {
    return normalizedBaseUrl.endsWith('/api') ? `${normalizedBaseUrl}/media/${value}` : `${normalizedBaseUrl}/${value}`
  }
  if (value.startsWith('/')) {
    return withApiOrigin(value)
  }
  return `${normalizedBaseUrl}/${value}`
}

function normalizeBaseUrl(value: string) {
  const stripped = stripTrailingSlash(value)
  if (/^(https?:)?\/\//i.test(stripped) || stripped.startsWith('/')) {
    return stripped
  }
  return `/${stripped}`
}

function stripTrailingSlash(value: string) {
  return value.replace(/\/+$/, '')
}
