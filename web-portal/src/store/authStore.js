import { create } from 'zustand'
import { getToken, setToken, clearToken, setUnauthorizedHandler } from '../api/client'
import { apiLogin, apiRegister, apiLogout, apiMe } from '../api'

export const useAuthStore = create((set, get) => ({
  user: null,
  logged: !!getToken(),

  async init() {
    if (!getToken()) return
    try {
      const user = await apiMe(true)
      set({ user, logged: true })
    } catch (e) {
      // token 失效等场景：401 拦截器已清理
      if (e.status === 401) set({ user: null, logged: false })
    }
  },

  async login(username, password) {
    const data = await apiLogin({ username, password })
    setToken(data.accessToken, data.refreshToken)
    set({ logged: true })
    await get().init()
    return data
  },

  async register(payload) {
    const data = await apiRegister(payload)
    setToken(data.accessToken, data.refreshToken)
    set({ logged: true })
    await get().init()
    return data
  },

  async logout() {
    try {
      await apiLogout()
    } catch (e) {
      /* 忽略登出接口失败 */
    }
    clearToken()
    set({ user: null, logged: false })
  },

  refreshMe() {
    return get().init()
  },
}))

// 401 时同步登录态
setUnauthorizedHandler(() => {
  useAuthStore.setState({ user: null, logged: false })
})
