import { defineStore } from 'pinia'
import { fetchCurrentAdmin, login, type LoginPayload } from '../api/auth'
import type { AdminProfile } from '../types/api'
import { clearStoredProfile, clearToken, getStoredProfile, getToken, setStoredProfile, setToken } from '../utils/storage'

export const useSessionStore = defineStore('session', {
  state: () => ({
    token: getToken(),
    profile: getStoredProfile<AdminProfile>()
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
    async refreshProfile() {
      if (!this.token) {
        return
      }
      this.profile = await fetchCurrentAdmin()
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
