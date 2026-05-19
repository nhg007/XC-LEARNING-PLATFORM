<template>
  <view class="page">
    <view class="hero">
      <view class="hero-top">
        <view class="hero-copy">
          <text class="eyebrow">{{ t('app.title') }}</text>
          <text class="title">{{ t('dialogue.title') }}</text>
          <text class="subtitle">{{ activeMaterial ? activeMaterial.title : t('dialogue.subtitle') }}</text>
        </view>
        <LanguageSwitch variant="hero" />
      </view>
    </view>

    <view v-if="checkingAccess" class="state-card">{{ t('dialogue.checkingAccess') }}</view>

    <view v-else-if="accessError" class="state-card error-card">
      <text>{{ accessError }}</text>
      <button class="plain-btn compact" size="mini" @click="prepareDialogue()">{{ t('common.retry') }}</button>
    </view>

    <view v-else-if="locked" class="locked-card">
      <text class="locked-title">{{ t('practice.lockedTitle') }}</text>
      <text class="muted">{{ t('dialogue.lockedDesc') }}</text>
      <button class="primary-btn full" @click="openMembership">{{ t('dialogue.openMembership') }}</button>
      <button class="plain-btn full" @click="goHome">{{ t('common.backHome') }}</button>
    </view>

    <template v-else-if="!activeMaterial">
      <view class="filter-row">
        <button class="filter-btn" :class="{ active: materialType === '' }" size="mini" @click="setMaterialType('')">{{ t('dialogue.all') }}</button>
        <button class="filter-btn" :class="{ active: materialType === 'drama' }" size="mini" @click="setMaterialType('drama')">{{ t('dialogue.drama') }}</button>
        <button class="filter-btn" :class="{ active: materialType === 'short_video' }" size="mini" @click="setMaterialType('short_video')">{{ t('dialogue.shortVideo') }}</button>
        <button class="filter-btn" :class="{ active: materialType === 'cartoon' }" size="mini" @click="setMaterialType('cartoon')">{{ t('dialogue.cartoon') }}</button>
      </view>

      <view v-if="loading && materials.length === 0" class="state-card">{{ t('common.loading') }}</view>
      <view v-else-if="materialError" class="state-card error-card">
        <text>{{ materialError }}</text>
        <button class="plain-btn compact" size="mini" @click="loadMaterials()">{{ t('common.retry') }}</button>
      </view>
      <view v-else-if="materials.length === 0" class="state-card">{{ t('dialogue.emptyMaterials') }}</view>
      <view v-else class="material-list">
        <view class="overview-card">
          <view>
            <text class="overview-label">{{ t('dialogue.subtitle') }}</text>
            <text class="overview-title">{{ t('dialogue.materialCount', { count: materials.length }) }}</text>
          </view>
          <text class="overview-pill">{{ t('feature.dialogue') }}</text>
        </view>
        <view v-for="item in materials" :key="item.id" class="material-card" @click="selectMaterial(item)">
          <image v-if="item.coverUrl" class="cover" mode="aspectFill" :src="mediaUrl(item.coverUrl)" />
          <view v-else class="cover placeholder">{{ t('dialogue.coverText') }}</view>
          <view class="material-body">
            <text class="tag">{{ materialTypeLabel(item.materialType) }}</text>
            <text class="material-title">{{ item.title }}</text>
            <text class="muted one-line">{{ item.description || t('dialogue.noDescription') }}</text>
            <text class="muted">{{ t('dialogue.lineCount', { count: item.totalLineCount || item.lineCount }) }}</text>
          </view>
        </view>
      </view>
    </template>

    <template v-else>
      <view class="material-head">
        <view>
          <text class="tag">{{ materialTypeLabel(activeMaterial.materialType) }}</text>
          <text class="material-title">{{ activeMaterial.title }}</text>
        </view>
        <button class="plain-btn" size="mini" @click="changeMaterial">{{ t('dialogue.changeMaterial') }}</button>
      </view>

      <view v-if="childMaterials.length > 0" class="filter-row">
        <button class="filter-btn" :class="{ active: scopeTab === 'all' }" size="mini" @click="scopeTab = 'all'">{{ t('dialogue.scopeAll') }}</button>
        <button class="filter-btn" :class="{ active: scopeTab === 'lessons' }" size="mini" @click="scopeTab = 'lessons'">{{ t('dialogue.scopeLessons') }}</button>
      </view>

      <view v-if="scopeTab === 'lessons' && childMaterials.length > 0" class="material-list">
        <view v-for="item in childMaterials" :key="item.id" class="material-card" @click="selectMaterial(item)">
          <image v-if="item.coverUrl" class="cover" mode="aspectFill" :src="mediaUrl(item.coverUrl)" />
          <view v-else class="cover placeholder">{{ t('dialogue.coverText') }}</view>
          <view class="material-body">
            <text class="tag">{{ materialTypeLabel(item.materialType) }}</text>
            <text class="material-title">{{ item.title }}</text>
            <text class="muted one-line">{{ item.description || t('dialogue.noDescription') }}</text>
            <text class="muted">{{ t('dialogue.lineCount', { count: item.totalLineCount || item.lineCount }) }}</text>
          </view>
        </view>
      </view>

      <template v-else>
        <view v-if="loading" class="state-card">{{ t('dialogue.loadingLines') }}</view>
        <view v-else-if="lineError" class="state-card error-card">
          <text>{{ lineError }}</text>
          <button class="plain-btn compact" size="mini" @click="reloadActiveMaterial">{{ t('common.retry') }}</button>
        </view>
        <view v-else-if="!currentLine" class="state-card">{{ t('dialogue.emptyLines') }}</view>
        <view v-else class="line-panel">
        <view class="line-meta">
          <text>{{ lineIndex + 1 }} / {{ lines.length }}</text>
          <view class="language-row">
            <button class="small-button" :class="{ active: meaningLanguage === 'ru' }" size="mini" @click="meaningLanguage = 'ru'">{{ t('common.ru') }}</button>
            <button class="small-button" :class="{ active: meaningLanguage === 'en' }" size="mini" @click="meaningLanguage = 'en'">{{ t('common.en') }}</button>
          </view>
        </view>

        <view class="translation-card">
          <text class="muted">{{ t('dialogue.referenceTranslation') }}</text>
          <text class="translation">{{ translationText || t('dialogue.noTranslation') }}</text>
        </view>

        <view class="audio-action-row">
          <button class="plain-btn" :loading="originalAudioPlaying" @click="playLineAudio">
            {{ originalAudioPlaying ? t('common.playing') : t('dialogue.playOriginal') }}
          </button>
          <button class="plain-btn" :loading="textAudioPlaying" @click="readLineText">
            {{ textAudioPlaying ? t('common.playing') : t('dialogue.readText') }}
          </button>
        </view>

        <view class="mode-row">
          <button class="mode-btn" :class="{ active: mode === 'order' }" size="mini" @click="setMode('order')">{{ t('dialogue.order') }}</button>
          <button class="mode-btn" :class="{ active: mode === 'dictation' }" size="mini" @click="setMode('dictation')">{{ t('dialogue.dictation') }}</button>
          <button class="mode-btn" :class="{ active: mode === 'shadow' }" size="mini" @click="setMode('shadow')">{{ t('dialogue.shadow') }}</button>
        </view>

        <view v-if="mode === 'order'" class="word-area">
          <view class="selected">
            <button
              v-for="(word, index) in selectedWords"
              :key="`${word}-${index}`"
              class="word selected-word"
              size="mini"
              @click="removeWord(index)"
            >
              {{ word }}
            </button>
            <text v-if="selectedWords.length === 0" class="muted">{{ t('dialogue.chooseWords') }}</text>
          </view>
          <view class="options">
            <button v-for="(word, index) in availableWords" :key="`${word}-${index}`" class="word" size="mini" @click="selectWord(word)">
              {{ word }}
            </button>
          </view>
        </view>

        <textarea
          v-else-if="mode === 'dictation'"
          v-model="answerText"
          class="answer-input"
          maxlength="4000"
          :placeholder="t('dialogue.inputDictation')"
        />

        <view v-else class="shadow-card">
          <text class="pinyin">{{ currentLine.pinyinText || '-' }}</text>
          <text class="hanzi">{{ currentLine.hanziText }}</text>
          <view class="record-row">
            <button v-if="!recorderRecording" class="plain-btn" size="mini" @click="recorder.start">{{ t('dialogue.startRecording') }}</button>
            <button v-else class="plain-btn danger-btn" size="mini" @click="recorder.stop">{{ t('dialogue.stopRecording') }}</button>
            <button class="plain-btn" size="mini" :disabled="!hasRecording" @click="playRecording">{{ t('dialogue.playRecording') }}</button>
          </view>
          <text class="muted">{{ t('dialogue.recordHint') }}</text>
        </view>

        <view v-if="speechRecord" class="speech-card">
          <view class="speech-head">
            <text class="analysis-title">{{ t('dialogue.speechTitle') }}</text>
            <text :class="['status-pill', speechStatusClass(speechRecord.asrStatus)]">{{ speechStatusLabel(speechRecord.asrStatus) }}</text>
          </view>
          <view v-if="speechRecord.score !== null" class="score-row">
            <view>
              <text class="muted">{{ t('dialogue.score') }}</text>
              <text class="score">{{ speechScoreLabel }}</text>
            </view>
            <view class="score-bar">
              <view class="score-fill" :style="{ width: speechScoreWidth }" />
            </view>
          </view>
          <text v-if="speechRecord.recognizedText" class="standard">{{ t('dialogue.recognizedText', { text: speechRecord.recognizedText }) }}</text>
          <text v-if="speechCompareResult" class="muted">
            {{ t('dialogue.standardLine', { text: speechCompareResult.standardText || currentLine?.hanziText || '-' }) }}
          </text>
          <text v-if="speechMismatchIndex !== null" class="muted">
            {{ t('dialogue.firstMismatch', { index: speechMismatchIndex + 1 }) }}
          </text>
          <text v-if="speechRecord.errorMessage" class="muted">{{ t('dialogue.errorReason', { message: speechRecord.errorMessage }) }}</text>
          <view class="speech-actions">
            <button
              v-if="speechRecord.asrStatus === 'failed'"
              class="plain-btn"
              size="mini"
              :loading="speechRetrying"
              @click="retryRecognition"
            >
              {{ t('dialogue.retryRecognition') }}
            </button>
            <button
              v-if="speechRecord.asrStatus === 'pending' || speechRecord.asrStatus === 'processing'"
              class="plain-btn"
              size="mini"
              @click="refreshSpeechRecord"
            >
              {{ t('dialogue.refreshStatus') }}
            </button>
            <button class="plain-btn" size="mini" @click="recordAgain">{{ t('dialogue.recordAgain') }}</button>
          </view>
        </view>

        <view v-if="result" :class="['result-card', result.correct ? 'success' : 'danger']">
          <text>{{ result.message }}</text>
          <text v-if="!result.correct" class="standard">{{ t('dialogue.standardAnswer', { answer: result.standardAnswer }) }}</text>
        </view>

        <view v-if="analysis" class="analysis-card">
          <text class="analysis-title">{{ t('dialogue.analysisTitle') }}</text>
          <text class="hanzi small">{{ analysis.hanziText }}</text>
          <text class="pinyin">{{ analysis.pinyinText || '-' }}</text>
          <text class="muted">{{ meaningLanguage === 'ru' ? analysis.translationRu || '-' : analysis.translationEn || '-' }}</text>
          <view v-if="analysis.vocabItems.length > 0" class="vocab-list">
            <view v-for="item in analysis.vocabItems" :key="item.id" class="vocab-card">
              <text class="vocab-word">{{ item.wordText }}</text>
              <text class="muted">{{ item.pinyin || '-' }}</text>
              <text class="muted">{{ meaningLanguage === 'ru' ? item.meaningRu || '-' : item.meaningEn || '-' }}</text>
            </view>
          </view>
        </view>

        <button class="primary-btn" :loading="submitting || speechUploading" @click="submitAnswer">
          {{ mode === 'shadow' ? t('dialogue.uploadRecording') : t('dialogue.submitAnswer') }}
        </button>
        <button class="plain-btn full" :loading="analysisLoading" @click="showAnalysis">
          {{ analysisLoading ? t('common.loading') : t('dialogue.viewAnalysis') }}
        </button>

        <view class="nav-row">
          <button class="plain-btn" size="mini" :disabled="lineIndex <= 0" @click="previousLine">{{ t('dialogue.previousLine') }}</button>
          <button class="plain-btn" size="mini" :disabled="lineIndex >= lines.length - 1" @click="nextLine">{{ t('dialogue.nextLine') }}</button>
        </view>
      </view>
      </template>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, onUnmounted, ref, watch } from 'vue'
import { onHide, onPullDownRefresh, onShow, onUnload } from '@dcloudio/uni-app'
import LanguageSwitch from '../../components/LanguageSwitch.vue'
import {
  checkDialogueLine,
  fetchDialogueLineAnalysis,
  fetchDialogueLines,
  fetchSpeechRecord,
  fetchVideoMaterials,
  retrySpeechRecord,
  uploadSpeechRecord
} from '../../api/dialogue'
import { resolveApiResourceUrl } from '../../api/http'
import { fetchMembershipStatus } from '../../api/home'
import { useAudioPlayer } from '../../composables/useAudioPlayer'
import { usePreferences } from '../../composables/usePreferences'
import { useRecorder } from '../../composables/useRecorder'
import { applyTabBarLocale, setPageTitle, useI18n } from '../../i18n'
import type {
  DialogueLine,
  DialogueLineAnalysis,
  DialogueLineCheckResult,
  SpeechRecord,
  VideoMaterial,
  VideoMaterialType
} from '../../types/api'
import { openPage, requireLogin, routes } from '../../utils/navigation'

type DialogueMode = 'order' | 'dictation' | 'shadow'
type AudioAction = 'original' | 'text' | 'recording'

interface SpeechCompareResult {
  correct?: boolean
  recognizedText?: string
  standardText?: string
  firstMismatchIndex?: number | null
}

const checkingAccess = ref(false)
const { locale, t } = useI18n()
const preferences = usePreferences()
const locked = ref(false)
const loading = ref(false)
const submitting = ref(false)
const analysisLoading = ref(false)
const speechUploading = ref(false)
const speechRetrying = ref(false)
const accessError = ref('')
const materialError = ref('')
const lineError = ref('')
const materialType = ref<VideoMaterialType | ''>('')
const materials = ref<VideoMaterial[]>([])
const activeMaterial = ref<VideoMaterial | null>(null)
const childMaterials = ref<VideoMaterial[]>([])
const lines = ref<DialogueLine[]>([])
const lineIndex = ref(0)
const mode = ref<DialogueMode>('order')
const scopeTab = ref<'all' | 'lessons'>('all')
const meaningLanguage = ref<'ru' | 'en'>('ru')
const selectedWords = ref<string[]>([])
const answerText = ref('')
const analysis = ref<DialogueLineAnalysis | null>(null)
const result = ref<DialogueLineCheckResult | null>(null)
const speechRecord = ref<SpeechRecord | null>(null)
const startedAt = ref(Date.now())
const activeAudioAction = ref<AudioAction | null>(null)
const audio = useAudioPlayer()
const recorder = useRecorder()
let speechPollTimer: ReturnType<typeof setTimeout> | null = null
let preferenceApplied = false

const originalAudioPlaying = computed(() => audio.playing.value && activeAudioAction.value === 'original')
const textAudioPlaying = computed(() => audio.playing.value && activeAudioAction.value === 'text')
const recorderRecording = computed(() => recorder.recording.value)
const hasRecording = computed(() => Boolean(recorder.recordUrl.value))
const currentLine = computed(() => lines.value[lineIndex.value] || null)
const translationText = computed(() => {
  const line = currentLine.value
  if (!line) {
    return ''
  }
  return meaningLanguage.value === 'ru' ? line.translationRu || '' : line.translationEn || ''
})
const availableWords = computed(() => {
  const line = currentLine.value
  if (!line) {
    return []
  }
  const selectedCount = selectedWords.value.reduce<Record<string, number>>((counts, word) => {
    counts[word] = (counts[word] || 0) + 1
    return counts
  }, {})
  const usedCount: Record<string, number> = {}
  return line.wordOptions.filter((word) => {
    const used = usedCount[word] || 0
    usedCount[word] = used + 1
    return used >= (selectedCount[word] || 0)
  })
})
const speechCompareResult = computed(() => parseSpeechCompareResult(speechRecord.value?.compareResult))
const speechScoreLabel = computed(() => {
  const score = speechRecord.value?.score
  return score === null || score === undefined ? '-' : t('dialogue.scoreValue', { score: Math.round(Number(score)) })
})
const speechScoreWidth = computed(() => {
  const score = Number(speechRecord.value?.score || 0)
  return `${Math.max(0, Math.min(100, score))}%`
})
const speechMismatchIndex = computed(() => {
  const index = speechCompareResult.value?.firstMismatchIndex
  return index === undefined || index === null ? null : index
})

onShow(() => {
  applyTabBarLocale()
  setPageTitle('dialogue.title')
  if (!requireLogin()) {
    return
  }
  void prepareDialogue()
})

watch(locale, () => {
  applyTabBarLocale()
  setPageTitle('dialogue.title')
})

watch(
  () => audio.playing.value,
  (playing) => {
    if (!playing) {
      activeAudioAction.value = null
    }
  }
)

onPullDownRefresh(() => {
  const task = activeMaterial.value ? reloadActiveMaterial() : prepareDialogue(true)
  void task.finally(() => uni.stopPullDownRefresh())
})

onHide(() => {
  cleanupTransientMedia()
})

onUnload(() => {
  cleanupTransientMedia()
})

async function prepareDialogue(forceMaterials = false) {
  checkingAccess.value = true
  accessError.value = ''
  try {
    if (!preferenceApplied) {
      const preference = await preferences.loadPreference()
      meaningLanguage.value = preference.translationLanguage
      preferenceApplied = true
    }
    const status = await fetchMembershipStatus()
    locked.value = !status.fullAccess
    if (locked.value) {
      activeMaterial.value = null
      childMaterials.value = []
      lines.value = []
      return
    }
    if (materials.value.length === 0 || forceMaterials) {
      await loadMaterials()
    }
  } catch {
    accessError.value = t('dialogue.accessFailed')
  } finally {
    checkingAccess.value = false
  }
}

async function loadMaterials() {
  loading.value = true
  materialError.value = ''
  try {
    const page = await fetchVideoMaterials(materialType.value)
    materials.value = page.records
  } catch {
    materialError.value = t('dialogue.materialsLoadFailed')
  } finally {
    loading.value = false
  }
}

async function setMaterialType(type: VideoMaterialType | '') {
  materialType.value = type
  await loadMaterials()
}

async function selectMaterial(material: VideoMaterial) {
  activeMaterial.value = material
  await reloadActiveMaterial()
}

async function reloadActiveMaterial() {
  if (!activeMaterial.value) {
    return
  }
  loading.value = true
  lineError.value = ''
  try {
    const [nextLines, childPage] = await Promise.all([
      fetchDialogueLines(activeMaterial.value.id),
      fetchVideoMaterials(activeMaterial.value.materialType, activeMaterial.value.id)
    ])
    lines.value = nextLines
    childMaterials.value = childPage.records
    scopeTab.value = 'all'
    lineIndex.value = 0
    resetPractice()
  } catch {
    lines.value = []
    lineError.value = t('dialogue.linesLoadFailed')
  } finally {
    loading.value = false
  }
}

function changeMaterial() {
  activeMaterial.value = null
  childMaterials.value = []
  scopeTab.value = 'all'
  lines.value = []
  lineError.value = ''
  resetPractice()
}

function setMode(value: DialogueMode) {
  mode.value = value
  resetPractice()
}

function selectWord(word: string) {
  selectedWords.value.push(word)
  result.value = null
}

function removeWord(index: number) {
  selectedWords.value.splice(index, 1)
  result.value = null
}

function playLineAudio() {
  const line = currentLine.value
  if (!line) {
    return
  }
  if (!line.audioUrl) {
    void uni.showToast({ icon: 'none', title: t('dialogue.noOriginalAudio') })
    return
  }
  activeAudioAction.value = 'original'
  audio.play(resolveApiResourceUrl(line.audioUrl))
}

function readLineText() {
  const line = currentLine.value
  if (!line) {
    return
  }
  activeAudioAction.value = 'text'
  audio.play(null, line.hanziText, 'zh')
}

function playRecording() {
  activeAudioAction.value = 'recording'
  audio.play(recorder.recordUrl.value)
}

async function submitAnswer() {
  const line = currentLine.value
  if (!line) {
    return
  }
  if (mode.value === 'shadow') {
    await uploadCurrentRecording(line)
    return
  }
  if (mode.value === 'order' && selectedWords.value.length === 0) {
    void uni.showToast({ icon: 'none', title: t('dialogue.needWords') })
    return
  }
  if (mode.value === 'dictation' && !answerText.value.trim()) {
    void uni.showToast({ icon: 'none', title: t('dialogue.needAnswer') })
    return
  }
  submitting.value = true
  try {
    result.value = await checkDialogueLine(line.id, {
      answerText: mode.value === 'dictation' ? answerText.value : undefined,
      orderedWords: mode.value === 'order' ? selectedWords.value : undefined,
      showedAnswer: Boolean(analysis.value),
      translationLanguage: meaningLanguage.value,
      durationSeconds: elapsedSeconds()
    })
    analysis.value = await fetchDialogueLineAnalysis(line.id)
  } catch {
    void uni.showToast({ icon: 'none', title: t('dialogue.submitFailed') })
  } finally {
    submitting.value = false
  }
}

async function uploadCurrentRecording(line: DialogueLine) {
  if (!recorder.recordUrl.value) {
    void uni.showToast({ icon: 'none', title: t('dialogue.needRecording') })
    return
  }
  speechUploading.value = true
  try {
    speechRecord.value = await uploadSpeechRecord(line.id, recorder.recordUrl.value, elapsedSeconds() * 1000)
    scheduleSpeechPolling()
  } catch {
    void uni.showToast({ icon: 'none', title: t('dialogue.uploadFailed') })
  } finally {
    speechUploading.value = false
  }
}

async function retryRecognition() {
  const recordId = speechRecord.value?.id
  if (!recordId) {
    return
  }
  speechRetrying.value = true
  stopSpeechPolling()
  try {
    speechRecord.value = await retrySpeechRecord(recordId)
    scheduleSpeechPolling()
    void uni.showToast({ icon: 'none', title: t('dialogue.retrySubmitted') })
  } catch {
    void uni.showToast({ icon: 'none', title: t('dialogue.retryFailed') })
  } finally {
    speechRetrying.value = false
  }
}

function recordAgain() {
  stopSpeechPolling()
  speechRecord.value = null
  recorder.clear()
}

async function showAnalysis() {
  const line = currentLine.value
  if (!line) {
    return
  }
  analysisLoading.value = true
  try {
    analysis.value = await fetchDialogueLineAnalysis(line.id)
  } catch {
    void uni.showToast({ icon: 'none', title: t('dialogue.analysisLoadFailed') })
  } finally {
    analysisLoading.value = false
  }
}

function previousLine() {
  if (lineIndex.value <= 0) {
    return
  }
  lineIndex.value -= 1
  resetPractice()
}

function nextLine() {
  if (lineIndex.value >= lines.value.length - 1) {
    return
  }
  lineIndex.value += 1
  resetPractice()
}

function resetPractice() {
  audio.stop()
  activeAudioAction.value = null
  stopSpeechPolling()
  recorder.clear()
  selectedWords.value = []
  answerText.value = ''
  analysis.value = null
  result.value = null
  speechRecord.value = null
  startedAt.value = Date.now()
}

function scheduleSpeechPolling() {
  stopSpeechPolling()
  const status = speechRecord.value?.asrStatus
  if (status !== 'pending' && status !== 'processing') {
    return
  }
  speechPollTimer = setTimeout(() => {
    void refreshSpeechRecord()
  }, 2500)
}

async function refreshSpeechRecord() {
  const recordId = speechRecord.value?.id
  if (!recordId) {
    return
  }
  try {
    speechRecord.value = await fetchSpeechRecord(recordId)
    scheduleSpeechPolling()
  } catch {
    stopSpeechPolling()
  }
}

function stopSpeechPolling() {
  if (speechPollTimer) {
    clearTimeout(speechPollTimer)
    speechPollTimer = null
  }
}

function cleanupTransientMedia() {
  audio.stop()
  activeAudioAction.value = null
  recorder.clear()
  stopSpeechPolling()
}

function goHome() {
  void openPage(routes.home)
}

function openMembership() {
  void openPage(routes.membership)
}

function mediaUrl(url?: string | null) {
  return resolveApiResourceUrl(url)
}

function materialTypeLabel(type: string) {
  const map: Record<string, string> = {
    drama: t('dialogue.drama'),
    short_video: t('dialogue.shortVideo'),
    cartoon: t('dialogue.cartoon')
  }
  return map[type] || type
}

function elapsedSeconds() {
  const seconds = Math.round((Date.now() - startedAt.value) / 1000)
  return Math.min(Math.max(seconds, 1), 24 * 60 * 60)
}

function speechStatusLabel(status: SpeechRecord['asrStatus']) {
  const map: Record<string, string> = {
    pending: t('dialogue.statusPending'),
    processing: t('dialogue.statusProcessing'),
    succeeded: t('dialogue.statusSucceeded'),
    failed: t('dialogue.statusFailed')
  }
  return status ? map[status] || status : t('dialogue.statusPending')
}

function speechStatusClass(status: SpeechRecord['asrStatus']) {
  if (status === 'succeeded') {
    return 'success'
  }
  if (status === 'failed') {
    return 'danger'
  }
  return 'pending'
}

function parseSpeechCompareResult(value?: string | null): SpeechCompareResult | null {
  if (!value) {
    return null
  }
  try {
    return JSON.parse(value) as SpeechCompareResult
  } catch {
    return null
  }
}

onUnmounted(stopSpeechPolling)
</script>

<style scoped>
.page {
  background: #eef5f7;
  min-height: 100vh;
  padding: 0 24rpx 32rpx;
}

.hero {
  background: #102033;
  border-bottom-left-radius: 32rpx;
  border-bottom-right-radius: 32rpx;
  box-sizing: border-box;
  color: #ffffff;
  margin: 0 -24rpx 24rpx;
  padding: 30rpx 24rpx 28rpx;
}

.hero-top {
  align-items: flex-start;
  display: flex;
  gap: 20rpx;
  justify-content: space-between;
}

.hero-copy {
  flex: 1;
  min-width: 0;
}

.eyebrow {
  color: #7dd3c7;
  display: block;
  font-size: 22rpx;
  font-weight: 700;
}

.title {
  display: block;
  font-size: 46rpx;
  font-weight: 700;
  line-height: 1.15;
  margin-top: 10rpx;
}

.subtitle {
  color: #cbd5e1;
  display: block;
  font-size: 26rpx;
  line-height: 1.5;
  margin-top: 14rpx;
}

.filter-row,
.mode-row,
.language-row,
.nav-row,
.record-row {
  display: flex;
  gap: 12rpx;
}

.filter-row {
  margin-bottom: 22rpx;
}

.mode-row {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 14rpx;
  box-sizing: border-box;
  margin: 22rpx 0 24rpx;
  padding: 8rpx;
}

.audio-action-row {
  display: grid;
  gap: 12rpx;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 20rpx;
}

.audio-action-row .plain-btn {
  margin-top: 0;
  width: 100%;
}

.filter-btn,
.mode-btn,
.small-button {
  margin: 0;
}

.mode-btn {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10rpx;
  color: #475569;
  flex: 1;
  font-weight: 700;
  min-height: 64rpx;
}

.filter-btn.active,
.mode-btn.active,
.small-button.active,
.primary-btn {
  background: #14796f;
  color: #ffffff;
}

.material-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.material-card,
.material-head,
.overview-card,
.state-card,
.line-panel,
.locked-card,
.translation-card,
.shadow-card,
.analysis-card,
.speech-card,
.result-card,
.vocab-card {
  background: #ffffff;
  border: 1px solid #d7e2ea;
  border-radius: 18rpx;
  box-shadow: 0 10rpx 30rpx rgba(15, 23, 42, 0.05);
  box-sizing: border-box;
}

.state-card {
  color: #64748b;
  display: flex;
  flex-direction: column;
  font-size: 26rpx;
  gap: 18rpx;
  padding: 34rpx 24rpx;
}

.error-card {
  background: #fff7ed;
  border-color: #fed7aa;
  color: #9a3412;
}

.overview-card {
  align-items: center;
  display: flex;
  gap: 18rpx;
  justify-content: space-between;
  padding: 24rpx;
}

.overview-label {
  color: #64748b;
  display: block;
  font-size: 22rpx;
  font-weight: 800;
}

.overview-title {
  color: #102033;
  display: block;
  font-size: 34rpx;
  font-weight: 900;
  line-height: 1.25;
  margin-top: 8rpx;
}

.overview-pill {
  background: #ccfbf1;
  border-radius: 999rpx;
  color: #14796f;
  flex: 0 0 auto;
  font-size: 22rpx;
  font-weight: 800;
  padding: 10rpx 16rpx;
}

.material-card {
  display: flex;
  gap: 18rpx;
  padding: 18rpx;
}

.cover {
  align-items: center;
  background: #eff6ff;
  color: #2563eb;
  display: flex;
  flex-shrink: 0;
  height: 156rpx;
  justify-content: center;
  width: 156rpx;
}

.placeholder {
  font-size: 28rpx;
  font-weight: 700;
}

.material-body {
  flex: 1;
  min-width: 0;
}

.tag {
  color: #2563eb;
  display: block;
  font-size: 24rpx;
  margin-bottom: 8rpx;
}

.material-title {
  display: block;
  font-size: 32rpx;
  font-weight: 700;
  margin-bottom: 8rpx;
}

.material-head {
  align-items: center;
  display: flex;
  justify-content: space-between;
  margin-bottom: 18rpx;
  padding: 20rpx;
}

.locked-card,
.line-panel,
.translation-card,
.shadow-card,
.analysis-card,
.result-card {
  padding: 24rpx;
}

.locked-title,
.analysis-title {
  display: block;
  font-size: 32rpx;
  font-weight: 700;
  margin-bottom: 10rpx;
}

.line-meta {
  align-items: center;
  color: #64748b;
  display: flex;
  font-size: 24rpx;
  justify-content: space-between;
  margin-bottom: 20rpx;
}

.translation-card {
  margin-bottom: 18rpx;
}

.translation {
  display: block;
  font-size: 34rpx;
  font-weight: 700;
  line-height: 1.45;
  margin-top: 8rpx;
}

.word-area {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
  margin-top: 22rpx;
}

.selected,
.options {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  min-height: 68rpx;
}

.word {
  margin: 0;
}

.selected-word {
  background: #ecfdf5;
}

.answer-input {
  background: #f8fafc;
  border: 1px solid #d1d5db;
  border-radius: 8rpx;
  box-sizing: border-box;
  margin-top: 22rpx;
  min-height: 220rpx;
  padding: 18rpx;
  width: 100%;
}

.shadow-card {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
  margin-top: 22rpx;
}

.pinyin {
  color: #2563eb;
  font-size: 28rpx;
}

.hanzi {
  display: block;
  font-family: var(--xc-hanzi-font-family);
  font-size: 38rpx;
  font-weight: 700;
  line-height: 1.45;
}

.hanzi.small {
  font-size: 32rpx;
}

.result-card {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
  margin-top: 20rpx;
}

.success {
  border-color: #22c55e;
}

.danger {
  border-color: #ef4444;
}

.standard {
  font-weight: 700;
}

.analysis-card {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
  margin-top: 20rpx;
}

.speech-card {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
  margin-top: 20rpx;
  padding: 24rpx;
}

.speech-head,
.speech-actions {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  justify-content: space-between;
}

.speech-actions {
  justify-content: flex-start;
  margin-top: 8rpx;
}

.status-pill {
  border-radius: 999rpx;
  font-size: 22rpx;
  padding: 6rpx 14rpx;
}

.status-pill.pending {
  background: #eff6ff;
  color: #1d4ed8;
}

.status-pill.success {
  background: #dcfce7;
  color: #166534;
}

.status-pill.danger {
  background: #fee2e2;
  color: #991b1b;
}

.score-row {
  align-items: center;
  display: flex;
  gap: 18rpx;
}

.score {
  color: #111827;
  display: block;
  font-size: 38rpx;
  font-weight: 700;
}

.score-bar {
  background: #e5e7eb;
  border-radius: 999rpx;
  flex: 1;
  height: 14rpx;
  overflow: hidden;
}

.score-fill {
  background: #22c55e;
  height: 100%;
}

.vocab-list {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
  margin-top: 12rpx;
}

.vocab-card {
  padding: 16rpx;
}

.vocab-word {
  display: block;
  font-size: 28rpx;
  font-weight: 700;
}

.primary-btn,
.full {
  margin-top: 20rpx;
  width: 100%;
}

.primary-btn {
  align-items: center;
  border-radius: 12rpx;
  display: flex;
  font-size: 28rpx;
  font-weight: 800;
  justify-content: center;
  min-height: 84rpx;
}

.plain-btn {
  align-items: center;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  border-radius: 12rpx;
  color: #102033;
  display: flex;
  font-size: 26rpx;
  font-weight: 800;
  justify-content: center;
  margin-left: 0;
  margin-right: 0;
  min-height: 72rpx;
}

.plain-btn.compact {
  margin-top: 0;
  width: 180rpx;
}

.danger-btn {
  border-color: #ef4444;
  color: #ef4444;
}

.nav-row {
  justify-content: space-between;
  margin-top: 18rpx;
}

.state {
  align-items: center;
  color: #64748b;
  display: flex;
  justify-content: center;
  padding: 80rpx 0;
}

.muted {
  color: #64748b;
  font-size: 24rpx;
  line-height: 1.6;
}

.one-line {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
