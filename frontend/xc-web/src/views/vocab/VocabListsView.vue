<template>
  <main class="app-shell">
    <header class="topbar">
      <div>
        <h1>{{ t('vocab.title') }}</h1>
        <p>{{ t('vocab.listSubtitle') }}</p>
      </div>
      <div class="top-actions">
        <LocaleSwitch />
        <v-btn prepend-icon="mdi-star-outline" variant="tonal" @click="$router.push('/favorites')">{{ t('vocab.favorites') }}</v-btn>
        <v-btn prepend-icon="mdi-arrow-left" variant="text" @click="$router.push('/')">{{ t('common.back') }}</v-btn>
        <v-btn prepend-icon="mdi-refresh" variant="tonal" :loading="loading" @click="loadLists">{{ t('common.refresh') }}</v-btn>
      </div>
    </header>

    <v-card class="panel" elevation="0">
      <v-progress-linear v-if="loading" class="loading-line" color="primary" indeterminate />
      <div class="panel-head">
        <div>
          <span class="panel-kicker">{{ t('vocab.browseLists') }}</span>
          <h2>{{ t('vocab.listCount', { count: total }) }}</h2>
        </div>
      </div>

      <v-alert v-if="errorMessage && !loading" class="status-alert" type="error" variant="tonal">
        <div class="status-alert-content">
          <span>{{ errorMessage }}</span>
          <v-btn size="small" variant="tonal" @click="loadLists">{{ t('common.refresh') }}</v-btn>
        </div>
      </v-alert>

      <div v-else-if="!loading && lists.length === 0" class="empty-state">
        {{ t('vocab.emptyLists') }}
      </div>

      <div v-else-if="lists.length > 0" class="list-grid">
        <v-card v-for="item in lists" :key="item.id" class="list-card" elevation="0" @click="openList(item.id)">
          <span class="list-type">{{ item.listType }}</span>
          <h3>{{ item.name }}</h3>
          <p>{{ item.description || t('vocab.noDescription') }}</p>
          <div class="list-footer">
            <span>{{ item.level || t('common.basic') }}</span>
            <v-btn prepend-icon="mdi-arrow-right" variant="text">{{ t('vocab.openList') }}</v-btn>
          </div>
        </v-card>
      </div>
    </v-card>
  </main>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import LocaleSwitch from '@/components/LocaleSwitch.vue'
import { fetchVocabLists } from '../../api/vocab'
import type { VocabList } from '../../types/api'

const { t } = useI18n()
const router = useRouter()
const loading = ref(false)
const errorMessage = ref('')
const lists = ref<VocabList[]>([])
const total = ref(0)

async function loadLists() {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await fetchVocabLists(100)
    lists.value = result.records
    total.value = result.total
  } catch (error) {
    console.error(error)
    errorMessage.value = t('vocab.loadFailed')
  } finally {
    loading.value = false
  }
}

async function openList(id: number) {
  await router.push(`/vocab/${id}`)
}

onMounted(loadLists)
</script>

<style scoped>
h1 {
  font-size: 34px;
  line-height: 1.2;
  margin: 0;
}

h2,
h3,
p {
  margin: 0;
}

.topbar {
  align-items: center;
  background: #142033;
  border: 1px solid #23324a;
  border-radius: 8px;
  color: #f8fafc;
  display: flex;
  gap: 18px;
  justify-content: space-between;
  margin-bottom: 22px;
  padding: 30px;
}

.topbar p {
  color: #cbd5e1;
  margin-top: 8px;
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

.panel {
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  overflow: hidden;
  padding: 24px;
  position: relative;
}

.loading-line {
  left: 0;
  position: absolute;
  right: 0;
  top: 0;
}

.panel-head {
  align-items: center;
  display: flex;
  gap: 16px;
  justify-content: space-between;
}

.panel-kicker {
  color: #14796f;
  display: block;
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 8px;
}

.empty-state {
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  color: #64748b;
  margin-top: 20px;
  padding: 34px;
  text-align: center;
}

.status-alert {
  margin-top: 20px;
}

.status-alert-content {
  align-items: center;
  display: flex;
  gap: 12px;
  justify-content: space-between;
}

.list-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-top: 22px;
}

.list-card {
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  cursor: pointer;
  display: grid;
  gap: 12px;
  min-height: 178px;
  padding: 20px;
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

.list-card:hover {
  border-color: #9db8e8;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
  transform: translateY(-2px);
}

.list-type {
  background: #eef4ff;
  border: 1px solid #c7d7fe;
  border-radius: 4px;
  color: #1d4ed8;
  display: inline-flex;
  font-size: 13px;
  font-weight: 700;
  justify-self: start;
  line-height: 1.2;
  padding: 5px 9px;
}

.list-card h3 {
  color: #172033;
  font-size: 21px;
  line-height: 1.25;
}

.list-card p {
  color: #64748b;
  line-height: 1.5;
}

.list-footer {
  align-items: center;
  align-self: end;
  border-top: 1px solid #e5edf6;
  color: #475569;
  display: flex;
  justify-content: space-between;
  padding-top: 12px;
}

.list-footer :deep(.v-btn) {
  border-radius: 4px;
  letter-spacing: 0;
}

@media (max-width: 980px) {
  .topbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .top-actions {
    justify-content: flex-start;
  }

  .list-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  h1 {
    font-size: 28px;
  }

  .topbar,
  .panel {
    padding: 20px 16px;
  }

  .list-grid {
    grid-template-columns: 1fr;
  }
}
</style>
