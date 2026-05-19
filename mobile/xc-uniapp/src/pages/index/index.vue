<template>
  <view class="page">
    <view class="hero">
      <view class="hero-top">
        <view class="hero-copy">
          <text class="eyebrow">{{ t('app.title') }}</text>
          <text class="title">{{ t('tab.home') }}</text>
          <text class="subtitle">{{ t('home.subtitle', { access: accessLabel, days: summary.currentStreakDays }) }}</text>
        </view>
        <LanguageSwitch variant="hero" />
      </view>
    </view>

    <view v-if="errorMessage" class="state-card error-card">
      <text>{{ errorMessage }}</text>
      <button class="retry-button" size="mini" @click="loadHome">{{ t('common.retry') }}</button>
    </view>

    <template v-else>
      <view class="today-card">
        <view class="section-head">
          <view>
            <text class="section-title">{{ t('home.todayPlan') }}</text>
            <text class="muted">{{ lastStudyLabel }}</text>
          </view>
          <text class="access-pill">{{ accessLabel }}</text>
        </view>
        <view class="metric-grid">
          <view class="metric strong">
            <text class="label">{{ t('home.remaining') }}</text>
            <text class="value">{{ remainingLabel }}</text>
          </view>
          <view class="metric">
            <text class="label">{{ t('home.accuracy') }}</text>
            <text class="value">{{ formatPercent(summary.overallAccuracyRate) }}</text>
          </view>
          <view class="metric">
            <text class="label">{{ t('home.streak') }}</text>
            <text class="value">{{ t('common.days', { days: summary.currentStreakDays }) }}</text>
          </view>
          <view class="metric">
            <text class="label">{{ t('home.wordsReviewed') }}</text>
            <text class="value">{{ summary.totalVocabReviewCount }}</text>
          </view>
        </view>
      </view>

      <view class="start-card" @click="openRecommended">
        <view class="start-copy">
          <text class="start-label">{{ t('home.recommended') }}</text>
          <text class="start-title">{{ recommendedTitle }}</text>
          <text class="start-desc">{{ recommendedDesc }}</text>
        </view>
        <text class="start-action">{{ t('home.start') }}</text>
      </view>

      <view class="quick-grid">
        <view v-for="item in actions" :key="item.key" class="quick-card" @click="openFeature(item.key)">
          <text :class="['card-mark', item.tone]">{{ t(item.mark) }}</text>
          <view class="quick-copy">
            <text class="card-title">{{ t(item.titleKey) }}</text>
            <text class="card-desc">{{ t(item.descKey) }}</text>
          </view>
        </view>
      </view>

      <view class="section">
        <view class="section-head">
          <text class="section-title">{{ t('home.vocabLists') }}</text>
          <text class="link-text" @click="openPage(routes.vocab)">{{ t('home.viewAll') }}</text>
        </view>
        <view v-if="loading" class="state-card">{{ t('common.loading') }}</view>
        <view v-for="item in vocabLists" :key="item.id" class="list-item" @click="openVocab(item.id)">
          <view class="list-copy">
            <text class="list-title">{{ item.name }}</text>
            <text class="muted">{{ item.level || item.listType }}</text>
          </view>
          <text class="arrow">›</text>
        </view>
        <text v-if="!loading && vocabLists.length === 0" class="muted empty">{{ t('home.emptyVocab') }}</text>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import LanguageSwitch from '../../components/LanguageSwitch.vue'
import { fetchLearningSummary, fetchMembershipStatus } from '../../api/home'
import { fetchVocabLists } from '../../api/vocab'
import { applyTabBarLocale, setPageTitle, useI18n } from '../../i18n'
import type { LearningSummary, MembershipStatus, VocabList } from '../../types/api'
import { openPage, openVocabStudy, requireLogin, routes } from '../../utils/navigation'

type FeatureKey = 'vocab' | 'favorites' | 'features' | 'membership' | 'records'

const { locale, t } = useI18n()
const actions: Array<{ key: FeatureKey; titleKey: string; descKey: string; mark: string; tone: string }> = [
  { key: 'vocab', titleKey: 'feature.vocab', descKey: 'home.vocabActionDesc', mark: 'features.vocabMark', tone: 'vocab' },
  { key: 'favorites', titleKey: 'vocab.favorites', descKey: 'features.favoritesDesc', mark: 'features.favoritesMark', tone: 'favorites' },
  { key: 'features', titleKey: 'tab.features', descKey: 'home.learningActionDesc', mark: 'features.learningMark', tone: 'learn' },
  { key: 'records', titleKey: 'feature.records', descKey: 'home.recordsActionDesc', mark: 'features.recordsMark', tone: 'records' },
  { key: 'membership', titleKey: 'home.member', descKey: 'home.memberActionDesc', mark: 'features.membershipMark', tone: 'member' }
]
const membership = ref<MembershipStatus>({
  accessLevel: 'free',
  fullAccess: false,
  trialEndsAt: null,
  membershipEndsAt: null,
  remainingSeconds: 0
})
const summary = ref<LearningSummary>({
  totalStudySeconds: 0,
  totalExerciseCount: 0,
  totalCorrectCount: 0,
  totalVocabReviewCount: 0,
  currentStreakDays: 0,
  longestStreakDays: 0,
  overallAccuracyRate: 0,
  lastStudyDate: null
})
const vocabLists = ref<VocabList[]>([])
const loading = ref(false)
const errorMessage = ref('')

const accessLabel = computed(() => {
  if (membership.value.accessLevel === 'member') {
    return t('home.member')
  }
  if (membership.value.accessLevel === 'trial') {
    return t('home.trial')
  }
  return t('home.free')
})
const remainingLabel = computed(() => {
  const days = Math.ceil(membership.value.remainingSeconds / 86400)
  return t('common.days', { days: Math.max(0, days) })
})
const lastStudyLabel = computed(() => {
  if (!summary.value.lastStudyDate) {
    return t('home.noStudyYet')
  }
  return t('home.lastStudy', { date: formatDate(summary.value.lastStudyDate) })
})
const recommendedTitle = computed(() => t('feature.vocab'))
const recommendedDesc = computed(() => t('home.startVocabDesc'))

async function loadHome() {
  applyTabBarLocale()
  setPageTitle('app.title')
  if (!requireLogin()) {
    return
  }
  loading.value = true
  errorMessage.value = ''
  try {
    const [status, learning, vocab] = await Promise.all([
      fetchMembershipStatus(),
      fetchLearningSummary(),
      fetchVocabLists(1, 4)
    ])
    membership.value = status
    summary.value = learning
    vocabLists.value = vocab.records
  } catch {
    errorMessage.value = t('home.loadFailed')
  } finally {
    loading.value = false
  }
}

onShow(loadHome)

watch(locale, () => {
  applyTabBarLocale()
  setPageTitle('app.title')
})

function openVocab(id: number) {
  void openVocabStudy(id)
}

function openRecommended() {
  void openPage(routes.vocab)
}

function openFeature(key: FeatureKey) {
  if (key === 'vocab') {
    void openPage(routes.vocab)
    return
  }
  if (key === 'favorites') {
    void openPage(routes.vocabFavorites)
    return
  }
  if (key === 'features') {
    void openPage(routes.features)
    return
  }
  if (key === 'membership') {
    void openPage(routes.membership)
    return
  }
  if (key === 'records') {
    void openPage(routes.records)
    return
  }
  void uni.showToast({ icon: 'none', title: t('home.inProgress') })
}

function formatPercent(value: number | null | undefined) {
  return `${Number(value || 0).toFixed(0)}%`
}

function formatDate(value: string) {
  const [, month = '0', day = '0'] = value.split('-')
  return `${Number(month)}/${Number(day)}`
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
  margin: 0 -24rpx 18rpx;
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
  line-height: 1.55;
  margin-top: 18rpx;
}

.state-card,
.today-card,
.start-card,
.quick-card,
.list-item {
  background: var(--xc-surface);
  border: 1px solid var(--xc-border);
  border-radius: 16rpx;
  box-shadow: var(--xc-shadow-card);
  box-sizing: border-box;
}

.state-card {
  color: #64748b;
  font-size: 26rpx;
  margin-bottom: 18rpx;
  padding: 26rpx;
}

.error-card {
  background: #fff7ed;
  border-color: #fed7aa;
  color: #9a3412;
}

.retry-button {
  background: #ffffff;
  border: 1px solid #fed7aa;
  border-radius: 12rpx;
  color: #9a3412;
  font-size: 24rpx;
  font-weight: 800;
  margin: 18rpx 0 0;
  min-height: 64rpx;
  width: 180rpx;
}

.today-card {
  padding: 26rpx;
}

.section-head {
  align-items: center;
  display: flex;
  gap: 18rpx;
  justify-content: space-between;
}

.section-title {
  color: var(--xc-ink);
  display: block;
  font-size: 34rpx;
  font-weight: 850;
}

.access-pill {
  background: var(--xc-primary-soft);
  border: 1px solid var(--xc-border-strong);
  border-radius: 999rpx;
  color: var(--xc-primary);
  flex: 0 0 auto;
  font-size: 22rpx;
  font-weight: 800;
  padding: 10rpx 16rpx;
}

.metric-grid {
  display: grid;
  gap: 14rpx;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 22rpx;
}

.metric {
  background: var(--xc-surface-raised);
  border: 1px solid var(--xc-border);
  border-radius: 18rpx;
  box-sizing: border-box;
  min-height: 124rpx;
  padding: 18rpx;
}

.metric.strong {
  background: var(--xc-primary-soft);
  border-color: var(--xc-border-strong);
}

.metric.strong .label {
  color: #0f766e;
}

.metric.strong .value {
  color: #14796f;
}

.label {
  color: #64748b;
  display: block;
  font-size: 24rpx;
}

.value {
  display: block;
  font-size: 36rpx;
  font-weight: 800;
  margin-top: 8rpx;
}

.start-card {
  align-items: center;
  background: linear-gradient(135deg, #ffffff 0%, #effaf7 100%) !important;
  display: flex;
  gap: 20rpx;
  justify-content: space-between;
  margin-top: 20rpx;
  min-height: 168rpx;
  padding: 28rpx;
  position: relative;
}

.start-copy {
  flex: 1;
  min-width: 0;
}

.start-label {
  color: var(--xc-primary);
  display: block;
  font-size: 22rpx;
  font-weight: 800;
}

.start-title {
  color: var(--xc-ink) !important;
  display: block;
  font-size: 36rpx;
  font-weight: 900;
  line-height: 1.25;
  margin-top: 8rpx;
}

.start-desc {
  color: var(--xc-muted) !important;
  display: block;
  font-size: 24rpx;
  line-height: 1.45;
  margin-top: 8rpx;
}

.start-action {
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

.quick-grid {
  display: grid;
  gap: 14rpx;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 18rpx;
}

.quick-card {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  position: relative;
  min-height: 198rpx;
  padding: 22rpx;
}

.card-mark {
  align-items: center;
  background: #e0f2fe;
  border-radius: 14rpx;
  color: #0369a1;
  display: flex;
  font-size: 24rpx;
  font-weight: 800;
  height: 54rpx;
  justify-content: center;
  width: 54rpx;
}

.card-mark.vocab {
  background: #ccfbf1;
  color: #14796f;
}

.card-mark.learn {
  background: #dbeafe;
  color: #1d4ed8;
}

.card-mark.favorites {
  background: #fef9c3;
  color: #a16207;
}

.card-mark.records {
  background: #e0f2fe;
  color: #0369a1;
}

.card-mark.member {
  background: #fef3c7;
  color: #92400e;
}

.quick-copy {
  margin-top: 16rpx;
}

.card-title {
  color: var(--xc-ink);
  display: block;
  font-size: 32rpx;
  font-weight: 900;
  margin-top: 18rpx;
}

.card-desc {
  color: #64748b;
  display: block;
  font-size: 22rpx;
  line-height: 1.58;
  margin-top: 8rpx;
}

.section {
  margin-top: 30rpx;
}

.link-text {
  color: var(--xc-primary);
  font-size: 24rpx;
  font-weight: 800;
}

.list-item {
  align-items: center;
  display: flex;
  gap: 18rpx;
  justify-content: space-between;
  margin-top: 14rpx;
  min-height: 122rpx;
  padding: 22rpx 24rpx;
}

.list-copy {
  flex: 1;
  min-width: 0;
}

.list-title {
  color: var(--xc-ink);
  display: block;
  font-size: 30rpx;
  font-weight: 900;
}

.muted {
  color: #64748b;
  display: block;
  font-size: 24rpx;
  margin-top: 6rpx;
  word-break: break-word;
}

.empty {
  margin-top: 16rpx;
}

.arrow {
  color: #94a3b8;
}
</style>
