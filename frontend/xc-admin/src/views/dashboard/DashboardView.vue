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

    <div v-loading="loading" class="metric-grid">
      <el-card v-for="item in metrics" :key="item.label" shadow="never" class="metric-card">
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
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Collection, DataAnalysis, Files, Refresh, School, User, Wallet } from '@element-plus/icons-vue'
import { fetchDashboardSummary } from '@/api/dashboard'
import type { AdminDashboardSummary } from '@/types/api'

const { t } = useI18n()
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

const metrics = computed(() => [
  { label: t('dashboard.userCount'), value: summary.value.userCount, icon: User },
  { label: t('dashboard.activeUserCount'), value: summary.value.activeUserCount, icon: User },
  { label: t('dashboard.disabledUserCount'), value: summary.value.disabledUserCount, icon: User },
  { label: t('dashboard.trialUserCount'), value: summary.value.trialUserCount, icon: Wallet },
  { label: t('dashboard.todayNewUserCount'), value: summary.value.todayNewUserCount, icon: DataAnalysis },
  { label: t('dashboard.activeMembershipCount'), value: summary.value.activeMembershipCount, icon: Wallet },
  { label: t('dashboard.activePlanCount'), value: summary.value.activePlanCount, icon: Wallet },
  { label: t('dashboard.todayOrderCount'), value: summary.value.todayOrderCount, icon: Files },
  { label: t('dashboard.pendingOrderCount'), value: summary.value.pendingOrderCount, icon: Files },
  { label: t('dashboard.paidOrderCount'), value: summary.value.paidOrderCount, icon: Files },
  { label: t('dashboard.todayPaidAmount'), value: formatAmount(summary.value.todayPaidAmount), icon: DataAnalysis },
  { label: t('dashboard.classCount'), value: summary.value.classCount, icon: School },
  { label: t('dashboard.classMemberCount'), value: summary.value.classMemberCount, icon: School },
  { label: t('dashboard.pendingClassMemberCount'), value: summary.value.pendingClassMemberCount, icon: School },
  { label: t('dashboard.todayActiveClassCount'), value: summary.value.todayActiveClassCount, icon: DataAnalysis },
  { label: t('dashboard.todayStudyEventCount'), value: summary.value.todayStudyEventCount, icon: DataAnalysis },
  { label: t('dashboard.vocabListCount'), value: summary.value.vocabListCount, icon: Collection },
  { label: t('dashboard.inactiveVocabListCount'), value: summary.value.inactiveVocabListCount, icon: Collection },
  { label: t('dashboard.vocabItemCount'), value: summary.value.vocabItemCount, icon: Collection },
  { label: t('dashboard.exerciseSetCount'), value: summary.value.exerciseSetCount, icon: Files },
  { label: t('dashboard.inactiveExerciseSetCount'), value: summary.value.inactiveExerciseSetCount, icon: Files },
  { label: t('dashboard.sentenceExerciseCount'), value: summary.value.sentenceExerciseCount, icon: Files },
  { label: t('dashboard.videoMaterialCount'), value: summary.value.videoMaterialCount, icon: Files },
  { label: t('dashboard.inactiveVideoMaterialCount'), value: summary.value.inactiveVideoMaterialCount, icon: Files },
  { label: t('dashboard.dialogueLineCount'), value: summary.value.dialogueLineCount, icon: Files }
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

.metric-card :deep(.el-card__body) {
  align-items: center;
  display: flex;
  gap: 16px;
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
