<template>
  <section class="placeholder-page">
    <el-card shadow="never">
      <template #header>
        <div class="placeholder-header">
          <span>{{ t(String(route.meta.title || '')) }}</span>
          <el-tag type="info">{{ t('common.comingSoon') }}</el-tag>
        </div>
      </template>
      <p>{{ t('common.comingSoon') }}</p>
    </el-card>
  </section>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'

const route = useRoute()
const { t } = useI18n()
</script>

<style scoped>
.placeholder-page {
  display: grid;
  gap: 16px;
}

.placeholder-header {
  align-items: center;
  display: flex;
  font-weight: 700;
  justify-content: space-between;
}

p {
  color: #64748b;
  margin: 0;
}
</style>
