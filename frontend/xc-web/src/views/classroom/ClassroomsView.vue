<template>
  <main class="app-shell">
    <header class="topbar">
      <div>
        <h1>{{ t('classroom.title') }}</h1>
        <p>{{ t('classroom.subtitle') }}</p>
      </div>
      <div class="top-actions">
        <LocaleSwitch />
        <v-btn prepend-icon="mdi-arrow-left" variant="text" @click="$router.push('/')">{{ t('common.back') }}</v-btn>
        <v-btn prepend-icon="mdi-refresh" variant="tonal" :loading="loading" @click="loadRooms">{{ t('common.refresh') }}</v-btn>
      </div>
    </header>

    <v-card class="actions-panel" elevation="0" rounded="lg">
      <v-form class="inline-form" @submit.prevent="createRoom">
        <v-text-field v-model="createForm.name" density="comfortable" hide-details :label="t('classroom.createName')" maxlength="100" variant="outlined" />
        <v-text-field v-model="createForm.description" density="comfortable" hide-details :label="t('classroom.createDescription')" maxlength="1000" variant="outlined" />
        <v-btn color="primary" type="submit" :loading="creating">{{ t('classroom.create') }}</v-btn>
      </v-form>
      <v-form class="inline-form" @submit.prevent="joinRoom">
        <v-text-field v-model="joinForm.inviteCode" density="comfortable" hide-details :label="t('classroom.inviteCode')" maxlength="64" variant="outlined" />
        <v-btn type="submit" variant="tonal" :loading="joining">{{ t('classroom.join') }}</v-btn>
      </v-form>
    </v-card>

    <section class="layout">
      <v-card class="room-list" elevation="0" rounded="lg">
        <div v-if="rooms.length === 0" class="empty-state">{{ t('classroom.emptyRooms') }}</div>
        <template v-else>
          <button
            v-for="room in rooms"
            :key="room.id"
            class="room-item"
            :class="{ active: room.id === activeId }"
            @click="selectRoom(room.id)"
          >
            <strong>{{ room.name }}</strong>
            <span>{{ roleLabel(room.memberRole) }} · {{ statusLabel(room.memberStatus) }}</span>
          </button>
        </template>
      </v-card>

      <v-card class="room-detail" elevation="0" rounded="lg">
        <v-progress-linear v-if="roomLoading" class="panel-loader" color="primary" indeterminate />
        <div v-if="!detail" class="empty-state detail-empty">{{ t('classroom.chooseRoom') }}</div>
        <template v-else>
          <div class="detail-header">
            <div>
              <h2>{{ detail.name }}</h2>
              <p>{{ detail.description || t('classroom.noDescription') }}</p>
            </div>
            <div class="code-box">
              <span>{{ t('classroom.inviteCode') }}</span>
              <strong>{{ detail.inviteCode }}</strong>
            </div>
          </div>

          <div class="metrics">
            <div>
              <span>{{ t('classroom.activeMembers') }}</span>
              <strong>{{ detail.activeMemberCount }}</strong>
            </div>
            <div>
              <span>{{ t('classroom.pendingMembers') }}</span>
              <strong>{{ detail.pendingMemberCount }}</strong>
            </div>
            <div>
              <span>{{ t('classroom.myRole') }}</span>
              <strong>{{ roleLabel(detail.memberRole) }}</strong>
            </div>
          </div>

          <v-alert
            v-if="detail.memberStatus !== 'active'"
            color="warning"
            icon="mdi-alert-outline"
            :text="t('classroom.inactiveAlert')"
            variant="tonal"
          />

          <template v-else>
            <div v-if="isTeacher" class="teacher-tools">
              <v-text-field v-model="addForm.email" density="comfortable" hide-details :label="t('classroom.addByEmail')" maxlength="255" variant="outlined" />
              <v-text-field v-model.number="addForm.userId" density="comfortable" hide-details :label="t('classroom.addByUserId')" min="1" type="number" variant="outlined" />
              <v-btn color="primary" :loading="adding" @click="addMember">{{ t('classroom.addMember') }}</v-btn>
            </div>

            <div class="table-title">{{ t('classroom.members') }}</div>
            <v-table density="comfortable">
              <thead>
                <tr>
                  <th>{{ t('classroom.table.member') }}</th>
                  <th>{{ t('classroom.table.role') }}</th>
                  <th>{{ t('classroom.table.status') }}</th>
                  <th v-if="isTeacher">{{ t('classroom.table.actions') }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in members" :key="row.userId">
                  <td>
                    <strong>{{ row.nickname || row.email || t('common.userFallback', { id: row.userId }) }}</strong>
                    <div class="muted">{{ row.email }}</div>
                  </td>
                  <td>{{ roleLabel(row.memberRole) }}</td>
                  <td>{{ statusLabel(row.status) }}</td>
                  <td v-if="isTeacher">
                    <div class="row-actions">
                      <v-btn v-if="row.status === 'pending_teacher_review'" size="small" variant="tonal" @click="review(row.userId, true)">{{ t('classroom.actions.approve') }}</v-btn>
                      <v-btn v-if="row.status === 'pending_teacher_review'" size="small" variant="tonal" @click="review(row.userId, false)">{{ t('classroom.actions.reject') }}</v-btn>
                      <v-btn v-if="row.memberRole !== 'teacher'" color="error" size="small" variant="text" @click="remove(row.userId)">{{ t('classroom.actions.remove') }}</v-btn>
                    </div>
                  </td>
                </tr>
              </tbody>
            </v-table>

            <div class="table-title">{{ t('classroom.stats') }}</div>
            <v-table density="comfortable">
              <thead>
                <tr>
                  <th>{{ t('classroom.table.member') }}</th>
                  <th>{{ t('classroom.table.studyTime') }}</th>
                  <th>{{ t('classroom.table.exercises') }}</th>
                  <th>{{ t('classroom.table.correct') }}</th>
                  <th>{{ t('classroom.table.accuracy') }}</th>
                  <th>{{ t('classroom.table.vocab') }}</th>
                  <th>{{ t('classroom.table.lastStudy') }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in stats" :key="row.userId">
                  <td>{{ row.nickname || row.email || t('common.userFallback', { id: row.userId }) }}</td>
                  <td>{{ formatDuration(row.studySeconds) }}</td>
                  <td>{{ row.exerciseCount }}</td>
                  <td>{{ row.correctCount }}</td>
                  <td>{{ row.accuracyRate }}%</td>
                  <td>{{ row.vocabReviewCount }}</td>
                  <td>{{ row.lastStudyAt ? new Date(row.lastStudyAt).toLocaleString() : '-' }}</td>
                </tr>
              </tbody>
            </v-table>
          </template>
        </template>
      </v-card>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import LocaleSwitch from '@/components/LocaleSwitch.vue'
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
import { confirmAction, notifySuccess, notifyWarning } from '../../utils/notify'

const { t } = useI18n()
const loading = ref(false)
const roomLoading = ref(false)
const creating = ref(false)
const joining = ref(false)
const adding = ref(false)
const rooms = ref<ClassRoom[]>([])
const activeId = ref<number | null>(null)
const detail = ref<ClassRoomDetail | null>(null)
const members = ref<ClassMember[]>([])
const stats = ref<ClassMemberStats[]>([])
const createForm = reactive({ name: '', description: '' })
const joinForm = reactive({ inviteCode: '' })
const addForm = reactive<{ email: string; userId?: number }>({ email: '', userId: undefined })

const isTeacher = computed(() => detail.value?.memberRole === 'teacher' && detail.value.memberStatus === 'active')

async function loadRooms() {
  loading.value = true
  try {
    rooms.value = await fetchClassRooms()
    if (!activeId.value && rooms.value.length > 0) {
      await selectRoom(rooms.value[0].id)
    } else if (activeId.value) {
      await selectRoom(activeId.value)
    }
  } finally {
    loading.value = false
  }
}

async function selectRoom(id: number) {
  activeId.value = id
  roomLoading.value = true
  try {
    detail.value = await fetchClassRoomDetail(id)
    if (detail.value.memberStatus === 'active') {
      const [memberRows, statRows] = await Promise.all([fetchClassMembers(id), fetchClassStats(id)])
      members.value = memberRows
      stats.value = statRows
    } else {
      members.value = []
      stats.value = []
    }
  } finally {
    roomLoading.value = false
  }
}

async function createRoom() {
  if (!createForm.name.trim()) {
    notifyWarning(t('classroom.notifications.nameRequired'))
    return
  }
  creating.value = true
  try {
    const room = await createClassRoom({ name: createForm.name.trim(), description: createForm.description.trim() || undefined })
    createForm.name = ''
    createForm.description = ''
    notifySuccess(t('classroom.notifications.created'))
    await loadRooms()
    await selectRoom(room.id)
  } finally {
    creating.value = false
  }
}

async function joinRoom() {
  if (!joinForm.inviteCode.trim()) {
    notifyWarning(t('classroom.notifications.inviteRequired'))
    return
  }
  joining.value = true
  try {
    const room = await joinClassRoom({ inviteCode: joinForm.inviteCode.trim() })
    joinForm.inviteCode = ''
    notifySuccess(t('classroom.notifications.joined'))
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
  if (!addForm.email.trim() && !addForm.userId) {
    notifyWarning(t('classroom.notifications.memberRequired'))
    return
  }
  adding.value = true
  try {
    await addClassMember(activeId.value, {
      email: addForm.email.trim() || undefined,
      userId: addForm.userId
    })
    addForm.email = ''
    addForm.userId = undefined
    notifySuccess(t('classroom.notifications.memberHandled'))
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
  notifySuccess(approved ? t('classroom.notifications.approved') : t('classroom.notifications.rejected'))
  await selectRoom(activeId.value)
}

async function remove(userId: number) {
  if (!activeId.value) {
    return
  }
  const confirmed = await confirmAction(t('classroom.confirmRemoveMessage'), t('classroom.confirmRemoveTitle'))
  if (!confirmed) {
    return
  }
  await removeClassMember(activeId.value, userId)
  notifySuccess(t('classroom.notifications.removed'))
  await selectRoom(activeId.value)
}

function roleLabel(role: string) {
  return role === 'teacher' ? t('classroom.roles.teacher') : t('classroom.roles.member')
}

function statusLabel(status: string) {
  return t(`classroom.statuses.${status}`)
}

function formatDuration(seconds: number) {
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  return hours > 0 ? t('units.durationHoursMinutes', { hours, minutes }) : t('units.durationMinutes', { minutes })
}

onMounted(loadRooms)
</script>

<style scoped>
h1,
h2 {
  margin: 0;
}

h1 {
  font-size: 28px;
}

h2 {
  font-size: 22px;
}

p {
  color: #64748b;
  margin: 8px 0 0;
}

.top-actions,
.inline-form,
.teacher-tools,
.row-actions {
  display: flex;
  gap: 10px;
}

.actions-panel,
.room-detail,
.room-list {
  border: 1px solid #e5e7eb;
}

.actions-panel {
  display: grid;
  gap: 12px;
  margin-bottom: 18px;
  padding: 16px;
}

.layout {
  display: grid;
  gap: 18px;
  grid-template-columns: 280px minmax(0, 1fr);
}

.room-list {
  align-self: start;
  display: grid;
  gap: 8px;
  padding: 12px;
}

.room-item {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  display: grid;
  gap: 6px;
  padding: 12px;
  text-align: left;
}

.room-item.active {
  border-color: #2563eb;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.12);
}

.room-item span,
.muted {
  color: #64748b;
  font-size: 13px;
}

.room-detail {
  min-height: 520px;
  padding: 18px;
}

.panel-loader {
  margin-bottom: 12px;
}

.detail-header {
  align-items: start;
  display: flex;
  gap: 16px;
  justify-content: space-between;
}

.code-box {
  border: 1px solid #dbeafe;
  border-radius: 8px;
  min-width: 160px;
  padding: 12px;
}

.code-box span,
.metrics span {
  color: #64748b;
  display: block;
  font-size: 13px;
}

.code-box strong,
.metrics strong {
  display: block;
  font-size: 20px;
  margin-top: 6px;
}

.metrics {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  margin: 18px 0;
}

.metrics > div {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px;
}

.teacher-tools {
  margin: 18px 0;
}

.table-title {
  font-size: 17px;
  font-weight: 700;
  margin: 20px 0 10px;
}

.empty-state {
  align-items: center;
  color: #64748b;
  display: flex;
  justify-content: center;
  min-height: 120px;
  text-align: center;
}

.detail-empty {
  min-height: 460px;
}

@media (max-width: 820px) {
  .layout {
    grid-template-columns: 1fr;
  }

  .inline-form,
  .teacher-tools,
  .detail-header {
    flex-direction: column;
  }
}
</style>
