<template>
  <view class="page">
    <view class="hero">
      <view class="hero-top">
        <view>
          <text class="eyebrow">{{ t('classroom.eyebrow') }}</text>
          <text class="title">{{ t('classroom.title') }}</text>
          <text class="subtitle">{{ t('classroom.subtitle', { count: rooms.length, active: activeRoomLabel }) }}</text>
        </view>
        <view class="hero-actions">
          <LanguageSwitch variant="hero" />
          <button class="icon-btn" size="mini" :loading="loading" @click="refreshPage">{{ t('common.refresh') }}</button>
        </view>
      </view>
    </view>

    <view v-if="errorMessage" class="state-card error-card">
      <text>{{ errorMessage }}</text>
      <button class="plain-btn compact" size="mini" @click="refreshPage">{{ t('common.retry') }}</button>
    </view>

    <view class="join-card">
      <view>
        <text class="section-title">{{ t('classroom.join') }}</text>
        <text class="hint">{{ t('classroom.studentJoinHint') }}</text>
      </view>
      <view class="join-form">
        <input v-model="inviteCode" class="input" :placeholder="t('classroom.inviteCode')" />
        <button class="primary-btn" :loading="joining" @click="joinRoom">{{ t('classroom.join') }}</button>
      </view>
      <text v-if="joinError" class="form-error">{{ joinError }}</text>
    </view>

    <view class="section">
      <view class="section-head">
        <text class="section-title">{{ t('classroom.myClasses') }}</text>
        <text class="muted">{{ t('classroom.classCount', { count: rooms.length }) }}</text>
      </view>

      <view v-if="loading && rooms.length === 0" class="state-card">{{ t('common.loading') }}</view>

      <view v-else-if="rooms.length === 0" class="empty-card">
        <text class="empty-title">{{ t('classroom.empty') }}</text>
        <text class="muted">{{ t('classroom.emptyHint') }}</text>
      </view>

      <view v-else class="room-list">
        <view v-for="room in rooms" :key="room.id" class="room-card" @click="openRoom(room.id)">
          <view class="room-main">
            <view class="room-title-row">
              <text class="item-title">{{ room.name }}</text>
              <text class="status-pill" :class="`status-${room.memberStatus}`">{{ statusLabel(room.memberStatus) }}</text>
            </view>
            <text class="muted">{{ room.description || t('classroom.noDescription') }}</text>
            <view class="room-meta">
              <text>{{ roleLabel(room.memberRole) }}</text>
              <text>{{ isTeacher(room) ? room.inviteCode : t('classroom.inviteManagedByTeacher') }}</text>
            </view>
          </view>
          <view class="room-actions">
            <button v-if="isTeacher(room)" class="code-btn" size="mini" @click.stop="copyInviteCode(room.inviteCode)">
              {{ t('classroom.copyInvite') }}
            </button>
            <button
              class="plain-btn detail-btn"
              :class="{ full: !isTeacher(room) }"
              size="mini"
              @click.stop="openRoom(room.id)"
            >
              {{ t('classroom.openDetail') }}
            </button>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import LanguageSwitch from '../../components/LanguageSwitch.vue'
import { fetchClassRooms, joinClassRoom } from '../../api/classroom'
import { applyTabBarLocale, setPageTitle, useI18n } from '../../i18n'
import type { ClassRoom } from '../../types/api'
import { openClassroomDetail, requireLogin } from '../../utils/navigation'

const { locale, t } = useI18n()

const loading = ref(false)
const joining = ref(false)
const errorMessage = ref('')
const joinError = ref('')
const rooms = ref<ClassRoom[]>([])
const inviteCode = ref('')

const activeRoomLabel = computed(() => rooms.value[0]?.name || t('classroom.noSelected'))

onShow(() => {
  applyTabBarLocale()
  setPageTitle('classroom.eyebrow')
  if (!requireLogin()) {
    return
  }
  void loadRooms(rooms.value.length === 0)
})

watch(locale, () => {
  applyTabBarLocale()
  setPageTitle('classroom.eyebrow')
})

onPullDownRefresh(() => {
  void refreshPage().finally(() => uni.stopPullDownRefresh())
})

async function refreshPage() {
  await loadRooms(true)
}

async function loadRooms(showLoading = true) {
  if (loading.value) {
    return
  }
  loading.value = showLoading
  errorMessage.value = ''
  try {
    rooms.value = await fetchClassRooms()
  } catch {
    errorMessage.value = t('classroom.loadFailed')
  } finally {
    loading.value = false
  }
}

async function joinRoom() {
  const code = inviteCode.value.trim()
  joinError.value = ''
  if (!code) {
    void uni.showToast({ icon: 'none', title: t('classroom.needInvite') })
    return
  }
  joining.value = true
  try {
    const room = await joinClassRoom({ inviteCode: code })
    inviteCode.value = ''
    void uni.showToast({ icon: 'none', title: t('classroom.joined') })
    await loadRooms(false)
    openClassroomDetail(room.id)
  } catch {
    joinError.value = t('classroom.joinFailed')
    void uni.showToast({ icon: 'none', title: t('classroom.joinFailed') })
  } finally {
    joining.value = false
  }
}

function openRoom(id: number) {
  openClassroomDetail(id)
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

function isTeacher(room: Pick<ClassRoom, 'memberRole'>) {
  return room.memberRole === 'teacher'
}

function statusLabel(status: string) {
  const labels: Record<string, string> = {
    active: t('classroom.statusActive'),
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
.section-head,
.room-title-row {
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
  font-size: 46rpx;
  font-weight: 800;
  line-height: 1.15;
  margin-top: 10rpx;
}

.subtitle,
.muted,
.hint {
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
.primary-btn,
.plain-btn,
.code-btn {
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

.primary-btn {
  background: #14796f;
  color: #ffffff;
  min-height: 78rpx;
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

.join-card,
.state-card,
.empty-card,
.room-card {
  background: #ffffff;
  border: 1px solid #d7e2ea;
  border-radius: 8rpx;
  box-sizing: border-box;
}

.join-card {
  border-radius: 24rpx;
  box-shadow: 0 12rpx 36rpx rgba(15, 23, 42, 0.06);
  padding: 24rpx;
}

.join-form {
  display: grid;
  gap: 14rpx;
  grid-template-columns: minmax(0, 1fr) 180rpx;
  margin-top: 22rpx;
}

.input {
  border: 1px solid #cbd5e1;
  border-radius: 8rpx;
  box-sizing: border-box;
  height: 78rpx;
  padding: 0 20rpx;
}

.form-error {
  color: #b45309;
  display: block;
  font-size: 23rpx;
  line-height: 1.4;
  margin-top: 14rpx;
}

.section {
  margin-top: 24rpx;
}

.section-title {
  color: #102033;
  display: block;
  font-size: 34rpx;
  font-weight: 800;
}

.state-card,
.empty-card {
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

.empty-title {
  color: #102033;
  display: block;
  font-size: 30rpx;
  font-weight: 800;
}

.room-list {
  display: grid;
  gap: 16rpx;
  margin-top: 16rpx;
}

.room-card {
  box-shadow: 0 10rpx 28rpx rgba(15, 23, 42, 0.05);
  display: grid;
  gap: 18rpx;
  padding: 22rpx;
}

.item-title {
  color: #102033;
  display: block;
  flex: 1;
  font-size: 31rpx;
  font-weight: 800;
  line-height: 1.3;
  word-break: break-all;
}

.status-pill {
  border-radius: 999rpx;
  flex-shrink: 0;
  font-size: 22rpx;
  font-weight: 700;
  padding: 7rpx 14rpx;
}

.status-active {
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
.status-removed {
  background: #fee2e2;
  color: #991b1b;
}

.room-meta {
  color: #14796f;
  display: flex;
  flex-wrap: wrap;
  font-size: 23rpx;
  font-weight: 700;
  gap: 12rpx;
  margin-top: 14rpx;
}

.room-actions {
  display: grid;
  gap: 12rpx;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.code-btn {
  background: #e2e8f0;
  color: #102033;
}

.detail-btn {
  min-height: 58rpx;
}

.detail-btn.full {
  grid-column: 1 / -1;
}

@media (max-width: 360px) {
  .join-form,
  .room-actions {
    grid-template-columns: 1fr;
  }
}
</style>
