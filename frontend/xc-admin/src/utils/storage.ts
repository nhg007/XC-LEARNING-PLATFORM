const TOKEN_KEY = 'xc_admin_token'
const PROFILE_KEY = 'xc_admin_profile'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

export function getStoredProfile<T>() {
  const raw = localStorage.getItem(PROFILE_KEY)
  return raw ? (JSON.parse(raw) as T) : null
}

export function setStoredProfile<T>(profile: T) {
  localStorage.setItem(PROFILE_KEY, JSON.stringify(profile))
}

export function clearStoredProfile() {
  localStorage.removeItem(PROFILE_KEY)
}
