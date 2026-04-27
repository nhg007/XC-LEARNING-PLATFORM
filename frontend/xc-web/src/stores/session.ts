import { defineStore } from 'pinia'
import { fetchCurrentUser, login, register, type LoginPayload, type RegisterPayload } from '../api/auth'
import type { UserProfile } from '../types/api'
import { clearStoredProfile, clearToken, getStoredProfile, getToken, setStoredProfile, setToken } from '../utils/storage'

export const useSessionStore = defineStore('session', {
  state: () => ({
    token: getToken(),
    profile: getStoredProfile<UserProfile>()
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token)
  },
  actions: {
    async login(payload: LoginPayload) {
      const data = await login(payload)
      this.token = data.accessToken
      this.profile = data.profile
      setToken(data.accessToken)
      setStoredProfile(data.profile)
    },
    async register(payload: RegisterPayload) {
      const data = await register(payload)
      this.token = data.accessToken
      this.profile = data.profile
      setToken(data.accessToken)
      setStoredProfile(data.profile)
    },
    async refreshProfile() {
      if (!this.token) {
        return
      }
      this.profile = await fetchCurrentUser()
      setStoredProfile(this.profile)
    },
    logout() {
      this.token = null
      this.profile = null
      clearToken()
      clearStoredProfile()
    }
  }
})
