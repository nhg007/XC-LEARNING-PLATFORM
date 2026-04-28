<template>
  <section class="membership-page">
    <div class="page-heading">
      <div>
        <h1>{{ t('memberships.title') }}</h1>
        <p>{{ t('memberships.subtitle') }}</p>
      </div>
      <el-button :loading="activeLoading" :icon="Refresh" @click="reloadActive">
        {{ t('common.refresh') }}
      </el-button>
    </div>

    <el-tabs v-model="activeTab" class="admin-tabs" @tab-change="handleTabChange">
      <el-tab-pane :label="t('memberships.tabs.plans')" name="plans">
        <el-card shadow="never" class="filter-card">
          <el-form class="filter-form" :model="planQuery" @submit.prevent>
            <el-form-item>
              <el-input
                v-model="planQuery.keyword"
                clearable
                :prefix-icon="Search"
                :placeholder="t('memberships.planKeyword')"
                @keyup.enter="searchPlans"
              />
            </el-form-item>
            <el-form-item>
              <el-select v-model="planQuery.status" clearable :placeholder="t('memberships.statusFilter')">
                <el-option :label="t('memberships.planStatus.active')" value="active" />
                <el-option :label="t('memberships.planStatus.inactive')" value="inactive" />
              </el-select>
            </el-form-item>
            <el-form-item class="filter-actions">
              <el-button type="primary" :icon="Search" @click="searchPlans">{{ t('common.search') }}</el-button>
              <el-button @click="resetPlanFilters">{{ t('memberships.reset') }}</el-button>
              <el-button type="primary" plain :icon="Plus" @click="openPlanDialog()">
                {{ t('memberships.actions.createPlan') }}
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" class="table-card">
          <template #header>
            <div class="card-header">
              <span>{{ t('memberships.planListTitle') }}</span>
              <span>{{ t('memberships.total', { total: planTotal }) }}</span>
            </div>
          </template>
          <el-table v-loading="planLoading" :data="plans" row-key="id" border :empty-text="t('memberships.emptyPlans')">
            <el-table-column prop="id" label="ID" width="84" />
            <el-table-column prop="name" :label="t('memberships.columns.planName')" min-width="180" />
            <el-table-column :label="t('memberships.columns.duration')" min-width="150">
              <template #default="{ row }">
                {{ formatDuration(row.durationUnit, row.durationValue, row.durationDays) }}
              </template>
            </el-table-column>
            <el-table-column :label="t('memberships.columns.price')" min-width="130">
              <template #default="{ row }">
                {{ formatMoney(row.price, row.currency) }}
              </template>
            </el-table-column>
            <el-table-column :label="t('memberships.columns.status')" width="120">
              <template #default="{ row }">
                <el-tag :type="row.status === 'active' ? 'success' : 'info'">
                  {{ t(`memberships.planStatus.${row.status}`) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="t('memberships.columns.updatedAt')" min-width="170">
              <template #default="{ row }">
                {{ formatDate(row.updatedAt) }}
              </template>
            </el-table-column>
            <el-table-column :label="t('memberships.columns.actions')" fixed="right" width="190">
              <template #default="{ row }">
                <el-button link type="primary" @click="openPlanDialog(row)">{{ t('memberships.actions.edit') }}</el-button>
                <el-button link :type="row.status === 'active' ? 'warning' : 'success'" @click="togglePlanStatus(row)">
                  {{ row.status === 'active' ? t('memberships.actions.disable') : t('memberships.actions.enable') }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-row">
            <el-pagination
              v-model:current-page="planQuery.page"
              v-model:page-size="planQuery.pageSize"
              :total="planTotal"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handlePlanPageSizeChange"
              @current-change="loadPlans"
            />
          </div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane :label="t('memberships.tabs.orders')" name="orders">
        <el-card shadow="never" class="filter-card">
          <el-form class="order-filter-form" :model="orderQuery" @submit.prevent>
            <el-form-item>
              <el-input
                v-model="orderQuery.keyword"
                clearable
                :prefix-icon="Search"
                :placeholder="t('memberships.orderKeyword')"
                @keyup.enter="searchOrders"
              />
            </el-form-item>
            <el-form-item>
              <el-select v-model="orderQuery.status" clearable :placeholder="t('memberships.orderStatusFilter')">
                <el-option :label="t('memberships.orderStatus.pending')" value="pending" />
                <el-option :label="t('memberships.orderStatus.paid')" value="paid" />
                <el-option :label="t('memberships.orderStatus.failed')" value="failed" />
                <el-option :label="t('memberships.orderStatus.refunded')" value="refunded" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="orderQuery.provider" clearable :placeholder="t('memberships.providerFilter')">
                <el-option :label="t('memberships.providers.wechat_pay')" value="wechat_pay" />
                <el-option :label="t('memberships.providers.alipay')" value="alipay" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="orderQuery.clientType" clearable :placeholder="t('memberships.clientFilter')">
                <el-option :label="t('memberships.clients.web')" value="web" />
                <el-option :label="t('memberships.clients.mobile')" value="mobile" />
                <el-option :label="t('memberships.clients.admin')" value="admin" />
              </el-select>
            </el-form-item>
            <el-form-item class="filter-actions">
              <el-button type="primary" :icon="Search" @click="searchOrders">{{ t('common.search') }}</el-button>
              <el-button @click="resetOrderFilters">{{ t('memberships.reset') }}</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" class="table-card">
          <template #header>
            <div class="card-header">
              <span>{{ t('memberships.orderListTitle') }}</span>
              <span>{{ t('memberships.total', { total: orderTotal }) }}</span>
            </div>
          </template>
          <el-table v-loading="orderLoading" :data="orders" row-key="id" border :empty-text="t('memberships.emptyOrders')">
            <el-table-column prop="orderNo" :label="t('memberships.columns.orderNo')" min-width="190" />
            <el-table-column :label="t('memberships.columns.user')" min-width="220">
              <template #default="{ row }">
                <div class="user-cell">
                  <strong>{{ row.userNickname || t('memberships.unnamed') }}</strong>
                  <span>{{ row.userEmail || row.userId }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column :label="t('memberships.columns.planName')" min-width="160">
              <template #default="{ row }">
                {{ row.planName || row.planId }}
              </template>
            </el-table-column>
            <el-table-column :label="t('memberships.columns.amount')" width="130">
              <template #default="{ row }">
                {{ formatMoney(row.amount, row.currency) }}
              </template>
            </el-table-column>
            <el-table-column :label="t('memberships.columns.provider')" width="130">
              <template #default="{ row }">
                {{ t(`memberships.providers.${row.provider}`) }}
              </template>
            </el-table-column>
            <el-table-column :label="t('memberships.columns.status')" width="120">
              <template #default="{ row }">
                <el-tag :type="orderStatusTag(row.status)">
                  {{ t(`memberships.orderStatus.${row.status}`) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="t('memberships.columns.createdAt')" min-width="170">
              <template #default="{ row }">
                {{ formatDate(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column :label="t('memberships.columns.actions')" fixed="right" width="120">
              <template #default="{ row }">
                <el-button link type="primary" @click="openOrderDetail(row.id)">{{ t('memberships.actions.detail') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-row">
            <el-pagination
              v-model:current-page="orderQuery.page"
              v-model:page-size="orderQuery.pageSize"
              :total="orderTotal"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleOrderPageSizeChange"
              @current-change="loadOrders"
            />
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="planDialogVisible" :title="editingPlan ? t('memberships.editPlanTitle') : t('memberships.createPlanTitle')" width="560px">
      <el-form ref="planFormRef" :model="planForm" :rules="planRules" label-position="top">
        <el-form-item :label="t('memberships.fields.name')" prop="name">
          <el-input v-model="planForm.name" :placeholder="t('memberships.fields.namePlaceholder')" />
        </el-form-item>
        <div class="form-grid">
          <el-form-item :label="t('memberships.fields.durationUnit')" prop="durationUnit">
            <el-select v-model="planForm.durationUnit" class="full-input">
              <el-option :label="t('memberships.durationUnits.day')" value="day" />
              <el-option :label="t('memberships.durationUnits.month')" value="month" />
              <el-option :label="t('memberships.durationUnits.custom')" value="custom" />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('memberships.fields.durationValue')" prop="durationValue">
            <el-input-number v-model="planForm.durationValue" class="full-input" :min="1" :max="3650" />
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item :label="t('memberships.fields.price')" prop="price">
            <el-input-number v-model="planForm.price" class="full-input" :min="0" :precision="2" :step="10" />
          </el-form-item>
          <el-form-item :label="t('memberships.fields.currency')" prop="currency">
            <el-input v-model="planForm.currency" maxlength="10" />
          </el-form-item>
        </div>
        <el-form-item :label="t('memberships.fields.status')" prop="status">
          <el-radio-group v-model="planForm.status">
            <el-radio-button label="active">{{ t('memberships.planStatus.active') }}</el-radio-button>
            <el-radio-button label="inactive">{{ t('memberships.planStatus.inactive') }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="planDialogVisible = false">{{ t('memberships.cancel') }}</el-button>
        <el-button type="primary" :loading="planSubmitting" @click="submitPlan">{{ t('memberships.submit') }}</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="orderDrawerVisible" :title="t('memberships.orderDetailTitle')" size="680px">
      <div v-loading="orderDetailLoading" class="detail-drawer">
        <template v-if="selectedOrder">
          <div class="detail-title">
            <div>
              <h2>{{ selectedOrder.orderNo }}</h2>
              <p>{{ selectedOrder.planName || selectedOrder.planId }}</p>
            </div>
            <el-tag :type="orderStatusTag(selectedOrder.status)">
              {{ t(`memberships.orderStatus.${selectedOrder.status}`) }}
            </el-tag>
          </div>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="ID">{{ selectedOrder.id }}</el-descriptions-item>
            <el-descriptions-item :label="t('memberships.columns.amount')">
              {{ formatMoney(selectedOrder.amount, selectedOrder.currency) }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('memberships.columns.user')">
              {{ selectedOrder.userEmail || selectedOrder.userId }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('memberships.columns.provider')">
              {{ t(`memberships.providers.${selectedOrder.provider}`) }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('memberships.columns.client')">
              {{ t(`memberships.clients.${selectedOrder.clientType}`) }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('memberships.providerTradeNo')">
              {{ selectedOrder.providerTradeNo || t('common.empty') }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('memberships.paymentUrl')" :span="2">
              {{ selectedOrder.paymentUrl || t('common.empty') }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('memberships.columns.createdAt')">
              {{ formatDate(selectedOrder.createdAt) }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('memberships.paidAt')">
              {{ formatDate(selectedOrder.paidAt) }}
            </el-descriptions-item>
          </el-descriptions>
        </template>
      </div>
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  createAdminMembershipPlan,
  fetchAdminMembershipPlans,
  fetchAdminPaymentOrderDetail,
  fetchAdminPaymentOrders,
  updateAdminMembershipPlan,
  updateAdminMembershipPlanStatus
} from '@/api/memberships'
import type {
  AdminMembershipPlan,
  AdminMembershipPlanQuery,
  AdminPaymentOrder,
  AdminPaymentOrderQuery,
  MembershipDurationUnit,
  MembershipPlanStatus,
  PaymentOrderStatus
} from '@/types/api'

const { t, locale } = useI18n()

const activeTab = ref<'plans' | 'orders'>('plans')
const planLoading = ref(false)
const orderLoading = ref(false)
const planSubmitting = ref(false)
const orderDetailLoading = ref(false)
const planDialogVisible = ref(false)
const orderDrawerVisible = ref(false)
const plans = ref<AdminMembershipPlan[]>([])
const orders = ref<AdminPaymentOrder[]>([])
const planTotal = ref(0)
const orderTotal = ref(0)
const editingPlan = ref<AdminMembershipPlan | null>(null)
const selectedOrder = ref<AdminPaymentOrder | null>(null)
const planFormRef = ref<FormInstance>()

const activeLoading = computed(() => activeTab.value === 'plans' ? planLoading.value : orderLoading.value)

const planQuery = reactive<AdminMembershipPlanQuery>({
  page: 1,
  pageSize: 20,
  keyword: '',
  status: ''
})

const orderQuery = reactive<AdminPaymentOrderQuery>({
  page: 1,
  pageSize: 20,
  keyword: '',
  status: '',
  provider: '',
  clientType: ''
})

const planForm = reactive<{
  name: string
  durationUnit: MembershipDurationUnit
  durationValue: number
  price: number
  currency: string
  status: MembershipPlanStatus
}>({
  name: '',
  durationUnit: 'month',
  durationValue: 1,
  price: 0,
  currency: 'CNY',
  status: 'active'
})

const planRules = computed<FormRules>(() => ({
  name: [
    { required: true, message: t('memberships.validation.nameRequired'), trigger: 'blur' },
    { max: 100, message: t('memberships.validation.nameTooLong'), trigger: 'blur' }
  ],
  durationUnit: [{ required: true, message: t('memberships.validation.durationUnitRequired'), trigger: 'change' }],
  durationValue: [{ required: true, message: t('memberships.validation.durationValueRequired'), trigger: 'change' }],
  price: [{ required: true, message: t('memberships.validation.priceRequired'), trigger: 'change' }],
  currency: [{ required: true, message: t('memberships.validation.currencyRequired'), trigger: 'blur' }]
}))

async function loadPlans() {
  planLoading.value = true
  try {
    const result = await fetchAdminMembershipPlans(planQuery)
    plans.value = result.records
    planTotal.value = result.total
  } finally {
    planLoading.value = false
  }
}

async function loadOrders() {
  orderLoading.value = true
  try {
    const result = await fetchAdminPaymentOrders(orderQuery)
    orders.value = result.records
    orderTotal.value = result.total
  } finally {
    orderLoading.value = false
  }
}

function reloadActive() {
  if (activeTab.value === 'plans') {
    void loadPlans()
    return
  }
  void loadOrders()
}

function handleTabChange() {
  reloadActive()
}

function searchPlans() {
  planQuery.page = 1
  void loadPlans()
}

function resetPlanFilters() {
  planQuery.keyword = ''
  planQuery.status = ''
  planQuery.page = 1
  void loadPlans()
}

function handlePlanPageSizeChange() {
  planQuery.page = 1
  void loadPlans()
}

function searchOrders() {
  orderQuery.page = 1
  void loadOrders()
}

function resetOrderFilters() {
  orderQuery.keyword = ''
  orderQuery.status = ''
  orderQuery.provider = ''
  orderQuery.clientType = ''
  orderQuery.page = 1
  void loadOrders()
}

function handleOrderPageSizeChange() {
  orderQuery.page = 1
  void loadOrders()
}

function openPlanDialog(plan?: AdminMembershipPlan) {
  editingPlan.value = plan || null
  planForm.name = plan?.name || ''
  planForm.durationUnit = plan?.durationUnit || 'month'
  planForm.durationValue = plan?.durationValue || 1
  planForm.price = Number(plan?.price || 0)
  planForm.currency = plan?.currency || 'CNY'
  planForm.status = plan?.status || 'active'
  planDialogVisible.value = true
}

async function submitPlan() {
  await planFormRef.value?.validate()
  planSubmitting.value = true
  try {
    const payload = {
      name: planForm.name.trim(),
      durationUnit: planForm.durationUnit,
      durationValue: planForm.durationValue,
      price: planForm.price,
      currency: planForm.currency.trim().toUpperCase(),
      status: planForm.status
    }
    if (editingPlan.value) {
      await updateAdminMembershipPlan(editingPlan.value.id, payload)
    } else {
      await createAdminMembershipPlan(payload)
    }
    planDialogVisible.value = false
    ElMessage.success(t('memberships.saved'))
    await loadPlans()
  } finally {
    planSubmitting.value = false
  }
}

async function togglePlanStatus(plan: AdminMembershipPlan) {
  const status: MembershipPlanStatus = plan.status === 'active' ? 'inactive' : 'active'
  const { value } = await ElMessageBox.prompt(
    t('memberships.statusReasonPlaceholder'),
    t('memberships.statusDialogTitle'),
    {
      confirmButtonText: t('memberships.submit'),
      cancelButtonText: t('memberships.cancel'),
      inputType: 'textarea',
      inputValidator: value => !value || value.length <= 1000 || t('memberships.validation.reasonTooLong')
    }
  )
  await updateAdminMembershipPlanStatus(plan.id, { status, reason: value || '' })
  ElMessage.success(t('memberships.saved'))
  await loadPlans()
}

async function openOrderDetail(orderId: number) {
  orderDrawerVisible.value = true
  orderDetailLoading.value = true
  try {
    selectedOrder.value = await fetchAdminPaymentOrderDetail(orderId)
  } finally {
    orderDetailLoading.value = false
  }
}

function orderStatusTag(status: PaymentOrderStatus) {
  if (status === 'paid') {
    return 'success'
  }
  if (status === 'pending') {
    return 'warning'
  }
  if (status === 'failed') {
    return 'danger'
  }
  return 'info'
}

function formatDuration(unit: MembershipDurationUnit, value: number, days: number) {
  return `${t(`memberships.durationUnits.${unit}`)} ${value} / ${t('memberships.days', { days })}`
}

function formatMoney(amount: number, currency: string) {
  try {
    return new Intl.NumberFormat(locale.value, {
      style: 'currency',
      currency
    }).format(Number(amount || 0))
  } catch {
    return `${Number(amount || 0).toFixed(2)} ${currency}`
  }
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

onMounted(loadPlans)
</script>

<style scoped>
.membership-page {
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

.admin-tabs {
  display: grid;
  gap: 14px;
}

.admin-tabs :deep(.el-tabs__header) {
  margin-bottom: 14px;
}

.filter-card :deep(.el-card__body) {
  padding-bottom: 0;
}

.filter-form,
.order-filter-form {
  display: grid;
  gap: 12px;
}

.filter-form {
  grid-template-columns: minmax(260px, 360px) 180px auto;
}

.order-filter-form {
  grid-template-columns: minmax(260px, 360px) 160px 160px 160px auto;
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

.pagination-row {
  justify-content: flex-end;
  padding-top: 16px;
}

.user-cell {
  display: grid;
  gap: 4px;
}

.user-cell strong {
  color: #172033;
}

.user-cell span {
  color: #64748b;
  font-size: 12px;
}

.form-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: 1fr 1fr;
}

.full-input {
  width: 100%;
}

.detail-drawer {
  min-height: 320px;
}

.detail-title {
  margin-bottom: 18px;
}

.detail-title h2 {
  color: #172033;
  font-size: 20px;
}

@media (max-width: 1100px) {
  .filter-form,
  .order-filter-form {
    grid-template-columns: 1fr 1fr;
  }

  .filter-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 720px) {
  .page-heading {
    align-items: flex-start;
    gap: 12px;
  }

  .filter-form,
  .order-filter-form,
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
