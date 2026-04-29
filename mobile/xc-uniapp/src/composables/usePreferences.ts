import { ref } from 'vue'
import { fetchUserPreference, updateUserPreference } from '../api/preferences'
import { setLocale, type MobileLocale } from '../i18n'
import type { UpdateUserPreferencePayload, UserPreference } from '../types/api'

const preference = ref<UserPreference | null>(null)
const loaded = ref(false)
const loading = ref(false)
const saving = ref(false)

export function uiLanguageToLocale(value: UserPreference['uiLanguage']): MobileLocale {
  return value === 'zh' ? 'zh-CN' : value
}

export function localeToUiLanguage(value: MobileLocale): UserPreference['uiLanguage'] {
  return value === 'zh-CN' ? 'zh' : value
}

export function usePreferences() {
  async function loadPreference(force = false) {
    if (!force && loaded.value && preference.value) {
      return preference.value
    }
    if (loading.value && preference.value) {
      return preference.value
    }
    loading.value = true
    try {
      preference.value = await fetchUserPreference()
      loaded.value = true
      setLocale(uiLanguageToLocale(preference.value.uiLanguage))
      return preference.value
    } finally {
      loading.value = false
    }
  }

  async function savePreference(payload: UpdateUserPreferencePayload) {
    saving.value = true
    try {
      preference.value = await updateUserPreference(payload)
      loaded.value = true
      if (payload.uiLanguage) {
        setLocale(uiLanguageToLocale(preference.value.uiLanguage))
      }
      return preference.value
    } finally {
      saving.value = false
    }
  }

  function resetPreference() {
    preference.value = null
    loaded.value = false
    loading.value = false
    saving.value = false
  }

  return {
    preference,
    loaded,
    loading,
    saving,
    loadPreference,
    savePreference,
    resetPreference
  }
}
