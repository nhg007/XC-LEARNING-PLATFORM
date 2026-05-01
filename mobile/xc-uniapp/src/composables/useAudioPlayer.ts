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
  const paused = ref(false)
  const currentTime = ref(0)
  const duration = ref(0)
  const seekable = ref(false)
  let audio: UniApp.InnerAudioContext | null = null
  let webAudio: HTMLAudioElement | null = null
  let webUtterance: SpeechSynthesisUtterance | null = null
  let activeSource = ''
  let requestId = 0

  function resetProgress() {
    currentTime.value = 0
    duration.value = 0
    seekable.value = false
  }

  function syncInnerAudioProgress(player: UniApp.InnerAudioContext) {
    currentTime.value = player.currentTime || 0
    duration.value = Number.isFinite(player.duration) ? player.duration : 0
    seekable.value = duration.value > 0
  }

  function syncWebAudioProgress(player: HTMLAudioElement) {
    currentTime.value = player.currentTime || 0
    duration.value = Number.isFinite(player.duration) ? player.duration : 0
    seekable.value = duration.value > 0
  }

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
    activeSource = ''
    if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
      window.speechSynthesis.cancel()
    }
    webUtterance = null
    playing.value = false
    paused.value = false
    resetProgress()
  }

  function pause() {
    if (audio) {
      audio.pause()
      syncInnerAudioProgress(audio)
      playing.value = false
      paused.value = true
      return
    }
    if (webAudio) {
      webAudio.pause()
      syncWebAudioProgress(webAudio)
      playing.value = false
      paused.value = true
      return
    }
    if (typeof window !== 'undefined' && 'speechSynthesis' in window && webUtterance) {
      window.speechSynthesis.pause()
      playing.value = false
      paused.value = true
    }
  }

  function resume() {
    if (audio) {
      audio.play()
      playing.value = true
      paused.value = false
      return
    }
    if (webAudio) {
      playing.value = true
      paused.value = false
      void webAudio.play().catch(() => {
        playing.value = false
        paused.value = true
      })
      return
    }
    if (typeof window !== 'undefined' && 'speechSynthesis' in window && webUtterance) {
      window.speechSynthesis.resume()
      playing.value = true
      paused.value = false
    }
  }

  function play(url?: string | null, fallbackText?: string | null, fallbackLanguage: SpeechLanguage = 'zh') {
    const source = url?.trim()
    if (!source) {
      speakFallback(fallbackText, fallbackLanguage)
      return
    }
    const normalizedSource = canUseWebAudio() ? toAbsoluteUrl(source) : source
    if (activeSource === normalizedSource && (audio || webAudio)) {
      resume()
      return
    }
    stop()
    if (canUseWebAudio()) {
      playWithWebAudio(normalizedSource, fallbackText, fallbackLanguage)
      return
    }
    playWithInnerAudio(normalizedSource)
  }

  function playWithInnerAudio(source: string) {
    const player = uni.createInnerAudioContext()
    audio = player
    activeSource = source
    player.src = source
    player.onPlay(() => {
      playing.value = true
      paused.value = false
      syncInnerAudioProgress(player)
    })
    player.onCanplay(() => {
      if (audio === player) {
        syncInnerAudioProgress(player)
      }
    })
    player.onTimeUpdate(() => {
      if (audio === player) {
        syncInnerAudioProgress(player)
      }
    })
    player.onEnded(() => {
      if (audio === player) {
        syncInnerAudioProgress(player)
        audio = null
        activeSource = ''
        playing.value = false
        paused.value = false
        player.destroy()
      }
    })
    player.onError(() => {
      if (audio === player) {
        audio = null
        activeSource = ''
        playing.value = false
        paused.value = false
        resetProgress()
        player.destroy()
      }
      void uni.showToast({ icon: 'none', title: t('common.audioFailed') })
    })
    player.play()
  }

  function playWithWebAudio(source: string, fallbackText?: string | null, fallbackLanguage: SpeechLanguage = 'zh') {
    const currentRequest = ++requestId
    const player = new Audio(source)
    let finished = false
    webAudio = player
    activeSource = source
    playing.value = true
    paused.value = false
    seekable.value = true

    const finish = () => {
      if (finished || webAudio !== player || currentRequest !== requestId) {
        return
      }
      finished = true
      syncWebAudioProgress(player)
      webAudio = null
      activeSource = ''
      playing.value = false
      paused.value = false
    }
    const fail = () => {
      if (finished || webAudio !== player || currentRequest !== requestId) {
        return
      }
      finished = true
      webAudio = null
      activeSource = ''
      playing.value = false
      paused.value = false
      resetProgress()
      if (!speakFallback(fallbackText, fallbackLanguage)) {
        void uni.showToast({ icon: 'none', title: t('common.audioFailed') })
      }
    }

    player.addEventListener('loadedmetadata', () => syncWebAudioProgress(player))
    player.addEventListener('durationchange', () => syncWebAudioProgress(player))
    player.addEventListener('timeupdate', () => syncWebAudioProgress(player))
    player.addEventListener('play', () => {
      if (webAudio === player && currentRequest === requestId) {
        playing.value = true
        paused.value = false
      }
    })
    player.addEventListener('pause', () => {
      if (webAudio === player && currentRequest === requestId && !player.ended) {
        syncWebAudioProgress(player)
        playing.value = false
        paused.value = true
      }
    })
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
    seekable.value = false
    currentTime.value = 0
    duration.value = 0
    utterance.onend = () => {
      if (currentRequest === requestId) {
        webUtterance = null
        playing.value = false
        paused.value = false
      }
    }
    utterance.onerror = () => {
      if (currentRequest === requestId) {
        webUtterance = null
        playing.value = false
        paused.value = false
        void uni.showToast({ icon: 'none', title: t('common.audioFailed') })
      }
    }
    webUtterance = utterance
    playing.value = true
    paused.value = false
    window.speechSynthesis.speak(utterance)
    return true
  }

  function seek(seconds: number, playAfterSeek = playing.value) {
    const nextTime = Math.min(Math.max(seconds, 0), duration.value)
    if (audio && seekable.value) {
      audio.seek(nextTime)
      currentTime.value = nextTime
      if (playAfterSeek) {
        resume()
      }
      return
    }
    if (webAudio && seekable.value) {
      webAudio.currentTime = nextTime
      syncWebAudioProgress(webAudio)
      if (playAfterSeek) {
        resume()
      }
    }
  }

  function seekByFraction(fraction: number, playAfterSeek = playing.value) {
    if (!duration.value) {
      return
    }
    seek(duration.value * Math.min(Math.max(fraction, 0), 1), playAfterSeek)
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
    paused,
    currentTime,
    duration,
    seekable,
    play,
    pause,
    resume,
    seek,
    seekByFraction,
    stop
  }
}
