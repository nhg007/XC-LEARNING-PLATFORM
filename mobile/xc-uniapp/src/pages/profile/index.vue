<template>
  <view class="page">
    <view class="hero">
      <view class="hero-top">
        <view class="hero-copy">
          <text class="eyebrow">{{ t('app.title') }}</text>
          <text class="title">{{ t('profile.title') }}</text>
          <text class="subtitle">{{ t('profile.subtitle') }}</text>
        </view>
        <LanguageSwitch variant="hero" />
      </view>
    </view>

    <view class="profile-card">
      <view class="avatar">{{ avatarText }}</view>
      <view class="profile-copy">
        <text class="profile-name">{{ displayName }}</text>
        <text class="profile-email">{{ profile?.email || '-' }}</text>
      </view>
      <text class="status-pill">{{ profile?.status || '-' }}</text>
    </view>

    <view class="quick-actions">
      <view class="quick-action" @click="openRecords">
        <text class="quick-mark record-mark">{{ t('features.recordsMark') }}</text>
        <view class="quick-copy">
          <text class="quick-title">{{ t('feature.records') }}</text>
          <text class="quick-desc">{{ t('profile.recordsDesc') }}</text>
        </view>
      </view>
      <view class="quick-action" @click="openFavorites">
        <text class="quick-mark favorites-mark">{{ t('features.favoritesMark') }}</text>
        <view class="quick-copy">
          <text class="quick-title">{{ t('vocab.favorites') }}</text>
          <text class="quick-desc">{{ t('features.favoritesDesc') }}</text>
        </view>
      </view>
      <view class="quick-action" @click="openMembership">
        <text class="quick-mark membership-mark">{{ t('features.membershipMark') }}</text>
        <view class="quick-copy">
          <text class="quick-title">{{ t('membership.title') }}</text>
          <text class="quick-desc">{{ t('profile.membershipDesc') }}</text>
        </view>
      </view>
    </view>

    <view class="access-card">
      <view class="section-head">
        <text class="section-title">{{ t('membership.currentAccess') }}</text>
        <text class="access-pill">{{ accessLabel }}</text>
      </view>
      <view class="access-grid">
        <view class="metric">
          <text class="metric-label">{{ t('membership.remaining') }}</text>
          <text class="metric-value">{{ remainingLabel }}</text>
        </view>
        <view class="metric">
          <text class="metric-label">{{ t('membership.membershipEndsAt') }}</text>
          <text class="metric-value small">{{ formatOptionalDate(status.membershipEndsAt) }}</text>
        </view>
      </view>
    </view>

    <view class="progress-card" @click="openRecords">
      <view class="section-head">
        <text class="section-title">{{ t('profile.learningOverview') }}</text>
        <text class="link-text">{{ t('profile.openRecords') }}</text>
      </view>
      <view class="progress-grid">
        <view class="metric">
          <text class="metric-label">{{ t('records.totalTime') }}</text>
          <text class="metric-value">{{ formatDuration(summary.totalStudySeconds) }}</text>
        </view>
        <view class="metric">
          <text class="metric-label">{{ t('records.accuracy') }}</text>
          <text class="metric-value">{{ formatPercent(summary.overallAccuracyRate) }}</text>
        </view>
        <view class="metric">
          <text class="metric-label">{{ t('profile.currentStreak') }}</text>
          <text class="metric-value">{{ t('common.days', { days: summary.currentStreakDays }) }}</text>
        </view>
        <view class="metric">
          <text class="metric-label">{{ t('records.words') }}</text>
          <text class="metric-value">{{ summary.totalVocabReviewCount }}</text>
        </view>
      </view>
    </view>

    <view class="settings-card">
      <text class="section-title">{{ t('profile.settings') }}</text>
      <view class="setting-row static">
        <view>
          <text class="setting-title">{{ t('profile.language') }}</text>
          <text class="setting-desc">{{ t('profile.languageDesc') }}</text>
        </view>
        <LanguageSwitch />
      </view>
    </view>

    <view class="settings-card">
      <view class="section-head">
        <text class="section-title">{{ t('profile.learningSettings') }}</text>
        <text v-if="preferenceSaving" class="saving-text">{{ t('common.loading') }}</text>
      </view>

      <view class="setting-block">
        <view>
          <text class="setting-title">{{ t('profile.vocabMeaningLanguage') }}</text>
          <text class="setting-desc">{{ t('profile.vocabMeaningLanguageDesc') }}</text>
        </view>
        <view class="segmented">
          <button
            class="segment"
            :class="{ active: preference?.vocabMeaningLanguage === 'ru' }"
            @click="savePreferenceField({ vocabMeaningLanguage: 'ru' })"
          >
            {{ t('common.ru') }}
          </button>
          <button
            class="segment"
            :class="{ active: preference?.vocabMeaningLanguage === 'en' }"
            @click="savePreferenceField({ vocabMeaningLanguage: 'en' })"
          >
            {{ t('common.en') }}
          </button>
        </view>
      </view>

      <view class="setting-block">
        <view>
          <text class="setting-title">{{ t('profile.translationLanguage') }}</text>
          <text class="setting-desc">{{ t('profile.translationLanguageDesc') }}</text>
        </view>
        <view class="segmented">
          <button
            class="segment"
            :class="{ active: preference?.translationLanguage === 'ru' }"
            @click="savePreferenceField({ translationLanguage: 'ru' })"
          >
            {{ t('common.ru') }}
          </button>
          <button
            class="segment"
            :class="{ active: preference?.translationLanguage === 'en' }"
            @click="savePreferenceField({ translationLanguage: 'en' })"
          >
            {{ t('common.en') }}
          </button>
        </view>
      </view>

      <view class="setting-block">
        <view>
          <text class="setting-title">{{ t('profile.matchingMeaningLanguage') }}</text>
          <text class="setting-desc">{{ t('profile.matchingMeaningLanguageDesc') }}</text>
        </view>
        <view class="segmented">
          <button
            class="segment"
            :class="{ active: preference?.matchingMeaningLanguage === 'ru' }"
            @click="savePreferenceField({ matchingMeaningLanguage: 'ru' })"
          >
            {{ t('common.ru') }}
          </button>
          <button
            class="segment"
            :class="{ active: preference?.matchingMeaningLanguage === 'en' }"
            @click="savePreferenceField({ matchingMeaningLanguage: 'en' })"
          >
            {{ t('common.en') }}
          </button>
        </view>
      </view>

      <view class="setting-row static">
        <view>
          <text class="setting-title">{{ t('profile.soundEnabled') }}</text>
          <text class="setting-desc">{{ t('profile.soundEnabledDesc') }}</text>
        </view>
        <button
          class="segment"
          :class="{ active: preference?.soundEnabled }"
          @click="savePreferenceField({ soundEnabled: !preference?.soundEnabled })"
        >
          {{ preference?.soundEnabled ? t('profile.on') : t('profile.off') }}
        </button>
      </view>

      <!-- <view class="setting-row static">
        <view>
          <text class="setting-title">{{ t('profile.apiBaseUrl') }}</text>
          <text class="setting-desc url">{{ apiBaseUrlLabel }}</text>
        </view>
      </view> -->
    </view>

    <button class="logout-button" @click="confirmLogout">{{ t('profile.logout') }}</button>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app';
import LanguageSwitch from '../../components/LanguageSwitch.vue';
import { fetchMembershipStatus } from '../../api/membership';
import { fetchLearningSummary } from '../../api/stats';
import { localeToUiLanguage, usePreferences } from '../../composables/usePreferences';
import { applyTabBarLocale, setPageTitle, useI18n } from '../../i18n';
import type { LearningSummary, MembershipStatus, UpdateUserPreferencePayload, UserProfile } from '../../types/api';
import { openPage, requireLogin, routes } from '../../utils/navigation';
import { clearSession, getProfile } from '../../utils/storage';

const { locale, t } = useI18n();
const preferences = usePreferences();
const { preference, saving: preferenceSaving } = preferences;
const profile = ref<UserProfile | null>(null);
const status = ref<MembershipStatus>({
  accessLevel: 'free',
  fullAccess: false,
  trialEndsAt: null,
  membershipEndsAt: null,
  remainingSeconds: 0,
});
const summary = ref<LearningSummary>({
  totalStudySeconds: 0,
  totalExerciseCount: 0,
  totalCorrectCount: 0,
  totalVocabReviewCount: 0,
  currentStreakDays: 0,
  longestStreakDays: 0,
  overallAccuracyRate: 0,
  lastStudyDate: null,
});

const displayName = computed(() => profile.value?.nickname || profile.value?.email || t('profile.guest'));
const avatarText = computed(() => displayName.value.slice(0, 1).toUpperCase());
const apiBaseUrlLabel = computed(() => import.meta.env.VITE_API_BASE_URL || '-');

const accessLabel = computed(() => {
  if (status.value.accessLevel === 'member') {
    return t('home.member');
  }
  if (status.value.accessLevel === 'trial') {
    return t('home.trial');
  }
  return t('home.free');
});

const remainingLabel = computed(() => {
  const days = Math.ceil(status.value.remainingSeconds / 86400);
  return t('common.days', { days: Math.max(0, days) });
});

onShow(() => {
  applyTabBarLocale();
  setPageTitle('profile.title');
  if (!requireLogin()) {
    return;
  }
  profile.value = getProfile();
  void loadPageData();
});

onPullDownRefresh(async () => {
  try {
    if (requireLogin()) {
      profile.value = getProfile();
      await loadPageData();
    }
  } finally {
    uni.stopPullDownRefresh();
  }
});

watch(locale, () => {
  applyTabBarLocale();
  setPageTitle('profile.title');
  const uiLanguage = localeToUiLanguage(locale.value);
  if (preference.value && preference.value.uiLanguage !== uiLanguage) {
    void savePreferenceField({ uiLanguage }, false);
  }
});

async function loadPageData() {
  await Promise.all([loadStatus(), loadSummary(), preferences.loadPreference(true)]);
}

async function loadStatus() {
  status.value = await fetchMembershipStatus();
}

async function loadSummary() {
  summary.value = await fetchLearningSummary();
}

async function savePreferenceField(payload: UpdateUserPreferencePayload, showToast = true) {
  await preferences.savePreference(payload);
  if (showToast) {
    void uni.showToast({ icon: 'none', title: t('profile.settingsSaved') });
  }
}

function openMembership() {
  void openPage(routes.membership);
}

function openRecords() {
  void openPage(routes.records);
}

function openFavorites() {
  void openPage(routes.vocabFavorites);
}

function confirmLogout() {
  uni.showModal({
    title: t('profile.logoutConfirmTitle'),
    content: t('profile.logoutConfirmContent'),
    cancelText: t('common.cancel'),
    confirmText: t('profile.logout'),
    success(result) {
      if (!result.confirm) {
        return;
      }
      clearSession();
      preferences.resetPreference();
      void uni.reLaunch({ url: routes.login });
    },
  });
}

function formatOptionalDate(value: string | null) {
  if (!value) {
    return '-';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '-';
  }
  const month = pad(date.getMonth() + 1);
  const day = pad(date.getDate());
  const hour = pad(date.getHours());
  const minute = pad(date.getMinutes());
  return `${date.getFullYear()}/${month}/${day} ${hour}:${minute}`;
}

function formatDuration(seconds: number | null | undefined) {
  const safeSeconds = Math.max(0, seconds || 0);
  const hours = Math.floor(safeSeconds / 3600);
  const minutes = Math.floor((safeSeconds % 3600) / 60);
  if (hours > 0) {
    return t('common.hoursMinutes', { hours, minutes });
  }
  if (minutes > 0) {
    return t('common.minutes', { minutes });
  }
  return t('common.seconds', { seconds: safeSeconds });
}

function formatPercent(value: number | null | undefined) {
  return `${Number(value || 0).toFixed(0)}%`;
}

function pad(value: number) {
  return String(value).padStart(2, '0');
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

.profile-card,
.access-card,
.progress-card,
.settings-card {
  background: #ffffff;
  border: 1px solid #d7e2ea;
  border-radius: 24rpx;
  box-shadow: 0 12rpx 36rpx rgba(15, 23, 42, 0.06);
  box-sizing: border-box;
  padding: 24rpx;
}

.profile-card {
  align-items: center;
  display: flex;
  gap: 18rpx;
}

.quick-actions {
  display: grid;
  gap: 14rpx;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 18rpx;
}

.quick-action {
  background: #ffffff;
  border: 1px solid #d7e2ea;
  border-radius: 18rpx;
  box-shadow: 0 10rpx 30rpx rgba(15, 23, 42, 0.05);
  box-sizing: border-box;
  min-height: 176rpx;
  padding: 20rpx;
}

.quick-mark {
  align-items: center;
  border-radius: 14rpx;
  display: flex;
  font-size: 26rpx;
  font-weight: 900;
  height: 58rpx;
  justify-content: center;
  width: 58rpx;
}

.record-mark {
  background: #e0f2fe;
  color: #0369a1;
}

.favorites-mark {
  background: #fef9c3;
  color: #a16207;
}

.membership-mark {
  background: #ccfbf1;
  color: #14796f;
}

.quick-copy {
  margin-top: 14rpx;
}

.quick-title {
  color: #102033;
  display: block;
  font-size: 28rpx;
  font-weight: 800;
  line-height: 1.3;
}

.quick-desc {
  color: #64748b;
  display: block;
  font-size: 22rpx;
  line-height: 1.45;
  margin-top: 6rpx;
}

.avatar {
  align-items: center;
  background: #14796f;
  border-radius: 20rpx;
  color: #ffffff;
  display: flex;
  flex: 0 0 92rpx;
  font-size: 40rpx;
  font-weight: 900;
  height: 92rpx;
  justify-content: center;
}

.profile-copy {
  flex: 1;
  min-width: 0;
}

.profile-name {
  color: #102033;
  display: block;
  font-size: 34rpx;
  font-weight: 800;
  line-height: 1.25;
  word-break: break-word;
}

.profile-email {
  color: #64748b;
  display: block;
  font-size: 24rpx;
  line-height: 1.45;
  margin-top: 6rpx;
  word-break: break-all;
}

.status-pill,
.access-pill {
  align-items: center;
  border-radius: 999rpx;
  display: flex;
  font-size: 22rpx;
  font-weight: 800;
  justify-content: center;
  line-height: 1;
  min-height: 46rpx;
  padding: 0 18rpx;
}

.status-pill {
  background: #f1f5f9;
  color: #475569;
}

.access-card,
.progress-card,
.settings-card {
  margin-top: 20rpx;
}

.section-head {
  align-items: center;
  display: flex;
  gap: 16rpx;
  justify-content: space-between;
}

.section-title {
  color: #102033;
  display: block;
  font-size: 34rpx;
  font-weight: 800;
}

.access-pill {
  background: #ccfbf1;
  color: #14796f;
}

.access-grid {
  display: grid;
  gap: 14rpx;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 22rpx;
}

.progress-grid {
  display: grid;
  gap: 14rpx;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 22rpx;
}

.metric {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 16rpx;
  box-sizing: border-box;
  min-height: 124rpx;
  padding: 18rpx;
}

.metric-label {
  color: #64748b;
  display: block;
  font-size: 24rpx;
  line-height: 1.5;
}

.metric-value {
  color: #102033;
  display: block;
  font-size: 34rpx;
  font-weight: 800;
  line-height: 1.25;
  margin-top: 8rpx;
  word-break: break-word;
}

.link-text {
  color: #14796f;
  font-size: 24rpx;
  font-weight: 800;
}

.metric-value.small {
  font-size: 24rpx;
}

.setting-row {
  align-items: center;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  gap: 18rpx;
  justify-content: space-between;
  min-height: 112rpx;
  padding: 22rpx 0;
}

.setting-block {
  border-bottom: 1px solid #e2e8f0;
  padding: 22rpx 0;
}

.setting-block:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}

.setting-row:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}

.setting-row.static {
  align-items: flex-start;
}

.setting-title {
  color: #102033;
  display: block;
  font-size: 28rpx;
  font-weight: 800;
  line-height: 1.35;
}

.setting-desc {
  color: #64748b;
  display: block;
  font-size: 23rpx;
  line-height: 1.45;
  margin-top: 8rpx;
  word-break: break-word;
}

.setting-desc.url {
  color: #475569;
  font-weight: 700;
  word-break: break-all;
}

.saving-text {
  color: #64748b;
  font-size: 23rpx;
}

.segmented {
  display: grid;
  gap: 12rpx;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 18rpx;
}

.segment,
.toggle-btn {
  border-radius: 14rpx;
  font-size: 27rpx;
  font-weight: 800;
  margin: 0;
  min-height: 76rpx;
}

.segment,
.toggle-btn {
  background: #f8fafc;
  border: 1px solid #d7e2ea;
  color: #102033;
}

.segment.active,
.toggle-btn.active {
  background: #14796f;
  border-color: #14796f;
  color: #ffffff;
}

.toggle-btn {
  flex: 0 0 150rpx;
}

.arrow {
  color: #94a3b8;
  flex: 0 0 auto;
  font-size: 28rpx;
}

.logout-button {
  background: #fee2e2;
  border: 1px solid #fecaca;
  border-radius: 14rpx;
  color: #b91c1c;
  font-size: 30rpx;
  font-weight: 800;
  margin: 28rpx 0 0;
  min-height: 88rpx;
}
</style>
