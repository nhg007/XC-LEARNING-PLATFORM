import type { ApiResponse } from '../types/api'
import { clearToken, getToken } from '../utils/storage'

const baseUrl = import.meta.env.VITE_API_BASE_URL

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: unknown
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
    return Promise.reject(new ApiError('VITE_API_BASE_URL 未配置', 'CONFIG_ERROR', '', 0))
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
          const error = new ApiError(payload?.message || '请求失败', payload?.code || 'BAD_REQUEST', payload?.traceId || '', response.statusCode)
          if (response.statusCode === 401) {
            clearToken()
            void uni.redirectTo({ url: '/pages/login/login' })
          }
          void uni.showToast({ icon: 'none', title: error.message })
          reject(error)
          return
        }
        resolve(payload.data)
      },
      fail(error) {
        void uni.showToast({ icon: 'none', title: '网络异常，请稍后重试' })
        reject(error)
      }
    })
  })
}
