<template>
  <nav class="page-tabs-shell">
    <div class="page-tabs-inner">
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
    </div>
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
  { path: '/favorites', title: t('vocab.favorites'), icon: 'mdi-star-outline' },
  { path: '/membership', title: t('membership.title'), icon: 'mdi-crown-outline' },
  { path: '/practice', title: t('practice.title'), icon: 'mdi-pencil-outline' },
  { path: '/dialogue', title: t('dialogue.title'), icon: 'mdi-movie-open-play-outline' },
  { path: '/matching', title: t('matching.title'), icon: 'mdi-vector-square' },
  { path: '/elimination', title: t('elimination.title'), icon: 'mdi-cards-outline' },
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
  border-bottom: 1px solid #d8e0ea;
}

.page-tabs-inner {
  box-sizing: border-box;
  margin: 0 auto;
  max-width: var(--xc-page-max-width);
  padding: 0 var(--xc-page-padding-x);
  width: 100%;
}

:deep(.v-tab) {
  border-radius: 0;
  font-size: 14px;
  height: 44px;
  letter-spacing: 0;
  min-width: 112px;
}

:deep(.v-tab--selected) {
  background: #eef4ff;
}

@media (max-width: 720px) {
  .page-tabs-inner {
    padding: 0 var(--xc-page-padding-x-mobile);
  }

  :deep(.v-tab) {
    min-width: 96px;
  }
}
</style>
