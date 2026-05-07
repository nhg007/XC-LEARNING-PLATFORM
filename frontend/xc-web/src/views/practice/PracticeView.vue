<template>
  <main class="app-shell practice-shell">
    <header class="topbar practice-hero">
      <div>
        <h1>{{ t('practice.title') }}</h1>
        <p>{{ headerText }}</p>
      </div>
      <div class="top-actions">
        <LocaleSwitch />
        <v-btn prepend-icon="mdi-arrow-left" variant="text" @click="$router.push('/')">{{ t('common.back') }}</v-btn>
        <v-btn prepend-icon="mdi-refresh" variant="tonal" :loading="loading" @click="loadSets">{{ t('common.refresh') }}</v-btn>
      </div>
    </header>

    <section v-if="!activeSet" class="selection-area">
      <v-progress-linear v-if="loading" class="grid-loader" color="primary" indeterminate />
      <template v-if="!selectedExerciseType">
        <div class="selection-heading">
          <span>{{ t('practice.stepMode') }}</span>
          <strong>{{ t('practice.chooseMode') }}</strong>
        </div>
        <div class="set-grid">
          <v-card
            v-for="item in practiceTypeCards"
            :key="item.type"
            class="set-card mode-card"
            :class="{ disabled: item.count === 0 }"
            elevation="0"
            @click="selectExerciseType(item.type)"
          >
            <span class="set-type">{{ typeLabel(item.type) }}</span>
            <h2>{{ typeLabel(item.type) }}</h2>
            <p>{{ t('practice.availableLevelCount', { count: item.levelCount }) }}</p>
          </v-card>
          <div v-if="sets.length === 0 && !loading" class="empty-state">{{ t('practice.noSets') }}</div>
        </div>
      </template>

      <template v-else>
        <div class="selection-heading">
          <span>{{ typeLabel(selectedExerciseType) }}</span>
          <strong>{{ t('practice.chooseLevel') }}</strong>
          <v-btn prepend-icon="mdi-arrow-left" variant="text" @click="clearSelectedExerciseType">{{ t('practice.changeMode') }}</v-btn>
        </div>
        <div class="set-grid">
          <v-card
            v-for="item in levelCards"
            :key="item.key"
            class="set-card"
            elevation="0"
            @click="selectLevelCard(item)"
          >
            <span class="set-type">{{ typeLabel(selectedExerciseType) }}</span>
            <h2>{{ item.label }}</h2>
            <p>{{ t('practice.setCount', { count: item.sets.length }) }}</p>
          </v-card>
          <div v-if="levelCards.length === 0 && !loading" class="empty-state">{{ t('practice.noLevels') }}</div>
        </div>
      </template>
    </section>

    <template v-else>
      <v-card v-if="childSets.length > 0" class="scope-panel" elevation="0">
        <v-tabs v-model="scopeTab" color="primary">
          <v-tab value="all">{{ t('practice.scopeAll') }}</v-tab>
          <v-tab value="lessons">{{ t('practice.scopeLessons') }}</v-tab>
        </v-tabs>
      </v-card>

      <section v-if="scopeTab === 'lessons' && childSets.length > 0" class="lesson-grid">
        <v-card v-for="lesson in childSets" :key="lesson.id" class="lesson-card" elevation="0" @click="selectLessonSet(lesson)">
          <span class="set-type">{{ typeLabel(lesson.exerciseType) }}</span>
          <h2>{{ lesson.title }}</h2>
          <p>{{ lesson.level || activeSet.level || t('common.basic') }}</p>
          <div class="lesson-footer">
            <span>{{ t('practice.questionCount', { count: lesson.totalActiveQuestionCount || lesson.activeQuestionCount }) }}</span>
            <v-btn append-icon="mdi-arrow-right" variant="text">{{ t('practice.start') }}</v-btn>
          </div>
        </v-card>
      </section>

      <section v-else class="practice-layout">
      <v-progress-linear v-if="loading" class="layout-loader" color="primary" indeterminate />
      <v-card class="question-panel" elevation="0">
        <div class="question-meta">
          <span class="question-type">{{ typeLabel(currentQuestion?.exerciseType || activeSet.exerciseType) }}</span>
          <span class="question-status" :class="{ learned: isCurrentQuestionLearned }">{{ currentQuestionStatusLabel }}</span>
          <span class="question-count">{{ questionIndex + 1 }}/{{ questions.length }}</span>
        </div>

        <template v-if="currentQuestion">
          <div class="prompt">
            <div
              v-if="needsAudio"
              class="audio-wave-player"
              :class="{ loading: audioLoading && !speech.speaking.value }"
            >
              <button
                class="wave-play-button"
                :aria-label="questionAudioLabel"
                type="button"
                @click="toggleQuestionAudio"
              >
                <v-icon :icon="questionAudioIcon" size="20" />
              </button>
              <span class="wave-time">{{ currentAudioTime }}</span>
              <div class="waveform" :aria-label="t('practice.audioWaveform')" role="group">
                <button
                  v-for="bar in waveformBars"
                  :key="bar.index"
                  class="wave-bar"
                  :class="{ active: bar.fraction <= audioProgress }"
                  :disabled="!canSeekAudio && !canPrepareSeekAudio"
                  :aria-label="t('practice.seekAudio', { percent: Math.round(bar.fraction * 100) })"
                  type="button"
                  @click="seekQuestionAudio(bar.fraction)"
                >
                  <span :style="{ height: `${bar.height}%` }" />
                </button>
              </div>
              <span class="wave-time">{{ totalAudioTime }}</span>
            </div>
            <strong v-if="questionPromptText">{{ questionPromptText }}</strong>
          </div>

          <div v-if="currentQuestion.wordOptions.length > 0" class="word-zone">
            <div class="selected-words">
              <v-btn v-for="(word, index) in selectedWords" :key="`${word}-${index}`" variant="tonal" @click="removeWord(index)">
                {{ word }}
              </v-btn>
            </div>
            <div class="option-words">
              <v-btn v-for="option in availableOptions" :key="option.id" color="secondary" variant="tonal" @click="selectWord(option.wordText)">
                {{ option.wordText }}
              </v-btn>
            </div>
          </div>

          <v-textarea
            v-else
            v-model="answerText"
            auto-grow
            counter="4000"
            :label="t('practice.answerPlaceholder')"
            maxlength="4000"
            rows="8"
            variant="outlined"
          />

          <div v-if="result" class="result" :class="{ ok: result.correct }">
            <strong>{{ result.message }}</strong>
            <span>{{ t('practice.standardAnswer', { answer: result.standardAnswer }) }}</span>
          </div>

          <div v-if="answer" class="answer-box">
            <strong>{{ answer.hanziAnswer }}</strong>
            <span v-if="answerPinyinText" class="answer-pinyin">{{ answerPinyinText }}</span>
            <span v-if="answer.explanation">{{ answer.explanation }}</span>
            <span>{{ meaningLanguage === 'ru' ? answer.translationRu : answer.translationEn }}</span>
          </div>
        </template>
      </v-card>

      <v-card class="side-panel" elevation="0">
        <span class="control-label">{{ t('common.referenceLanguage') }}</span>
        <v-btn-toggle v-model="meaningLanguage" class="language-toggle" color="primary" density="comfortable" mandatory variant="outlined">
          <v-btn value="ru">{{ t('common.russian') }}</v-btn>
          <v-btn value="en">{{ t('common.english') }}</v-btn>
        </v-btn-toggle>
        <v-btn block color="primary" :disabled="!currentQuestion" :loading="submitting" @click="submitAnswer">
          {{ t('practice.submit') }}
        </v-btn>
        <v-btn block :disabled="!currentQuestion" variant="tonal" @click="showAnswer">{{ t('practice.showAnswer') }}</v-btn>
        <v-btn
          block
          :color="currentQuestion?.favorite ? 'warning' : undefined"
          :disabled="!currentQuestion"
          :loading="favoriteActionId === currentQuestion?.id"
          variant="tonal"
          @click="toggleQuestionFavorite"
        >
          {{ currentQuestion?.favorite ? t('vocab.unfavorite') : t('vocab.favorite') }}
        </v-btn>
        <v-btn block :disabled="questionIndex <= 0" variant="tonal" @click="previousQuestion">{{ t('practice.previous') }}</v-btn>
        <v-btn block :disabled="questionIndex >= questions.length - 1" variant="tonal" @click="nextQuestion">{{ t('practice.next') }}</v-btn>
        <v-btn block variant="text" @click="switchSet">{{ t('practice.changeSet') }}</v-btn>
      </v-card>
      </section>
    </template>
  </main>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import LocaleSwitch from '@/components/LocaleSwitch.vue'
import { useSpeechPlayer } from '@/composables/useSpeechPlayer'
import { checkExercise, favoriteSentenceExercise, fetchExerciseAnswer, fetchExerciseQuestions, fetchExerciseSets, unfavoriteSentenceExercise } from '../../api/exercise'
import { usePreferenceStore } from '../../stores/preferences'
import type { ExerciseAnswer, ExerciseCheckResult, ExerciseSet, SentenceExercise, SentenceProgressStatus } from '../../types/api'
import { notifySuccess, notifyWarning } from '../../utils/notify'

const { t } = useI18n()
const preferences = usePreferenceStore()
const speech = useSpeechPlayer()
const loading = ref(false)
const submitting = ref(false)
const favoriteActionId = ref<number | null>(null)
const audioLoading = ref(false)
const sets = ref<ExerciseSet[]>([])
const activeSet = ref<ExerciseSet | null>(null)
const childSets = ref<ExerciseSet[]>([])
const scopeTab = ref<'all' | 'lessons'>('all')
const selectedExerciseType = ref<string | null>(null)
const questions = ref<SentenceExercise[]>([])
const questionIndex = ref(0)
const answerText = ref('')
const selectedWords = ref<string[]>([])
const result = ref<ExerciseCheckResult | null>(null)
const answer = ref<ExerciseAnswer | null>(null)
const meaningLanguage = ref<'ru' | 'en'>('ru')
const questionStartedAt = ref(Date.now())
const audioAnswerCache = new Map<number, { text: string; audioUrl: string | null }>()
const pendingSeekFraction = ref<number | null>(null)
const preferredExerciseTypes = ['audio_order', 'translation_order', 'audio_dictation', 'pinyin_dictation']
const learnedSentenceStatuses = new Set<SentenceProgressStatus>(['learned', 'reviewing', 'mastered'])
const waveformBars = Array.from({ length: 32 }, (_, index) => {
  const wave = Math.sin(index * 0.82) * 18 + Math.sin(index * 0.31 + 1.4) * 13
  return {
    index,
    fraction: index / 31,
    height: Math.round(Math.min(Math.max(42 + Math.abs(wave), 22), 82))
  }
})

const currentQuestion = computed(() => questions.value[questionIndex.value])
const isCurrentQuestionLearned = computed(() => {
  const status = currentQuestion.value?.progressStatus
  return Boolean(status && learnedSentenceStatuses.has(status))
})
const currentQuestionStatusLabel = computed(() => statusLabel(currentQuestion.value?.progressStatus || null))
const needsAudio = computed(() => currentQuestion.value?.exerciseType === 'audio_order' || currentQuestion.value?.exerciseType === 'audio_dictation')
const headerText = computed(() => {
  if (activeSet.value) {
    return activeSet.value.title
  }
  if (selectedExerciseType.value) {
    return t('practice.selectLevelForMode', { type: typeLabel(selectedExerciseType.value) })
  }
  return t('practice.chooseMode')
})
const questionAudioIcon = computed(() => {
  if (speech.speaking.value) {
    return 'mdi-pause'
  }
  return audioLoading.value ? 'mdi-loading' : 'mdi-play'
})
const questionAudioLabel = computed(() => {
  if (speech.speaking.value) {
    return t('common.pause')
  }
  if (speech.paused.value) {
    return t('practice.resumeAudio')
  }
  return audioLoading.value ? t('practice.audioPreparing') : t('practice.playAudio')
})
const audioProgress = computed(() => {
  if (!speech.duration.value) {
    return 0
  }
  return Math.min(Math.max(speech.currentTime.value / speech.duration.value, 0), 1)
})
const currentAudioTime = computed(() => formatAudioTime(speech.currentTime.value))
const totalAudioTime = computed(() => formatAudioTime(speech.duration.value))
const canSeekAudio = computed(() => speech.seekable.value && speech.duration.value > 0)
const canPrepareSeekAudio = computed(() => Boolean(currentQuestion.value?.audioUrl || audioAnswerCache.get(currentQuestion.value?.id || 0)?.audioUrl))
const questionPromptText = computed(() => {
  const question = currentQuestion.value
  if (!question || needsAudio.value) {
    return ''
  }
  if (question.pinyinPrompt) {
    return question.pinyinPrompt
  }
  return meaningLanguage.value === 'ru' ? question.translationRu || '' : question.translationEn || ''
})
const answerPinyinText = computed(() => answer.value?.pinyinPrompt || currentQuestion.value?.pinyinPrompt || '')
const availableExerciseTypes = computed(() => {
  const currentTypes = sets.value.map(item => item.exerciseType).filter(Boolean)
  return [...preferredExerciseTypes, ...currentTypes.filter(type => !preferredExerciseTypes.includes(type))]
})
const practiceTypeCards = computed(() => availableExerciseTypes.value.map(type => {
  const typeSets = sets.value.filter(item => item.exerciseType === type)
  return {
    type,
    count: typeSets.length,
    levelCount: new Set(typeSets.map(item => item.level || '')).size
  }
}))
const setsForSelectedType = computed(() => {
  if (!selectedExerciseType.value) {
    return []
  }
  return sets.value.filter(item => item.exerciseType === selectedExerciseType.value)
})
const levelCards = computed(() => {
  return setsForSelectedType.value
    .map(set => ({
      key: String(set.id),
      label: set.title,
      sets: [set],
      level: set.level || ''
    }))
    .sort((left, right) => levelSortWeight(left.level) - levelSortWeight(right.level) || left.label.localeCompare(right.label))
})
const availableOptions = computed(() => {
  const question = currentQuestion.value
  if (!question) {
    return []
  }
  const usedCounts = selectedWords.value.reduce<Record<string, number>>((counts, word) => {
    counts[word] = (counts[word] || 0) + 1
    return counts
  }, {})
  const seenCounts: Record<string, number> = {}
  return question.wordOptions.filter((option) => {
    const seen = seenCounts[option.wordText] || 0
    seenCounts[option.wordText] = seen + 1
    return seen >= (usedCounts[option.wordText] || 0)
  })
})

async function loadSets() {
  loading.value = true
  try {
    const page = await fetchExerciseSets({ pageSize: 100 })
    sets.value = page.records
    if (selectedExerciseType.value && !sets.value.some(item => item.exerciseType === selectedExerciseType.value)) {
      selectedExerciseType.value = null
    }
  } finally {
    loading.value = false
  }
}

function selectExerciseType(type: string) {
  if (!sets.value.some(item => item.exerciseType === type)) {
    return
  }
  selectedExerciseType.value = type
}

function clearSelectedExerciseType() {
  selectedExerciseType.value = null
  resetAnswer()
}

function selectLevelCard(card: { sets: ExerciseSet[] }) {
  const firstSet = card.sets[0]
  if (firstSet) {
    void selectSet(firstSet)
  }
}

async function selectSet(set: ExerciseSet) {
  activeSet.value = set
  scopeTab.value = 'all'
  loading.value = true
  try {
    const [children, page] = await Promise.all([
      fetchExerciseSets({ pageSize: 100, parentId: set.id, exerciseType: set.exerciseType }),
      fetchExerciseQuestions(set.id)
    ])
    childSets.value = children.records
    questions.value = page.records
    questionIndex.value = 0
    resetAnswer()
  } finally {
    loading.value = false
  }
}

async function selectLessonSet(set: ExerciseSet) {
  await selectSet(set)
}

function switchSet() {
  activeSet.value = null
  childSets.value = []
  scopeTab.value = 'all'
  questions.value = []
  questionIndex.value = 0
  resetAnswer()
}

async function submitAnswer() {
  const question = currentQuestion.value
  if (!question) {
    return
  }
  if (question.wordOptions.length > 0 && selectedWords.value.length === 0) {
    notifyWarning(t('practice.selectWordsWarning'))
    return
  }
  if (question.wordOptions.length === 0 && !answerText.value.trim()) {
    notifyWarning(t('practice.answerRequired'))
    return
  }
  submitting.value = true
  try {
    result.value = await checkExercise(question.id, {
      answerText: question.wordOptions.length > 0 ? undefined : answerText.value,
      orderedWords: question.wordOptions.length > 0 ? selectedWords.value : undefined,
      translationLanguage: meaningLanguage.value,
      showedAnswer: Boolean(answer.value),
      durationSeconds: elapsedSeconds()
    })
    applyQuestionProgress(question, result.value)
  } finally {
    submitting.value = false
  }
}

async function showAnswer() {
  const question = currentQuestion.value
  if (!question) {
    return
  }
  answer.value = await fetchExerciseAnswer(question.id)
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
    notifySuccess(result.favorite ? t('vocab.favorited') : t('vocab.unfavorited'))
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
  const cached = audioAnswerCache.get(question.id)
  if (cached) {
    return cached
  }
  audioLoading.value = true
  try {
    const questionAnswer = await fetchExerciseAnswer(question.id)
    const source = {
      text: questionAnswer.hanziAnswer,
      audioUrl: questionAnswer.audioUrl
    }
    audioAnswerCache.set(question.id, source)
    return source
  } finally {
    audioLoading.value = false
  }
}

async function playQuestionAudio() {
  const source = await getQuestionAudioSource()
  if (!source) {
    return
  }
  speech.playTargetText(source.text, source.audioUrl)
}

function toggleQuestionAudio() {
  if (speech.speaking.value) {
    speech.pause()
    return
  }
  if (speech.paused.value) {
    speech.resume()
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
    speech.seekByFraction(fraction, true)
    return
  }
  const source = await getQuestionAudioSource()
  if (!source?.audioUrl) {
    return
  }
  pendingSeekFraction.value = fraction
  speech.playTargetText(source.text, source.audioUrl)
}

function selectWord(word: string) {
  selectedWords.value.push(word)
  result.value = null
  answer.value = null
}

function removeWord(index: number) {
  selectedWords.value.splice(index, 1)
  result.value = null
  answer.value = null
}

function previousQuestion() {
  questionIndex.value = Math.max(0, questionIndex.value - 1)
  resetAnswer()
}

function nextQuestion() {
  questionIndex.value = Math.min(questions.value.length - 1, questionIndex.value + 1)
  resetAnswer()
}

function resetAnswer() {
  speech.stop()
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
  return t(`practice.types.${type}`)
}

function levelSortWeight(value: string) {
  const normalized = value.toUpperCase().replace(/\s+/g, '')
  const match = /^(YCT|HSK)(\d+)$/.exec(normalized)
  if (!match) {
    return 1000
  }
  const familyWeight = match[1] === 'YCT' ? 0 : 100
  return familyWeight + Number(match[2])
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
  () => preferences.preference?.translationLanguage,
  (value) => {
    if (value) {
      meaningLanguage.value = value
    }
  }
)

watch(meaningLanguage, (value) => {
  if (preferences.loaded && preferences.preference?.translationLanguage !== value) {
    void preferences.save({ translationLanguage: value })
  }
})

watch(
  () => speech.duration.value,
  (duration) => {
    if (duration > 0 && pendingSeekFraction.value !== null) {
      speech.seekByFraction(pendingSeekFraction.value, true)
      pendingSeekFraction.value = null
    }
  }
)

onMounted(async () => {
  await preferences.load()
  if (preferences.preference?.translationLanguage) {
    meaningLanguage.value = preferences.preference.translationLanguage
  }
  await loadSets()
})

onBeforeUnmount(speech.stop)
</script>

<style scoped>
h1 {
  font-size: 34px;
  line-height: 1.2;
  margin: 0;
}

h2 {
  font-size: 20px;
  line-height: 1.3;
  margin: 18px 0 8px;
}

p {
  color: #64748b;
  margin: 8px 0 0;
}

.practice-hero {
  background: #142033;
  border: 1px solid #23324a;
  border-radius: 8px;
  color: #f8fafc;
  margin-bottom: 22px;
  padding: 30px;
}

.practice-hero p {
  color: #cbd5e1;
}

.top-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
}

.top-actions :deep(.v-btn) {
  border-radius: 4px;
  letter-spacing: 0;
}

.top-actions :deep(.v-btn--variant-text) {
  color: #f8fafc;
}

.selection-area {
  display: grid;
  gap: 16px;
}

.selection-heading {
  align-items: center;
  background: #ffffff;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: space-between;
  padding: 18px 20px;
}

.selection-heading span {
  background: #eef4ff;
  border: 1px solid #c7d7fe;
  border-radius: 4px;
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 700;
  padding: 5px 9px;
}

.selection-heading strong {
  color: #172033;
  flex: 1;
  font-size: 18px;
}

.selection-heading :deep(.v-btn) {
  border-radius: 4px;
  letter-spacing: 0;
}

.set-grid {
  display: grid;
  gap: 18px;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
}

.grid-loader,
.layout-loader,
.empty-state {
  grid-column: 1 / -1;
}

.set-card,
.question-panel,
.side-panel {
  background: #ffffff;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
}

.set-card {
  cursor: pointer;
  min-height: 172px;
  padding: 24px;
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

.set-card:hover {
  border-color: #9db8e8;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
  transform: translateY(-2px);
}

.set-card.disabled {
  cursor: default;
  opacity: 0.55;
}

.set-card.disabled:hover {
  border-color: #dbe3ee;
  box-shadow: none;
  transform: none;
}

.scope-panel {
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  margin-bottom: 18px;
  padding: 0 12px;
}

.scope-panel :deep(.v-tab) {
  letter-spacing: 0;
}

.lesson-grid {
  display: grid;
  gap: 18px;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
}

.lesson-card {
  background: #ffffff;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  cursor: pointer;
  display: grid;
  gap: 12px;
  min-height: 172px;
  padding: 24px;
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

.lesson-card:hover {
  border-color: #9db8e8;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
  transform: translateY(-2px);
}

.lesson-card h2,
.lesson-card p {
  margin: 0;
}

.lesson-footer {
  align-items: center;
  align-self: end;
  border-top: 1px solid #e5edf6;
  color: #475569;
  display: flex;
  justify-content: space-between;
  padding-top: 12px;
}

.set-type,
.question-type {
  background: #eef4ff;
  border: 1px solid #c7d7fe;
  border-radius: 4px;
  color: #1d4ed8;
  display: inline-block;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.2;
  padding: 5px 9px;
}

.empty-state {
  align-items: center;
  color: #64748b;
  display: flex;
  justify-content: center;
  min-height: 160px;
}

.practice-layout {
  align-items: start;
  display: grid;
  gap: 22px;
  grid-template-columns: minmax(0, 1fr) 340px;
}

.question-panel {
  min-height: 560px;
  padding: 28px;
}

.question-meta {
  align-items: center;
  border-bottom: 1px solid #e5edf6;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: space-between;
  margin-bottom: 28px;
  padding-bottom: 18px;
}

.question-count {
  color: #475569;
  font-size: 15px;
  font-weight: 700;
  margin-left: auto;
}

.question-status {
  background: #f1f5f9;
  border: 1px solid #d7e2ea;
  border-radius: 4px;
  color: #475569;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.2;
  padding: 5px 9px;
}

.question-status.learned {
  background: #ecfdf5;
  border-color: #bbf7d0;
  color: #047857;
}

.prompt {
  align-items: center;
  background: #f8fafc;
  border: 1px solid #e5edf6;
  border-radius: 8px;
  display: flex;
  gap: 14px;
  margin-bottom: 22px;
  min-height: 116px;
  padding: 22px;
}

.prompt strong {
  font-size: 28px;
  line-height: 1.35;
}

.audio-wave-player {
  align-items: center;
  background: #ffffff;
  border: 1px solid #dbe3ee;
  border-radius: 999px;
  box-shadow: 0 8px 22px rgba(15, 23, 42, 0.06);
  display: grid;
  gap: 12px;
  grid-template-columns: 42px 48px minmax(0, 1fr) 48px;
  max-width: 100%;
  min-height: 60px;
  min-width: 0;
  overflow: hidden;
  padding: 8px 14px;
  width: 100%;
}

.wave-play-button {
  align-items: center;
  background: #2563eb;
  border: 0;
  border-radius: 999px;
  color: #ffffff;
  cursor: pointer;
  display: inline-flex;
  height: 38px;
  justify-content: center;
  padding: 0;
  transition:
    background 0.18s ease,
    transform 0.18s ease;
  width: 38px;
}

.wave-play-button:hover {
  background: #1d4ed8;
  transform: translateY(-1px);
}

.audio-wave-player.loading .wave-play-button :deep(.mdi-loading) {
  animation: audio-spin 0.85s linear infinite;
}

.wave-time {
  color: #64748b;
  font-size: 15px;
  font-variant-numeric: tabular-nums;
  font-weight: 700;
  text-align: center;
}

.waveform {
  align-items: center;
  display: grid;
  gap: 3px;
  grid-template-columns: repeat(32, minmax(2px, 1fr));
  height: 38px;
  min-width: 0;
}

.wave-bar {
  align-items: center;
  background: transparent;
  border: 0;
  cursor: pointer;
  display: flex;
  height: 38px;
  justify-content: center;
  margin: 0;
  padding: 0;
  width: 100%;
}

.wave-bar span {
  background: #dbeafe;
  border-radius: 999px;
  display: block;
  min-height: 8px;
  transition:
    background 0.16s ease,
    transform 0.16s ease;
  width: 100%;
}

.wave-bar.active span {
  background: #2563eb;
}

.wave-bar:hover span,
.wave-bar:focus-visible span {
  background: #60a5fa;
  transform: scaleY(1.08);
}

.wave-bar:disabled {
  cursor: default;
}

.wave-bar:disabled span {
  background: #e5edf6;
  opacity: 0.78;
}

.word-zone {
  display: grid;
  gap: 20px;
}

.selected-words,
.option-words {
  align-content: start;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  min-height: 132px;
  padding: 16px;
}

.selected-words {
  background: #f8fafc;
}

.option-words {
  background: #ffffff;
  min-height: 156px;
}

.selected-words :deep(.v-btn),
.option-words :deep(.v-btn) {
  border-radius: 4px;
  letter-spacing: 0;
}

@keyframes audio-spin {
  to {
    transform: rotate(360deg);
  }
}

.result,
.answer-box {
  border-radius: 8px;
  display: grid;
  gap: 8px;
  margin-top: 18px;
  padding: 14px;
}

.result {
  background: #fef2f2;
  color: #991b1b;
}

.result.ok {
  background: #ecfdf5;
  color: #047857;
}

.answer-box {
  background: #f8fafc;
  color: #334155;
}

.answer-pinyin {
  color: #64748b;
  font-size: 14px;
}

.side-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 22px;
  position: sticky;
  top: 70px;
}

.side-panel :deep(.v-btn) {
  border-radius: 4px;
  letter-spacing: 0;
  min-height: 44px;
}

.control-label {
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
}

.language-toggle {
  width: 100%;
}

.language-toggle :deep(.v-btn) {
  flex: 1;
}

@media (max-width: 980px) {
  .practice-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .top-actions {
    justify-content: flex-start;
  }

  .practice-layout {
    grid-template-columns: 1fr;
  }

  .side-panel {
    position: static;
  }
}

@media (max-width: 560px) {
  h1 {
    font-size: 28px;
  }

  .practice-hero,
  .question-panel,
  .side-panel {
    padding: 20px 16px;
  }

  .set-grid {
    grid-template-columns: 1fr;
  }

  .prompt {
    align-items: flex-start;
    flex-direction: column;
  }

  .audio-wave-player {
    gap: 8px;
    grid-template-columns: 36px 44px minmax(0, 1fr) 44px;
    padding: 8px 10px;
    width: 100%;
  }

  .wave-play-button {
    height: 34px;
    width: 34px;
  }

  .waveform {
    gap: 2px;
  }

  .wave-time {
    font-size: 13px;
  }

  .prompt strong {
    font-size: 24px;
  }
}
</style>
