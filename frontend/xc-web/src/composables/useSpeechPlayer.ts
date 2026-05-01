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
  const paused = ref(false)
  const currentTime = ref(0)
  const duration = ref(0)
  const seekable = ref(false)
  let activeAudio: HTMLAudioElement | null = null
  let activeAudioUrl = ''
  let activeUtterance: SpeechSynthesisUtterance | null = null
  let requestId = 0

  function resetProgress() {
    currentTime.value = 0
    duration.value = 0
    seekable.value = false
  }

  function syncAudioProgress(audio: HTMLAudioElement) {
    currentTime.value = audio.currentTime || 0
    duration.value = Number.isFinite(audio.duration) ? audio.duration : 0
    seekable.value = duration.value > 0
  }

  function stop() {
    requestId += 1
    if (activeAudio) {
      activeAudio.pause()
      activeAudio.src = ''
      activeAudio = null
    }
    activeAudioUrl = ''
    if ('speechSynthesis' in window) {
      window.speechSynthesis.cancel()
    }
    activeUtterance = null
    speaking.value = false
    paused.value = false
    resetProgress()
  }

  function pause() {
    if (activeAudio) {
      activeAudio.pause()
      syncAudioProgress(activeAudio)
      speaking.value = false
      paused.value = true
      return
    }
    if ('speechSynthesis' in window && activeUtterance) {
      window.speechSynthesis.pause()
      speaking.value = false
      paused.value = true
    }
  }

  function resume() {
    if (activeAudio) {
      speaking.value = true
      paused.value = false
      void activeAudio.play().catch(() => {
        if (activeAudio) {
          speaking.value = false
          paused.value = true
        }
      })
      return
    }
    if ('speechSynthesis' in window && activeUtterance) {
      window.speechSynthesis.resume()
      speaking.value = true
      paused.value = false
    }
  }

  function playAudioUrl(url: string) {
    if (activeAudio && activeAudioUrl === url) {
      resume()
      return
    }
    stop()
    const currentRequest = ++requestId
    const audio = new Audio(url)
    activeAudio = audio
    activeAudioUrl = url
    speaking.value = true
    paused.value = false
    seekable.value = true
    audio.addEventListener('loadedmetadata', () => {
      if (activeAudio === audio && currentRequest === requestId) {
        syncAudioProgress(audio)
      }
    })
    audio.addEventListener('durationchange', () => {
      if (activeAudio === audio && currentRequest === requestId) {
        syncAudioProgress(audio)
      }
    })
    audio.addEventListener('timeupdate', () => {
      if (activeAudio === audio && currentRequest === requestId) {
        syncAudioProgress(audio)
      }
    })
    audio.addEventListener('play', () => {
      if (activeAudio === audio && currentRequest === requestId) {
        speaking.value = true
        paused.value = false
      }
    })
    audio.addEventListener('pause', () => {
      if (activeAudio === audio && currentRequest === requestId && !audio.ended) {
        syncAudioProgress(audio)
        speaking.value = false
        paused.value = true
      }
    })
    audio.addEventListener('ended', () => {
      if (activeAudio === audio && currentRequest === requestId) {
        syncAudioProgress(audio)
        activeAudio = null
        activeAudioUrl = ''
        speaking.value = false
        paused.value = false
      }
    })
    audio.addEventListener('error', () => {
      if (activeAudio === audio && currentRequest === requestId) {
        activeAudio = null
        activeAudioUrl = ''
        speaking.value = false
        paused.value = false
        resetProgress()
      }
    })
    void audio.play().catch(() => {
      if (activeAudio === audio && currentRequest === requestId) {
        activeAudio = null
        activeAudioUrl = ''
        speaking.value = false
        paused.value = false
        resetProgress()
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
    seekable.value = false
    duration.value = 0
    currentTime.value = 0
    utterance.onend = () => {
      if (currentRequest === requestId) {
        activeUtterance = null
        speaking.value = false
        paused.value = false
      }
    }
    utterance.onerror = () => {
      if (currentRequest === requestId) {
        activeUtterance = null
        speaking.value = false
        paused.value = false
      }
    }
    activeUtterance = utterance
    speaking.value = true
    paused.value = false
    window.speechSynthesis.speak(utterance)
  }

  function seek(seconds: number, playAfterSeek = speaking.value) {
    if (!activeAudio || !seekable.value) {
      return
    }
    const nextTime = Math.min(Math.max(seconds, 0), duration.value || activeAudio.duration || 0)
    activeAudio.currentTime = nextTime
    syncAudioProgress(activeAudio)
    if (playAfterSeek) {
      resume()
    }
  }

  function seekByFraction(fraction: number, playAfterSeek = speaking.value) {
    if (!duration.value) {
      return
    }
    seek(duration.value * Math.min(Math.max(fraction, 0), 1), playAfterSeek)
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
    paused,
    currentTime,
    duration,
    seekable,
    stop,
    pause,
    resume,
    seek,
    seekByFraction,
    playAudioUrl,
    speakText,
    playTargetText
  }
}
