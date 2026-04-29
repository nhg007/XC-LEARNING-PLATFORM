<template>
  <view :class="['language-switch', variant]">
    <button
      v-for="item in supportedLocales"
      :key="item.value"
      size="mini"
      :class="['locale-button', { active: locale === item.value }]"
      @click="setLocale(item.value)"
    >
      {{ label(item.value) }}
    </button>
  </view>
</template>

<script setup lang="ts">
import { useI18n, type MobileLocale } from '../i18n'

withDefaults(defineProps<{
  variant?: 'surface' | 'hero'
}>(), {
  variant: 'surface'
})

const { locale, supportedLocales, setLocale } = useI18n()

function label(value: MobileLocale) {
  const labels: Record<MobileLocale, string> = {
    'zh-CN': '中',
    en: 'EN',
    ru: 'RU'
  }
  return labels[value]
}
</script>

<style scoped>
.language-switch {
  align-items: center;
  display: flex;
  gap: 8rpx;
}

.locale-button {
  align-items: center;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 8rpx;
  box-sizing: border-box;
  color: #475569;
  display: flex;
  font-size: 22rpx;
  font-weight: 700;
  height: 58rpx;
  justify-content: center;
  line-height: 58rpx;
  margin: 0;
  min-height: 58rpx;
  min-width: 72rpx;
  padding: 0;
  text-align: center;
}

.locale-button.active {
  background: #102033;
  color: #ffffff;
}

.language-switch.hero .locale-button {
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(255, 255, 255, 0.16);
  color: #e2e8f0;
}

.language-switch.hero .locale-button.active {
  background: #ffffff;
  color: #102033;
}
</style>
