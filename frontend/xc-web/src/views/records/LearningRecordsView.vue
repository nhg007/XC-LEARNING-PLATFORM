<template>
  <main class="app-shell">
    <header class="topbar records-hero">
      <div>
        <h1>{{ t('records.title') }}</h1>
        <p>{{ t('records.subtitle') }}</p>
      </div>
      <div class="top-actions">
        <LocaleSwitch />
        <v-btn prepend-icon="mdi-arrow-left" variant="text" @click="$router.push('/')">{{ t('common.back') }}</v-btn>
        <v-btn prepend-icon="mdi-refresh" variant="tonal" :loading="loading" @click="loadRecords">{{ t('common.refresh') }}</v-btn>
      </div>
    </header>

    <section class="summary-grid">
      <v-card class="summary-card" elevation="0">
        <span>{{ t('records.totalStudyTime') }}</span>
        <strong>{{ formatDuration(summary.totalStudySeconds) }}</strong>
      </v-card>
      <v-card class="summary-card" elevation="0">
        <span>{{ t('records.exerciseCount') }}</span>
        <strong>{{ summary.totalExerciseCount }}</strong>
      </v-card>
      <v-card class="summary-card" elevation="0">
        <span>{{ t('records.accuracy') }}</span>
        <strong>{{ summary.overallAccuracyRate }}%</strong>
      </v-card>
      <v-card class="summary-card" elevation="0">
        <span>{{ t('records.longestStreak') }}</span>
        <strong>{{ t('units.days', { count: summary.longestStreakDays }) }}</strong>
      </v-card>
    </section>

    <v-card class="report-card report-dashboard" elevation="0">
      <div class="section-header">
        <div>
          <div class="panel-title">{{ activeReportTitle }}</div>
          <p>{{ activeReportMeta }}</p>
        </div>
        <div class="section-tabs" role="tablist">
          <button
            v-for="tab in reportTabs"
            :key="tab.key"
            class="section-tab"
            :class="{ active: activeReportTab === tab.key }"
            type="button"
            :aria-selected="activeReportTab === tab.key"
            @click="activeReportTab = tab.key"
          >
            <span>{{ tab.label }}</span>
            <strong>{{ tab.meta }}</strong>
          </button>
        </div>
      </div>
      <div class="report-content">
        <div v-if="activeReportTab === 'study' && chartRows.length > 0" ref="studyTrendChartRef" class="echart echart-large" />
        <div v-else-if="activeReportTab === 'accuracy' && chartRows.length > 0" ref="accuracyChartRef" class="echart echart-large" />
        <div v-else-if="activeReportTab === 'mix' && activityTotal > 0" ref="activityMixChartRef" class="echart echart-large" />
        <div v-else class="chart-empty">{{ t('records.reports.noData') }}</div>
      </div>
    </v-card>

    <v-card class="panel data-panel" elevation="0">
      <div class="table-toolbar">
        <div class="section-tabs data-tabs" role="tablist">
          <button
            v-for="tab in dataTabs"
            :key="tab.key"
            class="section-tab"
            :class="{ active: activeDataTab === tab.key }"
            type="button"
            :aria-selected="activeDataTab === tab.key"
            @click="activeDataTab = tab.key"
          >
            <span>{{ tab.label }}</span>
            <strong>{{ tab.meta }}</strong>
          </button>
        </div>
        <v-select
          v-if="activeDataTab === 'events'"
          v-model="eventType"
          clearable
          density="compact"
          hide-details
          item-title="label"
          item-value="value"
          :items="eventTypeOptions"
          :label="t('records.allTypes')"
          style="max-width: 240px"
          variant="outlined"
          @update:model-value="() => loadEvents(1)"
        />
        <div v-if="activeDataTab === 'leaderboards'" class="leaderboard-filters">
          <v-select
            v-model="leaderboardPeriodType"
            density="compact"
            hide-details
            item-title="label"
            item-value="value"
            :items="leaderboardPeriodOptions"
            :label="t('records.leaderboardPeriod')"
            variant="outlined"
            @update:model-value="() => reloadLeaderboards()"
          />
          <v-select
            v-model="leaderboardMetricType"
            density="compact"
            hide-details
            item-title="label"
            item-value="value"
            :items="leaderboardMetricOptions"
            :label="t('records.leaderboardMetric')"
            variant="outlined"
            @update:model-value="() => reloadLeaderboards()"
          />
          <v-btn
            prepend-icon="mdi-refresh"
            variant="tonal"
            :loading="leaderboardLoading"
            @click="loadLeaderboards(leaderboardPage)"
          >
            {{ t('records.leaderboardRefresh') }}
          </v-btn>
        </div>
      </div>
      <v-progress-linear v-if="activeDataTab === 'daily' && loading" class="panel-loader" color="primary" indeterminate />
      <v-progress-linear v-if="activeDataTab === 'events' && eventLoading" class="panel-loader" color="primary" indeterminate />
      <v-progress-linear v-if="activeDataTab === 'attempts' && attemptLoading" class="panel-loader" color="primary" indeterminate />
      <v-progress-linear v-if="activeDataTab === 'leaderboards' && leaderboardLoading" class="panel-loader" color="primary" indeterminate />

      <div v-if="activeDataTab === 'daily'" class="table-scroll">
        <v-table class="records-table" density="comfortable">
          <thead>
            <tr>
              <th>{{ t('records.table.date') }}</th>
              <th>{{ t('records.table.studyTime') }}</th>
              <th>{{ t('records.table.exercises') }}</th>
              <th>{{ t('records.table.correct') }}</th>
              <th>{{ t('records.table.vocab') }}</th>
              <th>{{ t('records.table.accuracy') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in dailyStats" :key="row.statDate">
              <td>{{ row.statDate }}</td>
              <td>{{ formatDuration(row.studySeconds) }}</td>
              <td>{{ row.exerciseCount }}</td>
              <td>{{ row.correctCount }}</td>
              <td>{{ row.vocabReviewCount }}</td>
              <td>{{ row.accuracyRate }}%</td>
            </tr>
          </tbody>
        </v-table>
      </div>
      <div v-if="activeDataTab === 'daily' && dailyStats.length === 0 && !loading" class="empty-state">{{ t('records.emptyDailyStats') }}</div>

      <div v-if="activeDataTab === 'events'" class="table-scroll">
        <v-table class="records-table" density="comfortable">
          <thead>
            <tr>
              <th>{{ t('records.table.type') }}</th>
              <th>{{ t('records.table.result') }}</th>
              <th>{{ t('records.table.duration') }}</th>
              <th>{{ t('records.table.time') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in events" :key="row.id">
              <td>{{ eventTypeLabel(row.eventType) }}</td>
              <td>{{ resultLabel(row.result) }}</td>
              <td>{{ formatDuration(row.durationSeconds) }}</td>
              <td>{{ new Date(row.occurredAt).toLocaleString() }}</td>
            </tr>
          </tbody>
        </v-table>
      </div>
      <div v-if="activeDataTab === 'events' && events.length === 0 && !eventLoading" class="empty-state">{{ t('records.emptyEvents') }}</div>
      <v-pagination
        v-if="activeDataTab === 'events' && total > pageSize"
        v-model="page"
        class="pagination"
        :length="pageCount"
        @update:model-value="loadEvents"
      />

      <div v-if="activeDataTab === 'attempts'" class="table-scroll">
        <v-table class="records-table attempts-table" density="comfortable">
          <thead>
            <tr>
              <th>{{ t('records.table.type') }}</th>
              <th>{{ t('records.table.answer') }}</th>
              <th>{{ t('records.table.result') }}</th>
              <th>{{ t('records.table.referenceLanguage') }}</th>
              <th>{{ t('records.table.showedAnswer') }}</th>
              <th>{{ t('records.table.time') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in attempts" :key="row.id">
              <td>{{ exerciseTypeLabel(row.exerciseType) }}</td>
              <td class="answer-cell">{{ row.answerText }}</td>
              <td>{{ row.correct ? t('records.results.correct') : t('records.results.wrong') }}</td>
              <td>{{ languageLabel(row.translationLanguage) }}</td>
              <td>{{ row.showedAnswer ? t('common.yes') : t('common.no') }}</td>
              <td>{{ formatDateTime(row.answeredAt) }}</td>
            </tr>
          </tbody>
        </v-table>
      </div>
      <div v-if="activeDataTab === 'attempts' && attempts.length === 0 && !attemptLoading" class="empty-state">{{ t('records.emptyAttempts') }}</div>
      <v-pagination
        v-if="activeDataTab === 'attempts' && attemptTotal > attemptPageSize"
        v-model="attemptPage"
        class="pagination"
        :length="attemptPageCount"
        @update:model-value="loadAttempts"
      />

      <div v-if="activeDataTab === 'leaderboards'" class="table-scroll">
        <v-table class="records-table leaderboard-table" density="comfortable">
          <thead>
            <tr>
              <th>{{ t('records.table.rank') }}</th>
              <th>{{ t('records.table.user') }}</th>
              <th>{{ t('records.table.metric') }}</th>
              <th>{{ t('records.table.score') }}</th>
              <th>{{ t('records.table.period') }}</th>
              <th>{{ t('records.table.generatedAt') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in leaderboards" :key="row.id">
              <td>
                <span class="rank-badge" :class="rankClass(row.rankNo)">{{ row.rankNo }}</span>
              </td>
              <td>{{ row.nickname || t('common.userFallback', { id: row.userId }) }}</td>
              <td>{{ metricLabel(row.metricType) }}</td>
              <td>{{ formatLeaderboardScore(row) }}</td>
              <td>{{ formatLeaderboardPeriod(row) }}</td>
              <td>{{ formatDateTime(row.generatedAt) }}</td>
            </tr>
          </tbody>
        </v-table>
      </div>
      <div v-if="activeDataTab === 'leaderboards' && leaderboards.length === 0 && !leaderboardLoading" class="empty-state">
        {{ t('records.emptyLeaderboards') }}
      </div>
      <v-pagination
        v-if="activeDataTab === 'leaderboards' && leaderboardTotal > leaderboardPageSize"
        v-model="leaderboardPage"
        class="pagination"
        :length="leaderboardPageCount"
        @update:model-value="loadLeaderboards"
      />
    </v-card>
  </main>
</template>

<script setup lang="ts">
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import * as echarts from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, shallowRef, watch, type ShallowRef } from 'vue'
import { useI18n } from 'vue-i18n'
import LocaleSwitch from '@/components/LocaleSwitch.vue'
import { fetchExerciseAttempts } from '../../api/exercise'
import { fetchDailyStats, fetchLeaderboards, fetchLearningSummary, fetchStudyEvents } from '../../api/stats'
import type { DailyStat, ExerciseAttempt, LeaderboardEntry, LeaderboardMetricType, LeaderboardPeriodType, LearningSummary, StudyEvent } from '../../types/api'
import type { ECharts } from 'echarts/core'

echarts.use([BarChart, LineChart, PieChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

type ReportTab = 'study' | 'accuracy' | 'mix'
type DataTab = 'daily' | 'events' | 'attempts' | 'leaderboards'

const { t, locale } = useI18n()
const loading = ref(false)
const eventLoading = ref(false)
const attemptLoading = ref(false)
const leaderboardLoading = ref(false)
const page = ref(1)
const pageSize = 20
const total = ref(0)
const eventType = ref<string | null>(null)
const attemptPage = ref(1)
const attemptPageSize = 20
const attemptTotal = ref(0)
const leaderboardPage = ref(1)
const leaderboardPageSize = 10
const leaderboardTotal = ref(0)
const leaderboardPeriodType = ref<LeaderboardPeriodType>('all')
const leaderboardMetricType = ref<LeaderboardMetricType>('streak')
const activeReportTab = ref<ReportTab>('study')
const activeDataTab = ref<DataTab>('daily')
const studyTrendChartRef = ref<HTMLDivElement | null>(null)
const accuracyChartRef = ref<HTMLDivElement | null>(null)
const activityMixChartRef = ref<HTMLDivElement | null>(null)
const studyTrendChart = shallowRef<ECharts | null>(null)
const accuracyChart = shallowRef<ECharts | null>(null)
const activityMixChart = shallowRef<ECharts | null>(null)
let resizeTimer: number | undefined
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
const attempts = ref<ExerciseAttempt[]>([])
const leaderboards = ref<LeaderboardEntry[]>([])
const reportTabs = computed<Array<{ key: ReportTab; label: string; meta: string }>>(() => [
  { key: 'study', label: t('records.reports.studyTrend'), meta: t('records.reports.recentDays', { count: chartRows.value.length }) },
  { key: 'accuracy', label: t('records.reports.accuracyTrend'), meta: `${latestAccuracy.value}%` },
  { key: 'mix', label: t('records.reports.activityMix'), meta: t('records.reports.totalItems', { count: activityTotal.value }) }
])
const dataTabs = computed<Array<{ key: DataTab; label: string; meta: string }>>(() => [
  { key: 'daily', label: t('records.dailyStats'), meta: `${dailyStats.value.length}` },
  { key: 'events', label: t('records.events'), meta: `${total.value}` },
  { key: 'attempts', label: t('records.attempts'), meta: `${attemptTotal.value}` },
  { key: 'leaderboards', label: t('records.leaderboards'), meta: `${leaderboardTotal.value}` }
])
const eventTypeOptions = computed(() => [
  { label: t('records.eventTypes.exercise'), value: 'exercise' },
  { label: t('records.eventTypes.vocab'), value: 'vocab' },
  { label: t('records.eventTypes.matching_game'), value: 'matching_game' }
])
const leaderboardPeriodOptions = computed<Array<{ label: string; value: LeaderboardPeriodType }>>(() => [
  { label: t('records.leaderboardPeriods.all'), value: 'all' },
  { label: t('records.leaderboardPeriods.daily'), value: 'daily' },
  { label: t('records.leaderboardPeriods.weekly'), value: 'weekly' },
  { label: t('records.leaderboardPeriods.monthly'), value: 'monthly' }
])
const leaderboardMetricOptions = computed<Array<{ label: string; value: LeaderboardMetricType }>>(() => [
  { label: t('records.leaderboardMetrics.streak'), value: 'streak' },
  { label: t('records.leaderboardMetrics.accuracy'), value: 'accuracy' },
  { label: t('records.leaderboardMetrics.vocab_count'), value: 'vocab_count' },
  { label: t('records.leaderboardMetrics.game_score'), value: 'game_score' }
])
const pageCount = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))
const attemptPageCount = computed(() => Math.max(1, Math.ceil(attemptTotal.value / attemptPageSize)))
const leaderboardPageCount = computed(() => Math.max(1, Math.ceil(leaderboardTotal.value / leaderboardPageSize)))
const chartRows = computed(() => [...dailyStats.value].sort((a, b) => a.statDate.localeCompare(b.statDate)))
const latestAccuracy = computed(() => {
  const rows = chartRows.value
  return rows.length > 0 ? rows[rows.length - 1].accuracyRate : summary.value.overallAccuracyRate
})
const activityMix = computed(() => {
  const exerciseCount = chartRows.value.reduce((sum, row) => sum + row.exerciseCount, 0)
  const vocabCount = chartRows.value.reduce((sum, row) => sum + row.vocabReviewCount, 0)
  const matchingCount = events.value.filter(row => row.eventType === 'matching_game').length
  return [
    { key: 'exercise', label: t('records.eventTypes.exercise'), value: exerciseCount },
    { key: 'vocab', label: t('records.eventTypes.vocab'), value: vocabCount },
    { key: 'matching_game', label: t('records.eventTypes.matching_game'), value: matchingCount }
  ]
})
const activityTotal = computed(() => activityMix.value.reduce((sum, item) => sum + item.value, 0))
const activeReportTitle = computed(() => reportTabs.value.find(tab => tab.key === activeReportTab.value)?.label || '')
const activeReportMeta = computed(() => reportTabs.value.find(tab => tab.key === activeReportTab.value)?.meta || '')

async function loadRecords() {
  loading.value = true
  try {
    const [summaryData, dailyData] = await Promise.all([fetchLearningSummary(), fetchDailyStats(14)])
    summary.value = summaryData
    dailyStats.value = dailyData
    await Promise.all([loadEvents(page.value), loadAttempts(attemptPage.value), loadLeaderboards(leaderboardPage.value)])
  } finally {
    loading.value = false
  }
}

async function loadEvents(nextPage = page.value) {
  eventLoading.value = true
  try {
    page.value = nextPage
    const result = await fetchStudyEvents(nextPage, pageSize, eventType.value || undefined)
    events.value = result.records
    total.value = result.total
  } finally {
    eventLoading.value = false
  }
}

async function loadAttempts(nextPage = attemptPage.value) {
  attemptLoading.value = true
  try {
    attemptPage.value = nextPage
    const result = await fetchExerciseAttempts(nextPage, attemptPageSize)
    attempts.value = result.records
    attemptTotal.value = result.total
  } finally {
    attemptLoading.value = false
  }
}

async function loadLeaderboards(nextPage = leaderboardPage.value) {
  leaderboardLoading.value = true
  try {
    leaderboardPage.value = nextPage
    const result = await fetchLeaderboards({
      page: nextPage,
      pageSize: leaderboardPageSize,
      periodType: leaderboardPeriodType.value,
      metricType: leaderboardMetricType.value
    })
    leaderboards.value = result.records
    leaderboardTotal.value = result.total
  } finally {
    leaderboardLoading.value = false
  }
}

function reloadLeaderboards() {
  void loadLeaderboards(1)
}

async function renderCharts() {
  await nextTick()
  if (activeReportTab.value === 'study') {
    renderStudyTrendChart()
    disposeChart(accuracyChart)
    disposeChart(activityMixChart)
    return
  }
  if (activeReportTab.value === 'accuracy') {
    renderAccuracyChart()
    disposeChart(studyTrendChart)
    disposeChart(activityMixChart)
    return
  }
  renderActivityMixChart()
  disposeChart(studyTrendChart)
  disposeChart(accuracyChart)
}

function renderStudyTrendChart() {
  if (chartRows.value.length === 0) {
    disposeChart(studyTrendChart)
    return
  }
  const chart = ensureChart(studyTrendChartRef.value, studyTrendChart)
  if (!chart) {
    return
  }
  const studyLabel = t('records.table.studyTime')
  const exerciseLabel = t('records.table.exercises')
  const compact = isCompactChart()
  chart.setOption({
    animationDuration: 900,
    color: ['#2563eb', '#16a34a'],
    grid: compact
      ? { bottom: 46, left: 50, right: 24, top: 56 }
      : { bottom: 54, left: 86, right: 88, top: 62 },
    legend: {
      top: 8,
      icon: 'roundRect',
      itemGap: 20,
      itemHeight: 10,
      itemWidth: 22,
      textStyle: { color: '#475569', fontSize: 13 }
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(15, 23, 42, 0.92)',
      borderColor: 'rgba(148, 163, 184, 0.28)',
      textStyle: { color: '#e2e8f0' },
      formatter: (params: unknown) => formatAxisTooltip(params, studyLabel)
    },
    xAxis: {
      type: 'category',
      data: chartRows.value.map(row => formatShortDate(row.statDate)),
      axisLabel: { color: '#64748b', fontSize: 12, interval: 0 },
      axisLine: { lineStyle: { color: '#dbe3ee' } },
      axisTick: { show: false }
    },
    yAxis: [
      {
        type: 'value',
        name: studyLabel,
        nameGap: 22,
        nameTextStyle: { color: '#64748b', fontSize: 12 },
        axisLabel: { color: '#64748b', fontSize: 12, formatter: (value: number) => formatDuration(Number(value)) },
        splitLine: { lineStyle: { color: '#e8eef6', type: 'dashed' } }
      },
      {
        type: 'value',
        name: exerciseLabel,
        nameGap: 22,
        nameTextStyle: { color: '#64748b', fontSize: 12 },
        axisLabel: { color: '#64748b', fontSize: 12 },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: studyLabel,
        type: 'bar',
        barMaxWidth: 42,
        data: chartRows.value.map(row => row.studySeconds),
        itemStyle: {
          borderRadius: [4, 4, 0, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#60a5fa' },
            { offset: 1, color: '#2563eb' }
          ])
        }
      },
      {
        name: exerciseLabel,
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        symbol: 'circle',
        symbolSize: 10,
        data: chartRows.value.map(row => row.exerciseCount),
        lineStyle: { width: 4 },
        itemStyle: { color: '#16a34a' }
      }
    ]
  }, true)
}

function renderAccuracyChart() {
  if (chartRows.value.length === 0) {
    disposeChart(accuracyChart)
    return
  }
  const chart = ensureChart(accuracyChartRef.value, accuracyChart)
  if (!chart) {
    return
  }
  const compact = isCompactChart()
  chart.setOption({
    animationDuration: 900,
    color: ['#2563eb'],
    grid: compact
      ? { bottom: 42, left: 48, right: 18, top: 30 }
      : { bottom: 54, left: 72, right: 38, top: 42 },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(15, 23, 42, 0.92)',
      borderColor: 'rgba(148, 163, 184, 0.28)',
      textStyle: { color: '#e2e8f0' },
      valueFormatter: (value: number) => `${value}%`
    },
    xAxis: {
      type: 'category',
      data: chartRows.value.map(row => formatShortDate(row.statDate)),
      axisLabel: { color: '#64748b', fontSize: 12, interval: 0 },
      axisLine: { lineStyle: { color: '#dbe3ee' } },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 100,
      axisLabel: { color: '#64748b', fontSize: 12, formatter: '{value}%' },
      splitLine: { lineStyle: { color: '#e8eef6', type: 'dashed' } }
    },
    series: [
      {
        name: t('records.accuracy'),
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 10,
        data: chartRows.value.map(row => row.accuracyRate),
        lineStyle: { width: 4 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(37, 99, 235, 0.32)' },
            { offset: 1, color: 'rgba(37, 99, 235, 0.02)' }
          ])
        }
      }
    ]
  }, true)
}

function renderActivityMixChart() {
  if (activityTotal.value === 0) {
    disposeChart(activityMixChart)
    return
  }
  const chart = ensureChart(activityMixChartRef.value, activityMixChart)
  if (!chart) {
    return
  }
  const compact = isCompactChart()
  chart.setOption({
    animationDuration: 900,
    color: ['#2563eb', '#16a34a', '#d97706', '#475569'],
    legend: {
      icon: 'circle',
      itemGap: compact ? 16 : 20,
      orient: compact ? 'horizontal' : 'vertical',
      right: compact ? 'auto' : 32,
      bottom: compact ? 0 : 'auto',
      top: compact ? 'auto' : 'middle',
      textStyle: { color: '#475569', fontSize: 13 }
    },
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(15, 23, 42, 0.92)',
      borderColor: 'rgba(148, 163, 184, 0.28)',
      textStyle: { color: '#e2e8f0' }
    },
    series: [
      {
        name: t('records.reports.activityMix'),
        type: 'pie',
        radius: compact ? ['42%', '72%'] : ['36%', '64%'],
        center: compact ? ['50%', '43%'] : ['36%', '52%'],
        roseType: 'radius',
        avoidLabelOverlap: true,
        data: activityMix.value.map(item => ({ name: item.label, value: item.value })),
        itemStyle: {
          borderColor: '#ffffff',
          borderRadius: 4,
          borderWidth: 3
        },
        label: {
          color: '#334155',
          fontSize: 12,
          formatter: '{b}\n{d}%'
        },
        labelLine: {
          length: 14,
          length2: 10
        }
      }
    ]
  }, true)
}

function ensureChart(element: HTMLDivElement | null, chartRef: ShallowRef<ECharts | null>) {
  if (!element) {
    return null
  }
  if (!chartRef.value) {
    chartRef.value = echarts.init(element)
  }
  return chartRef.value
}

function disposeChart(chartRef: ShallowRef<ECharts | null>) {
  chartRef.value?.dispose()
  chartRef.value = null
}

function resizeCharts() {
  window.clearTimeout(resizeTimer)
  resizeTimer = window.setTimeout(() => {
    studyTrendChart.value?.resize()
    accuracyChart.value?.resize()
    activityMixChart.value?.resize()
    void renderCharts()
  }, 120)
}

function formatDuration(seconds: number) {
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  return hours > 0 ? t('units.durationHoursMinutes', { hours, minutes }) : t('units.durationMinutes', { minutes })
}

function formatShortDate(date: string) {
  return date.slice(5)
}

function isCompactChart() {
  return typeof window !== 'undefined' && window.innerWidth < 900
}

function formatAxisTooltip(params: unknown, durationSeriesName: string) {
  if (!Array.isArray(params) || params.length === 0) {
    return ''
  }
  const items = params as Array<{ axisValue?: string; marker?: string; seriesName?: string; value?: number }>
  const rows = items.map((item) => {
    const value = Number(item.value || 0)
    const formattedValue = item.seriesName === durationSeriesName ? formatDuration(value) : String(value)
    return `${item.marker || ''}${item.seriesName}: ${formattedValue}`
  })
  return [items[0].axisValue || '', ...rows].join('<br/>')
}

function eventTypeLabel(type: string) {
  return t(`records.eventTypes.${type}`)
}

function exerciseTypeLabel(type: string) {
  return t(`practice.types.${type}`)
}

function languageLabel(language: 'ru' | 'en' | null) {
  if (!language) {
    return '-'
  }
  return language === 'ru' ? t('common.russian') : t('common.english')
}

function resultLabel(result: string | null) {
  return result ? t(`records.results.${result}`) : '-'
}

function metricLabel(type: LeaderboardMetricType) {
  return t(`records.leaderboardMetrics.${type}`)
}

function formatLeaderboardScore(row: LeaderboardEntry) {
  const value = Number(row.scoreValue || 0)
  if (row.metricType === 'accuracy') {
    return `${value.toFixed(2)}%`
  }
  return value.toLocaleString()
}

function formatLeaderboardPeriod(row: LeaderboardEntry) {
  if (row.periodType === 'all') {
    return t('records.leaderboardPeriods.all')
  }
  return `${t(`records.leaderboardPeriods.${row.periodType}`)} ${row.periodStart}`
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString()
}

function rankClass(rank: number) {
  if (rank === 1) {
    return 'top-one'
  }
  if (rank === 2) {
    return 'top-two'
  }
  if (rank === 3) {
    return 'top-three'
  }
  return ''
}

watch([chartRows, activityMix, locale, activeReportTab], () => {
  void renderCharts()
})

onMounted(() => {
  window.addEventListener('resize', resizeCharts)
  void loadRecords()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  window.clearTimeout(resizeTimer)
  disposeChart(studyTrendChart)
  disposeChart(accuracyChart)
  disposeChart(activityMixChart)
})
</script>

<style scoped>
h1 {
  font-size: 34px;
  line-height: 1.2;
  margin: 0;
}

p {
  color: #64748b;
  margin: 8px 0 0;
}

.records-hero {
  background: #142033;
  border: 1px solid #23324a;
  border-radius: 8px;
  color: #f8fafc;
  margin-bottom: 22px;
  padding: 30px;
}

.records-hero p {
  color: #cbd5e1;
}

.top-actions,
.table-toolbar {
  align-items: center;
  display: flex;
  gap: 10px;
}

.top-actions {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.top-actions :deep(.v-btn) {
  border-radius: 4px;
  letter-spacing: 0;
}

.top-actions :deep(.v-btn--variant-text) {
  color: #f8fafc;
}

.table-toolbar {
  justify-content: space-between;
  margin-bottom: 18px;
}

.leaderboard-filters {
  align-items: center;
  display: flex;
  gap: 10px;
}

.leaderboard-filters :deep(.v-input) {
  width: 170px;
}

.leaderboard-filters :deep(.v-btn) {
  border-radius: 4px;
  letter-spacing: 0;
}

.summary-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin-bottom: 20px;
}

.summary-card,
.report-card,
.panel {
  background: #ffffff;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
}

.summary-card {
  min-height: 132px;
  padding: 24px;
}

.summary-card span {
  color: #64748b;
  display: block;
  font-size: 13px;
}

.summary-card strong {
  display: block;
  font-size: 30px;
  line-height: 1.15;
  margin-top: 12px;
}

.report-card {
  margin-bottom: 24px;
  min-height: 500px;
  padding: 26px;
}

.report-dashboard {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
}

.section-header {
  align-items: flex-start;
  display: flex;
  gap: 20px;
  justify-content: space-between;
  margin-bottom: 20px;
}

.section-tabs {
  align-items: center;
  background: #f8fafc;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  display: flex;
  overflow: hidden;
}

.section-tab {
  background: transparent;
  border: 0;
  border-right: 1px solid #dbe3ee;
  color: #475569;
  cursor: pointer;
  display: grid;
  gap: 3px;
  min-width: 148px;
  padding: 11px 16px;
  text-align: left;
}

.section-tab:last-child {
  border-right: 0;
}

.section-tab span {
  font-size: 14px;
  font-weight: 700;
}

.section-tab strong {
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
}

.section-tab.active {
  background: #ffffff;
  color: #1d4ed8;
  box-shadow: inset 0 -2px 0 #2563eb;
}

.section-tab.active strong {
  color: #2563eb;
}

.report-content {
  min-height: 390px;
}

.echart {
  height: 390px;
  width: 100%;
}

.echart-large {
  height: 390px;
}

.chart-empty {
  align-items: center;
  color: #64748b;
  display: flex;
  justify-content: center;
  min-height: 200px;
}

.panel {
  margin-bottom: 22px;
  padding: 24px;
}

.data-panel {
  min-height: 420px;
}

.panel-loader {
  margin-bottom: 8px;
}

.panel-title {
  font-size: 20px;
  font-weight: 700;
}

.table-scroll {
  overflow-x: auto;
}

.records-table {
  min-width: 760px;
}

.leaderboard-table {
  min-width: 860px;
}

.attempts-table {
  min-width: 920px;
}

.answer-cell {
  max-width: 280px;
  word-break: break-word;
}

.records-table :deep(th) {
  background: #f8fafc !important;
  background-color: #f8fafc !important;
  border-bottom: 1px solid #e5edf7 !important;
  box-shadow: none !important;
  color: #475569 !important;
  font-size: 13px;
  font-weight: 800;
  height: 48px;
}

.records-table :deep(td) {
  border-bottom: 1px solid #eef2f7;
  height: 54px;
}

.records-table :deep(tbody tr:hover) {
  background: var(--xc-table-hover-bg);
}

.rank-badge {
  align-items: center;
  background: #e8eef6;
  border: 1px solid #d7e0ed;
  border-radius: 4px;
  color: #334155;
  display: inline-flex;
  font-size: 13px;
  font-weight: 800;
  height: 28px;
  justify-content: center;
  min-width: 36px;
}

.rank-badge.top-one {
  background: #fff7d6;
  border-color: #facc15;
  color: #854d0e;
}

.rank-badge.top-two {
  background: #f1f5f9;
  border-color: #cbd5e1;
  color: #334155;
}

.rank-badge.top-three {
  background: #ffedd5;
  border-color: #fdba74;
  color: #9a3412;
}

.empty-state {
  color: #64748b;
  padding: 28px 0 12px;
  text-align: center;
}

.pagination {
  margin-top: 14px;
}

.pagination :deep(.v-btn) {
  border-radius: 4px;
}

@media (max-width: 980px) {
  .records-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .top-actions {
    justify-content: flex-start;
  }

  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .section-header {
    flex-direction: column;
  }

  .section-tabs {
    width: 100%;
  }

  .section-tab {
    min-width: 0;
    width: 100%;
  }
}

@media (max-width: 720px) {
  h1 {
    font-size: 28px;
  }

  .records-hero,
  .report-card,
  .panel {
    padding: 20px 16px;
  }

  .report-card {
    min-height: auto;
  }

  .section-tabs {
    align-items: stretch;
    flex-direction: column;
  }

  .section-tab {
    border-bottom: 1px solid #dbe3ee;
    border-right: 0;
  }

  .section-tab:last-child {
    border-bottom: 0;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }

  .table-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .leaderboard-filters {
    align-items: stretch;
    flex-direction: column;
  }

  .leaderboard-filters :deep(.v-input) {
    width: 100%;
  }

  .table-toolbar :deep(.v-input) {
    max-width: none !important;
  }

  .echart,
  .echart-large,
  .report-content {
    height: 300px;
    min-height: 300px;
  }
}
</style>
