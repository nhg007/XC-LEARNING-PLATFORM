<template>
  <view class="page">
    <view class="hero">
      <view class="hero-top">
        <view class="hero-copy">
          <text class="eyebrow">{{ t('app.title') }}</text>
          <text class="title">{{ t('more.title') }}</text>
          <text class="subtitle">{{ t('more.subtitle') }}</text>
        </view>
        <LanguageSwitch variant="hero" />
      </view>
    </view>

    <view class="summary-card" @click="openPage(routes.profile)">
      <view class="avatar">{{ avatarText }}</view>
      <view class="summary-copy">
        <text class="summary-label">{{ t('more.accountSummary') }}</text>
        <text class="summary-title">{{ displayName }}</text>
        <text v-if="accessError" class="summary-error">{{ accessError }}</text>
        <text v-else class="summary-desc">{{ accessSummary }}</text>
      </view>
      <text class="access-pill">{{ accessLoading ? t('common.loading') : accessLabel }}</text>
    </view>

    <view class="group">
      <text class="group-title">{{ t('more.learningSupport') }}</text>
      <view class="entry-card" @click="openPage(routes.classroom)">
        <view class="mark class-mark">{{ t('more.classroomMark') }}</view>
        <view class="entry-copy">
          <text class="entry-title">{{ t('feature.classroom') }}</text>
          <text class="entry-desc">{{ t('more.classroomDesc') }}</text>
        </view>
        <text class="arrow">></text>
      </view>
      <view class="entry-card" @click="openPage(routes.records)">
        <view class="mark record-mark">{{ t('more.recordsMark') }}</view>
        <view class="entry-copy">
          <text class="entry-title">{{ t('feature.records') }}</text>
          <text class="entry-desc">{{ t('more.recordsDesc') }}</text>
        </view>
        <text class="arrow">></text>
      </view>
    </view>

    <view class="group">
      <text class="group-title">{{ t('more.accountAccess') }}</text>
      <view class="entry-card" @click="openPage(routes.membership)">
        <view class="mark membership-mark">{{ t('more.membershipMark') }}</view>
        <view class="entry-copy">
          <text class="entry-title">{{ t('membership.title') }}</text>
          <text class="entry-desc">{{ t('more.membershipDesc') }}</text>
        </view>
        <text class="arrow">></text>
      </view>
      <view class="entry-card" @click="openPage(routes.profile)">
        <view class="mark profile-mark">{{ t('more.profileMark') }}</view>
        <view class="entry-copy">
          <text class="entry-title">{{ t('profile.title') }}</text>
          <text class="entry-desc">{{ t('more.profileDesc') }}</text>
        </view>
        <text class="arrow">></text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import { fetchMembershipStatus } from '../../api/membership'
import LanguageSwitch from '../../components/LanguageSwitch.vue'
import { applyTabBarLocale, setPageTitle, useI18n } from '../../i18n'
import type { MembershipStatus, UserProfile } from '../../types/api'
import { openPage, requireLogin, routes } from '../../utils/navigation'
import { getProfile } from '../../utils/storage'

const { locale, t } = useI18n()

const profile = ref<UserProfile | null>(null)
const accessLoading = ref(false)
const accessError = ref('')
const membershipStatus = ref<MembershipStatus>({
  accessLevel: 'free',
  fullAccess: false,
  trialEndsAt: null,
  membershipEndsAt: null,
  remainingSeconds: 0
})

const displayName = computed(() => profile.value?.nickname || profile.value?.email || t('profile.guest'))

const avatarText = computed(() => {
  const name = displayName.value.trim()
  return (name ? name.slice(0, 1) : t('profile.guest').slice(0, 1)).toUpperCase()
})

const remainingDays = computed(() => Math.max(0, Math.ceil(membershipStatus.value.remainingSeconds / 86400)))

const accessLabel = computed(() => {
  if (membershipStatus.value.accessLevel === 'member') {
    return t('home.member')
  }
  if (membershipStatus.value.accessLevel === 'trial') {
    return t('home.trial')
  }
  return t('home.free')
})

const accessSummary = computed(() => {
  if (membershipStatus.value.accessLevel === 'member') {
    return t('more.memberRemaining', { days: remainingDays.value })
  }
  if (membershipStatus.value.accessLevel === 'trial') {
    return t('more.trialRemaining', { days: remainingDays.value })
  }
  return t('more.freeSummary')
})

async function loadAccessSummary() {
  accessLoading.value = true
  accessError.value = ''
  try {
    membershipStatus.value = await fetchMembershipStatus()
  } catch {
    accessError.value = t('more.accessLoadFailed')
  } finally {
    accessLoading.value = false
  }
}

onShow(() => {
  applyTabBarLocale()
  setPageTitle('more.title')
  if (!requireLogin()) {
    return
  }
  profile.value = getProfile()
  void loadAccessSummary()
})

onPullDownRefresh(async () => {
  try {
    if (requireLogin()) {
      profile.value = getProfile()
      await loadAccessSummary()
    }
  } finally {
    uni.stopPullDownRefresh()
  }
})

watch(locale, () => {
  applyTabBarLocale()
  setPageTitle('more.title')
})
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

.summary-card {
  align-items: center;
  background: #ffffff;
  border: 1px solid #d7e2ea;
  border-radius: 22rpx;
  box-shadow: 0 12rpx 32rpx rgba(15, 23, 42, 0.06);
  box-sizing: border-box;
  display: flex;
  gap: 18rpx;
  margin-bottom: 28rpx;
  min-height: 142rpx;
  padding: 22rpx;
}

.avatar {
  align-items: center;
  background: linear-gradient(135deg, #0f766e 0%, #14b8a6 100%);
  border-radius: 24rpx;
  color: #ffffff;
  display: flex;
  flex: 0 0 78rpx;
  font-size: 34rpx;
  font-weight: 900;
  height: 78rpx;
  justify-content: center;
}

.summary-copy {
  flex: 1;
  min-width: 0;
}

.summary-label {
  color: #0f766e;
  display: block;
  font-size: 22rpx;
  font-weight: 800;
  line-height: 1.2;
}

.summary-title {
  color: #102033;
  display: block;
  font-size: 31rpx;
  font-weight: 900;
  line-height: 1.3;
  margin-top: 8rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.summary-desc,
.summary-error {
  display: block;
  font-size: 23rpx;
  line-height: 1.35;
  margin-top: 6rpx;
}

.summary-desc {
  color: #64748b;
}

.summary-error {
  color: #b45309;
}

.access-pill {
  align-items: center;
  background: #ecfdf5;
  border: 1px solid #99f6e4;
  border-radius: 999rpx;
  color: #0f766e;
  display: flex;
  flex: 0 0 auto;
  font-size: 22rpx;
  font-weight: 900;
  justify-content: center;
  line-height: 1;
  min-height: 52rpx;
  padding: 0 18rpx;
}

.group {
  margin-top: 26rpx;
}

.group-title {
  color: #102033;
  display: block;
  font-size: 34rpx;
  font-weight: 800;
  margin-bottom: 14rpx;
}

.entry-card {
  align-items: center;
  background: #ffffff;
  border: 1px solid #d7e2ea;
  border-radius: 18rpx;
  box-shadow: 0 10rpx 30rpx rgba(15, 23, 42, 0.05);
  box-sizing: border-box;
  display: flex;
  gap: 18rpx;
  margin-top: 14rpx;
  min-height: 132rpx;
  padding: 22rpx;
}

.mark {
  align-items: center;
  border-radius: 16rpx;
  display: flex;
  flex: 0 0 68rpx;
  font-size: 28rpx;
  font-weight: 900;
  height: 68rpx;
  justify-content: center;
}

.class-mark {
  background: #fef3c7;
  color: #92400e;
}

.record-mark {
  background: #e0f2fe;
  color: #0369a1;
}

.membership-mark {
  background: #ccfbf1;
  color: #14796f;
}

.profile-mark {
  background: #f1f5f9;
  color: #475569;
}

.entry-copy {
  flex: 1;
  min-width: 0;
}

.entry-title {
  color: #102033;
  display: block;
  font-size: 30rpx;
  font-weight: 800;
  line-height: 1.35;
}

.entry-desc {
  color: #64748b;
  display: block;
  font-size: 23rpx;
  line-height: 1.45;
  margin-top: 8rpx;
}

.arrow {
  color: #94a3b8;
  flex: 0 0 auto;
  font-size: 28rpx;
}
</style>
