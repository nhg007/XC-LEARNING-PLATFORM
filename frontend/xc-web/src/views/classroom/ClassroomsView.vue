<template>
  <main class="app-shell">
    <header class="topbar classroom-hero">
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
      <div>
        <h2>{{ t('classroom.joinTitle') }}</h2>
        <p>{{ t('classroom.joinHint') }}</p>
      </div>
      <v-form class="inline-form" @submit.prevent="joinRoom">
        <v-text-field v-model="joinForm.inviteCode" density="comfortable" hide-details :label="t('classroom.inviteCode')" maxlength="64" variant="outlined" />
        <v-btn color="primary" type="submit" :loading="joining">{{ t('classroom.join') }}</v-btn>
      </v-form>
    </v-card>

    <section class="layout">
      <v-card class="room-list" elevation="0" rounded="lg">
        <div class="panel-heading">
          <h2>{{ t('classroom.myRooms') }}</h2>
          <span>{{ t('classroom.roomCount', { count: rooms.length }) }}</span>
        </div>
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
            <span>{{ room.description || t('classroom.noDescription') }}</span>
            <small>{{ t('classroom.teacherLine', { name: room.teacherName || t('classroom.noTeacher') }) }} · {{ statusLabel(room.memberStatus) }}</small>
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
            <span class="status-badge">{{ statusLabel(detail.memberStatus) }}</span>
          </div>

          <div class="teacher-card">
            <div class="teacher-avatar">{{ teacherInitial }}</div>
            <div>
              <span>{{ t('classroom.teacher') }}</span>
              <strong>{{ detail.teacherName || t('classroom.noTeacher') }}</strong>
              <p>{{ detail.teacherContact || t('classroom.noTeacherContact') }}</p>
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
            <section class="section-heading">
              <h2>{{ t('classroom.classmates') }}</h2>
              <span>{{ t('classroom.memberCount', { count: members.length }) }}</span>
            </section>

            <div class="member-grid">
              <div v-for="row in members" :key="row.userId" class="member-card">
                <div class="member-avatar">{{ memberInitial(row) }}</div>
                <div>
                  <strong>{{ row.nickname || row.email || t('common.userFallback', { id: row.userId }) }}</strong>
                  <span>{{ row.email || t('common.empty') }}</span>
                </div>
              </div>
            </div>
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
  fetchClassMembers,
  fetchClassRoomDetail,
  fetchClassRooms,
  joinClassRoom
} from '../../api/classroom'
import type { ClassMember, ClassRoom, ClassRoomDetail } from '../../types/api'
import { notifySuccess, notifyWarning } from '../../utils/notify'

const { t } = useI18n()

const loading = ref(false)
const roomLoading = ref(false)
const joining = ref(false)
const rooms = ref<ClassRoom[]>([])
const activeId = ref<number | null>(null)
const detail = ref<ClassRoomDetail | null>(null)
const members = ref<ClassMember[]>([])
const joinForm = reactive({ inviteCode: '' })
const teacherInitial = computed(() => {
  const name = detail.value?.teacherName || detail.value?.teacherContact || ''
  return name.trim().slice(0, 1).toUpperCase() || 'T'
})

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
      members.value = await fetchClassMembers(id)
    } else {
      members.value = []
    }
  } finally {
    roomLoading.value = false
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

function statusLabel(status: string) {
  return t(`classroom.statuses.${status}`)
}

function memberInitial(member: ClassMember) {
  const name = member.nickname || member.email || String(member.userId)
  return name.trim().slice(0, 1).toUpperCase()
}

onMounted(loadRooms)
</script>

<style scoped>
h1,
h2 {
  margin: 0;
}

h1 {
  font-size: 34px;
  line-height: 1.2;
}

h2 {
  font-size: 22px;
}

p {
  color: #64748b;
  margin: 8px 0 0;
}

.classroom-hero {
  background: #142033;
  border: 1px solid #23324a;
  border-radius: 8px;
  color: #f8fafc;
  margin-bottom: 22px;
  padding: 30px;
}

.classroom-hero p {
  color: #cbd5e1;
}

.top-actions,
.inline-form {
  display: flex;
  gap: 10px;
}

.top-actions {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.top-actions :deep(.v-btn) {
  border-radius: 4px;
  letter-spacing: 0;
}

.top-actions :deep(.v-btn--variant-text) {
  color: #f8fafc;
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
  grid-template-columns: 1fr;
}

.room-list {
  padding: 16px;
}

.panel-heading {
  align-items: center;
  display: flex;
  justify-content: space-between;
  margin-bottom: 14px;
}

.panel-heading span {
  color: #64748b;
  font-weight: 700;
}

.room-item {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  display: grid;
  gap: 6px;
  min-height: 76px;
  margin-top: 10px;
  width: 100%;
  padding: 14px;
  text-align: left;
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease;
}

.room-item.active {
  border-color: #2563eb;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.12);
}

.room-item span,
.room-item small,
.muted {
  color: #64748b;
  font-size: 13px;
}

.room-item small {
  display: block;
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

.status-badge {
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  border-radius: 999px;
  color: #334155;
  flex-shrink: 0;
  font-size: 13px;
  font-weight: 800;
  padding: 7px 12px;
}

.teacher-card {
  align-items: center;
  background: #f8fafc;
  border: 1px solid #e5edf7;
  border-radius: 8px;
  display: flex;
  gap: 14px;
  margin-top: 16px;
  padding: 14px;
}

.teacher-avatar {
  align-items: center;
  background: #2563eb;
  border-radius: 8px;
  color: #ffffff;
  display: flex;
  flex-shrink: 0;
  font-size: 22px;
  font-weight: 900;
  height: 48px;
  justify-content: center;
  width: 48px;
}

.teacher-card span {
  color: #64748b;
  display: block;
  font-size: 13px;
}

.teacher-card strong {
  color: #102033;
  display: block;
  font-size: 18px;
  margin-top: 2px;
}

.teacher-card p {
  margin-top: 2px;
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

.section-heading {
  align-items: center;
  display: flex;
  justify-content: space-between;
  margin: 20px 0 12px;
}

.section-heading span {
  color: #64748b;
  font-weight: 700;
}

.member-grid {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
}

.member-card {
  align-items: center;
  background: #ffffff;
  border: 1px solid #e5edf7;
  border-radius: 8px;
  display: flex;
  gap: 12px;
  min-height: 72px;
  padding: 12px;
}

.member-avatar {
  align-items: center;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  color: #334155;
  display: flex;
  flex-shrink: 0;
  font-weight: 900;
  height: 42px;
  justify-content: center;
  width: 42px;
}

.member-card strong,
.member-card span {
  display: block;
}

.member-card span {
  color: #64748b;
  font-size: 13px;
  margin-top: 3px;
}

@media (max-width: 820px) {
  .classroom-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .top-actions {
    justify-content: flex-start;
  }

  .layout {
    grid-template-columns: 1fr;
  }

  .inline-form,
  .detail-header {
    flex-direction: column;
  }

}

@media (max-width: 560px) {
  h1 {
    font-size: 28px;
  }

  .classroom-hero {
    padding: 20px 16px;
  }

  .member-grid {
    grid-template-columns: 1fr;
  }
}
</style>
