import type { ApiResponse } from '../types/api'
import { t } from '../i18n'
import { redirectToLogin } from '../utils/navigation'
import { clearSession, getToken } from '../utils/storage'

const baseUrl = import.meta.env.VITE_API_BASE_URL

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: unknown
  silent?: boolean
  skipAuth?: boolean
}

interface UploadOptions {
  filePath: string
  name?: string
  formData?: Record<string, string | number | boolean>
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

export function request<T>(path: string, options: RequestOptions = {}) {
  if (!baseUrl) {
    return Promise.reject(new ApiError(t('common.configMissing'), 'CONFIG_ERROR', '', 0))
  }
  const headers: Record<string, string> = {
    Accept: 'application/json'
  }
  const token = getToken()
  if (!options.skipAuth && token) {
    headers.Authorization = `Bearer ${token}`
  }
  return new Promise<T>((resolve, reject) => {
    uni.request({
      url: `${baseUrl}${path}`,
      method: options.method || 'GET',
      data: options.data as UniApp.RequestOptions['data'],
      header: headers,
      timeout: 15000,
      success(response) {
        const payload = response.data as ApiResponse<T>
        if (response.statusCode < 200 || response.statusCode >= 300 || !payload.success) {
          const error = new ApiError(payload?.message || t('common.requestFailed'), payload?.code || 'BAD_REQUEST', payload?.traceId || '', response.statusCode)
          if (response.statusCode === 401) {
            clearSession()
            void redirectToLogin()
          }
          if (!options.silent) {
            void uni.showToast({ icon: 'none', title: error.message })
          }
          reject(error)
          return
        }
        resolve(payload.data)
      },
      fail(error) {
        if (!options.silent) {
          void uni.showToast({ icon: 'none', title: t('common.networkError') })
        }
        reject(error)
      }
    })
  })
}

export function uploadFile<T>(path: string, options: UploadOptions) {
  if (!baseUrl) {
    return Promise.reject(new ApiError(t('common.configMissing'), 'CONFIG_ERROR', '', 0))
  }
  const headers: Record<string, string> = {
    Accept: 'application/json'
  }
  const token = getToken()
  if (!options.skipAuth && token) {
    headers.Authorization = `Bearer ${token}`
  }
  return new Promise<T>((resolve, reject) => {
    uni.uploadFile({
      url: `${baseUrl}${path}`,
      filePath: options.filePath,
      name: options.name || 'file',
      formData: options.formData,
      header: headers,
      success(response) {
        const payload = parseUploadPayload<T>(response.data)
        if (response.statusCode < 200 || response.statusCode >= 300 || !payload?.success) {
          const error = new ApiError(payload?.message || t('common.uploadFailed'), payload?.code || 'BAD_REQUEST', payload?.traceId || '', response.statusCode)
          if (response.statusCode === 401) {
            clearSession()
            void redirectToLogin()
          }
          void uni.showToast({ icon: 'none', title: error.message })
          reject(error)
          return
        }
        resolve(payload.data)
      },
      fail(error) {
        void uni.showToast({ icon: 'none', title: t('common.networkError') })
        reject(error)
      }
    })
  })
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
  const normalizedBaseUrl = stripTrailingSlash(baseUrl)
  const apiOrigin = normalizedBaseUrl.endsWith('/api')
    ? normalizedBaseUrl.slice(0, -4)
    : normalizedBaseUrl
  if (value.startsWith('/api/')) {
    return `${apiOrigin}${value}`
  }
  if (value.startsWith('/')) {
    return `${apiOrigin}${value}`
  }
  return `${normalizedBaseUrl}/${value}`
}

function stripTrailingSlash(value: string) {
  return value.replace(/\/+$/, '')
}

function parseUploadPayload<T>(data: string | object) {
  if (typeof data !== 'string') {
    return data as ApiResponse<T>
  }
  try {
    return JSON.parse(data) as ApiResponse<T>
  } catch {
    return null
  }
}
