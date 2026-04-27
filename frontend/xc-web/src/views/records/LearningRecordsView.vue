<template>
  <main class="app-shell">
    <header class="topbar">
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
      <v-card class="summary-card" elevation="0" rounded="lg">
        <span>{{ t('records.totalStudyTime') }}</span>
        <strong>{{ formatDuration(summary.totalStudySeconds) }}</strong>
      </v-card>
      <v-card class="summary-card" elevation="0" rounded="lg">
        <span>{{ t('records.exerciseCount') }}</span>
        <strong>{{ summary.totalExerciseCount }}</strong>
      </v-card>
      <v-card class="summary-card" elevation="0" rounded="lg">
        <span>{{ t('records.accuracy') }}</span>
        <strong>{{ summary.overallAccuracyRate }}%</strong>
      </v-card>
      <v-card class="summary-card" elevation="0" rounded="lg">
        <span>{{ t('records.longestStreak') }}</span>
        <strong>{{ t('units.days', { count: summary.longestStreakDays }) }}</strong>
      </v-card>
    </section>

    <v-card class="panel" elevation="0" rounded="lg">
      <div class="panel-title">{{ t('records.dailyStats') }}</div>
      <v-progress-linear v-if="loading" class="panel-loader" color="primary" indeterminate />
      <v-table density="comfortable">
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
      <div v-if="dailyStats.length === 0 && !loading" class="empty-state">{{ t('records.emptyDailyStats') }}</div>
    </v-card>

    <v-card class="panel" elevation="0" rounded="lg">
      <div class="table-toolbar">
        <div class="panel-title">{{ t('records.events') }}</div>
        <v-select
          v-model="eventType"
          clearable
          density="compact"
          hide-details
          item-title="label"
          item-value="value"
          :items="eventTypeOptions"
          :label="t('records.allTypes')"
          style="max-width: 180px"
          variant="outlined"
          @update:model-value="() => loadEvents(1)"
        />
      </div>
      <v-progress-linear v-if="eventLoading" class="panel-loader" color="primary" indeterminate />
      <v-table density="comfortable">
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
      <div v-if="events.length === 0 && !eventLoading" class="empty-state">{{ t('records.emptyEvents') }}</div>
      <v-pagination
        v-if="total > pageSize"
        v-model="page"
        class="pagination"
        :length="pageCount"
        rounded="circle"
        @update:model-value="loadEvents"
      />
    </v-card>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import LocaleSwitch from '@/components/LocaleSwitch.vue'
import { fetchDailyStats, fetchLearningSummary, fetchStudyEvents } from '../../api/stats'
import type { DailyStat, LearningSummary, StudyEvent } from '../../types/api'

const { t } = useI18n()
const loading = ref(false)
const eventLoading = ref(false)
const page = ref(1)
const pageSize = 20
const total = ref(0)
const eventType = ref<string | null>(null)
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
const eventTypeOptions = computed(() => [
  { label: t('records.eventTypes.exercise'), value: 'exercise' },
  { label: t('records.eventTypes.vocab'), value: 'vocab' },
  { label: t('records.eventTypes.dialogue'), value: 'dialogue' },
  { label: t('records.eventTypes.matching_game'), value: 'matching_game' }
])
const pageCount = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

async function loadRecords() {
  loading.value = true
  try {
    const [summaryData, dailyData] = await Promise.all([fetchLearningSummary(), fetchDailyStats(14)])
    summary.value = summaryData
    dailyStats.value = dailyData
    await loadEvents(page.value)
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

function formatDuration(seconds: number) {
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  return hours > 0 ? t('units.durationHoursMinutes', { hours, minutes }) : t('units.durationMinutes', { minutes })
}

function eventTypeLabel(type: string) {
  return t(`records.eventTypes.${type}`)
}

function resultLabel(result: string | null) {
  return result ? t(`records.results.${result}`) : '-'
}

onMounted(loadRecords)
</script>

<style scoped>
h1 {
  font-size: 28px;
  margin: 0;
}

p {
  color: #64748b;
  margin: 8px 0 0;
}

.top-actions,
.table-toolbar {
  align-items: center;
  display: flex;
  gap: 10px;
}

.table-toolbar {
  justify-content: space-between;
  margin-bottom: 14px;
}

.summary-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  margin-bottom: 18px;
}

.summary-card,
.panel {
  border: 1px solid #e5e7eb;
}

.summary-card {
  padding: 16px;
}

.summary-card span {
  color: #64748b;
  display: block;
  font-size: 13px;
}

.summary-card strong {
  display: block;
  font-size: 22px;
  margin-top: 8px;
}

.panel {
  margin-bottom: 18px;
  padding: 18px;
}

.panel-loader {
  margin-bottom: 8px;
}

.panel-title {
  font-size: 18px;
  font-weight: 700;
}

.empty-state {
  color: #64748b;
  padding: 28px 0 12px;
  text-align: center;
}

.pagination {
  margin-top: 14px;
}

@media (max-width: 720px) {
  .table-toolbar {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
