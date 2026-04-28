export type LearningLanguage = 'zh' | 'ru' | 'en'
export type SupportedMeaningLanguage = 'ru' | 'en'

const learningLanguages: LearningLanguage[] = ['zh', 'ru', 'en']
const supportedMeaningLanguages: SupportedMeaningLanguage[] = ['ru', 'en']

function normalizeLanguage(value: string | undefined, fallback: LearningLanguage): LearningLanguage {
  return learningLanguages.includes(value as LearningLanguage) ? value as LearningLanguage : fallback
}

function normalizeMeaningLanguages(value: string | undefined, fallback: SupportedMeaningLanguage[]): SupportedMeaningLanguage[] {
  const items = (value || '')
    .split(',')
    .map(item => item.trim())
    .filter((item): item is SupportedMeaningLanguage => supportedMeaningLanguages.includes(item as SupportedMeaningLanguage))
  return items.length > 0 ? items : fallback
}

export const learningProfile = {
  sourceLanguage: normalizeLanguage(import.meta.env.VITE_LEARNING_SOURCE_LANGUAGE, 'ru'),
  targetLanguage: normalizeLanguage(import.meta.env.VITE_LEARNING_TARGET_LANGUAGE, 'zh'),
  meaningLanguages: normalizeMeaningLanguages(import.meta.env.VITE_LEARNING_MEANING_LANGUAGES, ['ru', 'en'])
}

export function targetHasPronunciationGuide() {
  return learningProfile.targetLanguage === 'zh'
}
