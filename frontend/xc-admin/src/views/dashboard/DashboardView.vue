<template>
  <section class="dashboard-page">
    <div class="page-heading">
      <div>
        <h1>{{ t('dashboard.title') }}</h1>
        <p>{{ t('dashboard.subtitle') }}</p>
      </div>
      <el-button :loading="loading" :icon="Refresh" @click="reload">
        {{ t('common.refresh') }}
      </el-button>
    </div>

    <div v-loading="loading" class="metric-sections">
      <section v-for="group in metricGroups" :key="group.title" class="metric-section">
        <div class="section-heading">
          <h2>{{ group.title }}</h2>
        </div>
        <div class="metric-grid">
          <el-card
            v-for="item in group.items"
            :key="item.label"
            shadow="never"
            class="metric-card"
            :class="{ 'metric-card--link': item.route }"
            :tabindex="item.route ? 0 : undefined"
            @click="openMetric(item)"
            @keyup.enter="openMetric(item)"
          >
            <div class="metric-icon">
              <el-icon>
                <component :is="item.icon" />
              </el-icon>
            </div>
            <div class="metric-content">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </el-card>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, type Component } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter, type LocationQueryRaw, type RouteLocationRaw } from 'vue-router'
import { Collection, DataAnalysis, Files, Refresh, School, User, Wallet } from '@element-plus/icons-vue'
import { fetchDashboardSummary } from '@/api/dashboard'
import type { AdminDashboardSummary } from '@/types/api'

const { t } = useI18n()
const router = useRouter()
const loading = ref(false)
const summary = ref<AdminDashboardSummary>({
  userCount: 0,
  activeUserCount: 0,
  disabledUserCount: 0,
  trialUserCount: 0,
  todayNewUserCount: 0,
  activeMembershipCount: 0,
  activePlanCount: 0,
  todayOrderCount: 0,
  pendingOrderCount: 0,
  paidOrderCount: 0,
  todayPaidAmount: 0,
  classCount: 0,
  classMemberCount: 0,
  pendingClassMemberCount: 0,
  todayActiveClassCount: 0,
  todayStudyEventCount: 0,
  vocabListCount: 0,
  inactiveVocabListCount: 0,
  vocabItemCount: 0,
  exerciseSetCount: 0,
  inactiveExerciseSetCount: 0,
  sentenceExerciseCount: 0,
  videoMaterialCount: 0,
  inactiveVideoMaterialCount: 0,
  dialogueLineCount: 0
})

interface MetricItem {
  label: string
  value: number | string
  icon: Component
  route?: RouteLocationRaw
}

const metricGroups = computed(() => [
  {
    title: t('dashboard.groups.userMembership'),
    items: [
      { label: t('dashboard.userCount'), value: summary.value.userCount, icon: User, route: routeTo('/users') },
      { label: t('dashboard.activeUserCount'), value: summary.value.activeUserCount, icon: User, route: routeTo('/users', { status: 'active' }) },
      { label: t('dashboard.disabledUserCount'), value: summary.value.disabledUserCount, icon: User, route: routeTo('/users', { status: 'disabled' }) },
      { label: t('dashboard.todayNewUserCount'), value: summary.value.todayNewUserCount, icon: DataAnalysis, route: routeTo('/users', { createdFrom: todayStartIso() }) },
      { label: t('dashboard.trialUserCount'), value: summary.value.trialUserCount, icon: Wallet, route: routeTo('/users', { accessLevel: 'trial' }) },
      { label: t('dashboard.activeMembershipCount'), value: summary.value.activeMembershipCount, icon: Wallet, route: routeTo('/users', { accessLevel: 'member' }) },
      { label: t('dashboard.activePlanCount'), value: summary.value.activePlanCount, icon: Wallet, route: routeTo('/memberships', { tab: 'plans', status: 'active' }) }
    ] satisfies MetricItem[]
  },
  {
    title: t('dashboard.groups.orders'),
    items: [
      { label: t('dashboard.todayOrderCount'), value: summary.value.todayOrderCount, icon: Files, route: routeTo('/memberships', { tab: 'orders', createdFrom: todayStartIso() }) },
      { label: t('dashboard.pendingOrderCount'), value: summary.value.pendingOrderCount, icon: Files, route: routeTo('/memberships', { tab: 'orders', status: 'pending' }) },
      { label: t('dashboard.paidOrderCount'), value: summary.value.paidOrderCount, icon: Files, route: routeTo('/memberships', { tab: 'orders', status: 'paid' }) },
      { label: t('dashboard.todayPaidAmount'), value: formatAmount(summary.value.todayPaidAmount), icon: DataAnalysis, route: routeTo('/memberships', { tab: 'orders', status: 'paid', createdFrom: todayStartIso() }) }
    ] satisfies MetricItem[]
  },
  {
    title: t('dashboard.groups.classLearning'),
    items: [
      { label: t('dashboard.classCount'), value: summary.value.classCount, icon: School, route: routeTo('/classrooms', { status: 'active' }) },
      { label: t('dashboard.classMemberCount'), value: summary.value.classMemberCount, icon: School },
      { label: t('dashboard.pendingClassMemberCount'), value: summary.value.pendingClassMemberCount, icon: School },
      { label: t('dashboard.todayActiveClassCount'), value: summary.value.todayActiveClassCount, icon: DataAnalysis },
      { label: t('dashboard.todayStudyEventCount'), value: summary.value.todayStudyEventCount, icon: DataAnalysis }
    ] satisfies MetricItem[]
  },
  {
    title: t('dashboard.groups.contentAssets'),
    items: [
      { label: t('dashboard.vocabListCount'), value: summary.value.vocabListCount, icon: Collection, route: routeTo('/content', { tab: 'lists', status: 'active' }) },
      { label: t('dashboard.inactiveVocabListCount'), value: summary.value.inactiveVocabListCount, icon: Collection, route: routeTo('/content', { tab: 'lists', status: 'inactive' }) },
      { label: t('dashboard.vocabItemCount'), value: summary.value.vocabItemCount, icon: Collection, route: routeTo('/content', { tab: 'items', status: 'active' }) },
      { label: t('dashboard.exerciseSetCount'), value: summary.value.exerciseSetCount, icon: Files, route: routeTo('/content', { tab: 'sets', status: 'active' }) },
      { label: t('dashboard.inactiveExerciseSetCount'), value: summary.value.inactiveExerciseSetCount, icon: Files, route: routeTo('/content', { tab: 'sets', status: 'inactive' }) },
      { label: t('dashboard.sentenceExerciseCount'), value: summary.value.sentenceExerciseCount, icon: Files, route: routeTo('/content', { tab: 'exercises', status: 'active' }) },
      { label: t('dashboard.videoMaterialCount'), value: summary.value.videoMaterialCount, icon: Files, route: routeTo('/content', { tab: 'materials', status: 'active' }) },
      { label: t('dashboard.inactiveVideoMaterialCount'), value: summary.value.inactiveVideoMaterialCount, icon: Files, route: routeTo('/content', { tab: 'materials', status: 'inactive' }) },
      { label: t('dashboard.dialogueLineCount'), value: summary.value.dialogueLineCount, icon: Files, route: routeTo('/content', { tab: 'lines' }) }
    ] satisfies MetricItem[]
  }
])

async function reload() {
  loading.value = true
  try {
    summary.value = await fetchDashboardSummary()
  } finally {
    loading.value = false
  }
}

function formatAmount(value: number) {
  return new Intl.NumberFormat(undefined, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(Number(value || 0))
}

function routeTo(path: string, query?: LocationQueryRaw): RouteLocationRaw {
  return { path, query }
}

function todayStartIso() {
  const date = new Date()
  date.setHours(0, 0, 0, 0)
  return date.toISOString()
}

function openMetric(item: MetricItem) {
  if (item.route) {
    void router.push(item.route)
  }
}

onMounted(reload)
</script>

<style scoped>
.dashboard-page {
  display: grid;
  gap: 20px;
}

.page-heading {
  align-items: center;
  display: flex;
  justify-content: space-between;
}

h1 {
  font-size: 24px;
  margin: 0;
}

p {
  color: #64748b;
  margin: 8px 0 0;
}

.metric-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.metric-sections {
  display: grid;
  gap: 22px;
}

.metric-section {
  display: grid;
  gap: 12px;
}

.section-heading {
  align-items: center;
  border-bottom: 1px solid #dbe3ef;
  display: flex;
  min-height: 34px;
}

h2 {
  color: #172033;
  font-size: 16px;
  margin: 0;
}

.metric-card :deep(.el-card__body) {
  align-items: center;
  display: flex;
  gap: 16px;
}

.metric-card--link {
  cursor: pointer;
  transition:
    border-color 0.16s ease,
    box-shadow 0.16s ease,
    transform 0.16s ease;
}

.metric-card--link:hover,
.metric-card--link:focus-visible {
  border-color: #93c5fd;
  box-shadow: 0 10px 24px rgb(37 99 235 / 12%);
  outline: none;
  transform: translateY(-1px);
}

.metric-icon {
  align-items: center;
  background: #eff6ff;
  border-radius: 8px;
  color: #2563eb;
  display: flex;
  font-size: 24px;
  height: 52px;
  justify-content: center;
  width: 52px;
}

.metric-content {
  display: grid;
  gap: 8px;
}

.metric-content span {
  color: #64748b;
  font-size: 13px;
}

.metric-content strong {
  color: #172033;
  font-size: 28px;
}
</style>
