import { createI18n } from 'vue-i18n'
import en from '@/locales/en'
import ru from '@/locales/ru'
import zhCN from '@/locales/zh-CN'

export type StudentLocale = 'zh-CN' | 'en' | 'ru'

const LOCALE_KEY = 'xc_web_locale'

function normalizeLocale(value: string | null): StudentLocale {
  if (value === 'en' || value === 'ru') {
    return value
  }
  return 'zh-CN'
}

export function getStoredLocale(): StudentLocale {
  return normalizeLocale(localStorage.getItem(LOCALE_KEY))
}

export const i18n = createI18n({
  legacy: false,
  locale: getStoredLocale(),
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': zhCN,
    en,
    ru
  }
})

export function setLocale(locale: StudentLocale) {
  i18n.global.locale.value = locale
  localStorage.setItem(LOCALE_KEY, locale)
  document.documentElement.lang = locale
}

export function t(key: string, params?: Record<string, unknown>) {
  return i18n.global.t(key, params || {})
}
