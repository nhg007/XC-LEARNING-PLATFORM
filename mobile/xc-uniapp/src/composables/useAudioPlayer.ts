import { onUnmounted, ref } from 'vue'
import { t } from '../i18n'

export function useAudioPlayer() {
  const playing = ref(false)
  let audio: UniApp.InnerAudioContext | null = null

  function stop() {
    if (audio) {
      audio.stop()
      audio.destroy()
      audio = null
    }
    playing.value = false
  }

  function play(url?: string | null) {
    const source = url?.trim()
    if (!source) {
      void uni.showToast({ icon: 'none', title: t('common.noAudio') })
      return
    }
    stop()
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

  onUnmounted(stop)

  return {
    playing,
    play,
    stop
  }
}
