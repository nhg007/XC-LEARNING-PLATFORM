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
          <view class="hero-button-row">
            <button class="icon-btn" size="mini" @click="goBack">{{ t('common.back') }}</button>
            <button class="icon-btn" size="mini" :loading="loading" @click="refreshPage">{{ t('common.refresh') }}</button>
          </view>
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
          <text class="student-pill">{{ statusLabel(detail.memberStatus) }}</text>
        </view>

        <view class="owner-card">
          <view class="owner-avatar">{{ ownerInitial }}</view>
          <view>
            <text class="label">{{ t('classroom.owner') }}</text>
            <text class="owner-name">{{ detail.ownerName || t('classroom.noOwner') }}</text>
            <text v-if="detail.ownerContact" class="muted">{{ detail.ownerContact }}</text>
          </view>
        </view>

        <view class="invite-card">
          <view>
            <text class="label">{{ t('classroom.inviteCode') }}</text>
            <text class="invite-code">{{ detail.inviteCode }}</text>
            <text class="muted">{{ t('classroom.inviteHint') }}</text>
          </view>
          <button class="copy-icon-btn" :aria-label="t('classroom.copyInvite')" @click="copyInviteCode(detail.inviteCode)">
            <view class="copy-icon" />
          </button>
        </view>

        <view v-if="detail.memberStatus !== 'active'" class="notice-card">
          <text>{{ t('classroom.pendingDetailHint', { status: statusLabel(detail.memberStatus) }) }}</text>
        </view>
      </view>

      <template v-if="detail.memberStatus === 'active'">
        <view class="panel-tabs" :class="{ single: !detail.createdByMe }">
          <button
            class="panel-tab"
            :class="{ active: activePanel === 'members' }"
            @click="activePanel = 'members'"
          >
            <text>{{ t('classroom.classmates') }}</text>
            <text>{{ t('classroom.memberCount', { count: members.length }) }}</text>
          </button>
          <button
            v-if="detail.createdByMe"
            class="panel-tab"
            :class="{ active: activePanel === 'stats' }"
            @click="activePanel = 'stats'"
          >
            <text>{{ t('classroom.learningRecords') }}</text>
            <text>{{ t('classroom.memberCount', { count: stats.length }) }}</text>
          </button>
        </view>

        <view v-if="activePanel === 'members'" class="section">
          <view class="section-head">
            <text class="section-title">{{ t('classroom.classmates') }}</text>
            <text class="muted">{{ t('classroom.memberCount', { count: members.length }) }}</text>
          </view>
          <view v-if="members.length === 0" class="state-card">{{ t('classroom.emptyMembers') }}</view>
          <view v-for="member in members" :key="member.id" class="member-card">
            <view class="member-avatar">{{ memberInitial(member) }}</view>
            <view class="member-main">
              <text class="item-title">{{ memberName(member) }}</text>
              <text v-if="member.email" class="muted">{{ member.email }}</text>
              <text class="muted">{{ member.joinedAt ? t('classroom.joinedAt', { time: formatDateTime(member.joinedAt) }) : t('classroom.noJoinedAt') }}</text>
            </view>
            <text class="member-status">{{ statusLabel(member.status) }}</text>
          </view>
          <view v-if="!detail.createdByMe" class="notice-card">
            <text>{{ t('classroom.creatorOnlyStats') }}</text>
          </view>
        </view>

        <view v-else class="section">
          <view class="section-head">
            <text class="section-title">{{ t('classroom.learningOverview') }}</text>
            <text class="muted">{{ t('classroom.memberCount', { count: stats.length }) }}</text>
          </view>
          <view class="summary-grid">
            <view class="summary-card">
              <text>{{ t('classroom.totalStudyTime') }}</text>
              <text>{{ formatStudyTime(totalStudySeconds) }}</text>
            </view>
            <view class="summary-card">
              <text>{{ t('classroom.exercises') }}</text>
              <text>{{ totalExerciseCount }}</text>
            </view>
            <view class="summary-card">
              <text>{{ t('classroom.averageAccuracy') }}</text>
              <text>{{ formatPercent(averageAccuracy) }}</text>
            </view>
            <view class="summary-card">
              <text>{{ t('classroom.activeLearners') }}</text>
              <text>{{ activeLearnerCount }}</text>
            </view>
          </view>
          <view v-if="stats.length === 0" class="state-card">{{ t('classroom.emptyStats') }}</view>
          <view v-for="row in statsRows" :key="row.userId" class="stats-card">
            <view class="stats-head">
              <view>
                <text class="item-title">{{ memberStatsName(row) }}</text>
                <text class="muted">{{ row.email || t('common.empty') }}</text>
              </view>
              <text class="accuracy-pill">{{ formatPercent(row.accuracyRate) }}</text>
            </view>
            <view class="stats-metrics">
              <view>
                <text>{{ t('classroom.studyTime') }}</text>
                <text>{{ formatStudyTime(row.studySeconds) }}</text>
              </view>
              <view>
                <text>{{ t('classroom.exercises') }}</text>
                <text>{{ row.exerciseCount }}</text>
              </view>
              <view>
                <text>{{ t('classroom.words') }}</text>
                <text>{{ row.vocabReviewCount }}</text>
              </view>
            </view>
            <text class="muted">{{ t('classroom.activitySummary', { vocab: row.vocabReviewCount, dialogue: row.dialogueCount, matching: row.matchingGameCount }) }}</text>
            <text class="muted">{{ row.lastStudyAt ? t('classroom.lastStudyAt', { time: formatDateTime(row.lastStudyAt) }) : t('classroom.noStudyRecord') }}</text>
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

type DetailPanel = 'members' | 'stats'

const { locale, t } = useI18n()

const classId = ref<number | null>(null)
const loading = ref(false)
const errorMessage = ref('')
const activePanel = ref<DetailPanel>('members')
const detail = ref<ClassRoomDetail | null>(null)
const members = ref<ClassMember[]>([])
const stats = ref<ClassMemberStats[]>([])
const ownerInitial = computed(() => {
  const name = detail.value?.ownerName || detail.value?.ownerContact || ''
  return name.trim().slice(0, 1).toUpperCase() || 'C'
})
const totalStudySeconds = computed(() => stats.value.reduce((sum, row) => sum + Number(row.studySeconds || 0), 0))
const totalExerciseCount = computed(() => stats.value.reduce((sum, row) => sum + Number(row.exerciseCount || 0), 0))
const totalCorrectCount = computed(() => stats.value.reduce((sum, row) => sum + Number(row.correctCount || 0), 0))
const averageAccuracy = computed(() => {
  if (totalExerciseCount.value <= 0) {
    return 0
  }
  return (totalCorrectCount.value / totalExerciseCount.value) * 100
})
const activeLearnerCount = computed(() => stats.value.filter(row => Boolean(row.lastStudyAt)).length)
const statsRows = computed(() => [...stats.value].sort((a, b) => {
  const left = a.lastStudyAt ? new Date(a.lastStudyAt).getTime() : 0
  const right = b.lastStudyAt ? new Date(b.lastStudyAt).getTime() : 0
  return right - left
}))

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
    if (!nextDetail.createdByMe && activePanel.value === 'stats') {
      activePanel.value = 'members'
    }
    if (nextDetail.memberStatus === 'active') {
      if (nextDetail.createdByMe) {
        const [nextMembers, nextStats] = await Promise.all([
          fetchClassMembers(nextDetail.id),
          fetchClassStats(nextDetail.id)
        ])
        members.value = nextMembers
        stats.value = nextStats
      } else {
        members.value = await fetchClassMembers(nextDetail.id)
        stats.value = []
      }
    } else {
      members.value = []
      stats.value = []
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

function memberInitial(member: ClassMember) {
  return memberName(member).trim().slice(0, 1).toUpperCase()
}

function memberName(member: ClassMember) {
  return member.nickname || member.email || `ID ${member.userId}`
}

function memberStatsName(member: ClassMemberStats) {
  return member.nickname || member.email || `ID ${member.userId}`
}

function formatStudyTime(seconds: number) {
  const safeSeconds = Math.max(0, seconds || 0)
  if (safeSeconds < 60) {
    return t('common.seconds', { seconds: safeSeconds })
  }
  const hours = Math.floor(safeSeconds / 3600)
  const minutes = Math.floor((safeSeconds % 3600) / 60)
  return hours > 0 ? t('common.hoursMinutes', { hours, minutes }) : t('common.minutes', { minutes })
}

function formatPercent(value: number) {
  return `${Number(value || 0).toFixed(1)}%`
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString()
}

function statusLabel(status: string) {
  const labels: Record<string, string> = {
    active: t('classroom.statusActive'),
    archived: t('classroom.statusArchived'),
    deleted: t('classroom.statusDeleted'),
    left: t('classroom.statusLeft'),
    removed: t('classroom.statusRemoved')
  }
  return labels[status] || status
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
.section-head {
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

.hero-button-row {
  display: flex;
  gap: 12rpx;
  justify-content: flex-end;
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
.plain-btn {
  border-radius: 8rpx;
  font-size: 26rpx;
  margin: 0;
}

.icon-btn {
  background: rgba(255, 255, 255, 0.14);
  color: #ffffff;
  height: 58rpx;
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

.invite-card .compact {
  margin-top: 0;
}

.state-card,
.overview-card,
.member-card {
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

.owner-card {
  align-items: center;
  background: #f8fafc;
  border: 1px solid #d7e2ea;
  border-radius: 12rpx;
  display: flex;
  gap: 18rpx;
  margin-top: 20rpx;
  padding: 18rpx;
}

.invite-card {
  align-items: center;
  background: #ecfeff;
  border: 1px solid #bae6fd;
  border-radius: 8rpx;
  display: flex;
  gap: 16rpx;
  justify-content: space-between;
  margin-top: 16rpx;
  padding: 18rpx;
}

.invite-card > view {
  min-width: 0;
}

.invite-code {
  color: #102033;
  display: block;
  font-size: 34rpx;
  font-weight: 900;
  letter-spacing: 3rpx;
  margin-top: 4rpx;
  word-break: break-all;
}

.copy-icon-btn {
  align-items: center;
  background: #ffffff;
  border: 1px solid #94a3b8;
  border-radius: 12rpx;
  box-sizing: border-box;
  display: flex;
  flex-shrink: 0;
  height: 72rpx;
  justify-content: center;
  margin: 0;
  min-height: 72rpx;
  padding: 0;
  width: 72rpx;
}

.copy-icon-btn::after {
  border: 0;
}

.copy-icon {
  border: 3rpx solid #102033;
  border-radius: 5rpx;
  box-sizing: border-box;
  height: 28rpx;
  position: relative;
  width: 24rpx;
}

.copy-icon::before {
  border: 3rpx solid #102033;
  border-radius: 5rpx;
  box-sizing: border-box;
  content: "";
  height: 28rpx;
  left: -12rpx;
  position: absolute;
  top: 8rpx;
  width: 24rpx;
}

.owner-avatar {
  align-items: center;
  background: #2563eb;
  border-radius: 12rpx;
  color: #ffffff;
  display: flex;
  flex-shrink: 0;
  font-size: 34rpx;
  font-weight: 900;
  height: 80rpx;
  justify-content: center;
  width: 80rpx;
}

.owner-name {
  color: #102033;
  display: block;
  font-size: 30rpx;
  font-weight: 800;
  margin-top: 6rpx;
}

.label {
  color: #64748b;
  display: block;
  font-size: 23rpx;
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

.section {
  margin-top: 22rpx;
}

.panel-tabs {
  background: #e8eef6;
  border: 1px solid #d7e2ea;
  border-radius: 16rpx;
  display: grid;
  gap: 0;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 22rpx;
  overflow: hidden;
  padding: 6rpx;
}

.panel-tabs.single {
  grid-template-columns: 1fr;
}

.panel-tab {
  background: transparent;
  border-radius: 12rpx;
  color: #475569;
  display: grid;
  gap: 4rpx;
  margin: 0;
  min-height: 76rpx;
  padding: 8rpx 14rpx;
  text-align: left;
}

.panel-tab::after {
  border: 0;
}

.panel-tab.active {
  background: #ffffff;
  color: #1d4ed8;
  box-shadow: 0 8rpx 18rpx rgba(15, 23, 42, 0.08);
}

.panel-tab text:first-child {
  font-size: 25rpx;
  font-weight: 800;
}

.panel-tab text:last-child {
  font-size: 21rpx;
}

.member-card {
  align-items: center;
  display: grid;
  gap: 16rpx;
  grid-template-columns: 72rpx minmax(0, 1fr) auto;
  margin-top: 14rpx;
  padding: 20rpx;
}

.member-avatar {
  align-items: center;
  background: #f1f5f9;
  border: 1px solid #d7e2ea;
  border-radius: 16rpx;
  color: #334155;
  display: flex;
  font-size: 28rpx;
  font-weight: 900;
  height: 72rpx;
  justify-content: center;
  width: 72rpx;
}

.member-main {
  min-width: 0;
}

.member-main .muted {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.member-status {
  background: #ecfdf5;
  border-radius: 999rpx;
  color: #0f766e;
  flex-shrink: 0;
  font-size: 21rpx;
  font-weight: 800;
  padding: 7rpx 12rpx;
}

.summary-grid {
  display: grid;
  gap: 14rpx;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 14rpx;
}

.summary-card {
  background: #ffffff;
  border: 1px solid #d7e2ea;
  border-radius: 8rpx;
  box-sizing: border-box;
  min-height: 128rpx;
  padding: 20rpx;
}

.summary-card text:first-child {
  color: #64748b;
  display: block;
  font-size: 22rpx;
}

.summary-card text:last-child {
  color: #102033;
  display: block;
  font-size: 34rpx;
  font-weight: 900;
  line-height: 1.15;
  margin-top: 12rpx;
}

.stats-card {
  background: #ffffff;
  border: 1px solid #d7e2ea;
  border-radius: 8rpx;
  display: grid;
  gap: 12rpx;
  margin-top: 14rpx;
  padding: 22rpx;
}

.stats-head {
  align-items: flex-start;
  display: flex;
  gap: 16rpx;
  justify-content: space-between;
}

.accuracy-pill {
  background: #eff6ff;
  border-radius: 999rpx;
  color: #1d4ed8;
  flex-shrink: 0;
  font-size: 22rpx;
  font-weight: 900;
  padding: 8rpx 14rpx;
}

.stats-metrics {
  display: grid;
  gap: 10rpx;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.stats-metrics > view {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8rpx;
  padding: 14rpx;
}

.stats-metrics text:first-child {
  color: #64748b;
  display: block;
  font-size: 21rpx;
}

.stats-metrics text:last-child {
  color: #102033;
  display: block;
  font-size: 28rpx;
  font-weight: 900;
  margin-top: 6rpx;
}

.item-title {
  color: #102033;
  display: block;
  font-size: 30rpx;
  font-weight: 800;
  line-height: 1.3;
  word-break: break-all;
}

@media (max-width: 360px) {
  .member-card,
  .stats-metrics {
    grid-template-columns: 1fr;
  }
}
</style>
