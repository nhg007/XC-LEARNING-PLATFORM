import { createI18n } from 'vue-i18n'
import en from '@/locales/en'
import ru from '@/locales/ru'
import zhCN from '@/locales/zh-CN'

export type AdminLocale = 'zh-CN' | 'en' | 'ru'

const LOCALE_KEY = 'xc_admin_locale'

export function getStoredLocale(): AdminLocale {
  const value = localStorage.getItem(LOCALE_KEY)
  if (value === 'en' || value === 'ru') {
    return value
  }
  return value === 'en' ? 'en' : 'zh-CN'
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

export function setLocale(locale: AdminLocale) {
  i18n.global.locale.value = locale
  localStorage.setItem(LOCALE_KEY, locale)
}

export function t(key: string) {
  return i18n.global.t(key)
}
