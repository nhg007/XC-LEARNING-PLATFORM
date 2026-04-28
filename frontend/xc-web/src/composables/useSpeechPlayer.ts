import { ref } from 'vue'
import { learningProfile, type LearningLanguage } from '@/config/learningProfile'

const speechLangMap: Record<LearningLanguage, string> = {
  zh: 'zh-CN',
  ru: 'ru-RU',
  en: 'en-US'
}

const voiceNameHints: Record<LearningLanguage, string[]> = {
  zh: ['chinese', 'mandarin', 'zhongwen', '中文', '普通话'],
  ru: ['russian', 'русский'],
  en: ['english']
}

function getAvailableVoices() {
  if (!('speechSynthesis' in window)) {
    return []
  }
  return window.speechSynthesis.getVoices()
}

function normalizeLang(value: string) {
  return value.toLowerCase().replace('_', '-')
}

function pickVoice(language: LearningLanguage, voices: SpeechSynthesisVoice[]) {
  const expected = normalizeLang(speechLangMap[language])
  const prefix = expected.split('-')[0]
  const hints = voiceNameHints[language]
  return voices.find(voice => normalizeLang(voice.lang) === expected)
    || voices.find(voice => normalizeLang(voice.lang).startsWith(`${prefix}-`))
    || voices.find(voice => hints.some(hint => voice.name.toLowerCase().includes(hint)))
    || null
}

export function useSpeechPlayer() {
  const speaking = ref(false)
  let activeAudio: HTMLAudioElement | null = null
  let activeUtterance: SpeechSynthesisUtterance | null = null
  let requestId = 0

  function stop() {
    requestId += 1
    if (activeAudio) {
      activeAudio.pause()
      activeAudio.src = ''
      activeAudio = null
    }
    if ('speechSynthesis' in window) {
      window.speechSynthesis.cancel()
    }
    activeUtterance = null
    speaking.value = false
  }

  function playAudioUrl(url: string) {
    stop()
    const currentRequest = ++requestId
    const audio = new Audio(url)
    activeAudio = audio
    speaking.value = true
    audio.addEventListener('ended', () => {
      if (activeAudio === audio && currentRequest === requestId) {
        activeAudio = null
        speaking.value = false
      }
    })
    audio.addEventListener('error', () => {
      if (activeAudio === audio && currentRequest === requestId) {
        activeAudio = null
        speaking.value = false
      }
    })
    void audio.play().catch(() => {
      if (activeAudio === audio && currentRequest === requestId) {
        activeAudio = null
        speaking.value = false
      }
    })
  }

  function speakText(text: string, language: LearningLanguage = learningProfile.targetLanguage) {
    const content = text.trim()
    if (!content || !('speechSynthesis' in window)) {
      return
    }
    stop()
    const currentRequest = ++requestId
    const utterance = new SpeechSynthesisUtterance(content)
    const voice = pickVoice(language, getAvailableVoices())
    utterance.lang = voice?.lang || speechLangMap[language]
    utterance.voice = voice
    utterance.rate = language === 'zh' ? 0.88 : 0.95
    utterance.onend = () => {
      if (currentRequest === requestId) {
        activeUtterance = null
        speaking.value = false
      }
    }
    utterance.onerror = () => {
      if (currentRequest === requestId) {
        activeUtterance = null
        speaking.value = false
      }
    }
    activeUtterance = utterance
    speaking.value = true
    window.speechSynthesis.speak(utterance)
  }

  function playTargetText(text: string, audioUrl?: string | null) {
    if (audioUrl) {
      playAudioUrl(audioUrl)
      return
    }
    speakText(text, learningProfile.targetLanguage)
  }

  return {
    speaking,
    stop,
    playAudioUrl,
    speakText,
    playTargetText
  }
}
