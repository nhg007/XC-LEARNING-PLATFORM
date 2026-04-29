<template>
  <view class="page">
    <view class="hero">
      <view class="hero-top">
        <view>
          <text class="eyebrow">{{ t('classroom.eyebrow') }}</text>
          <text class="title">{{ detail?.name || t('classroom.detailTitle') }}</text>
          <text class="subtitle">{{ detail?.description || t('classroom.detailSubtitle') }}</text>
        </view>
        <view class="hero-actions">
          <LanguageSwitch variant="hero" />
          <button class="icon-btn" size="mini" @click="goBack">{{ t('common.back') }}</button>
          <button class="icon-btn" size="mini" :loading="loading" @click="refreshPage">{{ t('common.refresh') }}</button>
        </view>
      </view>
    </view>

    <view v-if="errorMessage" class="state-card error-card">
      <text>{{ errorMessage }}</text>
      <button class="plain-btn compact" size="mini" @click="refreshPage">{{ t('common.retry') }}</button>
    </view>

    <view v-if="loading && !detail" class="state-card">{{ t('classroom.loadingDetail') }}</view>

    <template v-else-if="detail">
      <view class="overview-card">
        <view class="overview-head">
          <view>
            <text class="section-title">{{ t('classroom.classOverview') }}</text>
            <text class="muted">{{ t('classroom.statusPrefix', { status: statusLabel(detail.memberStatus) }) }}</text>
          </view>
          <button v-if="canManageClass" class="code-btn dark" size="mini" @click="copyInviteCode(detail.inviteCode)">
            {{ detail.inviteCode }}
          </button>
          <text v-else class="student-pill">{{ t('classroom.inviteManagedByTeacher') }}</text>
        </view>

        <view class="metrics">
          <view>
            <text class="label">{{ t('classroom.activeMembers') }}</text>
            <text class="value">{{ detail.activeMemberCount }}</text>
          </view>
          <view>
            <text class="label">{{ t('classroom.pendingMembers') }}</text>
            <text class="value">{{ detail.pendingMemberCount }}</text>
          </view>
          <view>
            <text class="label">{{ t('classroom.identity') }}</text>
            <text class="value small">{{ roleLabel(detail.memberRole) }}</text>
          </view>
        </view>

        <view v-if="detail.memberStatus !== 'active'" class="notice-card">
          <text>{{ t('classroom.pendingDetailHint', { status: statusLabel(detail.memberStatus) }) }}</text>
        </view>
      </view>

      <template v-if="detail.memberStatus === 'active'">
        <view class="tabs">
          <button :class="['tab-btn', { active: activeTab === 'members' }]" @click="activeTab = 'members'">
            {{ t('classroom.members') }}
          </button>
          <button :class="['tab-btn', { active: activeTab === 'stats' }]" @click="activeTab = 'stats'">
            {{ t('classroom.stats') }}
          </button>
        </view>

        <view v-if="activeTab === 'members'" class="section">
          <view class="section-head">
            <text class="section-title">{{ t('classroom.members') }}</text>
            <text class="muted">{{ t('classroom.memberCount', { count: members.length }) }}</text>
          </view>
          <view v-if="members.length === 0" class="state-card">{{ t('classroom.emptyMembers') }}</view>
          <view v-for="member in members" :key="member.id" class="member-card">
            <view>
              <text class="item-title">{{ member.nickname || member.email || `ID ${member.userId}` }}</text>
              <text class="muted">{{ member.email || t('common.empty') }}</text>
            </view>
            <view class="member-tags">
              <text class="role-pill" :class="{ teacher: member.memberRole === 'teacher' }">{{ roleLabel(member.memberRole) }}</text>
              <text class="status-pill" :class="`status-${member.status}`">{{ statusLabel(member.status) }}</text>
            </view>
          </view>
        </view>

        <view v-else class="section">
          <view class="section-head">
            <text class="section-title">{{ t('classroom.stats') }}</text>
            <text class="muted">{{ t('classroom.memberCount', { count: stats.length }) }}</text>
          </view>

          <view class="summary-grid">
            <view>
              <text class="label">{{ t('classroom.study') }}</text>
              <text class="summary-value">{{ formatDuration(totalStudySeconds) }}</text>
            </view>
            <view>
              <text class="label">{{ t('classroom.exercises') }}</text>
              <text class="summary-value">{{ totalExerciseCount }}</text>
            </view>
            <view>
              <text class="label">{{ t('classroom.averageAccuracy') }}</text>
              <text class="summary-value">{{ formatPercent(averageAccuracy) }}</text>
            </view>
          </view>

          <view v-if="stats.length === 0" class="state-card">{{ t('classroom.emptyStats') }}</view>
          <view v-for="item in stats" :key="item.userId" class="stat-card">
            <view class="stat-top">
              <view>
                <text class="item-title">{{ item.nickname || item.email || `ID ${item.userId}` }}</text>
                <text class="muted">{{ roleLabel(item.memberRole) }} · {{ t('classroom.recent', { time: item.lastStudyAt ? formatTime(item.lastStudyAt) : '-' }) }}</text>
              </view>
              <text class="score-pill">{{ formatPercent(item.accuracyRate) }}</text>
            </view>
            <view class="stat-grid">
              <view>
                <text class="label">{{ t('classroom.study') }}</text>
                <text class="metric-value">{{ formatDuration(item.studySeconds) }}</text>
              </view>
              <view>
                <text class="label">{{ t('classroom.exercises') }}</text>
                <text class="metric-value">{{ item.exerciseCount }}</text>
              </view>
              <view>
                <text class="label">{{ t('classroom.words') }}</text>
                <text class="metric-value">{{ item.vocabReviewCount }}</text>
              </view>
              <view>
                <text class="label">{{ t('classroom.dialogue') }}</text>
                <text class="metric-value">{{ item.dialogueCount }}</text>
              </view>
              <view>
                <text class="label">{{ t('classroom.matching') }}</text>
                <text class="metric-value">{{ item.matchingGameCount }}</text>
              </view>
            </view>
          </view>
        </view>
      </template>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { onLoad, onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import LanguageSwitch from '../../components/LanguageSwitch.vue'
import { fetchClassMembers, fetchClassRoomDetail, fetchClassStats } from '../../api/classroom'
import { applyTabBarLocale, setPageTitle, useI18n } from '../../i18n'
import type { ClassMember, ClassMemberStats, ClassRoomDetail } from '../../types/api'
import { openPage, requireLogin, routes } from '../../utils/navigation'

type DetailTab = 'members' | 'stats'

const { locale, t } = useI18n()

const classId = ref<number | null>(null)
const loading = ref(false)
const errorMessage = ref('')
const detail = ref<ClassRoomDetail | null>(null)
const members = ref<ClassMember[]>([])
const stats = ref<ClassMemberStats[]>([])
const activeTab = ref<DetailTab>('members')

const totalStudySeconds = computed(() => stats.value.reduce((sum, item) => sum + (item.studySeconds || 0), 0))
const totalExerciseCount = computed(() => stats.value.reduce((sum, item) => sum + (item.exerciseCount || 0), 0))
const canManageClass = computed(() => detail.value?.memberRole === 'teacher')
const averageAccuracy = computed(() => {
  if (stats.value.length === 0) {
    return 0
  }
  return stats.value.reduce((sum, item) => sum + Number(item.accuracyRate || 0), 0) / stats.value.length
})

onLoad(query => {
  const id = Number(query?.id)
  if (!requireLogin()) {
    return
  }
  if (!Number.isFinite(id) || id <= 0) {
    errorMessage.value = t('classroom.detailNeedId')
    return
  }
  classId.value = id
  void loadDetail(true)
})

onShow(() => {
  applyTabBarLocale()
  setPageTitle('classroom.detailTitle')
  if (!requireLogin()) {
    return
  }
})

watch(locale, () => {
  applyTabBarLocale()
  setPageTitle('classroom.detailTitle')
})

onPullDownRefresh(() => {
  void refreshPage().finally(() => uni.stopPullDownRefresh())
})

async function refreshPage() {
  await loadDetail(true)
}

async function loadDetail(showLoading = true) {
  if (!classId.value || loading.value) {
    return
  }
  loading.value = showLoading
  errorMessage.value = ''
  try {
    const nextDetail = await fetchClassRoomDetail(classId.value)
    detail.value = nextDetail
    if (nextDetail.memberStatus === 'active') {
      const [memberRows, statRows] = await Promise.all([
        fetchClassMembers(nextDetail.id),
        fetchClassStats(nextDetail.id)
      ])
      members.value = memberRows
      stats.value = statRows
    } else {
      members.value = []
      stats.value = []
      activeTab.value = 'members'
    }
  } catch {
    errorMessage.value = t('classroom.detailFailed')
  } finally {
    loading.value = false
  }
}

function goBack() {
  const pages = getCurrentPages()
  if (pages.length > 1) {
    uni.navigateBack()
    return
  }
  void openPage(routes.classroom)
}

function copyInviteCode(code: string) {
  void uni.setClipboardData({
    data: code,
    success: () => {
      void uni.showToast({ icon: 'none', title: t('classroom.copied') })
    }
  })
}

function roleLabel(role: string) {
  return role === 'teacher' ? t('classroom.roleTeacher') : t('classroom.roleMember')
}

function statusLabel(status: string) {
  const labels: Record<string, string> = {
    active: t('classroom.statusActive'),
    archived: t('classroom.statusArchived'),
    deleted: t('classroom.statusDeleted'),
    pending_teacher_review: t('classroom.statusPending'),
    invited: t('classroom.statusInvited'),
    rejected: t('classroom.statusRejected'),
    left: t('classroom.statusLeft'),
    removed: t('classroom.statusRemoved')
  }
  return labels[status] || status
}

function formatDuration(seconds: number | null | undefined) {
  const safeSeconds = Math.max(0, seconds || 0)
  const hours = Math.floor(safeSeconds / 3600)
  const minutes = Math.floor((safeSeconds % 3600) / 60)
  if (hours > 0) {
    return t('common.hoursMinutes', { hours, minutes })
  }
  if (minutes > 0) {
    return t('common.minutes', { minutes })
  }
  return t('common.seconds', { seconds: safeSeconds })
}

function formatPercent(value: number | null | undefined) {
  return `${Number(value || 0).toFixed(0)}%`
}

function formatTime(value: string) {
  return new Date(value).toLocaleDateString()
}
</script>

<style scoped>
.page {
  background: #eef5f7;
  min-height: 100vh;
  padding: 0 24rpx 32rpx;
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

.hero-top,
.overview-head,
.section-head,
.member-card,
.stat-top {
  align-items: flex-start;
  display: flex;
  justify-content: space-between;
  gap: 18rpx;
}

.hero-actions {
  align-items: flex-end;
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.eyebrow {
  color: #7dd3c7;
  display: block;
  font-size: 22rpx;
  font-weight: 700;
}

.title {
  display: block;
  font-size: 44rpx;
  font-weight: 800;
  line-height: 1.15;
  margin-top: 10rpx;
  word-break: break-all;
}

.subtitle,
.muted {
  color: #64748b;
  display: block;
  font-size: 24rpx;
  line-height: 1.5;
  margin-top: 8rpx;
}

.hero .subtitle {
  color: #cbd5e1;
  font-size: 26rpx;
  margin-top: 14rpx;
}

.icon-btn,
.plain-btn,
.code-btn,
.tab-btn {
  border-radius: 8rpx;
  font-size: 26rpx;
  margin: 0;
}

.icon-btn {
  background: rgba(255, 255, 255, 0.14);
  color: #ffffff;
  min-height: 58rpx;
  padding: 0 18rpx;
}

.plain-btn {
  background: #ffffff;
  border: 1px solid #cbd5e1;
  color: #102033;
  min-height: 72rpx;
}

.plain-btn.compact {
  margin-top: 14rpx;
  width: 160rpx;
}

.state-card,
.overview-card,
.member-card,
.stat-card {
  background: #ffffff;
  border: 1px solid #d7e2ea;
  border-radius: 8rpx;
  box-sizing: border-box;
}

.state-card {
  color: #64748b;
  font-size: 26rpx;
  margin-top: 14rpx;
  padding: 26rpx;
}

.error-card {
  background: #fff7ed;
  border-color: #fed7aa;
  color: #9a3412;
  margin-bottom: 18rpx;
}

.overview-card {
  border-radius: 24rpx;
  box-shadow: 0 12rpx 36rpx rgba(15, 23, 42, 0.06);
  padding: 24rpx;
}

.section-title {
  color: #102033;
  display: block;
  font-size: 34rpx;
  font-weight: 800;
}

.code-btn {
  background: #e2e8f0;
  color: #102033;
  flex-shrink: 0;
  min-height: 58rpx;
  padding: 0 18rpx;
}

.code-btn.dark {
  background: #102033;
  color: #ffffff;
}

.student-pill {
  align-items: center;
  background: #ecfdf5;
  border: 1px solid #99f6e4;
  border-radius: 999rpx;
  color: #0f766e;
  display: flex;
  flex-shrink: 0;
  font-size: 22rpx;
  font-weight: 800;
  line-height: 1.2;
  min-height: 58rpx;
  padding: 0 18rpx;
}

.metrics,
.summary-grid,
.stat-grid {
  display: flex;
  gap: 14rpx;
  margin-top: 18rpx;
}

.metrics > view,
.summary-grid > view,
.stat-grid > view {
  background: #f8fafc;
  border-radius: 8rpx;
  box-sizing: border-box;
  flex: 1;
  padding: 18rpx;
}

.label {
  color: #64748b;
  display: block;
  font-size: 23rpx;
}

.value,
.summary-value {
  color: #102033;
  display: block;
  font-size: 34rpx;
  font-weight: 800;
  margin-top: 6rpx;
}

.value.small {
  font-size: 26rpx;
}

.notice-card {
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 12rpx;
  color: #92400e;
  font-size: 24rpx;
  line-height: 1.5;
  margin-top: 18rpx;
  padding: 16rpx 18rpx;
}

.tabs {
  background: #ffffff;
  border: 1px solid #d7e2ea;
  border-radius: 18rpx;
  display: grid;
  gap: 10rpx;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 20rpx;
  padding: 8rpx;
}

.tab-btn {
  background: #f8fafc;
  color: #102033;
  min-height: 70rpx;
}

.tab-btn.active {
  background: #14796f;
  color: #ffffff;
  font-weight: 800;
}

.section {
  margin-top: 22rpx;
}

.member-card,
.stat-card {
  margin-top: 14rpx;
  padding: 20rpx;
}

.item-title {
  color: #102033;
  display: block;
  font-size: 30rpx;
  font-weight: 800;
  line-height: 1.3;
  word-break: break-all;
}

.member-tags {
  align-items: flex-end;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  gap: 8rpx;
}

.role-pill,
.status-pill,
.score-pill {
  border-radius: 999rpx;
  font-size: 22rpx;
  font-weight: 700;
  padding: 7rpx 14rpx;
}

.role-pill {
  background: #e0f2fe;
  color: #075985;
}

.role-pill.teacher {
  background: #ccfbf1;
  color: #0f766e;
}

.status-active,
.score-pill {
  background: #dcfce7;
  color: #166534;
}

.status-pending_teacher_review,
.status-invited {
  background: #fffbeb;
  color: #92400e;
}

.status-rejected,
.status-left,
.status-removed,
.status-deleted {
  background: #fee2e2;
  color: #991b1b;
}

.status-archived {
  background: #e2e8f0;
  color: #475569;
}

.summary-grid {
  flex-wrap: wrap;
}

.summary-grid > view {
  min-width: calc(33.333% - 10rpx);
}

.stat-grid {
  flex-wrap: wrap;
}

.stat-grid > view {
  min-width: calc(50% - 7rpx);
}

.metric-value {
  color: #102033;
  display: block;
  font-size: 28rpx;
  font-weight: 800;
  margin-top: 6rpx;
}

@media (max-width: 360px) {
  .metrics,
  .summary-grid {
    flex-direction: column;
  }
}
</style>
