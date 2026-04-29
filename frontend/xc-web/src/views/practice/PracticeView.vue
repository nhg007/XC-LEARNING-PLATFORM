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

    <section v-if="!activeSet" class="set-grid">
      <v-progress-linear v-if="loading" class="grid-loader" color="primary" indeterminate />
      <v-card
        v-for="item in sets"
        :key="item.id"
        class="set-card"
        elevation="0"
        @click="selectSet(item)"
      >
        <span class="set-type">{{ typeLabel(item.exerciseType) }}</span>
        <h2>{{ item.title }}</h2>
        <p>{{ item.level || t('common.basic') }}</p>
      </v-card>
      <div v-if="sets.length === 0 && !loading" class="empty-state">{{ t('practice.noSets') }}</div>
    </section>

    <section v-else class="practice-layout">
      <v-progress-linear v-if="loading" class="layout-loader" color="primary" indeterminate />
      <v-card class="question-panel" elevation="0">
        <div class="question-meta">
          <span class="question-type">{{ typeLabel(currentQuestion?.exerciseType || activeSet.exerciseType) }}</span>
          <span class="question-count">{{ questionIndex + 1 }}/{{ questions.length }}</span>
        </div>

        <template v-if="currentQuestion">
          <div class="prompt">
            <v-btn
              v-if="needsAudio"
              prepend-icon="mdi-volume-high"
              :loading="audioLoading || speech.speaking.value"
              variant="tonal"
              @click="playQuestionAudio"
            >
              {{ t('practice.playAudio') }}
            </v-btn>
            <strong v-if="currentQuestion.pinyinPrompt">{{ currentQuestion.pinyinPrompt }}</strong>
            <strong v-if="currentQuestion.exerciseType === 'translation_order'">{{ translationPrompt }}</strong>
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
        <v-btn block :disabled="questionIndex <= 0" variant="tonal" @click="previousQuestion">{{ t('practice.previous') }}</v-btn>
        <v-btn block :disabled="questionIndex >= questions.length - 1" variant="tonal" @click="nextQuestion">{{ t('practice.next') }}</v-btn>
        <v-btn block variant="text" @click="activeSet = null">{{ t('practice.changeSet') }}</v-btn>
      </v-card>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import LocaleSwitch from '@/components/LocaleSwitch.vue'
import { useSpeechPlayer } from '@/composables/useSpeechPlayer'
import { checkExercise, fetchExerciseAnswer, fetchExerciseQuestions, fetchExerciseSets } from '../../api/exercise'
import { usePreferenceStore } from '../../stores/preferences'
import type { ExerciseAnswer, ExerciseCheckResult, ExerciseSet, SentenceExercise } from '../../types/api'
import { notifyWarning } from '../../utils/notify'

const { t } = useI18n()
const preferences = usePreferenceStore()
const speech = useSpeechPlayer()
const loading = ref(false)
const submitting = ref(false)
const audioLoading = ref(false)
const sets = ref<ExerciseSet[]>([])
const activeSet = ref<ExerciseSet | null>(null)
const questions = ref<SentenceExercise[]>([])
const questionIndex = ref(0)
const answerText = ref('')
const selectedWords = ref<string[]>([])
const result = ref<ExerciseCheckResult | null>(null)
const answer = ref<ExerciseAnswer | null>(null)
const meaningLanguage = ref<'ru' | 'en'>('ru')
const questionStartedAt = ref(Date.now())
const audioAnswerCache = new Map<number, { text: string; audioUrl: string | null }>()

const currentQuestion = computed(() => questions.value[questionIndex.value])
const needsAudio = computed(() => currentQuestion.value?.exerciseType === 'audio_order' || currentQuestion.value?.exerciseType === 'audio_dictation')
const headerText = computed(() => activeSet.value ? activeSet.value.title : t('practice.selectSet'))
const translationPrompt = computed(() => {
  const question = currentQuestion.value
  if (!question) {
    return ''
  }
  return meaningLanguage.value === 'ru' ? question.translationRu || '' : question.translationEn || ''
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
    const page = await fetchExerciseSets()
    sets.value = page.records
  } finally {
    loading.value = false
  }
}

async function selectSet(set: ExerciseSet) {
  activeSet.value = set
  loading.value = true
  try {
    const page = await fetchExerciseQuestions(set.id)
    questions.value = page.records
    questionIndex.value = 0
    resetAnswer()
  } finally {
    loading.value = false
  }
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

async function playQuestionAudio() {
  const question = currentQuestion.value
  if (!question) {
    return
  }
  if (question.audioUrl) {
    speech.playTargetText('', question.audioUrl)
    return
  }
  const cached = audioAnswerCache.get(question.id)
  if (cached) {
    speech.playTargetText(cached.text, cached.audioUrl)
    return
  }
  audioLoading.value = true
  try {
    const questionAnswer = await fetchExerciseAnswer(question.id)
    audioAnswerCache.set(question.id, {
      text: questionAnswer.hanziAnswer,
      audioUrl: questionAnswer.audioUrl
    })
    speech.playTargetText(questionAnswer.hanziAnswer, questionAnswer.audioUrl)
  } finally {
    audioLoading.value = false
  }
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

function typeLabel(type: string) {
  return t(`practice.types.${type}`)
}

function elapsedSeconds() {
  const seconds = Math.round((Date.now() - questionStartedAt.value) / 1000)
  return Math.min(Math.max(seconds, 1), 24 * 60 * 60)
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
  justify-content: space-between;
  margin-bottom: 28px;
  padding-bottom: 18px;
}

.question-count {
  color: #475569;
  font-size: 15px;
  font-weight: 700;
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
.option-words :deep(.v-btn),
.prompt :deep(.v-btn) {
  border-radius: 4px;
  letter-spacing: 0;
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

  .prompt strong {
    font-size: 24px;
  }
}
</style>
