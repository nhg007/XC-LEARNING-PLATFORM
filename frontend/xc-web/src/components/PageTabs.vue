<template>
  <nav class="page-tabs-shell">
    <v-tabs
      :model-value="activeTab"
      color="primary"
      density="comfortable"
      show-arrows
      @update:model-value="openTab"
    >
      <v-tab v-for="item in tabs" :key="item.path" :value="item.path" :prepend-icon="item.icon">
        {{ item.title }}
      </v-tab>
    </v-tabs>
  </nav>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const tabs = computed(() => [
  { path: '/', title: t('app.title'), icon: 'mdi-home-outline' },
  { path: '/practice', title: t('practice.title'), icon: 'mdi-pencil-outline' },
  { path: '/classrooms', title: t('classroom.title'), icon: 'mdi-account-group-outline' },
  { path: '/records', title: t('records.title'), icon: 'mdi-chart-line' }
])
const activeTab = computed(() => route.path.startsWith('/vocab/') ? '/' : route.path)

async function openTab(value: unknown) {
  const path = String(value)
  if (path && path !== route.path) {
    await router.push(path)
  }
}
</script>

<style scoped>
.page-tabs-shell {
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
  margin: 0 auto;
  max-width: 1120px;
  padding: 0 24px;
}

@media (max-width: 720px) {
  .page-tabs-shell {
    padding: 0 12px;
  }
}
</style>
