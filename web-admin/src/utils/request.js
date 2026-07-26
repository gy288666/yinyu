import axios from 'axios'
import { message } from 'antd'
import { getToken, clearAuth } from './auth'

/**
 * 统一 axios 实例
 * - base /api，统一响应体 { code, message, data }
 * - code === 0 / 200 视为成功（以 docs/api.md 为准，兼容两种实现）
 * - 请求头自动带 Authorization: Bearer <token>
 * - HTTP 401 清除凭证并跳转登录
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 20000,
})

request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

let lastAuthTipAt = 0

function gotoLogin() {
  clearAuth()
  if (!window.location.pathname.startsWith('/login')) {
    window.location.href = '/login'
  }
}

request.interceptors.response.use(
  (response) => {
    const body = response.data
    // 文件流等非统一响应体直接返回
    if (!body || typeof body !== 'object' || !('code' in body)) {
      return body
    }
    if (body.code === 0 || body.code === 200) {
      return body.data
    }
    // token 失效业务码
    if (body.code === 10007) {
      const now = Date.now()
      if (now - lastAuthTipAt > 2000) {
        lastAuthTipAt = now
        message.error('登录已过期，请重新登录')
      }
      gotoLogin()
      return Promise.reject(new Error(body.message || 'token 失效'))
    }
    message.error(body.message || '操作失败')
    const err = new Error(body.message || '操作失败')
    err.code = body.code
    err.handled = true
    return Promise.reject(err)
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      const now = Date.now()
      if (now - lastAuthTipAt > 2000) {
        lastAuthTipAt = now
        message.error('登录已过期，请重新登录')
      }
      gotoLogin()
    } else if (status === 403) {
      message.error('没有操作权限')
    } else if (status === 404) {
      message.error('接口不存在（后端可能尚未实现）')
    } else if (status >= 500) {
      message.error(error.response?.data?.message || '服务器繁忙，请稍后重试')
    } else if (error.code === 'ECONNABORTED') {
      message.error('请求超时，请检查网络')
    } else {
      message.error('网络异常，无法连接后端服务')
    }
    error.handled = true
    return Promise.reject(error)
  }
)

export default request

/** 静默请求：失败时不抛出，返回 fallback，用于看板等可降级场景 */
export async function silentGet(url, params, fallback = null) {
  try {
    return await request.get(url, { params })
  } catch {
    return fallback
  }
}
