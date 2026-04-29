<template>
  <view class="page">
    <view class="hero">
      <view class="hero-copy">
        <text class="eyebrow">{{ t('app.title') }}</text>
        <text class="title">{{ t('vocab.title') }}</text>
        <text class="subtitle">{{ t('vocab.subtitle') }}</text>
      </view>
      <LanguageSwitch variant="hero" />
    </view>

    <view v-if="loading && lists.length === 0" class="state">{{ t('common.loading') }}</view>
    <view v-else-if="error" class="state">
      <text>{{ error }}</text>
      <button class="plain-btn" size="mini" @click="loadLists">{{ t('common.retry') }}</button>
    </view>
    <view v-else-if="lists.length === 0" class="state">{{ t('vocab.emptyList') }}</view>
    <template v-else>
      <view class="overview-card">
        <view>
          <text class="overview-label">{{ t('vocab.overview') }}</text>
          <text class="overview-title">{{ t('vocab.listCount', { count: total }) }}</text>
          <text class="muted">{{ t('vocab.progressSummary', { reviewed: reviewedWords, total: totalWords }) }}</text>
        </view>
        <text class="overview-rate">{{ completionRate }}</text>
      </view>

      <view v-if="recommendedList" class="continue-card" @click="openList(recommendedList.id)">
        <view class="continue-copy">
          <text class="continue-label">{{ t('vocab.continue') }}</text>
          <text class="continue-title">{{ recommendedList.name }}</text>
          <text class="continue-desc">{{ progressText(recommendedList.id) }}</text>
        </view>
        <text class="continue-action">{{ t('vocab.startStudy') }}</text>
      </view>

      <view class="section-head">
        <text class="section-title">{{ t('vocab.allLists') }}</text>
        <text class="muted">{{ lists.length }} / {{ total }}</text>
      </view>

      <view class="list">
        <view v-for="item in lists" :key="item.id" class="list-item" @click="openList(item.id)">
          <view class="main">
            <view class="list-top">
              <text class="list-title">{{ item.name }}</text>
              <text class="tag">{{ item.level || item.listType }}</text>
            </view>
            <text class="muted">{{ item.description || item.level || item.listType }}</text>
            <view class="progress-bar">
              <view class="progress-fill" :style="{ width: progressWidth(item.id) }" />
            </view>
            <view class="list-meta">
              <text>{{ progressText(item.id) }}</text>
              <text>{{ t('vocab.currentIndex', { index: currentIndexLabel(item.id) }) }}</text>
            </view>
          </view>
          <text class="arrow">></text>
        </view>
      </view>

      <button v-if="canLoadMore" class="plain-btn load-more" :loading="loadingMore" @click="loadMore">{{ t('vocab.loadMore') }}</button>
      <text v-else class="end-text">{{ t('vocab.noMoreLists') }}</text>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import LanguageSwitch from '../../components/LanguageSwitch.vue'
import { fetchVocabLists, fetchVocabProgress } from '../../api/vocab'
import { applyTabBarLocale, setPageTitle, useI18n } from '../../i18n'
import type { VocabList, VocabProgress } from '../../types/api'
import { openVocabStudy, requireLogin } from '../../utils/navigation'

const { locale, t } = useI18n()
const page = ref(1)
const pageSize = 20
const total = ref(0)
const loading = ref(false)
const loadingMore = ref(false)
const error = ref('')
const lists = ref<VocabList[]>([])
const progressByList = ref<Record<number, VocabProgress>>({})

const canLoadMore = computed(() => lists.value.length < total.value)
const recommendedList = computed(() => lists.value.find(item => progressByList.value[item.id]?.reviewedCount > 0) || lists.value[0] || null)
const totalWords = computed(() => Object.values(progressByList.value).reduce((sum, item) => sum + item.totalCount, 0))
const reviewedWords = computed(() => Object.values(progressByList.value).reduce((sum, item) => sum + item.reviewedCount, 0))
const completionRate = computed(() => {
  if (totalWords.value <= 0) {
    return '0%'
  }
  return `${Math.round((reviewedWords.value / totalWords.value) * 100)}%`
})

onShow(() => {
  applyTabBarLocale()
  setPageTitle('vocab.title')
  if (!requireLogin()) {
    return
  }
  void loadLists()
})

watch(locale, () => {
  applyTabBarLocale()
  setPageTitle('vocab.title')
})

onPullDownRefresh(() => {
  void loadLists().finally(() => uni.stopPullDownRefresh())
})

onReachBottom(() => {
  if (canLoadMore.value && !loadingMore.value) {
    void loadMore()
  }
})

async function loadLists() {
  loading.value = true
  error.value = ''
  page.value = 1
  try {
    const listPage = await fetchVocabLists(1, pageSize)
    lists.value = listPage.records
    total.value = listPage.total
    progressByList.value = {}
    await loadProgress(listPage.records)
  } catch {
    error.value = t('vocab.loadingFailed')
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  if (loadingMore.value || !canLoadMore.value) {
    return
  }
  loadingMore.value = true
  const nextPage = page.value + 1
  try {
    const listPage = await fetchVocabLists(nextPage, pageSize)
    lists.value = lists.value.concat(listPage.records)
    total.value = listPage.total
    page.value = nextPage
    await loadProgress(listPage.records)
  } catch {
    void uni.showToast({ icon: 'none', title: t('vocab.loadingFailed') })
  } finally {
    loadingMore.value = false
  }
}

async function loadProgress(records: VocabList[]) {
  const results = await Promise.all(records.map(async item => {
    try {
      return [item.id, await fetchVocabProgress(item.id)] as const
    } catch {
      return null
    }
  }))
  const nextProgress = { ...progressByList.value }
  results.forEach(result => {
    if (result) {
      nextProgress[result[0]] = result[1]
    }
  })
  progressByList.value = nextProgress
}

function openList(id: number) {
  void openVocabStudy(id)
}

function progressText(id: number) {
  const progress = progressByList.value[id]
  if (!progress) {
    return t('vocab.progressSummary', { reviewed: 0, total: 0 })
  }
  return t('vocab.progressSummary', { reviewed: progress.reviewedCount, total: progress.totalCount })
}

function progressWidth(id: number) {
  const progress = progressByList.value[id]
  if (!progress || progress.totalCount <= 0) {
    return '6%'
  }
  return `${Math.max(6, Math.min(100, Math.round((progress.reviewedCount / progress.totalCount) * 100)))}%`
}

function currentIndexLabel(id: number) {
  const progress = progressByList.value[id]
  if (!progress || progress.totalCount <= 0) {
    return 0
  }
  return Math.min(progress.currentIndex + 1, progress.totalCount)
}
</script>

<style scoped>
.page {
  background: #eef5f7;
  min-height: 100vh;
  padding: 0 24rpx 32rpx;
}

.hero {
  align-items: flex-start;
  background: #102033;
  border-bottom-left-radius: 32rpx;
  border-bottom-right-radius: 32rpx;
  box-sizing: border-box;
  color: #ffffff;
  display: flex;
  justify-content: space-between;
  margin: 0 -24rpx 22rpx;
  padding: 30rpx 24rpx 28rpx;
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

.title {
  display: block;
  font-size: 46rpx;
  font-weight: 700;
  line-height: 1.15;
  margin-top: 10rpx;
}

.subtitle {
  color: #cbd5e1;
  display: block;
  font-size: 26rpx;
  line-height: 1.55;
  margin-top: 14rpx;
}

.overview-card,
.continue-card,
.list-item {
  background: #ffffff;
  border: 1px solid #d7e2ea;
  border-radius: 18rpx;
  box-shadow: 0 10rpx 30rpx rgba(15, 23, 42, 0.05);
  box-sizing: border-box;
}

.overview-card {
  align-items: center;
  display: flex;
  gap: 18rpx;
  justify-content: space-between;
  padding: 24rpx;
}

.overview-label,
.continue-label {
  color: #64748b;
  display: block;
  font-size: 22rpx;
  font-weight: 800;
}

.overview-title {
  color: #102033;
  display: block;
  font-size: 34rpx;
  font-weight: 900;
  line-height: 1.25;
  margin-top: 8rpx;
}

.overview-rate {
  align-items: center;
  background: #ccfbf1;
  border-radius: 999rpx;
  color: #14796f;
  display: flex;
  flex: 0 0 auto;
  font-size: 30rpx;
  font-weight: 900;
  justify-content: center;
  min-height: 68rpx;
  min-width: 112rpx;
  padding: 0 18rpx;
}

.continue-card {
  align-items: center;
  display: flex;
  gap: 20rpx;
  justify-content: space-between;
  margin-top: 18rpx;
  padding: 24rpx;
}

.continue-copy {
  flex: 1;
  min-width: 0;
}

.continue-label {
  color: #0f766e;
}

.continue-title {
  color: #0f766e;
  display: block;
  font-size: 36rpx;
  font-weight: 900;
  line-height: 1.25;
  margin-top: 8rpx;
}

.continue-desc {
  color: #64748b;
  display: block;
  font-size: 24rpx;
  line-height: 1.45;
  margin-top: 8rpx;
}

.continue-action {
  background: #14796f;
  border-radius: 999rpx;
  color: #ffffff;
  flex: 0 0 auto;
  font-size: 24rpx;
  font-weight: 900;
  padding: 12rpx 18rpx;
}

.section-head {
  align-items: center;
  display: flex;
  justify-content: space-between;
  margin: 30rpx 0 14rpx;
}

.section-title {
  color: #102033;
  display: block;
  font-size: 34rpx;
  font-weight: 800;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.list-item {
  align-items: center;
  display: flex;
  gap: 18rpx;
  justify-content: space-between;
  padding: 24rpx;
}

.main {
  flex: 1;
  min-width: 0;
}

.list-top {
  align-items: flex-start;
  display: flex;
  gap: 14rpx;
  justify-content: space-between;
}

.list-title {
  color: #102033;
  display: block;
  flex: 1;
  font-size: 32rpx;
  font-weight: 800;
  line-height: 1.3;
  min-width: 0;
  word-break: break-word;
}

.tag {
  background: #ecfdf5;
  border-radius: 999rpx;
  color: #047857;
  flex-shrink: 0;
  font-size: 24rpx;
  padding: 8rpx 16rpx;
}

.progress-bar {
  background: #e2e8f0;
  border-radius: 999rpx;
  height: 12rpx;
  margin-top: 18rpx;
  overflow: hidden;
}

.progress-fill {
  background: #14796f;
  height: 100%;
}

.list-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
  margin-top: 14rpx;
}

.list-meta text {
  background: #f1f5f9;
  border-radius: 999rpx;
  color: #475569;
  font-size: 22rpx;
  padding: 6rpx 12rpx;
}

.state {
  align-items: center;
  background: #ffffff;
  border: 1px solid #d7e2ea;
  border-radius: 8rpx;
  color: #64748b;
  display: flex;
  flex-direction: column;
  gap: 18rpx;
  padding: 54rpx 24rpx;
}

.plain-btn {
  background: #ffffff;
  border: 1px solid #cbd5e1;
  border-radius: 8rpx;
  margin: 0;
}

.load-more {
  margin-top: 18rpx;
  width: 100%;
}

.end-text {
  color: #94a3b8;
  display: block;
  font-size: 24rpx;
  margin-top: 20rpx;
  text-align: center;
}

.arrow {
  color: #94a3b8;
  flex: 0 0 auto;
}

.muted {
  color: #64748b;
  display: block;
  font-size: 24rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  word-break: break-word;
}
</style>
