<template>
  <view class="page">
    <view class="header">
      <text class="title">班级</text>
      <text class="subtitle">创建、加入和查看班级学习统计</text>
    </view>

    <view class="panel">
      <input v-model="createName" class="input" placeholder="班级名称" />
      <input v-model="createDescription" class="input" placeholder="班级说明" />
      <button class="primary-btn" :loading="creating" @click="createRoom">创建班级</button>
    </view>

    <view class="panel">
      <input v-model="inviteCode" class="input" placeholder="邀请码" />
      <button class="plain-btn" :loading="joining" @click="joinRoom">加入班级</button>
    </view>

    <view class="section">
      <text class="section-title">我的班级</text>
      <view v-if="rooms.length === 0" class="empty">暂无班级</view>
      <view
        v-for="room in rooms"
        :key="room.id"
        class="list-item"
        :class="{ active: room.id === activeId }"
        @click="selectRoom(room.id)"
      >
        <view>
          <text class="item-title">{{ room.name }}</text>
          <text class="muted">{{ roleLabel(room.memberRole) }} · {{ statusLabel(room.memberStatus) }}</text>
        </view>
        <text class="muted">{{ room.inviteCode }}</text>
      </view>
    </view>

    <view v-if="detail" class="section">
      <view class="detail-card">
        <text class="item-title">{{ detail.name }}</text>
        <text class="muted">{{ detail.description || '暂无说明' }}</text>
        <view class="metrics">
          <view>
            <text class="label">正式成员</text>
            <text class="value">{{ detail.activeMemberCount }}</text>
          </view>
          <view>
            <text class="label">待审核</text>
            <text class="value">{{ detail.pendingMemberCount }}</text>
          </view>
        </view>
      </view>

      <view v-if="detail.memberStatus !== 'active'" class="notice">你的成员状态还不是正式成员，暂不能查看成员和统计。</view>

      <template v-else>
        <view v-if="isTeacher" class="panel">
          <input v-model="addEmail" class="input" placeholder="成员邮箱" />
          <input v-model="addUserId" class="input" type="number" placeholder="或用户 ID" />
          <button class="primary-btn" :loading="adding" @click="addMember">添加成员</button>
        </view>

        <text class="section-title">成员</text>
        <view v-for="member in members" :key="member.id" class="member-card">
          <view>
            <text class="item-title">{{ member.nickname || member.email || `用户 ${member.userId}` }}</text>
            <text class="muted">{{ roleLabel(member.memberRole) }} · {{ statusLabel(member.status) }}</text>
          </view>
          <view v-if="isTeacher" class="actions">
            <button v-if="member.status === 'pending_teacher_review'" size="mini" @click="review(member.userId, true)">通过</button>
            <button v-if="member.status === 'pending_teacher_review'" size="mini" @click="review(member.userId, false)">拒绝</button>
            <button v-if="member.memberRole !== 'teacher'" size="mini" @click="remove(member.userId)">移除</button>
          </view>
        </view>

        <text class="section-title">学习统计</text>
        <view v-for="item in stats" :key="item.userId" class="stat-card">
          <text class="item-title">{{ item.nickname || item.email || `用户 ${item.userId}` }}</text>
          <text class="muted">学习 {{ formatDuration(item.studySeconds) }} · 做题 {{ item.exerciseCount }} · 正确率 {{ item.accuracyRate }}%</text>
          <text class="muted">背词 {{ item.vocabReviewCount }} · 最近 {{ item.lastStudyAt ? formatTime(item.lastStudyAt) : '-' }}</text>
        </view>
      </template>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import {
  addClassMember,
  createClassRoom,
  fetchClassMembers,
  fetchClassRoomDetail,
  fetchClassRooms,
  fetchClassStats,
  joinClassRoom,
  removeClassMember,
  reviewClassMember
} from '../../api/classroom'
import type { ClassMember, ClassMemberStats, ClassRoom, ClassRoomDetail } from '../../types/api'

const creating = ref(false)
const joining = ref(false)
const adding = ref(false)
const rooms = ref<ClassRoom[]>([])
const activeId = ref<number | null>(null)
const detail = ref<ClassRoomDetail | null>(null)
const members = ref<ClassMember[]>([])
const stats = ref<ClassMemberStats[]>([])
const createName = ref('')
const createDescription = ref('')
const inviteCode = ref('')
const addEmail = ref('')
const addUserId = ref('')

const isTeacher = computed(() => detail.value?.memberRole === 'teacher' && detail.value.memberStatus === 'active')

onShow(() => {
  void loadRooms()
})

async function loadRooms() {
  rooms.value = await fetchClassRooms()
  if (!activeId.value && rooms.value.length > 0) {
    await selectRoom(rooms.value[0].id)
  } else if (activeId.value) {
    await selectRoom(activeId.value)
  }
}

async function selectRoom(id: number) {
  activeId.value = id
  detail.value = await fetchClassRoomDetail(id)
  if (detail.value.memberStatus === 'active') {
    const [memberRows, statRows] = await Promise.all([fetchClassMembers(id), fetchClassStats(id)])
    members.value = memberRows
    stats.value = statRows
  } else {
    members.value = []
    stats.value = []
  }
}

async function createRoom() {
  if (!createName.value.trim()) {
    void uni.showToast({ icon: 'none', title: '请输入班级名称' })
    return
  }
  creating.value = true
  try {
    const room = await createClassRoom({
      name: createName.value.trim(),
      description: createDescription.value.trim() || undefined
    })
    createName.value = ''
    createDescription.value = ''
    void uni.showToast({ icon: 'none', title: '班级已创建' })
    await loadRooms()
    await selectRoom(room.id)
  } finally {
    creating.value = false
  }
}

async function joinRoom() {
  if (!inviteCode.value.trim()) {
    void uni.showToast({ icon: 'none', title: '请输入邀请码' })
    return
  }
  joining.value = true
  try {
    const room = await joinClassRoom({ inviteCode: inviteCode.value.trim() })
    inviteCode.value = ''
    void uni.showToast({ icon: 'none', title: '已加入班级' })
    await loadRooms()
    await selectRoom(room.id)
  } finally {
    joining.value = false
  }
}

async function addMember() {
  if (!activeId.value) {
    return
  }
  const userId = addUserId.value ? Number(addUserId.value) : undefined
  if (!addEmail.value.trim() && !userId) {
    void uni.showToast({ icon: 'none', title: '请输入邮箱或用户 ID' })
    return
  }
  adding.value = true
  try {
    await addClassMember(activeId.value, {
      email: addEmail.value.trim() || undefined,
      userId
    })
    addEmail.value = ''
    addUserId.value = ''
    void uni.showToast({ icon: 'none', title: '成员已处理' })
    await selectRoom(activeId.value)
  } finally {
    adding.value = false
  }
}

async function review(userId: number, approved: boolean) {
  if (!activeId.value) {
    return
  }
  await reviewClassMember(activeId.value, userId, approved)
  void uni.showToast({ icon: 'none', title: approved ? '已通过' : '已拒绝' })
  await selectRoom(activeId.value)
}

function remove(userId: number) {
  if (!activeId.value) {
    return
  }
  void uni.showModal({
    title: '移除成员',
    content: '确认移除该成员？',
    success: async (result) => {
      if (!result.confirm || !activeId.value) {
        return
      }
      await removeClassMember(activeId.value, userId)
      void uni.showToast({ icon: 'none', title: '已移除' })
      await selectRoom(activeId.value)
    }
  })
}

function roleLabel(role: string) {
  return role === 'teacher' ? '老师' : '成员'
}

function statusLabel(status: string) {
  const labels: Record<string, string> = {
    active: '正式',
    pending_teacher_review: '待审核',
    invited: '已邀请',
    rejected: '已拒绝',
    left: '已退出',
    removed: '已移除'
  }
  return labels[status] || status
}

function formatDuration(seconds: number) {
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  return hours > 0 ? `${hours}小时${minutes}分` : `${minutes}分`
}

function formatTime(value: string) {
  return new Date(value).toLocaleDateString()
}
</script>

<style scoped>
.page {
  padding: 24rpx;
}

.header {
  margin-bottom: 24rpx;
}

.title {
  display: block;
  font-size: 42rpx;
  font-weight: 700;
}

.subtitle,
.muted,
.notice {
  color: #64748b;
  display: block;
  font-size: 24rpx;
  margin-top: 8rpx;
}

.panel,
.detail-card,
.list-item,
.member-card,
.stat-card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8rpx;
  box-sizing: border-box;
}

.panel {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  margin-bottom: 18rpx;
  padding: 20rpx;
}

.input {
  border: 1px solid #d1d5db;
  border-radius: 8rpx;
  box-sizing: border-box;
  height: 78rpx;
  padding: 0 20rpx;
}

.primary-btn {
  background: #2563eb;
  color: #ffffff;
}

.section {
  margin-top: 26rpx;
}

.section-title {
  display: block;
  font-size: 32rpx;
  font-weight: 700;
  margin-bottom: 14rpx;
}

.list-item,
.member-card,
.stat-card {
  display: flex;
  justify-content: space-between;
  margin-bottom: 14rpx;
  padding: 20rpx;
}

.list-item.active {
  border-color: #2563eb;
}

.item-title {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
}

.detail-card {
  padding: 22rpx;
}

.metrics {
  display: flex;
  gap: 16rpx;
  margin-top: 18rpx;
}

.metrics > view {
  flex: 1;
}

.label {
  color: #64748b;
  display: block;
  font-size: 24rpx;
}

.value {
  display: block;
  font-size: 34rpx;
  font-weight: 700;
  margin-top: 6rpx;
}

.notice {
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 8rpx;
  color: #92400e;
  padding: 18rpx;
}

.actions {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.empty {
  color: #64748b;
  font-size: 26rpx;
}
</style>
