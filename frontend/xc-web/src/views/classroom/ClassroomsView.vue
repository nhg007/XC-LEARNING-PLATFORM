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
      <h2>{{ t('classroom.joinTitle') }}</h2>
      <v-form class="inline-form" @submit.prevent="joinRoom">
        <v-text-field v-model="joinForm.inviteCode" density="comfortable" hide-details :label="t('classroom.inviteCode')" maxlength="64" variant="outlined" />
        <v-btn color="primary" type="submit" :loading="joining">{{ t('classroom.join') }}</v-btn>
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
            <section class="section-heading">
              <h2>{{ t('classroom.members') }}</h2>
              <span>{{ members.length }}</span>
            </section>

            <div class="table-scroll">
              <v-table class="classroom-table" density="comfortable">
                <thead>
                  <tr>
                    <th>{{ t('classroom.table.member') }}</th>
                    <th>{{ t('classroom.table.role') }}</th>
                    <th>{{ t('classroom.table.status') }}</th>
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
                  </tr>
                </tbody>
              </v-table>
            </div>
          </template>
        </template>
      </v-card>
    </section>
  </main>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
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

function roleLabel(role: string) {
  return role === 'teacher' ? t('classroom.roles.teacher') : t('classroom.roles.member')
}

function statusLabel(status: string) {
  return t(`classroom.statuses.${status}`)
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
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(auto-fill, minmax(220px, 260px));
  justify-content: start;
  padding: 12px;
}

.room-item {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  display: grid;
  gap: 6px;
  min-height: 76px;
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

.metrics span {
  color: #64748b;
  display: block;
  font-size: 13px;
}

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

.table-scroll {
  border-radius: 8px;
  max-height: 360px;
  overflow: auto;
}

.classroom-table {
  min-width: 640px;
}

.classroom-table :deep(th) {
  background: linear-gradient(180deg, var(--xc-table-header-bg) 0%, var(--xc-table-header-bg-strong) 100%);
  color: var(--xc-table-header-text);
  font-weight: 800;
  position: sticky;
  top: 0;
  z-index: 1;
}

.classroom-table :deep(td) {
  vertical-align: middle;
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

  .table-scroll {
    max-height: 320px;
  }
}
</style>
