<template>
  <main class="error-page">
    <div class="error-code">403</div>
    <h1>{{ t('common.noPermission') }}</h1>
    <el-button type="primary" @click="$router.push('/dashboard')">{{ t('menus.dashboard') }}</el-button>
  </main>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
</script>

<style scoped>
.error-page {
  align-items: center;
  display: grid;
  gap: 18px;
  justify-items: center;
  min-height: calc(100vh - 112px);
}

.error-code {
  color: #2563eb;
  font-size: 96px;
  font-weight: 800;
  line-height: 1;
}

h1 {
  font-size: 24px;
  margin: 0;
}
</style>
