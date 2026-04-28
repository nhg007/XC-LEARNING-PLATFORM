<template>
  <section class="classroom-page">
    <div class="page-heading">
      <div>
        <h1>{{ t('classrooms.title') }}</h1>
        <p>{{ t('classrooms.subtitle') }}</p>
      </div>
      <el-button :loading="loading" :icon="Refresh" @click="reload">
        {{ t('common.refresh') }}
      </el-button>
    </div>

    <el-card shadow="never" class="filter-card">
      <el-form class="filter-form" :model="query" @submit.prevent>
        <el-form-item>
          <el-input
            v-model="query.keyword"
            clearable
            :prefix-icon="Search"
            :placeholder="t('classrooms.keywordPlaceholder')"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item>
          <el-select v-model="query.status" clearable :placeholder="t('classrooms.statusFilter')">
            <el-option :label="t('classrooms.status.active')" value="active" />
            <el-option :label="t('classrooms.status.archived')" value="archived" />
            <el-option :label="t('classrooms.status.deleted')" value="deleted" />
          </el-select>
        </el-form-item>
        <el-form-item class="filter-actions">
          <el-button type="primary" :icon="Search" @click="search">{{ t('common.search') }}</el-button>
          <el-button @click="resetFilters">{{ t('classrooms.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="card-header">
          <span>{{ t('classrooms.listTitle') }}</span>
          <span>{{ t('classrooms.total', { total }) }}</span>
        </div>
      </template>

      <el-table v-loading="loading" :data="classrooms" row-key="id" border :empty-text="t('classrooms.emptyTable')">
        <el-table-column prop="id" label="ID" width="84" />
        <el-table-column :label="t('classrooms.columns.classroom')" min-width="250">
          <template #default="{ row }">
            <div class="main-cell">
              <strong>{{ row.name }}</strong>
              <span>{{ row.description || t('common.empty') }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="t('classrooms.columns.owner')" min-width="220">
          <template #default="{ row }">
            <div class="main-cell">
              <strong>{{ row.ownerNickname || t('classrooms.unnamed') }}</strong>
              <span>{{ row.ownerEmail || row.ownerUserId }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="inviteCode" :label="t('classrooms.columns.inviteCode')" width="130" />
        <el-table-column :label="t('classrooms.columns.status')" width="120">
          <template #default="{ row }">
            <el-tag :type="classStatusTag(row.status)">
              {{ t(`classrooms.status.${row.status}`) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('classrooms.columns.members')" min-width="160">
          <template #default="{ row }">
            {{ t('classrooms.memberSummary', { active: row.activeMemberCount, pending: row.pendingMemberCount }) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('classrooms.columns.learning')" min-width="210">
          <template #default="{ row }">
            <div class="main-cell">
              <strong>{{ formatDuration(row.totalStudySeconds) }}</strong>
              <span>{{ t('classrooms.exerciseSummary', { count: row.totalExerciseCount, rate: formatPercent(row.accuracyRate) }) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="t('classrooms.columns.lastStudyAt')" min-width="170">
          <template #default="{ row }">
            {{ formatDate(row.lastStudyAt) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('classrooms.columns.actions')" fixed="right" width="180">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.id)">{{ t('classrooms.actions.detail') }}</el-button>
            <el-button
              v-if="row.status !== 'deleted'"
              link
              :type="row.status === 'active' ? 'warning' : 'success'"
              @click="toggleClassStatus(row)"
            >
              {{ row.status === 'active' ? t('classrooms.actions.archive') : t('classrooms.actions.reopen') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-row">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handlePageSizeChange"
          @current-change="reload"
        />
      </div>
    </el-card>

    <el-drawer v-model="detailVisible" :title="t('classrooms.detailTitle')" size="860px">
      <div v-loading="detailLoading" class="detail-drawer">
        <template v-if="selectedDetail">
          <div class="detail-title">
            <div>
              <h2>{{ selectedDetail.name }}</h2>
              <p>{{ selectedDetail.description || t('common.empty') }}</p>
            </div>
            <el-tag :type="classStatusTag(selectedDetail.status)">
              {{ t(`classrooms.status.${selectedDetail.status}`) }}
            </el-tag>
          </div>

          <el-descriptions :column="3" border>
            <el-descriptions-item label="ID">{{ selectedDetail.id }}</el-descriptions-item>
            <el-descriptions-item :label="t('classrooms.columns.inviteCode')">{{ selectedDetail.inviteCode }}</el-descriptions-item>
            <el-descriptions-item :label="t('classrooms.columns.owner')">
              {{ selectedDetail.ownerEmail || selectedDetail.ownerUserId }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('classrooms.activeMembers')">{{ selectedDetail.activeMemberCount }}</el-descriptions-item>
            <el-descriptions-item :label="t('classrooms.pendingMembers')">{{ selectedDetail.pendingMemberCount }}</el-descriptions-item>
            <el-descriptions-item :label="t('classrooms.removedMembers')">{{ selectedDetail.removedMemberCount }}</el-descriptions-item>
          </el-descriptions>

          <section class="summary-grid">
            <div>
              <span>{{ t('classrooms.summary.studyTime') }}</span>
              <strong>{{ formatDuration(selectedDetail.totalStudySeconds) }}</strong>
            </div>
            <div>
              <span>{{ t('classrooms.summary.exerciseCount') }}</span>
              <strong>{{ selectedDetail.totalExerciseCount }}</strong>
            </div>
            <div>
              <span>{{ t('classrooms.summary.accuracy') }}</span>
              <strong>{{ formatPercent(selectedDetail.accuracyRate) }}</strong>
            </div>
            <div>
              <span>{{ t('classrooms.summary.lastStudyAt') }}</span>
              <strong>{{ formatDate(selectedDetail.lastStudyAt) }}</strong>
            </div>
          </section>

          <el-tabs class="detail-tabs">
            <el-tab-pane :label="t('classrooms.tabs.members')">
              <el-table :data="selectedDetail.members" border>
                <el-table-column prop="userId" label="ID" width="84" />
                <el-table-column :label="t('classrooms.columns.member')" min-width="220">
                  <template #default="{ row }">
                    <div class="main-cell">
                      <strong>{{ row.nickname || t('classrooms.unnamed') }}</strong>
                      <span>{{ row.email || row.userId }}</span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column :label="t('classrooms.columns.role')" width="120">
                  <template #default="{ row }">
                    {{ t(`classrooms.roles.${row.memberRole}`) }}
                  </template>
                </el-table-column>
                <el-table-column :label="t('classrooms.columns.memberStatus')" width="160">
                  <template #default="{ row }">
                    <el-tag :type="memberStatusTag(row.status)">
                      {{ t(`classrooms.memberStatus.${row.status}`) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column :label="t('classrooms.columns.joinedAt')" min-width="170">
                  <template #default="{ row }">
                    {{ formatDate(row.joinedAt) }}
                  </template>
                </el-table-column>
                <el-table-column :label="t('classrooms.columns.actions')" width="120">
                  <template #default="{ row }">
                    <el-button
                      v-if="row.memberRole !== 'teacher' && row.status !== 'removed'"
                      link
                      type="danger"
                      @click="removeMember(row)"
                    >
                      {{ t('classrooms.actions.remove') }}
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <el-tab-pane :label="t('classrooms.tabs.stats')">
              <el-table :data="selectedDetail.stats" border>
                <el-table-column :label="t('classrooms.columns.member')" min-width="220">
                  <template #default="{ row }">
                    <div class="main-cell">
                      <strong>{{ row.nickname || t('classrooms.unnamed') }}</strong>
                      <span>{{ row.email || row.userId }}</span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column :label="t('classrooms.summary.studyTime')" min-width="140">
                  <template #default="{ row }">
                    {{ formatDuration(row.studySeconds) }}
                  </template>
                </el-table-column>
                <el-table-column prop="exerciseCount" :label="t('classrooms.summary.exerciseCount')" width="110" />
                <el-table-column prop="correctCount" :label="t('classrooms.summary.correctCount')" width="110" />
                <el-table-column :label="t('classrooms.summary.accuracy')" width="120">
                  <template #default="{ row }">
                    {{ formatPercent(row.accuracyRate) }}
                  </template>
                </el-table-column>
                <el-table-column prop="vocabReviewCount" :label="t('classrooms.summary.vocabReview')" width="120" />
                <el-table-column :label="t('classrooms.summary.lastStudyAt')" min-width="170">
                  <template #default="{ row }">
                    {{ formatDate(row.lastStudyAt) }}
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </template>
      </div>
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import {
  fetchAdminClassRoomDetail,
  fetchAdminClassRooms,
  removeAdminClassMember,
  updateAdminClassRoomStatus
} from '@/api/classrooms'
import type {
  AdminClassMember,
  AdminClassRoomDetail,
  AdminClassRoomListItem,
  AdminClassRoomQuery,
  ClassRoomStatus
} from '@/types/api'

const { t, locale } = useI18n()

const loading = ref(false)
const detailLoading = ref(false)
const detailVisible = ref(false)
const classrooms = ref<AdminClassRoomListItem[]>([])
const selectedDetail = ref<AdminClassRoomDetail | null>(null)
const total = ref(0)

const query = reactive<AdminClassRoomQuery>({
  page: 1,
  pageSize: 20,
  keyword: '',
  status: ''
})

async function reload() {
  loading.value = true
  try {
    const result = await fetchAdminClassRooms(query)
    classrooms.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  void reload()
}

function resetFilters() {
  query.keyword = ''
  query.status = ''
  query.page = 1
  void reload()
}

function handlePageSizeChange() {
  query.page = 1
  void reload()
}

async function openDetail(classId: number) {
  detailVisible.value = true
  detailLoading.value = true
  try {
    selectedDetail.value = await fetchAdminClassRoomDetail(classId)
  } finally {
    detailLoading.value = false
  }
}

async function toggleClassStatus(row: AdminClassRoomListItem) {
  const status: Extract<ClassRoomStatus, 'active' | 'archived'> = row.status === 'active' ? 'archived' : 'active'
  const { value } = await ElMessageBox.prompt(
    t('classrooms.statusReasonPlaceholder'),
    t('classrooms.statusDialogTitle'),
    {
      confirmButtonText: t('classrooms.submit'),
      cancelButtonText: t('classrooms.cancel'),
      inputType: 'textarea',
      inputValidator: value => !value || value.length <= 1000 || t('classrooms.reasonTooLong')
    }
  )
  const detail = await updateAdminClassRoomStatus(row.id, { status, reason: value || '' })
  selectedDetail.value = detail
  ElMessage.success(t('classrooms.saved'))
  await reload()
}

async function removeMember(row: AdminClassMember) {
  if (!selectedDetail.value) {
    return
  }
  const { value } = await ElMessageBox.prompt(
    t('classrooms.removeReasonPlaceholder'),
    t('classrooms.removeDialogTitle'),
    {
      confirmButtonText: t('classrooms.submit'),
      cancelButtonText: t('classrooms.cancel'),
      inputType: 'textarea',
      inputValidator: value => !value || value.length <= 1000 || t('classrooms.reasonTooLong')
    }
  )
  await removeAdminClassMember(selectedDetail.value.id, row.userId, { reason: value || '' })
  ElMessage.success(t('classrooms.saved'))
  await openDetail(selectedDetail.value.id)
  await reload()
}

function classStatusTag(status: ClassRoomStatus) {
  if (status === 'active') {
    return 'success'
  }
  if (status === 'archived') {
    return 'warning'
  }
  return 'info'
}

function memberStatusTag(status: string) {
  if (status === 'active') {
    return 'success'
  }
  if (status === 'pending_teacher_review' || status === 'invited') {
    return 'warning'
  }
  if (status === 'removed' || status === 'rejected') {
    return 'danger'
  }
  return 'info'
}

function formatDuration(seconds: number) {
  if (!seconds) {
    return t('classrooms.minutes', { value: 0 })
  }
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  if (hours > 0) {
    return t('classrooms.hours', { hours, minutes })
  }
  return t('classrooms.minutes', { value: minutes })
}

function formatPercent(value: number) {
  return `${Number(value || 0).toFixed(2)}%`
}

function formatDate(value?: string | null) {
  if (!value) {
    return t('common.empty')
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return t('common.empty')
  }
  return new Intl.DateTimeFormat(locale.value, {
    dateStyle: 'medium',
    timeStyle: 'short'
  }).format(date)
}

onMounted(reload)
</script>

<style scoped>
.classroom-page {
  display: grid;
  gap: 16px;
}

.page-heading {
  align-items: center;
  display: flex;
  justify-content: space-between;
}

h1,
h2,
p {
  margin: 0;
}

h1 {
  color: #172033;
  font-size: 24px;
}

.page-heading p,
.detail-title p {
  color: #64748b;
  margin-top: 8px;
}

.filter-card :deep(.el-card__body) {
  padding-bottom: 0;
}

.filter-form {
  display: grid;
  gap: 12px;
  grid-template-columns: minmax(260px, 360px) 180px auto;
}

.filter-actions {
  justify-content: flex-end;
}

.card-header,
.pagination-row,
.detail-title {
  align-items: center;
  display: flex;
  justify-content: space-between;
}

.card-header {
  color: #64748b;
  font-size: 13px;
}

.card-header span:first-child {
  color: #172033;
  font-size: 16px;
  font-weight: 700;
}

.main-cell {
  display: grid;
  gap: 4px;
}

.main-cell strong {
  color: #172033;
}

.main-cell span {
  color: #64748b;
  font-size: 12px;
}

.pagination-row {
  justify-content: flex-end;
  padding-top: 16px;
}

.detail-drawer {
  min-height: 420px;
}

.detail-title {
  margin-bottom: 18px;
}

.detail-title h2 {
  color: #172033;
  font-size: 22px;
}

.summary-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin: 18px 0;
}

.summary-grid div {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  display: grid;
  gap: 8px;
  padding: 14px;
}

.summary-grid span {
  color: #64748b;
  font-size: 12px;
}

.summary-grid strong {
  color: #172033;
  font-size: 18px;
}

.detail-tabs {
  margin-top: 8px;
}

@media (max-width: 900px) {
  .filter-form,
  .summary-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 640px) {
  .page-heading {
    align-items: flex-start;
    gap: 12px;
  }

  .filter-form,
  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
