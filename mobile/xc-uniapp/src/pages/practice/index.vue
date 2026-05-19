<template>
  <view class="page">
    <view class="hero">
      <view class="hero-top">
        <view class="hero-copy">
          <text class="eyebrow">{{ t('app.title') }}</text>
          <text class="title">{{ t('practice.title') }}</text>
          <text class="subtitle">{{ headerText }}</text>
        </view>
        <LanguageSwitch variant="hero" />
      </view>
    </view>

    <view v-if="checkingAccess" class="state-card">
      <text>{{ t('practice.checkingAccess') }}</text>
    </view>

    <view v-else-if="accessError" class="state-card error-card">
      <text>{{ accessError }}</text>
      <button class="plain-btn compact" size="mini" @click="preparePractice()">{{ t('common.retry') }}</button>
    </view>

    <view v-else-if="locked" class="locked-card">
      <text class="locked-title">{{ t('practice.lockedTitle') }}</text>
      <text class="muted">{{ t('practice.lockedDesc') }}</text>
      <button class="primary-btn full" @click="openMembership">{{ t('practice.openMembership') }}</button>
      <button class="plain-btn full" @click="goHome">{{ t('common.backHome') }}</button>
    </view>

    <view v-else-if="!activeSet" class="section">
      <view v-if="loading && sets.length === 0" class="state-card">{{ t('common.loading') }}</view>
      <view v-else-if="setError" class="state-card error-card">
        <text>{{ setError }}</text>
        <button class="plain-btn compact" size="mini" @click="loadSets()">{{ t('common.retry') }}</button>
      </view>
      <view v-else-if="sets.length === 0" class="state-card">{{ t('practice.emptySets') }}</view>
      <view v-else class="set-list">
        <view class="overview-card">
          <view>
            <text class="overview-label">{{ t('practice.title') }}</text>
            <text class="overview-title">{{ t('practice.chooseSet') }}</text>
          </view>
          <text class="overview-pill">{{ t('feature.practice') }}</text>
        </view>
        <view
          v-for="item in sets"
          :key="item.id"
          class="set-item"
          @click="selectRootSet(item)"
        >
          <view class="set-copy">
            <view class="set-top">
              <text class="set-title">{{ item.title }}</text>
              <text class="tag">{{ item.level || t('common.default') }}</text>
            </view>
            <text class="muted">{{ t('practice.questionCount', { count: item.totalActiveQuestionCount || item.activeQuestionCount }) }}</text>
          </view>
          <view class="chevron-icon set-chevron" />
        </view>
      </view>
    </view>

    <view v-else class="practice">
      <view class="toolbar">
        <button class="plain-btn" size="mini" @click="switchSet">{{ t('practice.sets') }}</button>
        <text class="muted">{{ questionCounterText }}</text>
      </view>

      <view v-if="childSets.length > 0" class="scope-card">
        <button class="scope-tab" :class="{ active: scopeTab === 'all' }" @click="scopeTab = 'all'">{{ t('practice.scopeAll') }}</button>
        <button class="scope-tab" :class="{ active: scopeTab === 'lessons' }" @click="scopeTab = 'lessons'">{{ t('practice.scopeLessons') }}</button>
      </view>

      <view v-if="scopeTab === 'lessons' && childSets.length > 0" class="lesson-list">
        <view v-for="lesson in childSets" :key="lesson.id" class="lesson-item" @click="selectLessonSet(lesson)">
          <view>
            <text class="lesson-title">{{ lesson.title }}</text>
            <text class="muted">{{ lesson.level || activeSet.level || typeLabel(activeExerciseType) }}</text>
          </view>
          <view class="lesson-action">
            <text class="lesson-count">{{ t('practice.questionCount', { count: lesson.totalActiveQuestionCount || lesson.activeQuestionCount }) }}</text>
            <view class="lesson-links">
              <view v-if="lesson.childCount > 0" class="lesson-link" @click.stop="selectLessonSet(lesson)">
                <text>{{ t('practice.chooseCourse') }}</text>
                <view class="chevron-icon lesson-chevron" />
              </view>
              <view class="lesson-link" @click.stop="startLessonSet(lesson)">
                <text>{{ t('practice.startLearning') }}</text>
                <view class="chevron-icon lesson-chevron" />
              </view>
            </view>
          </view>
        </view>
      </view>

      <template v-else>
      <view v-if="questionLoading" class="state-card">{{ t('common.loading') }}</view>
      <view v-else-if="questionError" class="state-card error-card">
        <text>{{ questionError }}</text>
        <button class="plain-btn compact" size="mini" @click="reloadActiveSet">{{ t('common.retry') }}</button>
      </view>
      <view v-else-if="!currentQuestion" class="state-card">{{ t('practice.emptyQuestions') }}</view>
      <view v-else class="panel">
        <view class="meta">
          <text>{{ typeLabel(activeExerciseType) }}</text>
          <text class="status-chip" :class="{ learned: isCurrentQuestionLearned }">{{ currentQuestionStatusLabel }}</text>
          <text>{{ t('practice.sortOrder', { value: currentQuestion.sortOrder }) }}</text>
        </view>

        <text v-if="promptText" class="prompt">{{ promptText }}</text>

        <view v-if="needsAudio" class="audio-row">
          <view class="audio-wave-player" :class="{ loading: audioLoading && !audio.playing.value }">
            <button class="wave-play-button" :aria-label="audioButtonLabel" @click="toggleQuestionAudio">
              {{ audioButtonSymbol }}
            </button>
            <text class="wave-time">{{ currentAudioTime }}</text>
            <view class="waveform" :aria-label="t('practice.audioWaveform')">
              <button
                v-for="bar in waveformBars"
                :key="bar.index"
                class="wave-bar"
                :class="{ active: bar.fraction <= audioProgress }"
                :disabled="!canSeekAudio && !canPrepareSeekAudio"
                :aria-label="t('practice.seekAudio', { percent: Math.round(bar.fraction * 100) })"
                @click="seekQuestionAudio(bar.fraction)"
              >
                <text :style="{ height: `${bar.height}%` }" />
              </button>
            </view>
            <text class="wave-time">{{ totalAudioTime }}</text>
          </view>
        </view>

        <view v-if="usesWordOptions && currentQuestion.wordOptions.length > 0" class="word-area">
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
            <text v-if="selectedWords.length === 0" class="muted">{{ t('practice.chooseWords') }}</text>
          </view>
          <view class="options">
            <button
              v-for="option in availableOptions"
              :key="option.id"
              class="word"
              size="mini"
              @click="addWord(option.wordText)"
            >
              {{ option.wordText }}
            </button>
          </view>
        </view>

        <textarea
          v-else
          v-model="answerText"
          class="answer-input"
          maxlength="200"
          :placeholder="t('practice.inputAnswer')"
        />

        <view v-if="result" :class="['result', result.correct ? 'success' : 'danger']">
          <text>{{ result.correct ? t('practice.correct') : t('practice.almost') }}</text>
          <text v-if="!result.correct" class="standard">{{ t('practice.standardAnswer', { answer: result.standardAnswer }) }}</text>
          <text class="muted">{{ result.message }}</text>
        </view>

        <view v-if="answer" class="answer-card">
          <text class="answer-title">{{ t('practice.answer') }}</text>
          <text>{{ answer.hanziAnswer }}</text>
          <text v-if="answerPinyinText" class="answer-pinyin">{{ answerPinyinText }}</text>
          <text v-if="answer.translationRu" class="muted">{{ answer.translationRu }}</text>
          <text v-if="answer.translationEn" class="muted">{{ answer.translationEn }}</text>
        </view>

        <button class="primary-btn" :disabled="submitting || !canSubmit" @click="submitAnswer">
          {{ submitting ? t('practice.submitting') : t('practice.submitAnswer') }}
        </button>
        <button class="plain-btn full" :disabled="answerLoading" @click="showAnswer">
          {{ answerLoading ? t('common.loading') : t('practice.viewAnswer') }}
        </button>
        <button class="plain-btn full" :class="{ favorite: currentQuestion.favorite }" :loading="favoriteActionId === currentQuestion.id" @click="toggleQuestionFavorite">
          {{ currentQuestion.favorite ? t('vocab.unfavorite') : t('vocab.favorite') }}
        </button>

        <view class="nav-row">
          <button class="plain-btn" size="mini" :disabled="questionIndex === 0" @click="previousQuestion">
            {{ t('practice.previous') }}
          </button>
          <button class="plain-btn" size="mini" :disabled="questionIndex >= questions.length - 1" @click="nextQuestion">
            {{ t('practice.next') }}
          </button>
        </view>
      </view>
      </template>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { onHide, onPullDownRefresh, onShow, onUnload } from '@dcloudio/uni-app'
import LanguageSwitch from '../../components/LanguageSwitch.vue'
import { resolveApiResourceUrl } from '../../api/http'
import { checkExercise, favoriteSentenceExercise, fetchExerciseAnswer, fetchExerciseQuestions, fetchExerciseSets, unfavoriteSentenceExercise } from '../../api/exercise'
import { useAudioPlayer } from '../../composables/useAudioPlayer'
import { usePreferences } from '../../composables/usePreferences'
import { fetchMembershipStatus } from '../../api/home'
import { applyTabBarLocale, setPageTitle, useI18n } from '../../i18n'
import type { ExerciseAnswer, ExerciseCheckResult, ExerciseSet, SentenceExercise, SentenceProgressStatus } from '../../types/api'
import { openPage, requireLogin, routes } from '../../utils/navigation'

const { locale, t } = useI18n()
const preferences = usePreferences()
const checkingAccess = ref(false)
const locked = ref(false)
const loading = ref(false)
const questionLoading = ref(false)
const submitting = ref(false)
const favoriteActionId = ref<number | null>(null)
const answerLoading = ref(false)
const audioLoading = ref(false)
const accessError = ref('')
const setError = ref('')
const questionError = ref('')
const audio = useAudioPlayer()
const answerAudioCache = new Map<number, { text: string; audioUrl: string | null }>()
const sets = ref<ExerciseSet[]>([])
const activeSet = ref<ExerciseSet | null>(null)
const childSets = ref<ExerciseSet[]>([])
const scopeTab = ref<'all' | 'lessons'>('all')
const questions = ref<SentenceExercise[]>([])
const questionIndex = ref(0)
const answerText = ref('')
const selectedWords = ref<string[]>([])
const meaningLanguage = ref<'ru' | 'en'>('ru')
const result = ref<ExerciseCheckResult | null>(null)
const answer = ref<ExerciseAnswer | null>(null)
const questionStartedAt = ref(Date.now())
const pendingSeekFraction = ref<number | null>(null)
const orderingExerciseTypes = new Set(['audio_order', 'translation_order'])
const learnedSentenceStatuses = new Set<SentenceProgressStatus>(['learned', 'reviewing', 'mastered'])
const waveformBars = Array.from({ length: 36 }, (_, index) => {
  const wave = Math.sin(index * 0.86) * 18 + Math.sin(index * 0.37 + 1.1) * 12
  return {
    index,
    fraction: index / 35,
    height: Math.round(Math.min(Math.max(42 + Math.abs(wave), 22), 82))
  }
})
let preferenceApplied = false

const currentQuestion = computed(() => questions.value[questionIndex.value] || null)
const isCurrentQuestionLearned = computed(() => {
  const status = currentQuestion.value?.progressStatus
  return Boolean(status && learnedSentenceStatuses.has(status))
})
const currentQuestionStatusLabel = computed(() => statusLabel(currentQuestion.value?.progressStatus || null))
const audioButtonLabel = computed(() => {
  if (audio.playing.value) {
    return t('common.pause')
  }
  if (audio.paused.value) {
    return t('practice.resumeAudio')
  }
  return audioLoading.value ? t('practice.audioPreparing') : t('practice.playAudio')
})
const audioButtonSymbol = computed(() => {
  if (audio.playing.value) {
    return '||'
  }
  return audioLoading.value ? '...' : '>'
})
const audioProgress = computed(() => {
  if (!audio.duration.value) {
    return 0
  }
  return Math.min(Math.max(audio.currentTime.value / audio.duration.value, 0), 1)
})
const currentAudioTime = computed(() => formatAudioTime(audio.currentTime.value))
const totalAudioTime = computed(() => formatAudioTime(audio.duration.value))
const canSeekAudio = computed(() => audio.seekable.value && audio.duration.value > 0)
const canPrepareSeekAudio = computed(() => Boolean(currentQuestion.value?.audioUrl || answerAudioCache.get(currentQuestion.value?.id || 0)?.audioUrl))
const activeExerciseType = computed(() => activeSet.value?.exerciseType || 'audio_order')
const questionCounterText = computed(() => questions.value.length > 0 ? `${questionIndex.value + 1} / ${questions.value.length}` : '0 / 0')
const needsAudio = computed(() => activeExerciseType.value === 'audio_order' || activeExerciseType.value === 'audio_dictation')
const usesWordOptions = computed(() => {
  return orderingExerciseTypes.has(activeExerciseType.value)
})
const headerText = computed(() => {
  if (activeSet.value) {
    return activeSet.value.title
  }
  return t('practice.chooseSet')
})
const promptText = computed(() => {
  const question = currentQuestion.value
  if (!question) {
    return ''
  }
  const translation = meaningLanguage.value === 'ru' ? question.translationRu : question.translationEn
  if (needsAudio.value) {
    return ''
  }
  return question.pinyinPrompt || translation || question.translationRu || question.translationEn || t('practice.promptFallback')
})
const answerPinyinText = computed(() => answer.value?.pinyinPrompt || currentQuestion.value?.pinyinPrompt || '')
const availableOptions = computed(() => {
  const question = currentQuestion.value
  if (!question) {
    return []
  }
  const selectedCount = selectedWords.value.reduce<Record<string, number>>((acc, word) => {
    acc[word] = (acc[word] || 0) + 1
    return acc
  }, {})
  const usedCount: Record<string, number> = {}
  return question.wordOptions.filter((option) => {
    const used = usedCount[option.wordText] || 0
    usedCount[option.wordText] = used + 1
    return used >= (selectedCount[option.wordText] || 0)
  })
})
const canSubmit = computed(() => {
  const question = currentQuestion.value
  if (!question) {
    return false
  }
  if (usesWordOptions.value && question.wordOptions.length > 0) {
    return selectedWords.value.length > 0
  }
  return answerText.value.trim().length > 0
})

onShow(() => {
  applyTabBarLocale()
  setPageTitle('practice.title')
  if (!requireLogin()) {
    return
  }
  void preparePractice()
})

watch(locale, () => {
  applyTabBarLocale()
  setPageTitle('practice.title')
})

onPullDownRefresh(() => {
  const task = activeSet.value ? reloadActiveSet() : preparePractice(true)
  void task.finally(() => uni.stopPullDownRefresh())
})

onHide(() => {
  audio.stop()
})

onUnload(() => {
  audio.stop()
})

async function preparePractice(forceSets = false) {
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
      activeSet.value = null
      questions.value = []
      return
    }
    if (sets.value.length === 0 || forceSets) {
      await loadSets()
    }
  } catch {
    accessError.value = t('practice.accessFailed')
  } finally {
    checkingAccess.value = false
  }
}

async function loadSets() {
  loading.value = true
  setError.value = ''
  try {
    const page = await fetchExerciseSets()
    sets.value = page.records
  } catch {
    setError.value = t('practice.setsLoadFailed')
  } finally {
    loading.value = false
  }
}

async function selectSet(item: ExerciseSet, preferredScopeTab: 'all' | 'lessons' = 'all') {
  activeSet.value = item
  scopeTab.value = preferredScopeTab
  await reloadActiveSet()
}

async function selectRootSet(item: ExerciseSet) {
  await selectSet(item, item.childCount > 0 ? 'lessons' : 'all')
}

async function selectLessonSet(item: ExerciseSet) {
  await selectSet(item, item.childCount > 0 ? 'lessons' : 'all')
}

async function startLessonSet(item: ExerciseSet) {
  await selectSet(item, 'all')
}

async function reloadActiveSet() {
  if (!activeSet.value) {
    return
  }
  questionLoading.value = true
  questionError.value = ''
  questionIndex.value = 0
  resetAnswerState()
  try {
    const [children, data] = await Promise.all([
      fetchExerciseSets({ pageSize: 100, parentId: activeSet.value.id }),
      fetchExerciseQuestions(activeSet.value.id, activeExerciseType.value)
    ])
    childSets.value = children.records
    questions.value = data.records
  } catch {
    questions.value = []
    questionError.value = t('practice.questionsLoadFailed')
  } finally {
    questionLoading.value = false
  }
}

function switchSet() {
  activeSet.value = null
  childSets.value = []
  scopeTab.value = 'all'
  questions.value = []
  questionIndex.value = 0
  resetAnswerState()
}

function goHome() {
  void openPage(routes.home)
}

function openMembership() {
  void openPage(routes.membership)
}

function addWord(word: string) {
  selectedWords.value.push(word)
  result.value = null
  answer.value = null
}

function removeWord(index: number) {
  selectedWords.value.splice(index, 1)
  result.value = null
  answer.value = null
}

async function submitAnswer() {
  const question = currentQuestion.value
  if (!question || !canSubmit.value) {
    return
  }
  submitting.value = true
  try {
    result.value = await checkExercise(question.id, buildPayload(question))
    applyQuestionProgress(question, result.value)
  } catch {
    void uni.showToast({ icon: 'none', title: t('practice.submitFailed') })
  } finally {
    submitting.value = false
  }
}

async function showAnswer() {
  const question = currentQuestion.value
  if (!question) {
    return
  }
  answerLoading.value = true
  try {
    answer.value = await fetchExerciseAnswer(question.id)
  } catch {
    void uni.showToast({ icon: 'none', title: t('practice.answerLoadFailed') })
  } finally {
    answerLoading.value = false
  }
}

async function toggleQuestionFavorite() {
  const question = currentQuestion.value
  if (!question || favoriteActionId.value) {
    return
  }
  favoriteActionId.value = question.id
  try {
    const result = question.favorite
      ? await unfavoriteSentenceExercise(question.id)
      : await favoriteSentenceExercise(question.id)
    question.favorite = result.favorite
    void uni.showToast({ icon: 'none', title: result.favorite ? t('vocab.favorited') : t('vocab.unfavorited') })
  } catch {
    void uni.showToast({ icon: 'none', title: t('common.requestFailed') })
  } finally {
    favoriteActionId.value = null
  }
}

async function getQuestionAudioSource() {
  const question = currentQuestion.value
  if (!question) {
    return null
  }
  if (question.audioUrl) {
    return { text: '', audioUrl: question.audioUrl }
  }
  if (answerAudioCache.has(question.id)) {
    return answerAudioCache.get(question.id) || null
  }
  audioLoading.value = true
  try {
    const questionAnswer = await fetchExerciseAnswer(question.id)
    const source = {
      text: questionAnswer.hanziAnswer,
      audioUrl: questionAnswer.audioUrl
    }
    answerAudioCache.set(question.id, source)
    return source
  } catch {
    void uni.showToast({ icon: 'none', title: t('practice.audioFailed') })
    return null
  } finally {
    audioLoading.value = false
  }
}

async function playQuestionAudio() {
  const source = await getQuestionAudioSource()
  if (!source) {
    return
  }
  audio.play(resolveApiResourceUrl(source.audioUrl), source.text, 'zh')
}

function toggleQuestionAudio() {
  if (audio.playing.value) {
    audio.pause()
    return
  }
  if (audio.paused.value) {
    audio.resume()
    return
  }
  if (audioLoading.value) {
    return
  }
  void playQuestionAudio()
}

async function seekQuestionAudio(fraction: number) {
  if (audioLoading.value) {
    return
  }
  if (canSeekAudio.value) {
    audio.seekByFraction(fraction, true)
    return
  }
  const source = await getQuestionAudioSource()
  if (!source?.audioUrl) {
    return
  }
  pendingSeekFraction.value = fraction
  audio.play(resolveApiResourceUrl(source.audioUrl), source.text, 'zh')
}

function buildPayload(question: SentenceExercise) {
  if (usesWordOptions.value && question.wordOptions.length > 0) {
    return {
      orderedWords: selectedWords.value,
      showedAnswer: Boolean(answer.value),
      translationLanguage: meaningLanguage.value,
      durationSeconds: elapsedSeconds(),
      exerciseType: activeExerciseType.value
    }
  }
  return {
    answerText: answerText.value,
    showedAnswer: Boolean(answer.value),
    translationLanguage: meaningLanguage.value,
    durationSeconds: elapsedSeconds(),
    exerciseType: activeExerciseType.value
  }
}

function previousQuestion() {
  if (questionIndex.value === 0) {
    return
  }
  questionIndex.value -= 1
  resetAnswerState()
}

function nextQuestion() {
  if (questionIndex.value >= questions.value.length - 1) {
    return
  }
  questionIndex.value += 1
  resetAnswerState()
}

function resetAnswerState() {
  audio.stop()
  answerText.value = ''
  selectedWords.value = []
  result.value = null
  answer.value = null
  questionStartedAt.value = Date.now()
}

function applyQuestionProgress(question: SentenceExercise, checkResult: ExerciseCheckResult) {
  question.progressStatus = checkResult.progressStatus
  question.attemptCount = checkResult.attemptCount
  question.correctCount = checkResult.correctCount
  question.learnedAt = checkResult.learnedAt
  question.lastPracticedAt = checkResult.lastPracticedAt
  question.lastCorrectAt = checkResult.lastCorrectAt
  question.nextReviewAt = checkResult.nextReviewAt
}

function statusLabel(status: SentenceProgressStatus | null) {
  if (!status) {
    return t('practice.unlearned')
  }
  switch (status) {
    case 'mastered':
      return t('practice.mastered')
    case 'reviewing':
      return t('practice.reviewing')
    case 'learned':
      return t('practice.learned')
    default:
      return t('practice.learning')
  }
}

function typeLabel(type: string) {
  return t(`exercise.${type}`)
}

function elapsedSeconds() {
  const seconds = Math.round((Date.now() - questionStartedAt.value) / 1000)
  return Math.min(Math.max(seconds, 1), 24 * 60 * 60)
}

function formatAudioTime(seconds: number) {
  if (!Number.isFinite(seconds) || seconds <= 0) {
    return '00:00'
  }
  const totalSeconds = Math.floor(seconds)
  const minutes = Math.floor(totalSeconds / 60).toString().padStart(2, '0')
  const remainingSeconds = (totalSeconds % 60).toString().padStart(2, '0')
  return `${minutes}:${remainingSeconds}`
}

watch(
  () => audio.duration.value,
  (duration) => {
    if (duration > 0 && pendingSeekFraction.value !== null) {
      audio.seekByFraction(pendingSeekFraction.value, true)
      pendingSeekFraction.value = null
    }
  }
)
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
  margin: 0 -24rpx 22rpx;
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

.section,
.practice {
  display: block;
}

.set-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.set-item,
.overview-card,
.state-card,
.panel,
.locked-card,
.answer-card,
.result {
  background: var(--xc-surface);
  border: 1px solid var(--xc-border);
  border-radius: 16rpx;
  box-shadow: var(--xc-shadow-card);
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
  padding: 26rpx;
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
  background: var(--xc-primary-soft);
  border-radius: 999rpx;
  color: var(--xc-primary);
  flex: 0 0 auto;
  font-size: 22rpx;
  font-weight: 800;
  padding: 10rpx 16rpx;
}

.set-item {
  align-items: center;
  display: flex;
  gap: 18rpx;
  justify-content: space-between;
  min-height: 154rpx;
  padding: 26rpx 28rpx;
}

.set-item.disabled {
  opacity: 0.55;
}

.set-copy {
  flex: 1;
  min-width: 0;
}

.set-top {
  align-items: flex-start;
  display: flex;
  gap: 14rpx;
  justify-content: space-between;
}

.locked-card {
  display: flex;
  flex-direction: column;
  gap: 14rpx;
  padding: 28rpx;
}

.locked-title {
  display: block;
  font-size: 32rpx;
  font-weight: 700;
}

.set-title {
  color: #102033;
  display: block;
  flex: 1;
  font-size: 32rpx;
  font-weight: 800;
  line-height: 1.3;
  margin-bottom: 8rpx;
  min-width: 0;
  word-break: break-word;
}

.tag {
  background: var(--xc-primary-soft);
  border-radius: 999rpx;
  color: var(--xc-primary);
  flex: 0 0 auto;
  font-size: 22rpx;
  font-weight: 800;
  padding: 8rpx 14rpx;
}

.set-chevron {
  color: #94a3b8;
  flex: 0 0 auto;
}

.toolbar,
.meta,
.nav-row {
  align-items: center;
  display: flex;
  justify-content: space-between;
}

.toolbar {
  background: rgba(255, 253, 246, 0.96);
  border: 1px solid var(--xc-border);
  border-radius: 22rpx;
  box-sizing: border-box;
  margin-bottom: 18rpx;
  padding: 12rpx 14rpx;
}

.scope-card {
  background: rgba(255, 253, 246, 0.96);
  border: 1px solid var(--xc-border);
  border-radius: 22rpx;
  box-sizing: border-box;
  display: grid;
  gap: 10rpx;
  grid-template-columns: 1fr 1fr;
  margin-bottom: 18rpx;
  padding: 10rpx;
}

.scope-tab {
  background: transparent;
  border: 0;
  border-radius: 18rpx;
  color: var(--xc-muted);
  font-size: 26rpx;
  font-weight: 800;
  line-height: 1.2;
  min-height: 86rpx;
  padding: 0 12rpx;
}

.scope-tab.active {
  background: var(--xc-primary);
  color: #ffffff;
}

.lesson-list {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.lesson-item {
  align-items: center;
  background: var(--xc-surface);
  border: 1px solid var(--xc-border);
  border-radius: 16rpx;
  box-sizing: border-box;
  display: flex;
  gap: 18rpx;
  justify-content: space-between;
  min-height: 154rpx;
  padding: 26rpx 28rpx;
}

.lesson-title {
  color: #102033;
  display: block;
  font-size: 32rpx;
  font-weight: 900;
  line-height: 1.25;
}

.lesson-count {
  background: #eef4ff;
  border-radius: 999rpx;
  color: #1d4ed8;
  font-size: 22rpx;
  font-weight: 800;
  padding: 10rpx 16rpx;
}

.lesson-action {
  align-items: flex-end;
  display: flex;
  flex: 0 0 auto;
  flex-direction: column;
  gap: 10rpx;
}

.lesson-links {
  align-items: center;
  display: flex;
  gap: 12rpx;
  justify-content: flex-end;
}

.lesson-link {
  align-items: center;
  background: linear-gradient(180deg, #ffffff 0%, #f1f6f7 100%);
  border: 1px solid var(--xc-border);
  border-bottom: 5rpx solid var(--xc-button-secondary-edge);
  border-radius: 16rpx;
  box-shadow: inset 0 2rpx 0 rgba(255, 255, 255, 0.9), 0 6rpx 14rpx rgba(16, 24, 40, 0.06);
  box-sizing: border-box;
  color: var(--xc-primary);
  display: flex;
  font-size: 24rpx;
  font-weight: 900;
  gap: 8rpx;
  min-height: 54rpx;
  padding: 0 14rpx;
  white-space: nowrap;
}

.lesson-link:last-child {
  background: linear-gradient(180deg, var(--xc-button-primary-top) 0%, var(--xc-button-primary-mid) 58%, var(--xc-button-primary-bottom) 100%);
  border-color: #087064;
  border-bottom-color: #07564f;
  color: #ffffff;
}

.lesson-link:active {
  transform: translateY(3rpx);
}

.chevron-icon {
  border-right: 3rpx solid currentColor;
  border-top: 3rpx solid currentColor;
  box-sizing: border-box;
  height: 15rpx;
  transform: rotate(45deg);
  width: 15rpx;
}

.lesson-chevron {
  margin-top: 2rpx;
}

.panel {
  background: var(--xc-surface-raised) !important;
  border-radius: 24rpx !important;
  padding: 30rpx;
  position: relative;
}

.meta {
  color: #64748b;
  flex-wrap: wrap;
  font-size: 24rpx;
  gap: 12rpx;
  margin-bottom: 24rpx;
}

.status-chip {
  background: #f1f5f9;
  border: 1px solid #d7e2ea;
  border-radius: 8rpx;
  color: #475569;
  font-size: 22rpx;
  font-weight: 800;
  padding: 6rpx 12rpx;
}

.status-chip.learned {
  background: #ecfdf5;
  border-color: #bbf7d0;
  color: #047857;
}

.prompt {
  display: block;
  font-size: 42rpx;
  font-weight: 800;
  line-height: 1.45;
  margin-bottom: 24rpx;
}

.word-area {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
  margin-bottom: 24rpx;
}

.audio-row {
  margin-bottom: 24rpx;
}

.audio-wave-player {
  align-items: center;
  background: var(--xc-surface-raised);
  border: 1px solid var(--xc-border);
  border-radius: 24rpx;
  box-shadow: 0 8rpx 24rpx rgba(15, 23, 42, 0.06);
  box-sizing: border-box;
  display: grid;
  gap: 12rpx;
  grid-template-columns: 64rpx 72rpx minmax(0, 1fr) 72rpx;
  min-height: 96rpx;
  padding: 14rpx 18rpx;
  width: 100%;
}

.wave-play-button {
  align-items: center;
  background: var(--xc-primary);
  border: 0;
  border-radius: 999rpx;
  color: #ffffff;
  display: flex;
  font-size: 26rpx;
  font-weight: 900;
  height: 58rpx;
  justify-content: center;
  line-height: 1;
  margin: 0;
  min-height: 58rpx;
  padding: 0;
  width: 58rpx;
}

.wave-play-button::after,
.wave-bar::after {
  border: 0;
}

.audio-wave-player.loading .wave-play-button {
  opacity: 0.82;
}

.wave-time {
  color: var(--xc-muted);
  display: block;
  font-size: 22rpx;
  font-variant-numeric: tabular-nums;
  font-weight: 800;
  text-align: center;
}

.waveform {
  align-items: center;
  display: grid;
  gap: 4rpx;
  grid-template-columns: repeat(36, minmax(3rpx, 1fr));
  height: 50rpx;
  min-width: 0;
}

.wave-bar {
  align-items: center;
  background: transparent;
  border: 0;
  display: flex;
  height: 50rpx;
  justify-content: center;
  line-height: 1;
  margin: 0;
  min-height: 50rpx;
  padding: 0;
  width: 100%;
}

.wave-bar text {
  background: #d9eee7;
  border-radius: 999rpx;
  display: block;
  min-height: 8rpx;
  width: 100%;
}

.wave-bar.active text {
  background: var(--xc-primary);
}

.wave-bar[disabled] text {
  background: #e5edf6;
  opacity: 0.78;
}

.selected,
.options {
  align-items: center;
  background: var(--xc-surface-raised);
  border: 1px dashed var(--xc-border);
  border-radius: 22rpx;
  box-sizing: border-box;
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  min-height: 132rpx;
  padding: 20rpx;
}

.word {
  background: var(--xc-primary-soft);
  border: 1px solid var(--xc-border-strong);
  border-radius: 16rpx;
  color: var(--xc-primary);
  font-size: 26rpx;
  font-weight: 850;
  margin: 0;
  min-height: 70rpx;
  padding: 0 24rpx;
}

.selected-word {
  background: #d9f2e9;
}

.answer-input {
  background: var(--xc-surface-raised);
  border: 1px solid var(--xc-border);
  border-radius: 20rpx;
  box-sizing: border-box;
  min-height: 180rpx;
  padding: 18rpx;
  width: 100%;
}

.result {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
  margin-top: 20rpx;
  padding: 18rpx;
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

.answer-card {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
  margin-top: 18rpx;
  padding: 24rpx;
}

.answer-title {
  font-weight: 700;
}

.answer-pinyin {
  color: #64748b;
  font-size: 24rpx;
}

.primary-btn,
.full {
  margin-top: 20rpx;
  width: 100%;
}

.primary-btn {
  align-items: center;
  background: var(--xc-primary);
  border-radius: 14rpx;
  color: #ffffff;
  display: flex;
  font-size: 28rpx;
  font-weight: 800;
  justify-content: center;
  min-height: 84rpx;
}

.plain-btn {
  align-items: center;
  background: var(--xc-surface-raised);
  border: 1px solid var(--xc-border);
  border-radius: 14rpx;
  color: var(--xc-ink);
  display: flex;
  font-size: 26rpx;
  font-weight: 800;
  justify-content: center;
  margin-left: 0;
  margin-right: 0;
  min-height: 72rpx;
}

.plain-btn.favorite {
  border-color: #f59e0b;
  color: #b45309;
}

.plain-btn.compact {
  margin-top: 0;
  width: 180rpx;
}

.nav-row {
  margin-top: 18rpx;
}

.muted {
  color: #64748b;
  display: block;
  font-size: 24rpx;
  line-height: 1.45;
  word-break: break-word;
}
</style>
