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
        <text class="section-title">{{ t('classroom.create') }}</text>
        <text class="hint">{{ t('classroom.createHint') }}</text>
      </view>
      <view class="create-form">
        <input v-model="createName" class="input" :placeholder="t('classroom.createName')" />
        <input v-model="createDescription" class="input" :placeholder="t('classroom.createDescription')" />
        <button class="primary-btn" :loading="creating" @click="createRoom">{{ t('classroom.create') }}</button>
      </view>
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
              <text class="role-pill" :class="{ owner: room.createdByMe }">{{ room.createdByMe ? t('classroom.createdByMe') : t('classroom.joinedByMe') }}</text>
            </view>
            <text class="muted">{{ room.description || t('classroom.noDescription') }}</text>
            <view class="room-meta">
              <text>{{ t('classroom.ownerLine', { name: room.ownerName || t('classroom.noOwner') }) }}</text>
              <text class="status-pill" :class="`status-${room.memberStatus}`">{{ statusLabel(room.memberStatus) }}</text>
            </view>
          </view>
          <view class="chevron" />
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import LanguageSwitch from '../../components/LanguageSwitch.vue'
import { createClassRoom, fetchClassRooms, joinClassRoom } from '../../api/classroom'
import { applyTabBarLocale, setPageTitle, useI18n } from '../../i18n'
import type { ClassRoom } from '../../types/api'
import { openClassroomDetail, requireLogin } from '../../utils/navigation'

const { locale, t } = useI18n()

const loading = ref(false)
const creating = ref(false)
const joining = ref(false)
const errorMessage = ref('')
const joinError = ref('')
const rooms = ref<ClassRoom[]>([])
const createName = ref('')
const createDescription = ref('')
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

async function createRoom() {
  const name = createName.value.trim()
  if (!name) {
    void uni.showToast({ icon: 'none', title: t('classroom.needName') })
    return
  }
  creating.value = true
  try {
    const room = await createClassRoom({
      name,
      description: createDescription.value.trim() || undefined
    })
    createName.value = ''
    createDescription.value = ''
    void uni.showToast({ icon: 'none', title: t('classroom.created') })
    await loadRooms(false)
    upsertRoom(room)
    openClassroomDetail(room.id)
  } finally {
    creating.value = false
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
    upsertRoom(room)
    openClassroomDetail(room.id)
  } catch {
    joinError.value = t('classroom.joinFailed')
    void uni.showToast({ icon: 'none', title: t('classroom.joinFailed') })
  } finally {
    joining.value = false
  }
}

function upsertRoom(room: ClassRoom) {
  const index = rooms.value.findIndex(item => item.id === room.id)
  if (index >= 0) {
    rooms.value = rooms.value.map(item => (item.id === room.id ? room : item))
    return
  }
  rooms.value = [room, ...rooms.value]
}

function openRoom(id: number) {
  openClassroomDetail(id)
}

function statusLabel(status: string) {
  const labels: Record<string, string> = {
    active: t('classroom.statusActive'),
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
.room-title-row,
.room-meta {
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
  margin-top: 18rpx;
  padding: 24rpx;
}

.create-form,
.join-form {
  display: grid;
  gap: 14rpx;
  margin-top: 22rpx;
}

.join-form {
  grid-template-columns: minmax(0, 1fr) 180rpx;
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
  align-items: center;
  box-shadow: 0 10rpx 28rpx rgba(15, 23, 42, 0.05);
  display: flex;
  gap: 18rpx;
  padding: 22rpx;
}

.room-main {
  flex: 1;
  min-width: 0;
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

.role-pill {
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  border-radius: 8rpx;
  color: #475569;
  flex-shrink: 0;
  font-size: 21rpx;
  font-weight: 800;
  line-height: 1.2;
  padding: 7rpx 12rpx;
}

.role-pill.owner {
  background: #ecfdf5;
  border-color: #bbf7d0;
  color: #047857;
}

.status-pill {
  border-radius: 999rpx;
  flex-shrink: 0;
  font-size: 21rpx;
  font-weight: 800;
  padding: 6rpx 12rpx;
}

.status-active {
  background: #dcfce7;
  color: #166534;
}

.status-left,
.status-removed {
  background: #fee2e2;
  color: #991b1b;
}

.room-meta {
  color: #14796f;
  flex-wrap: wrap;
  font-size: 23rpx;
  font-weight: 700;
  gap: 12rpx;
  justify-content: flex-start;
  margin-top: 14rpx;
}

.code-btn {
  background: #e2e8f0;
  color: #102033;
}

.chevron {
  border-right: 4rpx solid #64748b;
  border-top: 4rpx solid #64748b;
  flex-shrink: 0;
  height: 18rpx;
  transform: rotate(45deg);
  width: 18rpx;
}

@media (max-width: 360px) {
  .join-form {
    grid-template-columns: 1fr;
  }

  .room-card {
    align-items: flex-start;
  }
}
</style>
