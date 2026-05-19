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

    <view v-if="childLists.length > 0" class="scope-card">
      <button class="scope-tab" :class="{ active: scopeTab === 'all' }" @click="scopeTab = 'all'">{{ t('vocab.scopeAll') }}</button>
      <button class="scope-tab" :class="{ active: scopeTab === 'lessons' }" @click="scopeTab = 'lessons'">{{ t('vocab.scopeLessons') }}</button>
    </view>

    <view v-if="scopeTab === 'lessons' && childLists.length > 0" class="lesson-list">
      <view v-for="lesson in childLists" :key="lesson.id" class="lesson-item" @click="openLesson(lesson.id)">
        <view>
          <text class="lesson-title">{{ lesson.name }}</text>
          <text class="muted">{{ lesson.description || lesson.level || currentList?.level || lesson.listType }}</text>
        </view>
        <text class="lesson-count">{{ t('vocab.wordCount', { count: lesson.totalActiveItemCount || lesson.activeItemCount }) }}</text>
      </view>
    </view>

    <template v-else>
    <view class="study-card" @click="toggleCard">
      <view class="card-meta">
        <text class="side-label">{{ currentSideLabel }}</text>
        <text class="status-chip" :class="{ learned: isCurrentLearned }">{{ currentStatusLabel }}</text>
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
        <button class="tool-button" :disabled="!currentStrokeOrderUrl" @click="openStrokeOrder">{{ t('vocab.strokeOrder') }}</button>
        <button class="tool-button" :disabled="!currentItem" @click="toggleFavorite">{{ currentItem?.favorite ? t('vocab.unfavorite') : t('vocab.favorite') }}</button>
        <button class="tool-button" :disabled="audioPlaying || !currentItem" @click="playPronunciation">{{ audioButtonLabel }}</button>
      </view>
    </view>

    <view class="nav-row">
      <button class="nav-button secondary" :disabled="currentIndex <= 0" @click="previousCard">{{ t('vocab.previous') }}</button>
      <button class="nav-button primary" :disabled="saving || !currentItem" @click="nextCard">{{ nextButtonLabel }}</button>
    </view>

    <view v-if="strokeDialogVisible" class="stroke-overlay" @click="closeStrokeOrder">
      <view class="stroke-panel" @click.stop>
        <view class="stroke-panel-header">
          <text class="stroke-title">{{ currentItem?.hanzi }} · {{ t('vocab.strokeOrder') }}</text>
          <button class="stroke-close" @click="closeStrokeOrder">×</button>
        </view>
        <template v-if="currentStrokeOrderUrl && !strokeImageFailed">
          <!-- #ifdef H5 -->
          <img
            :key="currentStrokeOrderUrl"
            class="stroke-image stroke-image-native"
            :src="currentStrokeOrderUrl"
            :alt="t('vocab.strokeOrder')"
            @load="strokeImageFailed = false"
            @error="handleStrokeImageError"
          />
          <!-- #endif -->
          <!-- #ifndef H5 -->
          <image
            :key="currentStrokeOrderUrl"
            class="stroke-image"
            mode="aspectFit"
            :src="currentStrokeOrderUrl"
            @load="strokeImageFailed = false"
            @error="handleStrokeImageError"
          />
          <!-- #endif -->
        </template>
        <view v-else-if="currentStrokeOrderUrl && strokeImageFailed" class="stroke-error">
          <text>{{ t('vocab.strokeOrderLoadFailed') }}</text>
        </view>
        <text v-else class="muted">{{ t('vocab.noStrokeOrder') }}</text>
      </view>
    </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { onHide, onLoad, onUnload } from '@dcloudio/uni-app'
import {
  favoriteVocabItem,
  fetchVocabItems,
  fetchVocabList,
  fetchVocabLists,
  fetchVocabProgress,
  unfavoriteVocabItem,
  updateVocabProgress
} from '../../api/vocab'
import { resolveApiResourceUrl } from '../../api/http'
import { useAudioPlayer } from '../../composables/useAudioPlayer'
import { usePreferences } from '../../composables/usePreferences'
import { setPageTitle, useI18n } from '../../i18n'
import LanguageSwitch from '../../components/LanguageSwitch.vue'
import type { VocabItem, VocabList, VocabProgress } from '../../types/api'
import { openVocabStudy, requireLogin } from '../../utils/navigation'

const { locale, t } = useI18n()
const preferences = usePreferences()
const vocabListId = ref(0)
const saving = ref(false)
const audio = useAudioPlayer()
const flipped = ref(false)
const showPinyin = ref(false)
const strokeDialogVisible = ref(false)
const strokeImageFailed = ref(false)
const meaningLanguage = ref<'ru' | 'en'>(locale.value === 'en' ? 'en' : 'ru')
const currentIndex = ref(0)
const cardStartedAt = ref(Date.now())
const scopeTab = ref<'all' | 'lessons'>('all')
const currentList = ref<VocabList | null>(null)
const childLists = ref<VocabList[]>([])
const items = ref<VocabItem[]>([])
const progress = ref<VocabProgress>({
  vocabListId: 0,
  currentIndex: 0,
  lastVocabItemId: null,
  reviewedCount: 0,
  learnedCount: 0,
  reviewingCount: 0,
  masteredCount: 0,
  totalCount: 0
})
const learnedStatuses = new Set(['learned', 'reviewing', 'mastered'])

const currentItem = computed(() => items.value[currentIndex.value])
const isCurrentLearned = computed(() => {
  const status = currentItem.value?.progressStatus
  return status ? learnedStatuses.has(status) : false
})
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
const currentStatusLabel = computed(() => {
  switch (currentItem.value?.progressStatus) {
    case 'mastered':
      return t('vocab.mastered')
    case 'reviewing':
      return t('vocab.reviewing')
    case 'learned':
      return t('vocab.learned')
    default:
      return t('vocab.unlearned')
  }
})
const meaningLanguageLabel = computed(() => (meaningLanguage.value === 'ru' ? t('common.ru') : t('common.en')))
const audioPlaying = computed(() => audio.playing.value)
const audioButtonLabel = computed(() => (audio.playing.value ? t('common.playing') : t('vocab.playPronunciation')))
const nextButtonLabel = computed(() => (saving.value ? t('common.loading') : t('vocab.markLearnedNext')))
const currentStrokeOrderUrl = computed(() => resolveApiResourceUrl(currentItem.value?.strokeOrderUrl))

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
  strokeDialogVisible.value = false
  resetCardTimer()
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
  strokeDialogVisible.value = false
  resetCardTimer()
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
      reviewedCount,
      itemStatus: 'learned',
      durationSeconds: elapsedSeconds()
    })
    markItemLearned(item)
    currentIndex.value = nextIndex
    flipped.value = false
    showPinyin.value = false
    strokeDialogVisible.value = false
    resetCardTimer()
  } finally {
    saving.value = false
  }
}

function markItemLearned(item: VocabItem) {
  const now = new Date().toISOString()
  item.progressStatus = item.progressStatus === 'mastered' ? 'mastered' : 'learned'
  item.reviewCount = (item.reviewCount || 0) + 1
  item.learnedAt = item.learnedAt || now
  item.lastReviewedAt = now
}

function resetCardTimer() {
  cardStartedAt.value = Date.now()
}

function elapsedSeconds() {
  const seconds = Math.round((Date.now() - cardStartedAt.value) / 1000)
  return Math.min(Math.max(seconds, 1), 24 * 60 * 60)
}

function playPronunciation() {
  const item = currentItem.value
  if (!item) {
    return
  }
  audio.play(resolveApiResourceUrl(item.audioUrl), item.hanzi, 'zh')
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

function openStrokeOrder() {
  if (!currentStrokeOrderUrl.value) {
    return
  }
  strokeImageFailed.value = false
  strokeDialogVisible.value = true
}

function closeStrokeOrder() {
  strokeDialogVisible.value = false
}

function handleStrokeImageError() {
  strokeImageFailed.value = true
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
  await loadListContext()
  await loadCards()
}

async function loadListContext() {
  const [detail, children] = await Promise.all([
    fetchVocabList(vocabListId.value),
    fetchVocabLists({ page: 1, pageSize: 100, parentId: vocabListId.value })
  ])
  currentList.value = detail
  childLists.value = children.records
  if (childLists.value.length === 0) {
    scopeTab.value = 'all'
  }
}

function openLesson(id: number) {
  void openVocabStudy(id)
}

watch(locale, () => {
  setPageTitle('vocab.title')
})

watch(currentStrokeOrderUrl, () => {
  strokeImageFailed.value = false
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
  background: var(--xc-accent);
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
  background: linear-gradient(90deg, var(--xc-yellow) 0%, #8ff0df 100%);
  border-radius: inherit;
  height: 100%;
  transition: width 0.2s ease;
}

.scope-card {
  background: rgba(255, 253, 246, 0.96);
  border: 1px solid var(--xc-border);
  border-radius: 22rpx;
  box-sizing: border-box;
  display: grid;
  gap: 10rpx;
  grid-template-columns: 1fr 1fr;
  margin-bottom: 18rpx;
  padding: 10rpx;
}

.scope-tab {
  background: transparent;
  border: 0;
  border-radius: 18rpx;
  color: var(--xc-muted);
  font-size: 26rpx;
  font-weight: 800;
  line-height: 1.2;
  min-height: 86rpx;
  padding: 0 12rpx;
}

.scope-tab.active {
  background: var(--xc-primary);
  color: #ffffff;
}

.lesson-list {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.lesson-item {
  align-items: center;
  background: var(--xc-surface);
  border: 1px solid var(--xc-border);
  border-radius: 16rpx;
  box-sizing: border-box;
  display: flex;
  gap: 18rpx;
  justify-content: space-between;
  min-height: 154rpx;
  padding: 26rpx 28rpx;
}

.lesson-title {
  color: #102033;
  display: block;
  font-size: 32rpx;
  font-weight: 900;
  line-height: 1.25;
}

.lesson-count {
  background: #eef4ff;
  border-radius: 999rpx;
  color: #1d4ed8;
  flex-shrink: 0;
  font-size: 22rpx;
  font-weight: 800;
  padding: 10rpx 16rpx;
}

.study-card {
  background: var(--xc-surface-raised) !important;
  border: 1px solid var(--xc-border);
  border-radius: 24rpx !important;
  box-sizing: border-box;
  box-shadow: var(--xc-shadow-raised);
  min-height: 590rpx;
  padding: 30rpx 30rpx 36rpx;
  position: relative;
}

.card-meta {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
  justify-content: space-between;
  min-height: 60rpx;
}

.side-label {
  background: var(--xc-primary-soft);
  border-radius: 999rpx;
  color: var(--xc-primary);
  flex-shrink: 0;
  font-size: 24rpx;
  font-weight: 850;
  padding: 12rpx 20rpx;
}

.meta-value {
  color: #94a3b8;
  flex-shrink: 0;
  font-size: 24rpx;
  font-weight: 700;
}

.status-chip {
  background: #f8fafc;
  border: 1px solid #cbd5e1;
  border-radius: 8rpx;
  color: #64748b;
  flex-shrink: 0;
  font-size: 22rpx;
  font-weight: 700;
  line-height: 1;
  padding: 10rpx 14rpx;
}

.status-chip.learned {
  background: #e8f7ef;
  border-color: #9bd7b4;
  color: #166534;
}

.face {
  align-items: center;
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-height: 468rpx;
  text-align: center;
}

.hanzi {
  color: var(--xc-ink);
  font-family: var(--xc-hanzi-font-family);
  font-size: 118rpx;
  font-synthesis: none;
  font-weight: 400;
  line-height: 1.1;
  max-width: 100%;
  -webkit-font-smoothing: antialiased;
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
  background: var(--xc-surface);
  border: 1px solid var(--xc-border);
  border-radius: 22rpx !important;
  box-shadow: var(--xc-shadow-card);
  margin-top: 22rpx;
  padding: 24rpx 24rpx 26rpx;
}

.panel-header {
  align-items: center;
  display: flex;
  gap: 18rpx;
  justify-content: space-between;
  padding-bottom: 18rpx;
  border-bottom: 1px solid rgba(214, 226, 213, 0.74);
}

.panel-title {
  color: var(--xc-ink);
  font-size: 26rpx;
  font-weight: 850;
}

.language-row {
  background: transparent;
  border: 0;
  border-radius: 0;
  display: flex;
  gap: 12rpx;
  padding: 0;
}

.actions {
  display: grid;
  gap: 16rpx;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 22rpx;
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
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.94) 0%, rgba(240, 247, 246, 0.96) 100%) !important;
  border: 1px solid rgba(190, 205, 211, 0.82) !important;
  border-bottom: 6rpx solid #cddde1 !important;
  border-radius: 18rpx !important;
  box-shadow:
    inset 0 2rpx 0 rgba(255, 255, 255, 0.88),
    0 8rpx 16rpx rgba(16, 24, 40, 0.06) !important;
  color: var(--xc-ink) !important;
  font-size: 26rpx;
  font-weight: 900;
  line-height: 1;
  min-height: 72rpx;
  min-width: 150rpx;
  padding: 0;
}

.small-button.active {
  background:
    linear-gradient(180deg, rgba(48, 198, 170, 0.92) 0%, rgba(18, 132, 117, 0.98) 58%, rgba(8, 100, 91, 1) 100%) !important;
  border-color: #0a7468 !important;
  border-bottom-color: #07564f !important;
  box-shadow:
    inset 0 2rpx 0 rgba(255, 255, 255, 0.28),
    0 8rpx 0 rgba(7, 86, 79, 0.1),
    0 12rpx 20rpx rgba(18, 132, 117, 0.18) !important;
  color: #ffffff !important;
}

.tool-button {
  align-items: center;
  background: var(--xc-surface-raised);
  border: 1px solid var(--xc-border);
  color: var(--xc-ink);
  display: flex;
  font-size: 26rpx;
  font-weight: 850;
  justify-content: center;
  line-height: 1.25;
  border-radius: 18rpx !important;
  min-height: 94rpx;
  padding: 0 16rpx;
  white-space: normal;
  word-break: break-word;
}

.tool-button:nth-child(5) {
  grid-column: 1 / -1;
}

.tool-button[disabled] {
  color: #94a3b8;
}

.nav-row {
  background: rgba(255, 254, 248, 0.96);
  border: 1px solid rgba(214, 226, 213, 0.9);
  border-radius: 24rpx;
  box-shadow: var(--xc-shadow-raised);
  display: grid;
  gap: 14rpx;
  grid-template-columns: 0.9fr 1.1fr;
  margin-top: 18rpx;
  padding: 16rpx;
  position: sticky;
  bottom: calc(18rpx + env(safe-area-inset-bottom));
  z-index: 5;
}

.nav-button {
  align-items: center;
  border-radius: 20rpx;
  box-sizing: border-box;
  display: flex;
  font-size: 28rpx;
  font-weight: 850;
  justify-content: center;
  line-height: 1;
  min-height: 90rpx;
  padding: 0 18rpx;
}

.secondary {
  background: var(--xc-surface-raised);
  color: var(--xc-ink);
}

.primary {
  background: var(--xc-primary);
  box-shadow: 0 12rpx 24rpx rgba(25, 122, 104, 0.22);
  color: #ffffff;
}

.nav-button[disabled] {
  box-shadow: none;
  color: #94a3b8;
  opacity: 1;
}

.stroke-overlay {
  align-items: center;
  background: rgba(15, 23, 42, 0.55);
  bottom: 0;
  display: flex;
  justify-content: center;
  left: 0;
  padding: 32rpx 24rpx;
  position: fixed;
  right: 0;
  top: 0;
  z-index: 40;
}

.stroke-panel {
  background: #ffffff;
  border-radius: 24rpx;
  box-sizing: border-box;
  max-height: 82vh;
  overflow: hidden;
  padding: 24rpx;
  width: 100%;
}

.stroke-panel-header {
  align-items: center;
  display: flex;
  gap: 16rpx;
  justify-content: space-between;
  margin-bottom: 18rpx;
}

.stroke-title {
  color: #102033;
  font-size: 30rpx;
  font-weight: 800;
  line-height: 1.35;
}

.stroke-close {
  background: #f1f5f9;
  border-radius: 999rpx;
  color: #475569;
  flex-shrink: 0;
  font-size: 34rpx;
  height: 64rpx;
  line-height: 1;
  padding: 0;
  width: 64rpx;
}

.stroke-image {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 18rpx;
  box-sizing: border-box;
  display: block;
  height: 58vh;
  width: 100%;
}

.stroke-image-native {
  object-fit: contain;
}

.stroke-error {
  align-items: center;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 18rpx;
  box-sizing: border-box;
  color: #64748b;
  display: flex;
  font-size: 26rpx;
  height: 58vh;
  justify-content: center;
  line-height: 1.5;
  padding: 32rpx;
  text-align: center;
  width: 100%;
}
</style>
