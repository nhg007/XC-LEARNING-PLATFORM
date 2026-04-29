import { onUnmounted, ref } from 'vue'
import { t } from '../i18n'

type SpeechLanguage = 'zh' | 'ru' | 'en'

const speechLangMap: Record<SpeechLanguage, string> = {
  zh: 'zh-CN',
  ru: 'ru-RU',
  en: 'en-US'
}

export function useAudioPlayer() {
  const playing = ref(false)
  let audio: UniApp.InnerAudioContext | null = null
  let webAudio: HTMLAudioElement | null = null
  let webUtterance: SpeechSynthesisUtterance | null = null
  let requestId = 0

  function stop() {
    requestId += 1
    if (audio) {
      audio.stop()
      audio.destroy()
      audio = null
    }
    if (webAudio) {
      webAudio.pause()
      webAudio.src = ''
      webAudio = null
    }
    if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
      window.speechSynthesis.cancel()
    }
    webUtterance = null
    playing.value = false
  }

  function play(url?: string | null, fallbackText?: string | null, fallbackLanguage: SpeechLanguage = 'zh') {
    const source = url?.trim()
    if (!source) {
      speakFallback(fallbackText, fallbackLanguage)
      return
    }
    stop()
    if (canUseWebAudio()) {
      playWithWebAudio(source, fallbackText, fallbackLanguage)
      return
    }
    playWithInnerAudio(source)
  }

  function playWithInnerAudio(source: string) {
    const player = uni.createInnerAudioContext()
    audio = player
    player.src = source
    player.onPlay(() => {
      playing.value = true
    })
    player.onEnded(() => {
      if (audio === player) {
        audio = null
        playing.value = false
        player.destroy()
      }
    })
    player.onError(() => {
      if (audio === player) {
        audio = null
        playing.value = false
        player.destroy()
      }
      void uni.showToast({ icon: 'none', title: t('common.audioFailed') })
    })
    player.play()
  }

  function playWithWebAudio(source: string, fallbackText?: string | null, fallbackLanguage: SpeechLanguage = 'zh') {
    const currentRequest = ++requestId
    const player = new Audio(toAbsoluteUrl(source))
    let finished = false
    webAudio = player
    playing.value = true

    const finish = () => {
      if (finished || webAudio !== player || currentRequest !== requestId) {
        return
      }
      finished = true
      webAudio = null
      playing.value = false
    }
    const fail = () => {
      if (finished || webAudio !== player || currentRequest !== requestId) {
        return
      }
      finished = true
      webAudio = null
      playing.value = false
      if (!speakFallback(fallbackText, fallbackLanguage)) {
        void uni.showToast({ icon: 'none', title: t('common.audioFailed') })
      }
    }

    player.addEventListener('ended', finish)
    player.addEventListener('error', fail)
    void player.play().catch(fail)
  }

  function speakFallback(text?: string | null, language: SpeechLanguage = 'zh') {
    const content = text?.trim()
    if (!content) {
      void uni.showToast({ icon: 'none', title: t('common.noAudio') })
      return false
    }
    if (typeof window === 'undefined' || !('speechSynthesis' in window)) {
      void uni.showToast({ icon: 'none', title: t('common.audioFallbackUnsupported') })
      return false
    }
    stop()
    const currentRequest = ++requestId
    const utterance = new SpeechSynthesisUtterance(content)
    utterance.lang = speechLangMap[language]
    utterance.rate = language === 'zh' ? 0.88 : 0.95
    utterance.onend = () => {
      if (currentRequest === requestId) {
        webUtterance = null
        playing.value = false
      }
    }
    utterance.onerror = () => {
      if (currentRequest === requestId) {
        webUtterance = null
        playing.value = false
        void uni.showToast({ icon: 'none', title: t('common.audioFailed') })
      }
    }
    webUtterance = utterance
    playing.value = true
    window.speechSynthesis.speak(utterance)
    return true
  }

  function canUseWebAudio() {
    return typeof Audio !== 'undefined'
  }

  function toAbsoluteUrl(source: string) {
    if (typeof window === 'undefined') {
      return source
    }
    return new URL(source, window.location.href).href
  }

  onUnmounted(stop)

  return {
    playing,
    play,
    stop
  }
}
