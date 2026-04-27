<template>
  <v-menu location="bottom end">
    <template #activator="{ props }">
      <v-btn v-bind="props" prepend-icon="mdi-translate" :variant="variant">
        {{ currentLabel }}
      </v-btn>
    </template>
    <v-list density="compact">
      <v-list-item
        v-for="item in locales"
        :key="item.value"
        :active="locale === item.value"
        @click="changeLocale(item.value)"
      >
        <v-list-item-title>{{ item.label }}</v-list-item-title>
      </v-list-item>
    </v-list>
  </v-menu>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { setLocale, type StudentLocale } from '@/plugins/i18n'

withDefaults(defineProps<{
  variant?: 'text' | 'tonal' | 'outlined' | 'flat' | 'elevated' | 'plain'
}>(), {
  variant: 'text'
})

const { locale, t } = useI18n()
const locales = computed(() => [
  { value: 'zh-CN' as const, label: t('language.zhCN') },
  { value: 'en' as const, label: t('language.en') },
  { value: 'ru' as const, label: t('language.ru') }
])
const currentLabel = computed(() => locales.value.find(item => item.value === locale.value)?.label || t('language.label'))

function changeLocale(value: StudentLocale) {
  setLocale(value)
}
</script>
