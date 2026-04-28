<template>
  <section class="users-page">
    <div class="page-heading">
      <div>
        <h1>{{ t('users.title') }}</h1>
        <p>{{ t('users.subtitle') }}</p>
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
            :placeholder="t('users.keywordPlaceholder')"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item>
          <el-select v-model="query.status" :placeholder="t('users.statusFilter')" clearable>
            <el-option :label="t('users.status.active')" value="active" />
            <el-option :label="t('users.status.disabled')" value="disabled" />
            <el-option :label="t('users.status.deleted')" value="deleted" />
          </el-select>
        </el-form-item>
        <el-form-item class="filter-actions">
          <el-button type="primary" :icon="Search" @click="search">{{ t('common.search') }}</el-button>
          <el-button @click="resetFilters">{{ t('users.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="card-header">
          <span>{{ t('users.listTitle') }}</span>
          <span>{{ t('users.total', { total }) }}</span>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="users"
        row-key="id"
        border
        class="user-table"
        :empty-text="t('users.emptyTable')"
      >
        <el-table-column prop="id" label="ID" width="84" />
        <el-table-column :label="t('users.columns.user')" min-width="260">
          <template #default="{ row }">
            <div class="user-cell">
              <strong>{{ row.nickname || t('users.unnamed') }}</strong>
              <span>{{ row.email }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="t('users.columns.status')" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light">
              {{ t(`users.status.${row.status}`) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('users.columns.access')" width="130">
          <template #default="{ row }">
            <el-tag :type="accessTagType(row.accessLevel)" effect="plain">
              {{ t(`users.access.${row.accessLevel}`) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('users.columns.membership')" min-width="170">
          <template #default="{ row }">
            {{ formatDate(row.membershipEndsAt || row.trialEndsAt) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('users.columns.learning')" min-width="210">
          <template #default="{ row }">
            <div class="learning-cell">
              <span>{{ formatDuration(row.totalStudySeconds) }}</span>
              <span>{{ t('users.exerciseSummary', { count: row.totalExerciseCount, rate: formatPercent(row.overallAccuracyRate) }) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="t('users.columns.createdAt')" min-width="170">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('users.columns.lastLoginAt')" min-width="170">
          <template #default="{ row }">
            {{ formatDate(row.lastLoginAt) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('users.columns.actions')" fixed="right" width="210">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.id)">{{ t('users.actions.detail') }}</el-button>
            <el-button link :type="row.status === 'active' ? 'warning' : 'success'" @click="confirmStatus(row)">
              {{ row.status === 'active' ? t('users.actions.disable') : t('users.actions.enable') }}
            </el-button>
            <el-button link type="primary" @click="openMembership(row)">{{ t('users.actions.membership') }}</el-button>
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

    <el-drawer v-model="detailVisible" :title="t('users.detailTitle')" size="720px">
      <div v-loading="detailLoading" class="detail-drawer">
        <template v-if="selectedDetail">
          <div class="detail-title">
            <div>
              <h2>{{ selectedDetail.nickname || t('users.unnamed') }}</h2>
              <p>{{ selectedDetail.email }}</p>
            </div>
            <el-tag :type="accessTagType(selectedDetail.accessLevel)">
              {{ t(`users.access.${selectedDetail.accessLevel}`) }}
            </el-tag>
          </div>

          <el-descriptions :column="2" border>
            <el-descriptions-item label="ID">{{ selectedDetail.id }}</el-descriptions-item>
            <el-descriptions-item :label="t('users.columns.status')">
              {{ t(`users.status.${selectedDetail.status}`) }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('users.trialPeriod')">
              {{ formatDate(selectedDetail.trialStartedAt) }} - {{ formatDate(selectedDetail.trialEndsAt) }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('users.columns.membership')">
              {{ formatDate(selectedDetail.membershipEndsAt) }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('users.columns.createdAt')">
              {{ formatDate(selectedDetail.createdAt) }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('users.columns.lastLoginAt')">
              {{ formatDate(selectedDetail.lastLoginAt) }}
            </el-descriptions-item>
          </el-descriptions>

          <section class="detail-section">
            <h3>{{ t('users.learningTitle') }}</h3>
            <div class="summary-grid">
              <div>
                <span>{{ t('users.summary.studyTime') }}</span>
                <strong>{{ formatDuration(selectedDetail.learningSummary.totalStudySeconds) }}</strong>
              </div>
              <div>
                <span>{{ t('users.summary.exerciseCount') }}</span>
                <strong>{{ selectedDetail.learningSummary.totalExerciseCount }}</strong>
              </div>
              <div>
                <span>{{ t('users.summary.correctCount') }}</span>
                <strong>{{ selectedDetail.learningSummary.totalCorrectCount }}</strong>
              </div>
              <div>
                <span>{{ t('users.summary.accuracy') }}</span>
                <strong>{{ formatPercent(selectedDetail.learningSummary.overallAccuracyRate) }}</strong>
              </div>
              <div>
                <span>{{ t('users.summary.vocabReview') }}</span>
                <strong>{{ selectedDetail.learningSummary.totalVocabReviewCount }}</strong>
              </div>
              <div>
                <span>{{ t('users.summary.streak') }}</span>
                <strong>{{ selectedDetail.learningSummary.currentStreakDays }}</strong>
              </div>
            </div>
          </section>

          <section class="detail-section">
            <h3>{{ t('users.membershipRecords') }}</h3>
            <el-table :data="selectedDetail.memberships" border>
              <el-table-column prop="id" label="ID" width="72" />
              <el-table-column :label="t('users.membershipPlan')" min-width="150">
                <template #default="{ row }">
                  {{ row.planName || t('users.manualAdjustment') }}
                </template>
              </el-table-column>
              <el-table-column :label="t('users.membershipPeriod')" min-width="230">
                <template #default="{ row }">
                  {{ formatDate(row.startedAt) }} - {{ formatDate(row.endsAt) }}
                </template>
              </el-table-column>
              <el-table-column prop="status" :label="t('users.columns.status')" width="110" />
              <el-table-column prop="adjustReason" :label="t('users.reason')" min-width="180" />
            </el-table>
          </section>
        </template>
      </div>
    </el-drawer>

    <el-dialog v-model="membershipVisible" :title="t('users.membershipDialogTitle')" width="520px">
      <el-form ref="membershipFormRef" :model="membershipForm" :rules="membershipRules" label-position="top">
        <el-form-item :label="t('users.membershipAction')" prop="action">
          <el-radio-group v-model="membershipForm.action">
            <el-radio-button label="grant">{{ t('users.actions.grantMembership') }}</el-radio-button>
            <el-radio-button label="cancel">{{ t('users.actions.cancelMembership') }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <template v-if="membershipForm.action === 'grant'">
          <el-form-item :label="t('users.startedAt')">
            <el-date-picker v-model="membershipForm.startedAt" type="datetime" class="full-input" />
          </el-form-item>
          <el-form-item :label="t('users.endsAt')">
            <el-date-picker v-model="membershipForm.endsAt" type="datetime" class="full-input" />
          </el-form-item>
        </template>
        <el-form-item :label="t('users.reason')" prop="reason">
          <el-input v-model="membershipForm.reason" type="textarea" :rows="4" :placeholder="t('users.reasonPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="membershipVisible = false">{{ t('users.cancel') }}</el-button>
        <el-button type="primary" :loading="membershipSubmitting" @click="submitMembership">
          {{ t('users.submit') }}
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import {
  adjustAdminUserMembership,
  fetchAdminUserDetail,
  fetchAdminUsers,
  updateAdminUserStatus
} from '@/api/users'
import type {
  AdminUserDetail,
  AdminUserListItem,
  AdminUserQuery,
  UserStatus
} from '@/types/api'

const { t, locale } = useI18n()

const loading = ref(false)
const detailLoading = ref(false)
const membershipSubmitting = ref(false)
const detailVisible = ref(false)
const membershipVisible = ref(false)
const users = ref<AdminUserListItem[]>([])
const total = ref(0)
const selectedDetail = ref<AdminUserDetail | null>(null)
const selectedUser = ref<AdminUserListItem | null>(null)
const membershipFormRef = ref<FormInstance>()

const query = reactive<AdminUserQuery>({
  page: 1,
  pageSize: 20,
  keyword: '',
  status: ''
})

const membershipForm = reactive<{
  action: 'grant' | 'cancel'
  startedAt: Date | null
  endsAt: Date | null
  reason: string
}>({
  action: 'grant',
  startedAt: null,
  endsAt: null,
  reason: ''
})

const membershipRules = computed<FormRules>(() => ({
  reason: [
    { required: true, message: t('users.reasonRequired'), trigger: 'blur' },
    { max: 1000, message: t('users.reasonTooLong'), trigger: 'blur' }
  ]
}))

async function reload() {
  loading.value = true
  try {
    const result = await fetchAdminUsers(query)
    users.value = result.records
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

async function openDetail(userId: number) {
  detailVisible.value = true
  detailLoading.value = true
  try {
    selectedDetail.value = await fetchAdminUserDetail(userId)
  } finally {
    detailLoading.value = false
  }
}

async function confirmStatus(row: AdminUserListItem) {
  const status: Extract<UserStatus, 'active' | 'disabled'> = row.status === 'active' ? 'disabled' : 'active'
  const { value } = await ElMessageBox.prompt(
    t('users.statusReasonPlaceholder'),
    t('users.statusDialogTitle'),
    {
      confirmButtonText: t('users.submit'),
      cancelButtonText: t('users.cancel'),
      inputType: 'textarea',
      inputValidator: value => !value || value.length <= 1000 || t('users.reasonTooLong')
    }
  )
  const detail = await updateAdminUserStatus(row.id, { status, reason: value || '' })
  selectedDetail.value = detail
  ElMessage.success(t('users.saved'))
  await reload()
}

function openMembership(row: AdminUserListItem) {
  selectedUser.value = row
  membershipForm.action = 'grant'
  membershipForm.startedAt = new Date()
  membershipForm.endsAt = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000)
  membershipForm.reason = ''
  membershipVisible.value = true
}

async function submitMembership() {
  if (!selectedUser.value) {
    return
  }
  await membershipFormRef.value?.validate()
  if (membershipForm.action === 'grant') {
    if (!membershipForm.startedAt || !membershipForm.endsAt) {
      ElMessage.warning(t('users.periodRequired'))
      return
    }
    if (membershipForm.endsAt.getTime() <= membershipForm.startedAt.getTime()) {
      ElMessage.warning(t('users.periodInvalid'))
      return
    }
  }
  membershipSubmitting.value = true
  try {
    const detail = await adjustAdminUserMembership(selectedUser.value.id, {
      action: membershipForm.action,
      startedAt: membershipForm.action === 'grant' ? membershipForm.startedAt?.toISOString() : undefined,
      endsAt: membershipForm.action === 'grant' ? membershipForm.endsAt?.toISOString() : undefined,
      reason: membershipForm.reason.trim()
    })
    selectedDetail.value = detail
    membershipVisible.value = false
    ElMessage.success(t('users.saved'))
    await reload()
  } finally {
    membershipSubmitting.value = false
  }
}

function statusTagType(status: UserStatus) {
  if (status === 'active') {
    return 'success'
  }
  if (status === 'disabled') {
    return 'warning'
  }
  return 'info'
}

function accessTagType(accessLevel: string) {
  if (accessLevel === 'member') {
    return 'success'
  }
  if (accessLevel === 'trial') {
    return 'warning'
  }
  return 'info'
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

function formatDuration(seconds: number) {
  if (!seconds) {
    return t('users.minutes', { value: 0 })
  }
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  if (hours > 0) {
    return t('users.hours', { hours, minutes })
  }
  return t('users.minutes', { value: minutes })
}

function formatPercent(value: number) {
  return `${Number(value || 0).toFixed(2)}%`
}

onMounted(reload)
</script>

<style scoped>
.users-page {
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
h3,
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

.user-table {
  width: 100%;
}

.user-cell,
.learning-cell {
  display: grid;
  gap: 4px;
}

.user-cell strong {
  color: #172033;
}

.user-cell span,
.learning-cell span + span {
  color: #64748b;
  font-size: 12px;
}

.pagination-row {
  justify-content: flex-end;
  padding-top: 16px;
}

.detail-drawer {
  min-height: 360px;
}

.detail-title {
  margin-bottom: 18px;
}

.detail-title h2 {
  color: #172033;
  font-size: 22px;
}

.detail-section {
  display: grid;
  gap: 12px;
  margin-top: 22px;
}

.detail-section h3 {
  color: #172033;
  font-size: 16px;
}

.summary-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
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
  font-size: 20px;
}

.full-input {
  width: 100%;
}

@media (max-width: 900px) {
  .page-heading {
    align-items: flex-start;
    gap: 12px;
  }

  .filter-form {
    grid-template-columns: 1fr;
  }

  .summary-grid {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
