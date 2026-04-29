<template>
  <view class="page">
    <view class="hero">
      <view>
        <text class="eyebrow">{{ t('records.eyebrow') }}</text>
        <text class="title">{{ t('records.title') }}</text>
        <text class="subtitle">{{ t('records.subtitle', { days: summary.currentStreakDays, date: summary.lastStudyDate || '-' }) }}</text>
      </view>
      <view class="hero-actions">
        <LanguageSwitch variant="hero" />
        <button class="icon-btn" size="mini" :loading="loading" @click="refreshRecords">{{ t('common.refresh') }}</button>
      </view>
    </view>

    <view v-if="errorMessage" class="state-card error-card">
      <text>{{ errorMessage }}</text>
      <button class="plain-btn compact" size="mini" @click="refreshRecords">{{ t('common.retry') }}</button>
    </view>

    <view v-else-if="loading && events.length === 0" class="state-card">{{ t('common.loading') }}</view>

    <template v-else>
      <view class="panel-tabs">
        <button
          v-for="item in recordPanels"
          :key="item.value"
          size="mini"
          :class="['panel-tab', { active: activePanel === item.value }]"
          @click="selectPanel(item.value)"
        >
          {{ t(item.label) }}
        </button>
      </view>

      <template v-if="activePanel === 'overview'">
        <view class="today-card">
          <view class="today-head">
            <view>
              <text class="label">{{ t('records.today') }}</text>
              <text class="today-date">{{ todayLabel }}</text>
            </view>
            <text v-if="!todayStat" class="today-empty">{{ t('records.todayEmpty') }}</text>
          </view>
          <view class="today-metrics">
            <view class="today-metric">
              <text class="label">{{ t('records.totalTime') }}</text>
              <text class="metric-value">{{ formatDuration(todayStat?.studySeconds) }}</text>
            </view>
            <view class="today-metric">
              <text class="label">{{ t('records.practice') }}</text>
              <text class="metric-value">{{ todayStat?.exerciseCount || 0 }}</text>
            </view>
            <view class="today-metric">
              <text class="label">{{ t('records.words') }}</text>
              <text class="metric-value">{{ todayStat?.vocabReviewCount || 0 }}</text>
            </view>
            <view class="today-metric">
              <text class="label">{{ t('records.accuracy') }}</text>
              <text class="metric-value">{{ formatPercent(todayStat?.accuracyRate) }}</text>
            </view>
          </view>
        </view>

        <view class="summary-grid">
          <view class="summary-card strong">
            <text class="label">{{ t('records.totalTime') }}</text>
            <text class="value">{{ formatDuration(summary.totalStudySeconds) }}</text>
          </view>
          <view class="summary-card">
            <text class="label">{{ t('records.accuracy') }}</text>
            <text class="value">{{ formatPercent(summary.overallAccuracyRate) }}</text>
          </view>
          <view class="summary-card">
            <text class="label">{{ t('records.practice') }}</text>
            <text class="value">{{ summary.totalExerciseCount }}</text>
          </view>
          <view class="summary-card">
            <text class="label">{{ t('records.words') }}</text>
            <text class="value">{{ summary.totalVocabReviewCount }}</text>
          </view>
        </view>

        <view class="week-card">
          <view class="section-head compact-head">
            <text class="section-title">{{ t('records.last7') }}</text>
            <text class="muted">{{ t('records.longest', { days: summary.longestStreakDays }) }}</text>
          </view>
          <view class="week-chart">
            <view v-for="item in visibleDailyStats" :key="item.statDate" class="day-column">
              <view class="day-bar-track">
                <view class="day-bar-fill" :style="{ height: dailyBarHeight(item.studySeconds) }" />
              </view>
              <text class="day-label">{{ formatDateLabel(item.statDate) }}</text>
              <text class="day-value">{{ formatDurationShort(item.studySeconds) }}</text>
            </view>
          </view>
        </view>

        <view class="module-card">
          <view class="section-head compact-head">
            <text class="section-title">{{ t('records.modules') }}</text>
            <text class="muted">{{ t('records.last7') }}</text>
          </view>
          <view class="module-list">
            <view v-for="item in moduleStats" :key="item.key" class="module-row">
              <view class="module-top">
                <text class="module-name">{{ item.label }}</text>
                <text class="module-value">{{ item.value }}</text>
              </view>
              <view class="module-bar">
                <view class="module-fill" :style="{ width: item.width }" />
              </view>
            </view>
          </view>
        </view>
      </template>

      <view v-else class="section events-section">
        <view class="section-head">
          <text class="section-title">{{ t('records.events') }}</text>
          <text class="muted">{{ events.length }} / {{ total }}</text>
        </view>
        <view class="filter-row">
          <button
            v-for="item in eventFilters"
            :key="item.value"
            size="mini"
            :class="['filter-btn', { active: activeEventType === item.value }]"
            @click="selectEventType(item.value)"
          >
            {{ t(item.label) }}
          </button>
        </view>

        <view v-if="events.length === 0" class="state-card">{{ t('records.emptyEvents') }}</view>
        <view v-for="item in events" :key="item.id" class="event-card">
          <view :class="['event-dot', item.eventType]" />
          <view class="event-body">
            <view class="event-title-row">
              <text class="item-title">{{ eventTypeLabel(item.eventType) }}</text>
              <text :class="['result-pill', resultTone(item.result)]">{{ resultLabel(item.result) }}</text>
            </view>
            <text class="muted">{{ formatDuration(item.durationSeconds) }} · {{ formatTime(item.occurredAt) }}</text>
            <text v-if="item.targetId !== null" class="event-target">{{ t('records.target', { id: item.targetId }) }}</text>
          </view>
        </view>

        <button v-if="canLoadMore" class="plain-btn full" :loading="loadingMore" @click="loadMore">{{ t('records.loadMore') }}</button>
        <text v-else-if="events.length > 0" class="end-text">{{ t('records.noMore') }}</text>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import LanguageSwitch from '../../components/LanguageSwitch.vue'
import { fetchDailyStats, fetchLearningSummary, fetchStudyEvents } from '../../api/stats'
import { applyTabBarLocale, setPageTitle, useI18n } from '../../i18n'
import type { DailyStat, LearningSummary, StudyEvent } from '../../types/api'
import { requireLogin } from '../../utils/navigation'

type EventFilterValue = '' | 'exercise' | 'vocab' | 'dialogue' | 'matching_game'
type RecordsPanel = 'overview' | 'events'

const { locale, t } = useI18n()
const page = ref(1)
const pageSize = 20
const total = ref(0)
const loading = ref(false)
const loadingMore = ref(false)
const errorMessage = ref('')
const activePanel = ref<RecordsPanel>('overview')
const activeEventType = ref<EventFilterValue>('')
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

const recordPanels: Array<{ label: string; value: RecordsPanel }> = [
  { label: 'records.overviewTab', value: 'overview' },
  { label: 'records.detailTab', value: 'events' }
]

const eventFilters: Array<{ label: string; value: EventFilterValue }> = [
  { label: 'records.all', value: '' },
  { label: 'records.practice', value: 'exercise' },
  { label: 'records.words', value: 'vocab' },
  { label: 'records.dialogue', value: 'dialogue' },
  { label: 'records.matching', value: 'matching_game' }
]

const canLoadMore = computed(() => events.value.length < total.value)
const maxDailySeconds = computed(() => Math.max(1, ...dailyStats.value.map(item => item.studySeconds || 0)))
const visibleDailyStats = computed(() => [...dailyStats.value].reverse())
const todayKey = computed(() => localDateKey(new Date()))
const todayStat = computed(() => dailyStats.value.find(item => item.statDate === todayKey.value) || null)
const todayLabel = computed(() => formatDateLabel(todayStat.value?.statDate || todayKey.value))
const moduleStats = computed(() => {
  locale.value
  const rows = [
    {
      key: 'practice',
      label: t('records.practice'),
      value: sumDaily(item => item.exerciseCount)
    },
    {
      key: 'words',
      label: t('records.words'),
      value: sumDaily(item => item.vocabReviewCount)
    },
    {
      key: 'dialogue',
      label: t('records.dialogue'),
      value: sumDaily(item => item.dialogueCount)
    },
    {
      key: 'matching',
      label: t('records.matching'),
      value: sumDaily(item => item.matchingGameCount)
    }
  ]
  const maxValue = Math.max(1, ...rows.map(item => item.value))
  return rows.map(item => ({
    ...item,
    width: `${Math.max(6, Math.round((item.value / maxValue) * 100))}%`
  }))
})

onShow(() => {
  applyTabBarLocale()
  setPageTitle('records.eyebrow')
  if (!requireLogin()) {
    return
  }
  void loadRecords(events.value.length === 0)
})

watch(locale, () => {
  applyTabBarLocale()
  setPageTitle('records.eyebrow')
})

onPullDownRefresh(() => {
  void refreshRecords().finally(() => uni.stopPullDownRefresh())
})

onReachBottom(() => {
  if (activePanel.value === 'events' && canLoadMore.value && !loadingMore.value) {
    void loadMore()
  }
})

async function refreshRecords() {
  await loadRecords(true)
}

async function loadRecords(showLoading = true) {
  if (loading.value) {
    return
  }
  loading.value = showLoading
  errorMessage.value = ''
  page.value = 1
  try {
    const [summaryData, dailyData, eventPage] = await Promise.all([
      fetchLearningSummary(),
      fetchDailyStats(7),
      fetchStudyEvents(1, pageSize, activeEventType.value || undefined)
    ])
    summary.value = summaryData
    dailyStats.value = dailyData
    events.value = eventPage.records
    total.value = eventPage.total
  } catch {
    errorMessage.value = t('records.loadFailed')
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  if (loadingMore.value || !canLoadMore.value) {
    return
  }
  loadingMore.value = true
  const nextPage = page.value + 1
  try {
    const eventPage = await fetchStudyEvents(nextPage, pageSize, activeEventType.value || undefined)
    events.value = events.value.concat(eventPage.records)
    total.value = eventPage.total
    page.value = nextPage
  } catch {
    void uni.showToast({ icon: 'none', title: t('records.moreFailed') })
  } finally {
    loadingMore.value = false
  }
}

function selectEventType(value: EventFilterValue) {
  if (activeEventType.value === value) {
    return
  }
  activeEventType.value = value
  void loadRecords(true)
}

function selectPanel(value: RecordsPanel) {
  activePanel.value = value
}

function dailyBarHeight(seconds: number) {
  const height = Math.max(8, Math.round((seconds / maxDailySeconds.value) * 100))
  return `${Math.min(100, height)}%`
}

function sumDaily(selector: (item: DailyStat) => number) {
  return dailyStats.value.reduce((sum, item) => sum + selector(item), 0)
}

function localDateKey(date: Date) {
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}

function formatDuration(seconds: number | null | undefined) {
  const safeSeconds = Math.max(0, seconds || 0)
  const hours = Math.floor(safeSeconds / 3600)
  const minutes = Math.floor((safeSeconds % 3600) / 60)
  if (hours > 0) {
    return t('common.hoursMinutes', { hours, minutes })
  }
  if (minutes > 0) {
    return t('common.minutes', { minutes })
  }
  return t('common.seconds', { seconds: safeSeconds })
}

function formatDurationShort(seconds: number | null | undefined) {
  const safeSeconds = Math.max(0, seconds || 0)
  const hours = Math.floor(safeSeconds / 3600)
  const minutes = Math.floor((safeSeconds % 3600) / 60)
  if (hours > 0) {
    return t('common.hoursMinutes', { hours, minutes })
  }
  return t('common.minutes', { minutes })
}

function formatPercent(value: number | null | undefined) {
  return `${Number(value || 0).toFixed(0)}%`
}

function formatDateLabel(value: string) {
  const [, month = '0', day = '0'] = value.split('-')
  return `${Number(month)}/${Number(day)}`
}

function formatTime(value: string) {
  return new Date(value).toLocaleString()
}

function eventTypeLabel(type: string) {
  const labels: Record<string, string> = {
    exercise: t('records.eventExercise'),
    vocab: t('records.eventVocab'),
    dialogue: t('records.eventDialogue'),
    matching_game: t('records.eventMatching')
  }
  return labels[type] || type
}

function resultLabel(result: string | null) {
  const labels: Record<string, string> = {
    correct: t('records.resultCorrect'),
    wrong: t('records.resultWrong'),
    completed: t('records.resultCompleted'),
    submitted: t('records.resultSubmitted')
  }
  return result ? labels[result] || result : t('records.resultDefault')
}

function resultTone(result: string | null) {
  if (result === 'correct' || result === 'completed') {
    return 'success'
  }
  if (result === 'wrong') {
    return 'danger'
  }
  return 'neutral'
}
</script>

<style scoped>
.page {
  background: #eef5f7;
  min-height: 100vh;
  padding: 0 24rpx 32rpx;
}

.hero {
  align-items: flex-start;
  background: #102033;
  border-bottom-left-radius: 32rpx;
  border-bottom-right-radius: 32rpx;
  box-sizing: border-box;
  color: #ffffff;
  display: flex;
  justify-content: space-between;
  margin: 0 -24rpx 22rpx;
  padding: 30rpx 24rpx 28rpx;
}

.hero-actions {
  align-items: flex-end;
  display: flex;
  flex-direction: column;
  gap: 12rpx;
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

.subtitle,
.muted {
  color: #64748b;
  display: block;
  font-size: 24rpx;
  line-height: 1.5;
  margin-top: 8rpx;
}

.hero .subtitle {
  color: #cbd5e1;
  font-size: 26rpx;
  margin-top: 14rpx;
}

.icon-btn,
.plain-btn {
  border-radius: 8rpx;
  font-size: 26rpx;
  margin: 0;
}

.icon-btn {
  background: rgba(255, 255, 255, 0.14);
  color: #ffffff;
  min-height: 58rpx;
  padding: 0 18rpx;
}

.plain-btn {
  background: #ffffff;
  border: 1px solid #cbd5e1;
  color: #102033;
}

.plain-btn.full {
  margin-top: 14rpx;
  width: 100%;
}

.plain-btn.compact {
  margin-top: 14rpx;
  width: 160rpx;
}

.panel-tabs {
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  border-radius: 14rpx;
  box-sizing: border-box;
  display: grid;
  gap: 8rpx;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-bottom: 18rpx;
  padding: 8rpx;
}

.panel-tab {
  align-items: center;
  background: transparent;
  border-radius: 10rpx;
  box-sizing: border-box;
  color: #475569;
  display: flex;
  font-size: 26rpx;
  font-weight: 800;
  height: 68rpx;
  justify-content: center;
  line-height: 1;
  margin: 0;
  min-height: 68rpx;
  padding: 0;
}

.panel-tab::after {
  border: 0;
}

.panel-tab.active {
  background: #14796f;
  box-shadow: 0 8rpx 18rpx rgba(20, 121, 111, 0.18);
  color: #ffffff;
}

.state-card,
.summary-card,
.today-card,
.module-card,
.week-card,
.event-card {
  background: #ffffff;
  border: 1px solid #d7e2ea;
  border-radius: 8rpx;
  box-sizing: border-box;
}

.state-card {
  color: #64748b;
  font-size: 26rpx;
  margin-bottom: 18rpx;
  padding: 26rpx;
}

.today-card {
  margin-bottom: 18rpx;
  overflow: hidden;
  padding: 22rpx;
}

.today-head,
.module-top {
  align-items: center;
  display: flex;
  justify-content: space-between;
}

.today-date {
  color: #102033;
  display: block;
  font-size: 34rpx;
  font-weight: 800;
  margin-top: 6rpx;
}

.today-empty {
  background: #f1f5f9;
  border-radius: 999rpx;
  color: #64748b;
  font-size: 22rpx;
  padding: 8rpx 14rpx;
}

.today-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 20rpx;
}

.today-metric {
  background: #f8fafc;
  border-radius: 8rpx;
  box-sizing: border-box;
  padding: 18rpx;
  width: calc(50% - 6rpx);
}

.metric-value {
  color: #102033;
  display: block;
  font-size: 30rpx;
  font-weight: 800;
  margin-top: 8rpx;
}

.error-card {
  background: #fff7ed;
  border-color: #fed7aa;
  color: #9a3412;
}

.summary-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 14rpx;
}

.summary-card {
  padding: 22rpx;
  width: calc(50% - 7rpx);
}

.summary-card.strong {
  background: #ffffff;
  border-color: #d7e2ea;
}

.summary-card.strong .label {
  color: #0f766e;
}

.summary-card.strong .value {
  color: #14796f;
}

.label {
  color: #64748b;
  display: block;
  font-size: 24rpx;
}

.value {
  display: block;
  font-size: 36rpx;
  font-weight: 800;
  margin-top: 8rpx;
}

.module-card {
  margin-top: 18rpx;
  padding: 22rpx;
}

.week-card {
  margin-top: 18rpx;
  padding: 22rpx;
}

.compact-head .section-title {
  font-size: 30rpx;
}

.module-list {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
  margin-top: 18rpx;
}

.module-name {
  color: #475569;
  display: block;
  font-size: 24rpx;
  font-weight: 700;
}

.module-value {
  color: #102033;
  display: block;
  font-size: 28rpx;
  font-weight: 800;
}

.module-bar {
  background: #e2e8f0;
  border-radius: 999rpx;
  height: 12rpx;
  margin-top: 10rpx;
  overflow: hidden;
}

.module-fill {
  background: #14796f;
  height: 100%;
}

.week-chart {
  align-items: flex-end;
  display: grid;
  gap: 8rpx;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  margin-top: 18rpx;
}

.day-column {
  align-items: center;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.day-bar-track {
  align-items: flex-end;
  background: #eef5f7;
  border-radius: 999rpx;
  display: flex;
  height: 112rpx;
  justify-content: center;
  overflow: hidden;
  width: 22rpx;
}

.day-bar-fill {
  background: #14796f;
  border-radius: inherit;
  min-height: 8rpx;
  width: 100%;
}

.day-label {
  color: #64748b;
  display: block;
  font-size: 20rpx;
  line-height: 1.2;
  margin-top: 10rpx;
}

.day-value {
  color: #102033;
  display: block;
  font-size: 20rpx;
  font-weight: 800;
  line-height: 1.2;
  margin-top: 4rpx;
  max-width: 80rpx;
  overflow: hidden;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.section {
  margin-top: 30rpx;
}

.events-section {
  margin-top: 18rpx;
}

.section-head,
.event-title-row {
  align-items: center;
  display: flex;
  justify-content: space-between;
}

.section-title {
  color: #102033;
  display: block;
  font-size: 34rpx;
  font-weight: 800;
}

.event-card {
  margin-top: 14rpx;
  padding: 20rpx;
}

.item-title {
  color: #102033;
  display: block;
  font-size: 30rpx;
  font-weight: 800;
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
  margin-top: 14rpx;
}

.filter-btn {
  align-items: center;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  border-radius: 8rpx;
  box-sizing: border-box;
  color: #475569;
  display: flex;
  font-size: 24rpx;
  height: 58rpx;
  justify-content: center;
  line-height: 1;
  margin: 0;
  padding: 0 18rpx;
}

.filter-btn.active {
  background: #14796f;
  border-color: #14796f;
  color: #ffffff;
}

.event-card {
  align-items: flex-start;
  display: flex;
  gap: 18rpx;
}

.event-dot {
  background: #94a3b8;
  border-radius: 999rpx;
  height: 18rpx;
  margin-top: 12rpx;
  width: 18rpx;
}

.event-dot.exercise {
  background: #2563eb;
}

.event-dot.vocab {
  background: #14796f;
}

.event-dot.dialogue {
  background: #d97706;
}

.event-dot.matching_game {
  background: #7c3aed;
}

.event-body {
  flex: 1;
}

.event-target {
  color: #94a3b8;
  display: block;
  font-size: 22rpx;
  margin-top: 6rpx;
}

.result-pill {
  border-radius: 999rpx;
  font-size: 22rpx;
  padding: 6rpx 14rpx;
}

.result-pill.success {
  background: #dcfce7;
  color: #166534;
}

.result-pill.danger {
  background: #fee2e2;
  color: #991b1b;
}

.result-pill.neutral {
  background: #e2e8f0;
  color: #475569;
}

.end-text {
  color: #94a3b8;
  display: block;
  font-size: 24rpx;
  margin-top: 18rpx;
  text-align: center;
}
</style>
