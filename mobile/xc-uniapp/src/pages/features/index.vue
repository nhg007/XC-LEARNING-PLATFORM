<template>
  <view class="page">
    <view class="hero">
      <view class="hero-top">
        <view class="hero-copy">
          <text class="eyebrow">{{ t('app.title') }}</text>
          <text class="title">{{ t('features.title') }}</text>
          <text class="subtitle">{{ t('features.subtitle') }}</text>
        </view>
        <LanguageSwitch variant="hero" />
      </view>
    </view>

    <view class="access-card">
      <view>
        <text class="access-label">{{ t('membership.currentAccess') }}</text>
        <text class="access-title">{{ accessLabel }}</text>
        <text class="access-desc">{{ accessHint }}</text>
      </view>
      <button v-if="!membership.fullAccess" class="access-button" size="mini" @click="openPage(routes.membership)">
        {{ t('features.upgrade') }}
      </button>
    </view>

    <view class="spotlight-card" @click="openRecommended">
      <view class="spotlight-copy">
        <text class="spotlight-label">{{ t('features.recommended') }}</text>
        <text class="spotlight-title">{{ recommendedTitle }}</text>
        <text class="spotlight-desc">{{ recommendedDesc }}</text>
        <view v-if="recommendedProgressPercent > 0" class="progress-track">
          <view class="progress-fill" :style="{ width: `${recommendedProgressPercent}%` }" />
        </view>
      </view>
      <text class="spotlight-action">{{ t('features.startNow') }}</text>
    </view>

    <view class="group">
      <text class="group-title">{{ t('features.learning') }}</text>
      <view class="feature-grid">
        <view v-for="item in learningFeatures" :key="item.key" class="feature-card" @click="openFeature(item)">
          <view :class="['mark', item.key]">{{ t(item.mark) }}</view>
          <view class="feature-copy">
            <text class="feature-title">{{ t(item.titleKey) }}</text>
            <text class="feature-desc">{{ t(item.descKey) }}</text>
          </view>
          <text :class="['status-pill', statusTone(item)]">{{ featureStatusLabel(item) }}</text>
        </view>
      </view>
    </view>

    <view class="group">
      <text class="group-title">{{ t('features.review') }}</text>
      <view class="feature-grid">
        <view v-for="item in reviewFeatures" :key="item.key" class="feature-card" @click="openFeature(item)">
          <view class="mark review">{{ t(item.mark) }}</view>
          <view class="feature-copy">
            <text class="feature-title">{{ t(item.titleKey) }}</text>
            <text class="feature-desc">{{ t(item.descKey) }}</text>
          </view>
          <text class="arrow">›</text>
        </view>
      </view>
    </view>

    <view class="group">
      <text class="group-title">{{ t('features.classroom') }}</text>
      <view class="feature-card wide" @click="openPage(routes.classroom)">
        <view class="mark class-mark">{{ t('features.classroomMark') }}</view>
        <view class="feature-copy">
          <text class="feature-title">{{ t('feature.classroom') }}</text>
          <text class="feature-desc">{{ t('features.classroomDesc') }}</text>
        </view>
        <text class="arrow">›</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import LanguageSwitch from '../../components/LanguageSwitch.vue'
import { fetchMembershipStatus } from '../../api/membership'
import { fetchVocabLists, fetchVocabProgress } from '../../api/vocab'
import { applyTabBarLocale, setPageTitle, useI18n } from '../../i18n'
import type { MembershipStatus, VocabList, VocabProgress } from '../../types/api'
import { openPage, requireLogin, routes } from '../../utils/navigation'

interface FeatureItem {
  key: string
  titleKey: string
  descKey: string
  mark: string
  route: string
  locked?: boolean
}

const { locale, t } = useI18n()
const membership = reactive<MembershipStatus>({
  accessLevel: 'free',
  fullAccess: false,
  trialEndsAt: null,
  membershipEndsAt: null,
  remainingSeconds: 0
})
const recommendedList = ref<VocabList | null>(null)
const recommendedProgress = ref<VocabProgress | null>(null)
const recommendationLoading = ref(false)
const recommendationError = ref('')

const learningFeatures: FeatureItem[] = [
  { key: 'vocab', titleKey: 'feature.vocab', descKey: 'features.vocabDesc', mark: 'features.vocabMark', route: routes.vocab },
  { key: 'favorites', titleKey: 'vocab.favorites', descKey: 'features.favoritesDesc', mark: 'features.favoritesMark', route: routes.vocabFavorites },
  { key: 'practice', titleKey: 'feature.practice', descKey: 'features.practiceDesc', mark: 'features.practiceMark', route: routes.practice, locked: true },
  { key: 'matching', titleKey: 'feature.matching', descKey: 'features.matchingDesc', mark: 'features.matchingMark', route: routes.matching, locked: true },
  { key: 'elimination', titleKey: 'feature.elimination', descKey: 'features.eliminationDesc', mark: 'features.eliminationMark', route: routes.elimination, locked: true }
]

const reviewFeatures: FeatureItem[] = [
  { key: 'membership', titleKey: 'membership.title', descKey: 'features.membershipDesc', mark: 'features.membershipMark', route: routes.membership },
  { key: 'records', titleKey: 'feature.records', descKey: 'features.recordsDesc', mark: 'features.recordsMark', route: routes.records }
]

const accessLabel = computed(() => {
  if (membership.accessLevel === 'member') {
    return t('home.member')
  }
  if (membership.accessLevel === 'trial') {
    return t('home.trial')
  }
  return t('home.free')
})

const accessHint = computed(() => {
  if (membership.fullAccess) {
    const days = Math.ceil(membership.remainingSeconds / 86400)
    return t('features.fullHint', { days: Math.max(0, days) })
  }
  return t('features.freeHint')
})

const recommendedTitle = computed(() => {
  if (recommendationLoading.value) {
    return t('features.recommendationLoading')
  }
  return recommendedList.value?.name || t('feature.vocab')
})

const recommendedDesc = computed(() => {
  if (recommendationError.value) {
    return recommendationError.value
  }
  if (!recommendedList.value) {
    return t('features.recommendationEmpty')
  }
  if (recommendedProgress.value && recommendedProgress.value.totalCount > 0) {
    return t('features.recommendationProgress', {
      reviewed: recommendedProgress.value.reviewedCount,
      total: recommendedProgress.value.totalCount
    })
  }
  return t('features.recommendedDesc')
})

const recommendedProgressPercent = computed(() => {
  const progress = recommendedProgress.value
  if (!progress || progress.totalCount <= 0) {
    return 0
  }
  return Math.min(100, Math.round((progress.reviewedCount / progress.totalCount) * 100))
})

onShow(() => {
  applyTabBarLocale()
  setPageTitle('features.title')
  if (!requireLogin()) {
    return
  }
  void loadPageData()
})

onPullDownRefresh(async () => {
  try {
    if (requireLogin()) {
      await loadPageData()
    }
  } finally {
    uni.stopPullDownRefresh()
  }
})

watch(locale, () => {
  applyTabBarLocale()
  setPageTitle('features.title')
})

async function loadPageData() {
  await Promise.all([loadMembership(), loadRecommendation()])
}

async function loadMembership() {
  try {
    const status = await fetchMembershipStatus()
    membership.accessLevel = status.accessLevel
    membership.fullAccess = status.fullAccess
    membership.trialEndsAt = status.trialEndsAt
    membership.membershipEndsAt = status.membershipEndsAt
    membership.remainingSeconds = status.remainingSeconds
  } catch {
    void uni.showToast({ icon: 'none', title: t('common.requestFailed') })
  }
}

async function loadRecommendation() {
  recommendationLoading.value = true
  recommendationError.value = ''
  try {
    const page = await fetchVocabLists(1, 1)
    const first = page.records[0] || null
    recommendedList.value = first
    recommendedProgress.value = first ? await fetchVocabProgress(first.id) : null
  } catch {
    recommendedList.value = null
    recommendedProgress.value = null
    recommendationError.value = t('features.recommendationLoadFailed')
  } finally {
    recommendationLoading.value = false
  }
}

function openRecommended() {
  void openPage(routes.vocab)
}

function openFeature(item: FeatureItem) {
  if (item.locked && !membership.fullAccess) {
    void uni.showToast({ icon: 'none', title: t('home.locked') })
    void openPage(routes.membership)
    return
  }
  void openPage(item.route)
}

function featureStatusLabel(item: FeatureItem) {
  if (!item.locked) {
    return t('features.freeAccess')
  }
  return membership.fullAccess ? t('features.unlocked') : t('features.memberOnly')
}

function statusTone(item: FeatureItem) {
  if (!item.locked) {
    return 'free'
  }
  return membership.fullAccess ? 'unlocked' : 'locked'
}
</script>

<style scoped>
.page {
  background: #eef5f7;
  min-height: 100vh;
  padding: 0 24rpx 36rpx;
}

.hero {
  background: #102033;
  border-bottom-left-radius: 32rpx;
  border-bottom-right-radius: 32rpx;
  box-sizing: border-box;
  color: #ffffff;
  margin: 0 -24rpx 22rpx;
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
  line-height: 1.5;
  margin-top: 14rpx;
}

.group {
  margin-top: 26rpx;
}

.access-card,
.spotlight-card {
  background: var(--xc-surface);
  border: 1px solid var(--xc-border);
  border-radius: 16rpx;
  box-shadow: var(--xc-shadow-card);
  box-sizing: border-box;
}

.access-card {
  align-items: center;
  display: flex;
  gap: 18rpx;
  justify-content: space-between;
  margin-top: 18rpx;
  padding: 24rpx;
  position: relative;
}

.access-label,
.spotlight-label {
  color: var(--xc-muted);
  display: block;
  font-size: 22rpx;
  font-weight: 850;
}

.access-title {
  color: var(--xc-ink);
  display: block;
  font-size: 36rpx;
  font-weight: 850;
  line-height: 1.25;
  margin-top: 6rpx;
}

.access-desc {
  color: var(--xc-muted);
  display: block;
  font-size: 23rpx;
  line-height: 1.58;
  margin-top: 8rpx;
}

.access-button {
  background: var(--xc-primary);
  border: 1px solid var(--xc-primary);
  border-radius: 14rpx;
  box-shadow: 0 12rpx 24rpx rgba(25, 122, 104, 0.18);
  color: #ffffff;
  flex: 0 0 auto;
  font-size: 24rpx;
  font-weight: 850;
  margin: 0;
  min-height: 64rpx;
  padding: 0 20rpx;
}

.spotlight-card {
  align-items: center;
  background: linear-gradient(135deg, #ffffff 0%, #effaf7 100%) !important;
  display: flex;
  gap: 20rpx;
  justify-content: space-between;
  margin-top: 18rpx;
  min-height: 168rpx;
  padding: 28rpx;
  position: relative;
}

.spotlight-copy {
  flex: 1;
  min-width: 0;
}

.spotlight-label {
  color: var(--xc-primary) !important;
}

.spotlight-title {
  color: var(--xc-ink) !important;
  display: block;
  font-size: 36rpx;
  font-weight: 850;
  line-height: 1.25;
  margin-top: 8rpx;
}

.spotlight-desc {
  color: var(--xc-muted) !important;
  display: block;
  font-size: 24rpx;
  line-height: 1.58;
  margin-top: 8rpx;
}

.progress-track {
  background: var(--xc-surface-soft);
  border-radius: 999rpx;
  height: 10rpx;
  margin-top: 18rpx;
  overflow: hidden;
}

.progress-fill {
  background: var(--xc-primary);
  border-radius: inherit;
  height: 100%;
  min-width: 10rpx;
}

.spotlight-action {
  background: var(--xc-primary) !important;
  border-radius: 18rpx;
  box-shadow: 0 10rpx 22rpx rgba(18, 132, 117, 0.18);
  color: #ffffff !important;
  flex: 0 0 auto;
  font-size: 26rpx;
  font-weight: 900;
  min-height: 104rpx;
  min-width: 138rpx;
  padding: 0 24rpx;
}

.group-title {
  color: var(--xc-ink);
  display: block;
  font-size: 34rpx;
  font-weight: 850;
  margin-bottom: 14rpx;
}

.feature-grid {
  display: grid;
  gap: 14rpx;
}

.feature-card {
  align-items: center;
  background: var(--xc-surface);
  border: 1px solid var(--xc-border);
  border-radius: 16rpx;
  box-shadow: var(--xc-shadow-card);
  box-sizing: border-box;
  display: flex;
  gap: 18rpx;
  min-height: 136rpx;
  padding: 22rpx;
}

.feature-card.wide {
  min-height: 148rpx;
}

.mark {
  align-items: center;
  background: var(--xc-primary-soft);
  border: 1px solid var(--xc-border-strong);
  border-radius: 16rpx;
  color: var(--xc-primary);
  display: flex;
  flex: 0 0 68rpx;
  font-size: 28rpx;
  font-weight: 900;
  height: 68rpx;
  justify-content: center;
}

.mark.practice {
  background: #dbeafe;
  color: #1d4ed8;
}

.mark.favorites {
  background: #fef9c3;
  color: #a16207;
}

.mark.matching {
  background: #ede9fe;
  color: #6d28d9;
}

.mark.elimination {
  background: #ffedd5;
  color: #c2410c;
}

.mark.review {
  background: #e0f2fe;
  color: #0369a1;
}

.class-mark {
  background: #fef3c7;
  color: #92400e;
}

.feature-copy {
  flex: 1;
  min-width: 0;
}

.feature-title {
  color: var(--xc-ink);
  display: block;
  font-size: 32rpx;
  font-weight: 900;
  line-height: 1.35;
}

.feature-desc {
  color: var(--xc-muted);
  display: block;
  font-size: 23rpx;
  line-height: 1.58;
  margin-top: 8rpx;
}

.status-pill {
  background: var(--xc-surface-soft);
  border: 1px solid var(--xc-border);
  border-radius: 999rpx;
  color: var(--xc-muted);
  flex: 0 0 auto;
  font-size: 22rpx;
  font-weight: 800;
  padding: 10rpx 18rpx;
}

.status-pill.free {
  background: var(--xc-primary-soft);
  border-color: var(--xc-border-strong);
  color: var(--xc-primary);
}

.status-pill.unlocked {
  background: var(--xc-primary-soft);
  border-color: var(--xc-border-strong);
  color: var(--xc-primary);
}

.status-pill.locked {
  background: var(--xc-surface-soft);
  color: var(--xc-muted);
}

.arrow {
  color: var(--xc-muted-soft);
  flex: 0 0 auto;
  font-size: 28rpx;
}
</style>
