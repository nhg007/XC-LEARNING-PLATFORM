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
  activeMembershipCount: 0,
  classCount: 0,
  todayStudyEventCount: 0,
  vocabListCount: 0,
  exerciseSetCount: 0
})

const metrics = computed(() => [
  { label: t('dashboard.userCount'), value: summary.value.userCount, icon: User },
  { label: t('dashboard.activeMembershipCount'), value: summary.value.activeMembershipCount, icon: Wallet },
  { label: t('dashboard.classCount'), value: summary.value.classCount, icon: School },
  { label: t('dashboard.todayStudyEventCount'), value: summary.value.todayStudyEventCount, icon: DataAnalysis },
  { label: t('dashboard.vocabListCount'), value: summary.value.vocabListCount, icon: Collection },
  { label: t('dashboard.exerciseSetCount'), value: summary.value.exerciseSetCount, icon: Files }
])

async function reload() {
  loading.value = true
  try {
    summary.value = await fetchDashboardSummary()
  } finally {
    loading.value = false
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
