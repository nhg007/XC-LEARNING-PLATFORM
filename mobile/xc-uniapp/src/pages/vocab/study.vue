<template>
  <view class="page">
    <view class="hero">
      <view class="hero-top">
        <view class="hero-copy">
          <text class="eyebrow">{{ t('vocab.title') }}</text>
          <view class="hero-title-row">
            <text class="title">{{ t('vocab.title') }}</text>
            <text class="progress-pill">{{ progressLabel }}</text>
          </view>
          <text class="subtitle">{{ t('vocab.studySubtitle') }}</text>
        </view>
        <LanguageSwitch variant="hero" />
      </view>
      <view class="progress-track">
        <view class="progress-fill" :style="{ width: progressPercent }" />
      </view>
    </view>

    <view class="study-card" @click="toggleCard">
      <view class="card-meta">
        <text class="side-label">{{ currentSideLabel }}</text>
        <text class="meta-value">{{ meaningLanguageLabel }}</text>
      </view>
      <template v-if="currentItem">
        <view v-if="!flipped" class="face">
          <text class="hanzi">{{ currentItem.hanzi }}</text>
          <text class="pinyin" :class="{ hidden: !showPinyin }">{{ currentItem.pinyin || '-' }}</text>
        </view>
        <view v-else class="face">
          <text class="meaning">{{ currentMeaning }}</text>
          <text v-if="currentItem.exampleSentence" class="example">{{ currentItem.exampleSentence }}</text>
        </view>
      </template>
      <text v-else class="muted">{{ t('vocab.emptyItems') }}</text>
    </view>

    <view class="control-panel">
      <view class="panel-header">
        <text class="panel-title">{{ t('vocab.referenceLanguage') }}</text>
        <view class="language-row">
          <button class="small-button" :class="{ active: meaningLanguage === 'ru' }" @click="meaningLanguage = 'ru'">{{ t('common.ru') }}</button>
          <button class="small-button" :class="{ active: meaningLanguage === 'en' }" @click="meaningLanguage = 'en'">{{ t('common.en') }}</button>
        </view>
      </view>

      <view class="actions">
        <button class="tool-button" @click="showPinyin = !showPinyin">{{ showPinyin ? t('vocab.hidePinyin') : t('vocab.showPinyin') }}</button>
        <button class="tool-button" @click="toggleCard">{{ flipped ? t('vocab.showHanzi') : t('vocab.showMeaning') }}</button>
        <button class="tool-button" :disabled="!currentItem" @click="toggleFavorite">{{ currentItem?.favorite ? t('vocab.unfavorite') : t('vocab.favorite') }}</button>
        <button class="tool-button" :disabled="audioPlaying || !currentItem" @click="playPronunciation">{{ audioButtonLabel }}</button>
      </view>
    </view>

    <view class="nav-row">
      <button class="nav-button secondary" :disabled="currentIndex <= 0" @click="previousCard">{{ t('vocab.previous') }}</button>
      <button class="nav-button primary" :disabled="saving || !currentItem" @click="nextCard">{{ nextButtonLabel }}</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { onHide, onLoad, onUnload } from '@dcloudio/uni-app'
import {
  favoriteVocabItem,
  fetchVocabItems,
  fetchVocabProgress,
  unfavoriteVocabItem,
  updateVocabProgress
} from '../../api/vocab'
import { resolveApiResourceUrl } from '../../api/http'
import { useAudioPlayer } from '../../composables/useAudioPlayer'
import { usePreferences } from '../../composables/usePreferences'
import { setPageTitle, useI18n } from '../../i18n'
import LanguageSwitch from '../../components/LanguageSwitch.vue'
import type { VocabItem, VocabProgress } from '../../types/api'
import { requireLogin } from '../../utils/navigation'

const { locale, t } = useI18n()
const preferences = usePreferences()
const vocabListId = ref(0)
const saving = ref(false)
const audio = useAudioPlayer()
const flipped = ref(false)
const showPinyin = ref(false)
const meaningLanguage = ref<'ru' | 'en'>(locale.value === 'en' ? 'en' : 'ru')
const currentIndex = ref(0)
const items = ref<VocabItem[]>([])
const progress = ref<VocabProgress>({
  vocabListId: 0,
  currentIndex: 0,
  lastVocabItemId: null,
  reviewedCount: 0,
  totalCount: 0
})

const currentItem = computed(() => items.value[currentIndex.value])
const progressTotal = computed(() => progress.value.totalCount || items.value.length)
const progressCurrent = computed(() => (items.value.length === 0 ? 0 : currentIndex.value + 1))
const currentMeaning = computed(() => {
  const item = currentItem.value
  if (!item) {
    return ''
  }
  return meaningLanguage.value === 'ru' ? item.meaningRu || '-' : item.meaningEn || '-'
})
const progressLabel = computed(() => {
  return `${progressCurrent.value}/${progressTotal.value || 0}`
})
const progressPercent = computed(() => {
  if (!progressTotal.value) {
    return '0%'
  }
  return `${Math.min(100, Math.round((progressCurrent.value / progressTotal.value) * 100))}%`
})
const currentSideLabel = computed(() => (flipped.value ? t('vocab.meaningSide') : t('vocab.wordSide')))
const meaningLanguageLabel = computed(() => (meaningLanguage.value === 'ru' ? t('common.ru') : t('common.en')))
const audioPlaying = computed(() => audio.playing.value)
const audioButtonLabel = computed(() => (audio.playing.value ? t('common.playing') : t('vocab.playPronunciation')))
const nextButtonLabel = computed(() => (saving.value ? t('common.loading') : t('vocab.next')))

async function loadCards() {
  const [itemPage, currentProgress] = await Promise.all([
    fetchVocabItems(vocabListId.value),
    fetchVocabProgress(vocabListId.value)
  ])
  items.value = itemPage.records
  progress.value = currentProgress
  currentIndex.value = Math.min(currentProgress.currentIndex, Math.max(itemPage.records.length - 1, 0))
  flipped.value = false
  showPinyin.value = false
}

function toggleCard() {
  if (!currentItem.value) {
    return
  }
  flipped.value = !flipped.value
}

function previousCard() {
  audio.stop()
  currentIndex.value = Math.max(0, currentIndex.value - 1)
  flipped.value = false
  showPinyin.value = false
}

async function nextCard() {
  const item = currentItem.value
  if (!item) {
    return
  }
  audio.stop()
  const nextIndex = Math.min(currentIndex.value + 1, Math.max(items.value.length - 1, 0))
  const reviewedCount = Math.max(progress.value.reviewedCount, currentIndex.value + 1)
  saving.value = true
  try {
    progress.value = await updateVocabProgress(vocabListId.value, {
      currentIndex: nextIndex,
      lastVocabItemId: item.id,
      reviewedCount
    })
    currentIndex.value = nextIndex
    flipped.value = false
    showPinyin.value = false
  } finally {
    saving.value = false
  }
}

function playPronunciation() {
  const item = currentItem.value
  if (!item) {
    return
  }
  audio.play(resolveApiResourceUrl(item.audioUrl))
}

async function toggleFavorite() {
  const item = currentItem.value
  if (!item) {
    return
  }
  const result = item.favorite ? await unfavoriteVocabItem(item.id) : await favoriteVocabItem(item.id)
  item.favorite = result.favorite
  void uni.showToast({ icon: 'none', title: result.favorite ? t('vocab.favorited') : t('vocab.unfavorited') })
}

onLoad((query) => {
  setPageTitle('vocab.title')
  if (!requireLogin()) {
    return
  }
  vocabListId.value = Number(query?.listId || 0)
  if (!vocabListId.value) {
    void uni.navigateBack()
    return
  }
  void initializePage()
})

async function initializePage() {
  const preference = await preferences.loadPreference()
  meaningLanguage.value = preference.vocabMeaningLanguage
  await loadCards()
}

watch(locale, () => {
  setPageTitle('vocab.title')
})

onHide(() => {
  audio.stop()
})

onUnload(() => {
  audio.stop()
})
</script>

<style scoped>
.page {
  background: #eef5f7;
  box-sizing: border-box;
  min-height: 100vh;
  padding: 0 24rpx 32rpx;
}

.hero {
  background: #102033;
  border-bottom-left-radius: 32rpx;
  border-bottom-right-radius: 32rpx;
  box-sizing: border-box;
  margin: 0 -24rpx 24rpx;
  padding: 30rpx 24rpx 28rpx;
}

.hero-top {
  align-items: flex-start;
  display: flex;
  gap: 20rpx;
  justify-content: space-between;
}

.hero-copy {
  flex: 1;
  min-width: 0;
}

.eyebrow {
  color: #7dd3c7;
  display: block;
  font-size: 22rpx;
  font-weight: 700;
}

.hero-title-row {
  align-items: center;
  display: flex;
  gap: 16rpx;
  margin-top: 10rpx;
}

.title {
  color: #ffffff;
  display: inline-block;
  font-size: 46rpx;
  font-weight: 700;
  line-height: 1.15;
}

.progress-pill {
  background: rgba(20, 121, 111, 0.92);
  border-radius: 999rpx;
  color: #ffffff;
  flex-shrink: 0;
  font-size: 24rpx;
  font-weight: 700;
  padding: 8rpx 18rpx;
}

.subtitle {
  color: #cbd5e1;
  display: block;
  font-size: 26rpx;
  line-height: 1.5;
  margin-top: 14rpx;
}

.progress-track {
  background: rgba(255, 255, 255, 0.16);
  border-radius: 999rpx;
  height: 12rpx;
  margin-top: 28rpx;
  overflow: hidden;
}

.progress-fill {
  background: #7dd3c7;
  border-radius: inherit;
  height: 100%;
  transition: width 0.2s ease;
}

.study-card {
  background: #ffffff;
  border: 1px solid #d7e2ea;
  border-radius: 24rpx;
  box-sizing: border-box;
  box-shadow: 0 16rpx 44rpx rgba(15, 23, 42, 0.08);
  min-height: 520rpx;
  padding: 28rpx;
}

.card-meta {
  align-items: center;
  display: flex;
  justify-content: space-between;
}

.side-label {
  background: #e9f7f5;
  border-radius: 999rpx;
  color: #14796f;
  font-size: 24rpx;
  font-weight: 700;
  padding: 10rpx 18rpx;
}

.meta-value {
  color: #94a3b8;
  font-size: 24rpx;
  font-weight: 700;
}

.face {
  align-items: center;
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-height: 420rpx;
  text-align: center;
}

.hanzi {
  color: #102033;
  font-size: 104rpx;
  font-weight: 800;
  line-height: 1.1;
  max-width: 100%;
  word-break: break-word;
}

.pinyin {
  color: #14796f;
  display: block;
  font-size: 34rpx;
  line-height: 1.4;
  margin-top: 26rpx;
  min-height: 48rpx;
}

.pinyin.hidden {
  visibility: hidden;
}

.meaning {
  color: #102033;
  font-size: 40rpx;
  font-weight: 700;
  line-height: 1.45;
  max-width: 100%;
  word-break: break-word;
}

.example,
.muted {
  color: #64748b;
  font-size: 26rpx;
  line-height: 1.7;
  margin-top: 24rpx;
  text-align: center;
}

.control-panel {
  background: #ffffff;
  border: 1px solid #d7e2ea;
  border-radius: 24rpx;
  box-shadow: 0 12rpx 36rpx rgba(15, 23, 42, 0.06);
  margin-top: 22rpx;
  padding: 22rpx;
}

.panel-header {
  align-items: center;
  display: flex;
  gap: 18rpx;
  justify-content: space-between;
}

.panel-title {
  color: #475569;
  font-size: 26rpx;
  font-weight: 700;
}

.language-row {
  background: #f1f5f9;
  border-radius: 12rpx;
  display: flex;
  gap: 0;
  padding: 4rpx;
}

.actions {
  display: grid;
  gap: 16rpx;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 20rpx;
}

button {
  border: 0;
  border-radius: 8rpx;
  font-size: 28rpx;
  margin: 0;
}

button::after {
  border: 0;
}

.small-button {
  background: transparent;
  border-radius: 10rpx;
  color: #475569;
  font-size: 26rpx;
  line-height: 1;
  min-width: 132rpx;
  padding: 18rpx 0;
}

.small-button.active {
  background: #14796f;
  color: #ffffff;
  font-weight: 700;
}

.tool-button {
  align-items: center;
  background: #f8fafc;
  color: #102033;
  display: flex;
  font-size: 27rpx;
  font-weight: 700;
  justify-content: center;
  line-height: 1.25;
  min-height: 92rpx;
  padding: 0 16rpx;
  white-space: normal;
  word-break: break-word;
}

.tool-button[disabled] {
  color: #94a3b8;
}

.nav-row {
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(215, 226, 234, 0.86);
  border-radius: 24rpx;
  box-shadow: 0 18rpx 42rpx rgba(15, 23, 42, 0.1);
  display: grid;
  gap: 14rpx;
  grid-template-columns: 0.9fr 1.1fr;
  margin-top: 18rpx;
  padding: 14rpx;
  position: sticky;
  bottom: calc(18rpx + env(safe-area-inset-bottom));
  z-index: 5;
}

.nav-button {
  align-items: center;
  border-radius: 16rpx;
  box-sizing: border-box;
  display: flex;
  font-size: 28rpx;
  font-weight: 700;
  justify-content: center;
  line-height: 1;
  min-height: 82rpx;
  padding: 0 18rpx;
}

.secondary {
  background: #f8fafc;
  color: #475569;
}

.primary {
  background: #14796f;
  box-shadow: 0 12rpx 24rpx rgba(20, 121, 111, 0.2);
  color: #ffffff;
}

.nav-button[disabled] {
  box-shadow: none;
  color: #94a3b8;
  opacity: 1;
}
</style>
