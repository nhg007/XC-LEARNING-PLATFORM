<template>
  <view class="page">
    <view class="header">
      <text class="title">学习记录</text>
      <text class="subtitle">最近学习、每日统计和总体进度</text>
    </view>

    <view class="summary">
      <view>
        <text class="label">总时长</text>
        <text class="value">{{ formatDuration(summary.totalStudySeconds) }}</text>
      </view>
      <view>
        <text class="label">正确率</text>
        <text class="value">{{ summary.overallAccuracyRate }}%</text>
      </view>
      <view>
        <text class="label">练习</text>
        <text class="value">{{ summary.totalExerciseCount }}</text>
      </view>
      <view>
        <text class="label">最长连续</text>
        <text class="value">{{ summary.longestStreakDays }}天</text>
      </view>
    </view>

    <view class="section">
      <text class="section-title">每日统计</text>
      <view v-for="item in dailyStats" :key="item.statDate" class="record-card">
        <text class="item-title">{{ item.statDate }}</text>
        <text class="muted">学习 {{ formatDuration(item.studySeconds) }} · 做题 {{ item.exerciseCount }} · 背词 {{ item.vocabReviewCount }}</text>
        <text class="muted">正确 {{ item.correctCount }} · 正确率 {{ item.accuracyRate }}%</text>
      </view>
    </view>

    <view class="section">
      <text class="section-title">学习行为</text>
      <view v-for="item in events" :key="item.id" class="record-card">
        <text class="item-title">{{ eventTypeLabel(item.eventType) }} · {{ resultLabel(item.result) }}</text>
        <text class="muted">{{ formatDuration(item.durationSeconds) }} · {{ formatTime(item.occurredAt) }}</text>
      </view>
      <button v-if="events.length < total" class="plain-btn" :loading="loadingMore" @click="loadMore">加载更多</button>
      <text v-if="events.length === 0" class="muted">暂无学习行为</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { fetchDailyStats, fetchLearningSummary, fetchStudyEvents } from '../../api/stats'
import type { DailyStat, LearningSummary, StudyEvent } from '../../types/api'

const page = ref(1)
const pageSize = 20
const total = ref(0)
const loadingMore = ref(false)
const summary = ref<LearningSummary>({
  totalStudySeconds: 0,
  totalExerciseCount: 0,
  totalCorrectCount: 0,
  totalVocabReviewCount: 0,
  currentStreakDays: 0,
  longestStreakDays: 0,
  overallAccuracyRate: 0,
  lastStudyDate: null
})
const dailyStats = ref<DailyStat[]>([])
const events = ref<StudyEvent[]>([])

onShow(() => {
  void loadRecords()
})

async function loadRecords() {
  page.value = 1
  const [summaryData, dailyData, eventPage] = await Promise.all([
    fetchLearningSummary(),
    fetchDailyStats(7),
    fetchStudyEvents(1, pageSize)
  ])
  summary.value = summaryData
  dailyStats.value = dailyData
  events.value = eventPage.records
  total.value = eventPage.total
}

async function loadMore() {
  loadingMore.value = true
  try {
    page.value += 1
    const eventPage = await fetchStudyEvents(page.value, pageSize)
    events.value = events.value.concat(eventPage.records)
    total.value = eventPage.total
  } finally {
    loadingMore.value = false
  }
}

function formatDuration(seconds: number) {
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  return hours > 0 ? `${hours}小时${minutes}分` : `${minutes}分`
}

function formatTime(value: string) {
  return new Date(value).toLocaleString()
}

function eventTypeLabel(type: string) {
  const labels: Record<string, string> = {
    exercise: '练习',
    vocab: '背词',
    dialogue: '台词',
    matching_game: '连连看'
  }
  return labels[type] || type
}

function resultLabel(result: string | null) {
  const labels: Record<string, string> = {
    correct: '正确',
    wrong: '错误',
    completed: '完成'
  }
  return result ? labels[result] || result : '-'
}
</script>

<style scoped>
.page {
  padding: 24rpx;
}

.header {
  margin-bottom: 24rpx;
}

.title {
  display: block;
  font-size: 42rpx;
  font-weight: 700;
}

.subtitle,
.muted {
  color: #64748b;
  display: block;
  font-size: 24rpx;
  margin-top: 8rpx;
}

.summary {
  display: flex;
  flex-wrap: wrap;
  gap: 14rpx;
}

.summary > view,
.record-card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8rpx;
  box-sizing: border-box;
}

.summary > view {
  padding: 20rpx;
  width: calc(50% - 7rpx);
}

.label {
  color: #64748b;
  display: block;
  font-size: 24rpx;
}

.value {
  display: block;
  font-size: 34rpx;
  font-weight: 700;
  margin-top: 8rpx;
}

.section {
  margin-top: 28rpx;
}

.section-title {
  display: block;
  font-size: 32rpx;
  font-weight: 700;
  margin-bottom: 14rpx;
}

.record-card {
  margin-bottom: 14rpx;
  padding: 20rpx;
}

.item-title {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
}
</style>
