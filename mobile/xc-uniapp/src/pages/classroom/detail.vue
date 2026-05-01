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

        <view class="teacher-card">
          <view class="teacher-avatar">{{ teacherInitial }}</view>
          <view>
            <text class="label">{{ t('classroom.teacher') }}</text>
            <text class="teacher-name">{{ detail.teacherName || t('classroom.noTeacher') }}</text>
            <text class="muted">{{ detail.teacherContact || t('classroom.noTeacherContact') }}</text>
          </view>
        </view>

        <view v-if="detail.memberStatus !== 'active'" class="notice-card">
          <text>{{ t('classroom.pendingDetailHint', { status: statusLabel(detail.memberStatus) }) }}</text>
        </view>
      </view>

      <template v-if="detail.memberStatus === 'active'">
        <view class="section">
          <view class="section-head">
            <text class="section-title">{{ t('classroom.classmates') }}</text>
            <text class="muted">{{ t('classroom.memberCount', { count: members.length }) }}</text>
          </view>
          <view v-if="members.length === 0" class="state-card">{{ t('classroom.emptyMembers') }}</view>
          <view v-for="member in members" :key="member.id" class="member-card">
            <view>
              <text class="item-title">{{ member.nickname || member.email || `ID ${member.userId}` }}</text>
              <text class="muted">{{ member.email || t('common.empty') }}</text>
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
import { fetchClassMembers, fetchClassRoomDetail } from '../../api/classroom'
import { applyTabBarLocale, setPageTitle, useI18n } from '../../i18n'
import type { ClassMember, ClassRoomDetail } from '../../types/api'
import { openPage, requireLogin, routes } from '../../utils/navigation'

const { locale, t } = useI18n()

const classId = ref<number | null>(null)
const loading = ref(false)
const errorMessage = ref('')
const detail = ref<ClassRoomDetail | null>(null)
const members = ref<ClassMember[]>([])
const teacherInitial = computed(() => {
  const name = detail.value?.teacherName || detail.value?.teacherContact || ''
  return name.trim().slice(0, 1).toUpperCase() || 'T'
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
      members.value = await fetchClassMembers(nextDetail.id)
    } else {
      members.value = []
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
.member-card {
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

.teacher-card {
  align-items: center;
  background: #f8fafc;
  border: 1px solid #d7e2ea;
  border-radius: 12rpx;
  display: flex;
  gap: 18rpx;
  margin-top: 20rpx;
  padding: 18rpx;
}

.teacher-avatar {
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

.teacher-name {
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

.member-card {
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

</style>
