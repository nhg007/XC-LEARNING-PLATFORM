import { ref } from 'vue'
import { t } from '../i18n'

interface RecorderManagerLike {
  onStop(callback: (result: { tempFilePath: string }) => void): void
  onError(callback: () => void): void
  start(options?: Record<string, unknown>): void
  stop(): void
}

type UniWithRecorder = typeof uni & {
  getRecorderManager?: () => RecorderManagerLike
}

export function useRecorder() {
  const recording = ref(false)
  const recordUrl = ref('')
  const manager = (uni as UniWithRecorder).getRecorderManager?.()

  if (manager) {
    manager.onStop((result) => {
      recordUrl.value = result.tempFilePath
      recording.value = false
    })
    manager.onError(() => {
      recording.value = false
      void uni.showToast({ icon: 'none', title: t('common.recordFailed') })
    })
  }

  function start() {
    if (!manager) {
      void uni.showToast({ icon: 'none', title: t('common.recordUnsupported') })
      return
    }
    recordUrl.value = ''
    recording.value = true
    manager.start({
      duration: 60000,
      sampleRate: 16000,
      numberOfChannels: 1,
      encodeBitRate: 48000,
      format: 'mp3'
    })
  }

  function stop() {
    if (!manager || !recording.value) {
      return
    }
    manager.stop()
  }

  function clear() {
    if (manager && recording.value) {
      manager.stop()
    }
    recordUrl.value = ''
    recording.value = false
  }

  return {
    recording,
    recordUrl,
    start,
    stop,
    clear
  }
}
