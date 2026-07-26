const TOKEN_KEY = 'yinyu_admin_token'
const REFRESH_KEY = 'yinyu_admin_refresh_token'
const INFO_KEY = 'yinyu_admin_info'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function getRefreshToken() {
  return localStorage.getItem(REFRESH_KEY)
}

export function setRefreshToken(token) {
  localStorage.setItem(REFRESH_KEY, token)
}

export function getAdminInfo() {
  try {
    return JSON.parse(localStorage.getItem(INFO_KEY)) || {}
  } catch {
    return {}
  }
}

export function setAdminInfo(info) {
  localStorage.setItem(INFO_KEY, JSON.stringify(info || {}))
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_KEY)
  localStorage.removeItem(INFO_KEY)
}

/** 判断当前管理员是否拥有某权限标识；无 permissions 数据时默认放行（后端仍会鉴权） */
export function hasPerm(perm) {
  const { permissions } = getAdminInfo()
  if (!Array.isArray(permissions) || permissions.length === 0) return true
  return permissions.includes(perm)
}
