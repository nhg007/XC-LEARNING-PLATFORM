<template>
  <section class="reports-page">
    <div class="page-heading">
      <div>
        <h1>{{ t('reports.title') }}</h1>
        <p>{{ t('reports.subtitle') }}</p>
      </div>
      <el-button :loading="reportLoading || leaderboardLoading" :icon="Refresh" @click="reloadAll">
        {{ t('common.refresh') }}
      </el-button>
    </div>

    <el-card shadow="never" class="report-card">
      <div class="toolbar report-toolbar">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          :range-separator="t('reports.to')"
          :start-placeholder="t('reports.dateFrom')"
          :end-placeholder="t('reports.dateTo')"
        />
        <el-input
          v-model="reportQuery.keyword"
          clearable
          :placeholder="t('reports.userKeyword')"
          @keyup.enter="searchReport"
        />
        <el-button type="primary" :icon="Search" @click="searchReport">{{ t('reports.actions.search') }}</el-button>
        <el-button @click="resetReportFilters">{{ t('reports.actions.reset') }}</el-button>
      </div>

      <div v-loading="reportLoading" class="report-body">
        <div class="summary-grid">
          <div v-for="item in summaryItems" :key="item.label" class="summary-card">
            <el-icon>
              <component :is="item.icon" />
            </el-icon>
            <div>
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </div>

        <section class="chart-panel">
          <div class="section-heading">
            <div>
              <h2>{{ t('reports.trendTitle') }}</h2>
              <span>{{ currentSummary.dateFrom }} - {{ currentSummary.dateTo }}</span>
            </div>
            <el-segmented v-model="chartMetric" :options="chartMetricOptions" />
          </div>
          <div v-if="dailyStats.length" class="trend-chart">
            <svg :viewBox="`0 0 ${chartWidth} ${chartHeight}`" preserveAspectRatio="none">
              <line class="axis" :x1="chartLeft" :x2="chartWidth - chartRight" :y1="chartBottomY" :y2="chartBottomY" />
              <line class="axis" :x1="chartLeft" :x2="chartLeft" :y1="chartTop" :y2="chartBottomY" />
              <polyline class="area" :points="chartAreaPoints" />
              <polyline class="trend-line" :points="chartLinePoints" />
              <circle
                v-for="point in chartPoints"
                :key="`${point.x}-${point.y}`"
                class="trend-point"
                :cx="point.x"
                :cy="point.y"
                r="3.5"
              />
              <text class="axis-label" :x="chartLeft" :y="chartHeight - 8">{{ dailyStats[0]?.statDate }}</text>
              <text class="axis-label end" :x="chartWidth - chartRight" :y="chartHeight - 8">{{ dailyStats[dailyStats.length - 1]?.statDate }}</text>
              <text class="axis-label" :x="chartLeft + 4" :y="chartTop + 12">{{ formatChartValue(chartMax) }}</text>
            </svg>
          </div>
          <el-empty v-else :description="t('reports.emptyTrend')" />
        </section>
      </div>
    </el-card>

    <el-card shadow="never" class="report-card">
      <template #header>
        <div class="card-header">
          <span>{{ t('reports.userReportTitle') }}</span>
          <span>{{ t('reports.total', { total: userTotal }) }}</span>
        </div>
      </template>
      <el-table v-loading="reportLoading" :data="userRecords" row-key="userId" border :empty-text="t('reports.emptyUsers')" @sort-change="handleReportSortChange">
        <el-table-column :label="t('reports.columns.user')" min-width="230">
          <template #default="{ row }">
            <div class="stack-cell">
              <strong>{{ row.nickname || t('reports.unnamed') }}</strong>
              <span>{{ row.email || row.userId }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="t('reports.columns.status')" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'info'">
              {{ formatUserStatus(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="studySeconds" :label="t('reports.columns.studyTime')" min-width="140" sortable="custom">
          <template #default="{ row }">{{ formatDuration(row.studySeconds) }}</template>
        </el-table-column>
        <el-table-column prop="exerciseCount" :label="t('reports.columns.exerciseCount')" width="120" sortable="custom" />
        <el-table-column prop="correctCount" :label="t('reports.columns.correctCount')" width="120" sortable="custom" />
        <el-table-column prop="accuracyRate" :label="t('reports.columns.accuracy')" width="120" sortable="custom">
          <template #default="{ row }">{{ formatPercent(row.accuracyRate) }}</template>
        </el-table-column>
        <el-table-column prop="vocabReviewCount" :label="t('reports.columns.vocabReview')" width="130" sortable="custom" />
        <el-table-column prop="dialogueCount" :label="t('reports.columns.dialogue')" width="120" sortable="custom" />
        <el-table-column prop="matchingGameCount" :label="t('reports.columns.matching')" width="120" sortable="custom" />
        <el-table-column prop="lastStudyDate" :label="t('reports.columns.lastStudyDate')" width="150" sortable="custom">
          <template #default="{ row }">{{ row.lastStudyDate || t('common.empty') }}</template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="reportQuery.page"
        v-model:page-size="reportQuery.pageSize"
        background
        layout="total, sizes, prev, pager, next"
        :page-sizes="[10, 20, 50, 100]"
        :total="userTotal"
        @current-change="loadLearningReport"
        @size-change="loadLearningReport"
      />
    </el-card>

    <el-card shadow="never" class="report-card">
      <template #header>
        <div class="card-header">
          <span>{{ t('reports.leaderboardTitle') }}</span>
          <span>{{ t('reports.total', { total: leaderboardTotal }) }}</span>
        </div>
      </template>
      <div class="toolbar leaderboard-toolbar">
        <el-select v-model="leaderboardQuery.periodType" clearable :placeholder="t('reports.periodTypeFilter')">
          <el-option v-for="item in periodTypes" :key="item" :label="t(`reports.periodTypes.${item}`)" :value="item" />
        </el-select>
        <el-date-picker v-model="leaderboardPeriodStart" type="date" :placeholder="t('reports.periodStart')" />
        <el-select v-model="leaderboardQuery.metricType" clearable :placeholder="t('reports.metricTypeFilter')">
          <el-option v-for="item in metricTypes" :key="item" :label="t(`reports.metricTypes.${item}`)" :value="item" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="searchLeaderboards">{{ t('reports.actions.search') }}</el-button>
        <el-button @click="resetLeaderboardFilters">{{ t('reports.actions.reset') }}</el-button>
      </div>
      <el-table v-loading="leaderboardLoading" :data="leaderboardRecords" row-key="id" border :empty-text="t('reports.emptyLeaderboards')" @sort-change="handleLeaderboardSortChange">
        <el-table-column prop="rankNo" :label="t('reports.columns.rank')" width="90" sortable="custom" />
        <el-table-column :label="t('reports.columns.user')" min-width="230">
          <template #default="{ row }">
            <div class="stack-cell">
              <strong>{{ row.nickname || t('reports.unnamed') }}</strong>
              <span>{{ row.email || row.userId }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="metricType" :label="t('reports.columns.metricType')" width="150" sortable="custom">
          <template #default="{ row }">{{ t(`reports.metricTypes.${row.metricType}`) }}</template>
        </el-table-column>
        <el-table-column prop="scoreValue" :label="t('reports.columns.score')" width="130" sortable="custom">
          <template #default="{ row }">{{ formatScore(row.scoreValue, row.metricType) }}</template>
        </el-table-column>
        <el-table-column :label="t('reports.columns.period')" min-width="180">
          <template #default="{ row }">{{ t(`reports.periodTypes.${row.periodType}`) }} / {{ row.periodStart }}</template>
        </el-table-column>
        <el-table-column prop="generatedAt" :label="t('reports.columns.generatedAt')" width="180" sortable="custom">
          <template #default="{ row }">{{ formatDateTime(row.generatedAt) }}</template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="leaderboardQuery.page"
        v-model:page-size="leaderboardQuery.pageSize"
        background
        layout="total, sizes, prev, pager, next"
        :page-sizes="[10, 20, 50, 100]"
        :total="leaderboardTotal"
        @current-change="loadLeaderboards"
        @size-change="loadLeaderboards"
      />
    </el-card>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Collection, DataAnalysis, Files, Refresh, Search, User } from '@element-plus/icons-vue'
import { fetchAdminLeaderboards, fetchAdminLearningReport } from '@/api/reports'
import type {
  AdminDailyLearningReport,
  AdminLeaderboardEntry,
  AdminLeaderboardQuery,
  AdminLearningReport,
  AdminLearningReportQuery,
  AdminLearningReportSummary,
  LeaderboardMetricType,
  LeaderboardPeriodType
} from '@/types/api'
import { applyTableSort, type TableSortChange } from '@/utils/tableSort'

type ChartMetric = 'studySeconds' | 'exerciseCount' | 'activeUserCount' | 'accuracyRate'

const { t } = useI18n()

const reportLoading = ref(false)
const leaderboardLoading = ref(false)
const report = ref<AdminLearningReport | null>(null)
const leaderboardRecords = ref<AdminLeaderboardEntry[]>([])
const leaderboardTotal = ref(0)
const dateRange = ref<[Date, Date] | null>(defaultDateRange())
const leaderboardPeriodStart = ref<Date | null>(null)
const chartMetric = ref<ChartMetric>('studySeconds')
const periodTypes: LeaderboardPeriodType[] = ['daily', 'weekly', 'monthly', 'all']
const metricTypes: LeaderboardMetricType[] = ['streak', 'accuracy', 'vocab_count', 'game_score']

const reportQuery = reactive<AdminLearningReportQuery>({
  page: 1,
  pageSize: 20,
  keyword: '',
  sortBy: '',
  sortDirection: ''
})

const leaderboardQuery = reactive<AdminLeaderboardQuery>({
  page: 1,
  pageSize: 20,
  periodType: '',
  metricType: '',
  sortBy: '',
  sortDirection: ''
})

const emptySummary: AdminLearningReportSummary = {
  dateFrom: '',
  dateTo: '',
  activeUserCount: 0,
  studySeconds: 0,
  exerciseCount: 0,
  correctCount: 0,
  vocabReviewCount: 0,
  dialogueCount: 0,
  matchingGameCount: 0,
  accuracyRate: 0
}

const chartWidth = 900
const chartHeight = 260
const chartLeft = 46
const chartRight = 20
const chartTop = 18
const chartBottom = 36
const chartBottomY = chartHeight - chartBottom

const currentSummary = computed(() => report.value?.summary || emptySummary)
const dailyStats = computed(() => report.value?.dailyStats || [])
const userRecords = computed(() => report.value?.users.records || [])
const userTotal = computed(() => report.value?.users.total || 0)

const summaryItems = computed(() => [
  { label: t('reports.summary.activeUsers'), value: currentSummary.value.activeUserCount, icon: User },
  { label: t('reports.summary.studyTime'), value: formatDuration(currentSummary.value.studySeconds), icon: DataAnalysis },
  { label: t('reports.summary.exercises'), value: currentSummary.value.exerciseCount, icon: Files },
  { label: t('reports.summary.accuracy'), value: formatPercent(currentSummary.value.accuracyRate), icon: DataAnalysis },
  { label: t('reports.summary.vocabReview'), value: currentSummary.value.vocabReviewCount, icon: Collection },
  { label: t('reports.summary.dialogue'), value: currentSummary.value.dialogueCount, icon: Files },
  { label: t('reports.summary.matching'), value: currentSummary.value.matchingGameCount, icon: Files }
])

const chartMetricOptions = computed(() => [
  { label: t('reports.chartMetrics.studySeconds'), value: 'studySeconds' },
  { label: t('reports.chartMetrics.exerciseCount'), value: 'exerciseCount' },
  { label: t('reports.chartMetrics.activeUserCount'), value: 'activeUserCount' },
  { label: t('reports.chartMetrics.accuracyRate'), value: 'accuracyRate' }
])

const chartValues = computed(() => dailyStats.value.map(item => Number(item[chartMetric.value]) || 0))
const chartMax = computed(() => Math.max(1, ...chartValues.value))
const chartPoints = computed(() => {
  const values = chartValues.value
  const plotWidth = chartWidth - chartLeft - chartRight
  const plotHeight = chartHeight - chartTop - chartBottom
  return values.map((value, index) => {
    const x = chartLeft + (values.length <= 1 ? plotWidth / 2 : (index / (values.length - 1)) * plotWidth)
    const y = chartTop + (1 - value / chartMax.value) * plotHeight
    return { x, y, value }
  })
})
const chartLinePoints = computed(() => chartPoints.value.map(point => `${point.x},${point.y}`).join(' '))
const chartAreaPoints = computed(() => {
  if (!chartPoints.value.length) {
    return ''
  }
  const first = chartPoints.value[0]
  const last = chartPoints.value[chartPoints.value.length - 1]
  return `${chartLinePoints.value} ${last.x},${chartBottomY} ${first.x},${chartBottomY}`
})

onMounted(async () => {
  await reloadAll()
})

async function reloadAll() {
  await Promise.all([loadLearningReport(), loadLeaderboards()])
}

async function loadLearningReport() {
  reportLoading.value = true
  try {
    const [from, to] = dateRange.value || []
    report.value = await fetchAdminLearningReport({
      ...reportQuery,
      dateFrom: from ? toDateString(from) : undefined,
      dateTo: to ? toDateString(to) : undefined
    })
  } finally {
    reportLoading.value = false
  }
}

async function loadLeaderboards() {
  leaderboardLoading.value = true
  try {
    const result = await fetchAdminLeaderboards({
      ...leaderboardQuery,
      periodStart: leaderboardPeriodStart.value ? toDateString(leaderboardPeriodStart.value) : undefined
    })
    leaderboardRecords.value = result.records
    leaderboardTotal.value = result.total
  } finally {
    leaderboardLoading.value = false
  }
}

function searchReport() {
  reportQuery.page = 1
  void loadLearningReport()
}

function resetReportFilters() {
  reportQuery.page = 1
  reportQuery.keyword = ''
  dateRange.value = defaultDateRange()
  void loadLearningReport()
}

function searchLeaderboards() {
  leaderboardQuery.page = 1
  void loadLeaderboards()
}

function resetLeaderboardFilters() {
  leaderboardQuery.page = 1
  leaderboardQuery.periodType = ''
  leaderboardQuery.metricType = ''
  leaderboardPeriodStart.value = null
  void loadLeaderboards()
}

function handleReportSortChange(event: TableSortChange) {
  applyTableSort(reportQuery, event)
  void loadLearningReport()
}

function handleLeaderboardSortChange(event: TableSortChange) {
  applyTableSort(leaderboardQuery, event)
  void loadLeaderboards()
}

function defaultDateRange(): [Date, Date] {
  const to = new Date()
  const from = new Date()
  from.setDate(to.getDate() - 29)
  return [from, to]
}

function toDateString(value: Date) {
  const year = value.getFullYear()
  const month = String(value.getMonth() + 1).padStart(2, '0')
  const day = String(value.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function formatDuration(seconds: number) {
  const safeSeconds = Math.max(0, seconds || 0)
  if (safeSeconds < 60) {
    return t('reports.seconds', { value: safeSeconds })
  }
  const minutes = Math.floor(safeSeconds / 60)
  if (minutes < 60) {
    return t('reports.minutes', { value: minutes })
  }
  const hours = Math.floor(minutes / 60)
  const restMinutes = minutes % 60
  return t('reports.hours', { hours, minutes: restMinutes })
}

function formatPercent(value: number) {
  return `${Number(value || 0).toFixed(2)}%`
}

function formatChartValue(value: number) {
  if (chartMetric.value === 'studySeconds') {
    return formatDuration(value)
  }
  if (chartMetric.value === 'accuracyRate') {
    return formatPercent(value)
  }
  return String(Math.round(value))
}

function formatScore(value: number, metricType: LeaderboardMetricType) {
  if (metricType === 'accuracy') {
    return formatPercent(value)
  }
  return Number(value || 0).toLocaleString()
}

function formatDateTime(value?: string | null) {
  if (!value) {
    return t('common.empty')
  }
  return new Date(value).toLocaleString()
}

function formatUserStatus(status?: string | null) {
  if (!status) {
    return t('common.empty')
  }
  const key = `users.status.${status}`
  const translated = t(key)
  return translated === key ? status : translated
}
</script>

<style scoped>
.reports-page {
  display: grid;
  gap: 16px;
}

.page-heading {
  align-items: center;
  display: flex;
  justify-content: space-between;
}

.page-heading h1 {
  font-size: 26px;
  line-height: 1.2;
  margin: 0;
}

.page-heading p {
  color: #64748b;
  margin: 8px 0 0;
}

.report-card {
  border-radius: 4px;
}

.toolbar {
  align-items: center;
  display: grid;
  gap: 10px;
  grid-template-columns: minmax(220px, 1fr) max-content max-content;
  justify-content: start;
  margin-bottom: 16px;
}

.report-toolbar {
  grid-template-columns: minmax(360px, 420px) minmax(280px, 1fr) max-content max-content;
}

.leaderboard-toolbar {
  grid-template-columns: 180px 180px 210px max-content max-content;
}

.toolbar :deep(.el-date-editor),
.toolbar :deep(.el-input),
.toolbar :deep(.el-select) {
  min-width: 0;
  width: 100%;
}

.toolbar :deep(.el-button) {
  justify-self: start;
  min-width: 88px;
}

.report-body {
  display: grid;
  gap: 16px;
}

.summary-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(7, minmax(120px, 1fr));
}

.summary-card {
  align-items: center;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  display: flex;
  gap: 12px;
  min-height: 78px;
  padding: 14px;
}

.summary-card .el-icon {
  background: #e8f3ff;
  border-radius: 4px;
  color: #2563eb;
  flex: 0 0 auto;
  font-size: 20px;
  height: 36px;
  width: 36px;
}

.summary-card div {
  display: grid;
  gap: 5px;
}

.summary-card span {
  color: #64748b;
  font-size: 12px;
}

.summary-card strong {
  color: #111827;
  font-size: 20px;
}

.chart-panel {
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  padding: 16px;
}

.section-heading,
.card-header {
  align-items: center;
  display: flex;
  font-weight: 700;
  justify-content: space-between;
}

.section-heading h2 {
  font-size: 18px;
  margin: 0 0 6px;
}

.section-heading span,
.card-header span:last-child {
  color: #64748b;
  font-size: 13px;
  font-weight: 500;
}

.trend-chart {
  height: 280px;
  margin-top: 12px;
  width: 100%;
}

.trend-chart svg {
  display: block;
  height: 100%;
  width: 100%;
}

.axis {
  stroke: #cbd5e1;
  stroke-width: 1;
}

.area {
  fill: #dbeafe;
  opacity: 0.75;
  stroke: none;
}

.trend-line {
  fill: none;
  stroke: #2563eb;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 3;
}

.trend-point {
  fill: #ffffff;
  stroke: #2563eb;
  stroke-width: 2;
}

.axis-label {
  fill: #64748b;
  font-size: 11px;
}

.axis-label.end {
  text-anchor: end;
}

.stack-cell {
  display: grid;
  gap: 4px;
}

.stack-cell span {
  color: #64748b;
  font-size: 12px;
}

.el-pagination {
  justify-content: flex-end;
  margin-top: 16px;
}

@media (max-width: 1280px) {
  .summary-grid {
    grid-template-columns: repeat(3, minmax(160px, 1fr));
  }
}

@media (max-width: 960px) {
  .toolbar,
  .leaderboard-toolbar,
  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
