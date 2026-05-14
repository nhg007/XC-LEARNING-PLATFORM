/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_LEARNING_SOURCE_LANGUAGE?: 'zh' | 'ru' | 'en'
  readonly VITE_LEARNING_TARGET_LANGUAGE?: 'zh' | 'ru' | 'en'
  readonly VITE_LEARNING_MEANING_LANGUAGES?: string
  readonly VITE_ENABLE_MEMBERSHIP_PURCHASE?: string
}
