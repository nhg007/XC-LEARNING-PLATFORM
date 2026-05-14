<template>
  <main class="app-shell classroom-page">
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
      <div class="action-block">
        <div class="action-copy">
          <h2>{{ t('classroom.createTitle') }}</h2>
          <p>{{ t('classroom.createHint') }}</p>
        </div>
        <v-form class="create-form" @submit.prevent="createRoom">
          <v-text-field v-model="createForm.name" density="comfortable" hide-details :label="t('classroom.className')" maxlength="100" variant="outlined" />
          <v-text-field v-model="createForm.description" density="comfortable" hide-details :label="t('classroom.classDescription')" maxlength="1000" variant="outlined" />
          <v-btn color="primary" prepend-icon="mdi-plus" type="submit" :loading="creating">{{ t('classroom.create') }}</v-btn>
        </v-form>
      </div>
      <div class="action-block">
        <div class="action-copy">
          <h2>{{ t('classroom.joinTitle') }}</h2>
          <p>{{ t('classroom.joinHint') }}</p>
        </div>
        <v-form class="join-form" @submit.prevent="joinRoom">
          <v-text-field v-model="joinForm.inviteCode" density="comfortable" hide-details :label="t('classroom.inviteCode')" maxlength="64" variant="outlined" />
          <v-btn color="primary" prepend-icon="mdi-login" type="submit" :loading="joining">{{ t('classroom.join') }}</v-btn>
        </v-form>
      </div>
    </v-card>

    <section class="classroom-workspace">
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
            <span class="room-item-head">
              <strong>{{ room.name }}</strong>
              <span class="room-role" :class="{ owner: room.createdByMe }">{{ room.createdByMe ? t('classroom.createdByMe') : t('classroom.joinedByMe') }}</span>
            </span>
            <span class="room-desc">{{ room.description || t('classroom.noDescription') }}</span>
            <small>{{ t('classroom.ownerLine', { name: room.ownerName || t('classroom.noOwner') }) }} · {{ statusLabel(room.memberStatus) }}</small>
          </button>
        </template>
      </v-card>

      <v-card class="room-detail" elevation="0" rounded="lg">
        <v-progress-linear v-if="roomLoading" class="panel-loader" color="primary" indeterminate />
        <div v-if="!detail" class="empty-state detail-empty">{{ t('classroom.chooseRoom') }}</div>
        <template v-else>
          <div class="detail-header">
            <div>
              <div class="detail-title-row">
                <h2>{{ detail.name }}</h2>
                <span class="status-badge">{{ statusLabel(detail.memberStatus) }}</span>
              </div>
              <p>{{ detail.description || t('classroom.noDescription') }}</p>
            </div>
          </div>

          <div class="detail-meta-grid">
            <div class="owner-card">
              <div class="owner-avatar">{{ ownerInitial }}</div>
              <div>
                <span>{{ t('classroom.owner') }}</span>
                <strong>{{ detail.ownerName || t('classroom.noOwner') }}</strong>
                <p v-if="detail.ownerContact">{{ detail.ownerContact }}</p>
              </div>
            </div>
            <div class="invite-card">
              <div>
                <span>{{ t('classroom.inviteCode') }}</span>
                <strong>{{ detail.inviteCode }}</strong>
                <p>{{ t('classroom.inviteHint') }}</p>
              </div>
              <v-btn
                class="copy-icon-btn"
                icon="mdi-content-copy"
                :title="t('classroom.copyInvite')"
                variant="outlined"
                @click="copyInviteCode(detail.inviteCode)"
              />
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
            <div class="detail-tabs" role="tablist">
              <button
                class="detail-tab"
                :class="{ active: activeDetailTab === 'members' }"
                type="button"
                :aria-selected="activeDetailTab === 'members'"
                @click="activeDetailTab = 'members'"
              >
                <span>{{ t('classroom.classmates') }}</span>
                <strong>{{ t('classroom.memberCount', { count: members.length }) }}</strong>
              </button>
              <button
                v-if="detail.createdByMe"
                class="detail-tab"
                :class="{ active: activeDetailTab === 'stats' }"
                type="button"
                :aria-selected="activeDetailTab === 'stats'"
                @click="activeDetailTab = 'stats'"
              >
                <span>{{ t('classroom.learningRecords') }}</span>
                <strong>{{ t('classroom.memberCount', { count: stats.length }) }}</strong>
              </button>
            </div>

            <section v-if="activeDetailTab === 'members'" class="detail-section">
              <div class="section-heading">
                <div>
                  <h3>{{ t('classroom.members') }}</h3>
                  <p>{{ detail.createdByMe ? t('classroom.membersOwnerHint') : t('classroom.membersHint') }}</p>
                </div>
                <span>{{ t('classroom.memberCount', { count: members.length }) }}</span>
              </div>

              <div v-if="members.length > 0" class="member-grid">
                <div
                  v-for="row in members"
                  :key="row.userId"
                  class="member-card"
                  :class="{ actionable: detail.createdByMe }"
                  :role="detail.createdByMe ? 'button' : undefined"
                  :tabindex="detail.createdByMe ? 0 : undefined"
                  :title="detail.createdByMe ? t('classroom.openMemberRecords') : undefined"
                  @click="openMemberRecords(row)"
                  @keyup.enter="openMemberRecords(row)"
                  @keyup.space.prevent="openMemberRecords(row)"
                >
                  <div class="member-avatar">{{ memberInitial(row) }}</div>
                  <div class="member-info">
                    <strong :class="{ 'member-link': detail.createdByMe }">{{ memberName(row) }}</strong>
                    <span v-if="row.email">{{ row.email }}</span>
                    <small>{{ row.joinedAt ? t('classroom.joinedAt', { time: formatDateTime(row.joinedAt) }) : t('classroom.noJoinedAt') }}</small>
                  </div>
                  <span class="member-status">{{ statusLabel(row.status) }}</span>
                </div>
              </div>
              <div v-else class="empty-state compact-empty">{{ t('classroom.emptyMembers') }}</div>

              <v-alert
                v-if="!detail.createdByMe"
                class="creator-lock"
                color="info"
                icon="mdi-lock-outline"
                :text="t('classroom.creatorOnlyStats')"
                variant="tonal"
              />
            </section>

            <section v-else class="detail-section">
              <div class="section-heading">
                <div>
                  <h3>{{ t('classroom.learningOverview') }}</h3>
                  <p>{{ t('classroom.learningOverviewHint') }}</p>
                </div>
              </div>

              <div class="summary-grid">
                <v-card class="summary-card" elevation="0">
                  <span>{{ t('classroom.totalStudyTime') }}</span>
                  <strong>{{ formatStudyTime(totalStudySeconds) }}</strong>
                </v-card>
                <v-card class="summary-card" elevation="0">
                  <span>{{ t('classroom.totalExercises') }}</span>
                  <strong>{{ totalExerciseCount }}</strong>
                </v-card>
                <v-card class="summary-card" elevation="0">
                  <span>{{ t('classroom.averageAccuracy') }}</span>
                  <strong>{{ formatPercent(averageAccuracy) }}</strong>
                </v-card>
                <v-card class="summary-card" elevation="0">
                  <span>{{ t('classroom.activeLearners') }}</span>
                  <strong>{{ activeLearnerCount }}</strong>
                </v-card>
              </div>

              <div v-if="statsRows.length > 0" class="table-scroll">
                <v-table class="stats-table" density="comfortable">
                  <thead>
                    <tr>
                      <th>{{ t('classroom.table.member') }}</th>
                      <th>{{ t('classroom.table.studyTime') }}</th>
                      <th>{{ t('classroom.table.exercises') }}</th>
                      <th>{{ t('classroom.table.accuracy') }}</th>
                      <th>{{ t('classroom.table.activity') }}</th>
                      <th>{{ t('classroom.table.lastStudy') }}</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="row in statsRows" :key="row.userId">
                      <td>
                        <strong>{{ memberStatsName(row) }}</strong>
                        <small>{{ row.email || t('classroom.noOwnerContact') }}</small>
                      </td>
                      <td>{{ formatStudyTime(row.studySeconds) }}</td>
                      <td>{{ row.exerciseCount }}</td>
                      <td>{{ formatPercent(row.accuracyRate) }}</td>
                      <td>{{ t('classroom.activitySummary', { vocab: row.vocabReviewCount, dialogue: row.dialogueCount, matching: row.matchingGameCount }) }}</td>
                      <td>{{ row.lastStudyAt ? formatDateTime(row.lastStudyAt) : t('classroom.noStudyRecord') }}</td>
                    </tr>
                  </tbody>
                </v-table>
              </div>
              <div v-else class="empty-state compact-empty">{{ t('classroom.noStudyRecord') }}</div>
            </section>
          </template>
        </template>
      </v-card>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import LocaleSwitch from '@/components/LocaleSwitch.vue'
import {
  createClassRoom,
  fetchClassMembers,
  fetchClassRoomDetail,
  fetchClassRooms,
  fetchClassStats,
  joinClassRoom
} from '../../api/classroom'
import type { ClassMember, ClassMemberStats, ClassRoom, ClassRoomDetail } from '../../types/api'
import { notifySuccess, notifyWarning } from '../../utils/notify'

type DetailTab = 'members' | 'stats'

const { t } = useI18n()
const router = useRouter()

const loading = ref(false)
const roomLoading = ref(false)
const creating = ref(false)
const joining = ref(false)
const rooms = ref<ClassRoom[]>([])
const activeId = ref<number | null>(null)
const activeDetailTab = ref<DetailTab>('members')
const detail = ref<ClassRoomDetail | null>(null)
const members = ref<ClassMember[]>([])
const stats = ref<ClassMemberStats[]>([])
const createForm = reactive({ name: '', description: '' })
const joinForm = reactive({ inviteCode: '' })
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
  activeDetailTab.value = 'members'
  roomLoading.value = true
  try {
    detail.value = await fetchClassRoomDetail(id)
    if (detail.value.memberStatus === 'active') {
      if (detail.value.createdByMe) {
        const [nextMembers, nextStats] = await Promise.all([
          fetchClassMembers(id),
          fetchClassStats(id)
        ])
        members.value = nextMembers
        stats.value = nextStats
      } else {
        members.value = await fetchClassMembers(id)
        stats.value = []
      }
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
    const room = await createClassRoom({
      name: createForm.name.trim(),
      description: createForm.description.trim() || undefined
    })
    createForm.name = ''
    createForm.description = ''
    notifySuccess(t('classroom.notifications.created'))
    await loadRooms()
    upsertRoom(room)
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
    upsertRoom(room)
    await selectRoom(room.id)
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

function statusLabel(status: string) {
  return t(`classroom.statuses.${status}`)
}

function memberInitial(member: ClassMember) {
  const name = memberName(member)
  return name.trim().slice(0, 1).toUpperCase()
}

function memberName(member: ClassMember) {
  return member.nickname || member.email || t('common.userFallback', { id: member.userId })
}

function memberStatsName(member: ClassMemberStats) {
  return member.nickname || member.email || t('common.userFallback', { id: member.userId })
}

async function openMemberRecords(member: ClassMember) {
  if (!detail.value?.createdByMe) {
    return
  }
  await router.push({
    path: '/records',
    query: {
      classId: String(detail.value.id),
      userId: String(member.userId),
      name: memberName(member)
    }
  })
}

async function copyInviteCode(code: string) {
  try {
    await writeClipboardText(code)
    notifySuccess(t('classroom.notifications.copied'))
  } catch {
    notifyWarning(t('classroom.notifications.copyFailed'))
  }
}

async function writeClipboardText(text: string) {
  if (navigator.clipboard?.writeText && window.isSecureContext) {
    await navigator.clipboard.writeText(text)
    return
  }

  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.setAttribute('readonly', 'true')
  textarea.style.left = '-9999px'
  textarea.style.opacity = '0'
  textarea.style.position = 'fixed'
  textarea.style.top = '0'
  document.body.appendChild(textarea)
  textarea.focus()
  textarea.select()
  const copied = document.execCommand('copy')
  document.body.removeChild(textarea)
  if (!copied) {
    throw new Error('copy failed')
  }
}

function formatStudyTime(seconds: number) {
  const safeSeconds = Math.max(0, seconds || 0)
  if (safeSeconds < 60) {
    return t('units.durationSeconds', { seconds: safeSeconds })
  }
  const hours = Math.floor(safeSeconds / 3600)
  const minutes = Math.floor((safeSeconds % 3600) / 60)
  return hours > 0 ? t('units.durationHoursMinutes', { hours, minutes }) : t('units.durationMinutes', { minutes })
}

function formatPercent(value: number) {
  return `${Number(value || 0).toFixed(1)}%`
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString()
}

onMounted(loadRooms)
</script>

<style scoped>
.classroom-page {
  max-width: 1220px;
}

h1,
h2,
h3 {
  margin: 0;
}

h1 {
  font-size: 34px;
  line-height: 1.2;
}

h2 {
  font-size: 22px;
}

h3 {
  font-size: 18px;
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

.top-actions {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
}

.top-actions :deep(.v-btn),
.actions-panel :deep(.v-btn),
.invite-card :deep(.v-btn) {
  border-radius: 4px;
  letter-spacing: 0;
}

.top-actions :deep(.v-btn--variant-text) {
  color: #f8fafc;
}

.actions-panel,
.room-detail,
.room-list {
  background: #ffffff;
  border: 1px solid #dbe3ee;
}

.actions-panel {
  display: grid;
  gap: 18px;
  grid-template-columns: minmax(0, 1.15fr) minmax(0, 0.85fr);
  margin-bottom: 18px;
  padding: 18px;
}

.action-block {
  display: grid;
  gap: 14px;
}

.action-copy h2 {
  font-size: 20px;
}

.action-copy p {
  line-height: 1.65;
}

.create-form {
  align-items: center;
  display: grid;
  gap: 10px;
  grid-template-columns: minmax(160px, 0.9fr) minmax(220px, 1.25fr) auto;
}

.join-form {
  align-items: center;
  display: grid;
  gap: 10px;
  grid-template-columns: minmax(0, 1fr) auto;
}

.actions-panel :deep(.v-field),
.actions-panel :deep(.v-btn) {
  min-height: 48px;
}

.classroom-workspace {
  align-items: start;
  display: grid;
  gap: 18px;
  grid-template-columns: 340px minmax(0, 1fr);
}

.room-list,
.room-detail {
  padding: 18px;
}

.room-list {
  position: sticky;
  top: 18px;
}

.panel-heading,
.section-heading {
  align-items: center;
  display: flex;
  gap: 14px;
  justify-content: space-between;
}

.panel-heading {
  margin-bottom: 14px;
}

.panel-heading span,
.section-heading > span {
  color: #64748b;
  flex-shrink: 0;
  font-weight: 700;
}

.room-item {
  background: #ffffff;
  border: 1px solid #e5edf7;
  border-radius: 8px;
  cursor: pointer;
  display: grid;
  gap: 8px;
  margin-top: 10px;
  min-height: 108px;
  padding: 14px;
  text-align: left;
  transition:
    background-color 0.18s ease,
    border-color 0.18s ease,
    box-shadow 0.18s ease;
  width: 100%;
}

.room-item:hover {
  background: #f8fbff;
  border-color: #cbdaf0;
}

.room-item.active {
  background: #f8fbff;
  border-color: #2563eb;
  box-shadow: inset 3px 0 0 #2563eb;
}

.room-item-head {
  align-items: flex-start;
  display: flex;
  gap: 10px;
  justify-content: space-between;
}

.room-item-head strong {
  color: #102033;
  font-size: 16px;
  line-height: 1.35;
}

.room-role {
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
  color: #475569;
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 800;
  padding: 3px 7px;
}

.room-role.owner {
  background: #ecfdf5;
  border-color: #bbf7d0;
  color: #047857;
}

.room-desc,
.room-item small {
  color: #64748b;
  display: block;
  font-size: 13px;
  line-height: 1.55;
}

.room-desc {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.room-detail {
  min-height: 620px;
}

.panel-loader {
  margin-bottom: 12px;
}

.detail-header {
  align-items: flex-start;
  display: flex;
  gap: 16px;
  justify-content: space-between;
}

.detail-title-row {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.status-badge,
.member-status {
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  border-radius: 999px;
  color: #334155;
  flex-shrink: 0;
  font-size: 13px;
  font-weight: 800;
  padding: 6px 11px;
}

.detail-meta-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: minmax(0, 1fr) minmax(280px, 0.9fr);
  margin: 18px 0;
}

.owner-card {
  align-items: center;
  background: #f8fafc;
  border: 1px solid #e5edf7;
  border-radius: 8px;
  display: flex;
  gap: 14px;
  padding: 14px;
}

.owner-avatar {
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

.owner-card span,
.invite-card span,
.summary-card span,
.stats-table small {
  color: #64748b;
  display: block;
  font-size: 13px;
}

.owner-card strong,
.invite-card strong {
  color: #102033;
  display: block;
  font-size: 18px;
  margin-top: 2px;
}

.owner-card p,
.invite-card p {
  font-size: 13px;
  margin-top: 2px;
}

.invite-card {
  align-items: center;
  background: #ecfeff;
  border: 1px solid #bae6fd;
  border-radius: 8px;
  display: flex;
  gap: 14px;
  justify-content: space-between;
  padding: 14px;
}

.invite-card strong {
  font-size: 20px;
  letter-spacing: 0.08em;
}

.copy-icon-btn {
  flex-shrink: 0;
  height: 40px;
  min-width: 40px;
  width: 40px;
}

.detail-tabs {
  align-items: center;
  background: #f8fafc;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  display: flex;
  margin-top: 20px;
  overflow: hidden;
}

.detail-tab {
  background: transparent;
  border: 0;
  border-right: 1px solid #dbe3ee;
  color: #475569;
  cursor: pointer;
  display: grid;
  gap: 3px;
  min-width: 172px;
  padding: 11px 16px;
  text-align: left;
}

.detail-tab:last-child {
  border-right: 0;
}

.detail-tab span {
  font-size: 14px;
  font-weight: 800;
}

.detail-tab strong {
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
}

.detail-tab.active {
  background: #ffffff;
  color: #1d4ed8;
  box-shadow: inset 0 -2px 0 #2563eb;
}

.detail-tab.active strong {
  color: #2563eb;
}

.detail-section {
  margin-top: 18px;
}

.section-heading {
  margin-bottom: 14px;
}

.section-heading p {
  line-height: 1.55;
}

.member-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.member-card {
  align-items: center;
  background: #ffffff;
  border: 1px solid #e5edf7;
  border-radius: 8px;
  display: grid;
  gap: 12px;
  grid-template-columns: 42px minmax(0, 1fr) auto;
  min-height: 82px;
  padding: 12px;
}

.member-card.actionable {
  cursor: pointer;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.member-card.actionable:hover,
.member-card.actionable:focus-visible {
  border-color: #93c5fd;
  box-shadow: 0 12px 28px rgba(37, 99, 235, 0.12);
  outline: none;
  transform: translateY(-1px);
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

.member-info {
  min-width: 0;
}

.member-info strong,
.member-info span,
.member-info small {
  display: block;
}

.member-info strong {
  color: #102033;
}

.member-info strong.member-link {
  color: #1d4ed8;
}

.member-info span,
.member-info small {
  color: #64748b;
  font-size: 13px;
  margin-top: 3px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.creator-lock {
  margin-top: 14px;
}

.summary-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin-bottom: 16px;
}

.summary-card {
  background: #ffffff;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  min-height: 118px;
  padding: 20px;
}

.summary-card strong {
  color: #102033;
  display: block;
  font-size: 28px;
  line-height: 1.15;
  margin-top: 12px;
}

.table-scroll {
  overflow-x: auto;
}

.stats-table {
  min-width: 860px;
}

.stats-table :deep(th) {
  background: #f8fafc !important;
  background-color: #f8fafc !important;
  border-bottom: 1px solid #e5edf7 !important;
  box-shadow: none !important;
  color: #475569 !important;
  font-size: 13px;
  font-weight: 800;
  height: 48px;
}

.stats-table :deep(td) {
  border-bottom: 1px solid #eef2f7;
  height: 58px;
  vertical-align: middle;
}

.stats-table :deep(td:first-child) {
  min-width: 190px;
}

.stats-table :deep(td:first-child strong) {
  display: block;
}

.stats-table :deep(tbody tr:hover) {
  background: var(--xc-table-hover-bg);
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
  min-height: 520px;
}

.compact-empty {
  min-height: 120px;
}

@media (max-width: 1120px) {
  .classroom-workspace {
    grid-template-columns: 300px minmax(0, 1fr);
  }

  .create-form {
    grid-template-columns: 1fr;
  }

  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .classroom-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .top-actions {
    justify-content: flex-start;
  }

  .actions-panel,
  .classroom-workspace,
  .detail-meta-grid {
    grid-template-columns: 1fr;
  }

  .room-list {
    position: static;
  }
}

@media (max-width: 680px) {
  h1 {
    font-size: 28px;
  }

  .classroom-hero,
  .room-list,
  .room-detail,
  .actions-panel {
    padding: 20px 16px;
  }

  .join-form,
  .member-card {
    grid-template-columns: 1fr;
  }

  .detail-tabs {
    align-items: stretch;
    flex-direction: column;
  }

  .detail-tab {
    border-bottom: 1px solid #dbe3ee;
    border-right: 0;
    min-width: 0;
    width: 100%;
  }

  .detail-tab:last-child {
    border-bottom: 0;
  }

  .member-grid,
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .invite-card {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
