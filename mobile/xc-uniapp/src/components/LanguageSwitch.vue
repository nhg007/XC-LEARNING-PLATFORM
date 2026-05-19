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
  background: transparent;
  border: 0;
  box-shadow: none;
  display: flex;
  gap: 10rpx;
  padding: 0;
}

.language-switch.hero {
  -webkit-backdrop-filter: none !important;
  backdrop-filter: none !important;
  background: none !important;
  background-color: transparent !important;
  border: 0 !important;
  border-radius: 0 !important;
  box-shadow: none !important;
  filter: none !important;
  padding: 0 !important;
}

.language-switch.hero::before,
.language-switch.hero::after {
  content: none !important;
  display: none !important;
}

.locale-button {
  align-items: center;
  background: linear-gradient(180deg, #ffffff 0%, #f0f5f6 100%);
  border: 1px solid var(--xc-border, rgba(15, 23, 42, 0.08));
  border-bottom: 5rpx solid var(--xc-button-secondary-edge, #d5e2e6);
  border-radius: 16rpx;
  box-sizing: border-box;
  color: var(--xc-muted, #475569);
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
  background: linear-gradient(180deg, var(--xc-button-primary-top, #27c0a3) 0%, var(--xc-primary, #128475) 60%, var(--xc-primary-strong, #0b665d) 100%);
  border-color: var(--xc-primary, #128475);
  border-bottom-color: var(--xc-primary-strong, #0b665d);
  color: #ffffff;
}

.language-switch.hero .locale-button {
  background: transparent !important;
  border-color: transparent !important;
  border-bottom-color: transparent !important;
  box-shadow: none !important;
  color: var(--xc-hero-muted, #e2e8f0) !important;
  min-width: 64rpx;
}

.language-switch.hero .locale-button.active {
  background: #ffffff !important;
  border-color: #ffffff !important;
  border-bottom-color: #d5e2e6 !important;
  box-shadow: none !important;
  color: var(--xc-ink, #102033) !important;
}
</style>
