import axios from 'axios'
import { toast } from '../components/toast'

const TOKEN_KEY = 'yinyu_token'
const REFRESH_KEY = 'yinyu_refresh_token'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}
export function setToken(accessToken, refreshToken) {
  if (accessToken) localStorage.setItem(TOKEN_KEY, accessToken)
  if (refreshToken) localStorage.setItem(REFRESH_KEY, refreshToken)
}
export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_KEY)
}
export function getRefreshToken() {
  return localStorage.getItem(REFRESH_KEY) || ''
}

let onUnauthorized = null
export function setUnauthorizedHandler(fn) {
  onUnauthorized = fn
}

const client = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

client.interceptors.request.use((config) => {
  const token = getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 统一响应处理：{ code, message, data }，code=0/200 视为成功
client.interceptors.response.use(
  (resp) => {
    const body = resp.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 0 || body.code === 200) {
        return body.data
      }
      const err = new Error(body.message || '请求失败')
      err.code = body.code
      err.silent = resp.config.silent === true
      if (!err.silent) toast.error(body.message || `请求失败(${body.code})`)
      return Promise.reject(err)
    }
    return body
  },
  (error) => {
    const status = error.response?.status
    const body = error.response?.data
    const err = new Error(body?.message || error.message || '网络异常')
    err.code = body?.code ?? status
    err.status = status
    err.silent = error.config?.silent === true
    if (status === 401) {
      clearToken()
      if (!err.silent) toast.warn(body?.message || '登录已过期，请重新登录')
      if (onUnauthorized) onUnauthorized()
    } else if (!err.silent) {
      toast.error(body?.message || (status ? `请求失败(${status})` : '网络异常，请稍后重试'))
    }
    return Promise.reject(err)
  }
)

export default client
