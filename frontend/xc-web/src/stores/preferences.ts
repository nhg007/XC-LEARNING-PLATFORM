import { defineStore } from 'pinia'
import { fetchUserPreference, updateUserPreference } from '../api/preferences'
import { setLocale, type StudentLocale } from '../plugins/i18n'
import type { UpdateUserPreferencePayload, UserPreference } from '../types/api'

export function uiLanguageToLocale(value: UserPreference['uiLanguage']): StudentLocale {
  return value === 'zh' ? 'zh-CN' : value
}

export function localeToUiLanguage(value: StudentLocale): UserPreference['uiLanguage'] {
  return value === 'zh-CN' ? 'zh' : value
}

export const usePreferenceStore = defineStore('preferences', {
  state: () => ({
    preference: null as UserPreference | null,
    loaded: false,
    loading: false
  }),
  actions: {
    async load() {
      if (this.loaded || this.loading) {
        return this.preference
      }
      this.loading = true
      try {
        this.preference = await fetchUserPreference()
        this.loaded = true
        setLocale(uiLanguageToLocale(this.preference.uiLanguage))
        return this.preference
      } finally {
        this.loading = false
      }
    },
    async save(payload: UpdateUserPreferencePayload) {
      this.preference = await updateUserPreference(payload)
      this.loaded = true
      if (payload.uiLanguage && this.preference) {
        setLocale(uiLanguageToLocale(this.preference.uiLanguage))
      }
      return this.preference
    },
    reset() {
      this.preference = null
      this.loaded = false
      this.loading = false
    }
  }
})
