<template>
  <view class="page">
    <view class="header">
      <text class="title">句子练习</text>
      <text class="subtitle">{{ headerText }}</text>
    </view>

    <view v-if="!activeSet" class="section">
      <view v-if="loading" class="empty">加载中...</view>
      <view v-else-if="sets.length === 0" class="empty">暂无练习题集</view>
      <view v-else class="set-list">
        <view v-for="item in sets" :key="item.id" class="set-item" @click="selectSet(item)">
          <view>
            <text class="set-title">{{ item.title }}</text>
            <text class="muted">{{ typeLabel(item.exerciseType) }} · {{ item.level || '默认' }}</text>
          </view>
          <text class="arrow">></text>
        </view>
      </view>
    </view>

    <view v-else class="practice">
      <view class="toolbar">
        <button class="plain-btn" size="mini" @click="switchSet">题集</button>
        <text class="muted">{{ questionIndex + 1 }} / {{ questions.length }}</text>
      </view>

      <view v-if="!currentQuestion" class="empty">当前题集暂无题目</view>
      <view v-else class="panel">
        <view class="meta">
          <text>{{ typeLabel(currentQuestion.exerciseType) }}</text>
          <text>序号 {{ currentQuestion.sortOrder }}</text>
        </view>

        <text class="prompt">{{ promptText }}</text>

        <view v-if="currentQuestion.wordOptions.length > 0" class="word-area">
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
            <text v-if="selectedWords.length === 0" class="muted">按正确顺序选择词语</text>
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
          placeholder="输入你的答案"
        />

        <view v-if="result" :class="['result', result.correct ? 'success' : 'danger']">
          <text>{{ result.correct ? '回答正确' : '还差一点' }}</text>
          <text v-if="!result.correct" class="standard">标准答案：{{ result.standardAnswer }}</text>
          <text class="muted">{{ result.message }}</text>
        </view>

        <view v-if="answer" class="answer-card">
          <text class="answer-title">答案</text>
          <text>{{ answer.hanziAnswer }}</text>
          <text v-if="answer.translationRu" class="muted">{{ answer.translationRu }}</text>
          <text v-if="answer.translationEn" class="muted">{{ answer.translationEn }}</text>
        </view>

        <button class="primary-btn" :disabled="submitting || !canSubmit" @click="submitAnswer">
          {{ submitting ? '提交中...' : '提交答案' }}
        </button>
        <button class="plain-btn full" :disabled="answerLoading" @click="showAnswer">
          {{ answerLoading ? '加载中...' : '查看答案' }}
        </button>

        <view class="nav-row">
          <button class="plain-btn" size="mini" :disabled="questionIndex === 0" @click="previousQuestion">
            上一题
          </button>
          <button class="plain-btn" size="mini" :disabled="questionIndex >= questions.length - 1" @click="nextQuestion">
            下一题
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { checkExercise, fetchExerciseAnswer, fetchExerciseQuestions, fetchExerciseSets } from '../../api/exercise'
import type { ExerciseAnswer, ExerciseCheckResult, ExerciseSet, SentenceExercise } from '../../types/api'

const loading = ref(false)
const submitting = ref(false)
const answerLoading = ref(false)
const sets = ref<ExerciseSet[]>([])
const activeSet = ref<ExerciseSet | null>(null)
const questions = ref<SentenceExercise[]>([])
const questionIndex = ref(0)
const answerText = ref('')
const selectedWords = ref<string[]>([])
const result = ref<ExerciseCheckResult | null>(null)
const answer = ref<ExerciseAnswer | null>(null)

const currentQuestion = computed(() => questions.value[questionIndex.value] || null)
const headerText = computed(() => activeSet.value?.title || '选择题集开始练习')
const promptText = computed(() => {
  const question = currentQuestion.value
  if (!question) {
    return ''
  }
  return question.pinyinPrompt || question.translationRu || question.translationEn || '请根据音频或提示完成句子'
})
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
  if (question.wordOptions.length > 0) {
    return selectedWords.value.length > 0
  }
  return answerText.value.trim().length > 0
})

onLoad(() => {
  void loadSets()
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

async function selectSet(item: ExerciseSet) {
  activeSet.value = item
  questionIndex.value = 0
  resetAnswerState()
  const data = await fetchExerciseQuestions(item.id)
  questions.value = data.records
}

function switchSet() {
  activeSet.value = null
  questions.value = []
  questionIndex.value = 0
  resetAnswerState()
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
  } finally {
    answerLoading.value = false
  }
}

function buildPayload(question: SentenceExercise) {
  if (question.wordOptions.length > 0) {
    return { orderedWords: selectedWords.value }
  }
  return { answerText: answerText.value }
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
  answerText.value = ''
  selectedWords.value = []
  result.value = null
  answer.value = null
}

function typeLabel(type: string) {
  const map: Record<string, string> = {
    word_order: '词语排序',
    dictation: '听写',
    pinyin_to_hanzi: '拼音写汉字',
    translation_to_hanzi: '翻译成中文'
  }
  return map[type] || type
}
</script>

<style scoped>
.page {
  padding: 24rpx;
}

.header {
  margin-bottom: 28rpx;
}

.title {
  display: block;
  font-size: 42rpx;
  font-weight: 700;
}

.subtitle {
  color: #64748b;
  display: block;
  font-size: 26rpx;
  margin-top: 10rpx;
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
.panel,
.answer-card,
.result {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8rpx;
  box-sizing: border-box;
}

.set-item {
  align-items: center;
  display: flex;
  justify-content: space-between;
  padding: 24rpx;
}

.set-title {
  display: block;
  font-size: 32rpx;
  font-weight: 700;
  margin-bottom: 8rpx;
}

.arrow {
  color: #94a3b8;
  font-size: 42rpx;
}

.toolbar,
.meta,
.nav-row {
  align-items: center;
  display: flex;
  justify-content: space-between;
}

.toolbar {
  margin-bottom: 18rpx;
}

.panel {
  padding: 24rpx;
}

.meta {
  color: #64748b;
  font-size: 24rpx;
  margin-bottom: 20rpx;
}

.prompt {
  display: block;
  font-size: 38rpx;
  font-weight: 700;
  line-height: 1.45;
  margin-bottom: 24rpx;
}

.word-area {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
  margin-bottom: 24rpx;
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
  padding: 18rpx;
}

.answer-title {
  font-weight: 700;
}

.primary-btn,
.full {
  margin-top: 20rpx;
  width: 100%;
}

.primary-btn {
  background: #2563eb;
  color: #ffffff;
}

.plain-btn {
  margin-left: 0;
  margin-right: 0;
}

.nav-row {
  margin-top: 18rpx;
}

.empty,
.muted {
  color: #64748b;
  font-size: 24rpx;
}
</style>
