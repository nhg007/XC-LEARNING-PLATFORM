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
          <div class="quick-view-bar">
            <span>{{ t('memberships.exceptionQuickTitle') }}</span>
            <el-radio-group v-model="orderQuery.exceptionType" size="small" @change="handleOrderExceptionChange">
              <el-radio-button label="">{{ t('memberships.exceptionViews.allOrders') }}</el-radio-button>
              <el-radio-button
                v-for="option in orderExceptionOptions"
                :key="option.type"
                :label="option.type"
              >
                {{ option.label }}
              </el-radio-button>
            </el-radio-group>
          </div>
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
                <el-option :label="t('memberships.providers.offline')" value="offline" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="orderQuery.clientType" clearable :placeholder="t('memberships.clientFilter')">
                <el-option :label="t('memberships.clients.web')" value="web" />
                <el-option :label="t('memberships.clients.mobile')" value="mobile" />
                <el-option :label="t('memberships.clients.admin')" value="admin" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="orderQuery.exceptionType" clearable :placeholder="t('memberships.exceptionFilter')">
                <el-option :label="t('memberships.exceptionViews.allExceptions')" value="all" />
                <el-option :label="t('memberships.orderExceptions.pending_timeout')" value="pending_timeout" />
                <el-option :label="t('memberships.orderExceptions.callback_failed')" value="callback_failed" />
                <el-option :label="t('memberships.orderExceptions.amount_mismatch')" value="amount_mismatch" />
                <el-option :label="t('memberships.orderExceptions.provider_mismatch')" value="provider_mismatch" />
                <el-option :label="t('memberships.orderExceptions.membership_missing')" value="membership_missing" />
              </el-select>
            </el-form-item>
            <el-form-item class="filter-actions">
              <el-button type="primary" :icon="Search" @click="searchOrders">{{ t('common.search') }}</el-button>
              <el-button @click="resetOrderFilters">{{ t('memberships.reset') }}</el-button>
              <el-button v-if="canUpdateOrders" type="primary" plain :icon="Plus" @click="openOfflineOrderDialog">
                {{ t('memberships.actions.createOfflinePayment') }}
              </el-button>
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
            <el-table-column :label="t('memberships.columns.exception')" min-width="190">
              <template #default="{ row }">
                <div v-if="row.exceptionTypes?.length" class="exception-tags">
                  <el-tag
                    v-for="exceptionType in row.exceptionTypes"
                    :key="exceptionType"
                    :type="orderExceptionTag(exceptionType)"
                    effect="plain"
                  >
                    {{ t(`memberships.orderExceptions.${exceptionType}`) }}
                  </el-tag>
                </div>
                <span v-else>{{ t('common.empty') }}</span>
              </template>
            </el-table-column>
            <el-table-column :label="t('memberships.latestCallback')" min-width="180">
              <template #default="{ row }">
                <div v-if="row.latestNotificationProcessStatus" class="callback-cell">
                  <el-tag :type="notificationProcessTag(row.latestNotificationProcessStatus)" effect="plain">
                    {{ t(`memberships.processStatus.${row.latestNotificationProcessStatus}`) }}
                  </el-tag>
                  <span>{{ row.latestNotificationResultCode || t('common.empty') }}</span>
                </div>
                <span v-else>{{ t('common.empty') }}</span>
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

      <el-tab-pane :label="t('memberships.tabs.notifications')" name="notifications">
        <el-card shadow="never" class="filter-card">
          <el-form class="notification-filter-form" :model="notificationQuery" @submit.prevent>
            <el-form-item>
              <el-input
                v-model="notificationQuery.keyword"
                clearable
                :prefix-icon="Search"
                :placeholder="t('memberships.notificationKeyword')"
                @keyup.enter="searchNotifications"
              />
            </el-form-item>
            <el-form-item>
              <el-select v-model="notificationQuery.provider" clearable :placeholder="t('memberships.providerFilter')">
                <el-option :label="t('memberships.providers.wechat_pay')" value="wechat_pay" />
                <el-option :label="t('memberships.providers.alipay')" value="alipay" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select
                v-model="notificationQuery.processStatus"
                clearable
                :placeholder="t('memberships.notificationStatusFilter')"
              >
                <el-option :label="t('memberships.processStatus.handled')" value="handled" />
                <el-option :label="t('memberships.processStatus.ignored')" value="ignored" />
                <el-option :label="t('memberships.processStatus.failed')" value="failed" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select
                v-model="notificationQuery.signatureValid"
                clearable
                :placeholder="t('memberships.signatureFilter')"
              >
                <el-option :label="t('memberships.signature.valid')" :value="true" />
                <el-option :label="t('memberships.signature.invalid')" :value="false" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-input
                v-model="notificationQuery.resultCode"
                clearable
                :placeholder="t('memberships.resultCodeFilter')"
                @keyup.enter="searchNotifications"
              />
            </el-form-item>
            <el-form-item class="filter-actions">
              <el-button type="primary" :icon="Search" @click="searchNotifications">{{ t('common.search') }}</el-button>
              <el-button @click="resetNotificationFilters">{{ t('memberships.reset') }}</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" class="table-card">
          <template #header>
            <div class="card-header">
              <span>{{ t('memberships.notificationListTitle') }}</span>
              <span>{{ t('memberships.total', { total: notificationTotal }) }}</span>
            </div>
          </template>
          <el-table
            v-loading="notificationLoading"
            :data="notifications"
            row-key="id"
            border
            :empty-text="t('memberships.emptyNotifications')"
          >
            <el-table-column :label="t('memberships.receivedAt')" min-width="170">
              <template #default="{ row }">
                {{ formatDate(row.receivedAt) }}
              </template>
            </el-table-column>
            <el-table-column :label="t('memberships.columns.orderNo')" min-width="190">
              <template #default="{ row }">
                {{ row.orderNo || row.orderId || t('common.empty') }}
              </template>
            </el-table-column>
            <el-table-column :label="t('memberships.columns.user')" min-width="200">
              <template #default="{ row }">
                <div class="user-cell">
                  <strong>{{ row.userNickname || t('memberships.unnamed') }}</strong>
                  <span>{{ row.userEmail || row.userId || t('common.empty') }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column :label="t('memberships.columns.provider')" width="130">
              <template #default="{ row }">
                {{ t(`memberships.providers.${row.provider}`) }}
              </template>
            </el-table-column>
            <el-table-column :label="t('memberships.processStatusLabel')" width="120">
              <template #default="{ row }">
                <el-tag :type="notificationProcessTag(row.processStatus)">
                  {{ t(`memberships.processStatus.${row.processStatus}`) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="resultCode" :label="t('memberships.resultCode')" min-width="170" />
            <el-table-column :label="t('memberships.signatureValid')" width="120">
              <template #default="{ row }">
                <el-tag :type="row.signatureValid ? 'success' : 'danger'">
                  {{ row.signatureValid ? t('memberships.signature.valid') : t('memberships.signature.invalid') }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="resultMessage" :label="t('memberships.resultMessage')" min-width="220" show-overflow-tooltip />
            <el-table-column :label="t('memberships.columns.actions')" fixed="right" width="120">
              <template #default="{ row }">
                <el-button link type="primary" @click="openNotificationDetail(row)">
                  {{ t('memberships.actions.detail') }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-row">
            <el-pagination
              v-model:current-page="notificationQuery.page"
              v-model:page-size="notificationQuery.pageSize"
              :total="notificationTotal"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleNotificationPageSizeChange"
              @current-change="loadNotifications"
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

    <el-dialog v-model="offlineOrderDialogVisible" :title="t('memberships.createOfflinePaymentTitle')" width="620px">
      <el-form ref="offlineOrderFormRef" :model="offlineOrderForm" :rules="offlineOrderRules" label-position="top">
        <el-form-item :label="t('memberships.fields.userKeyword')" prop="userKeyword">
          <el-input
            v-model="offlineOrderForm.userKeyword"
            :placeholder="t('memberships.fields.userKeywordPlaceholder')"
            maxlength="128"
          />
        </el-form-item>
        <el-form-item :label="t('memberships.fields.offlinePlan')" prop="planId">
          <el-select
            v-model="offlineOrderForm.planId"
            class="full-input"
            filterable
            :loading="offlinePlansLoading"
            :placeholder="t('memberships.fields.offlinePlanPlaceholder')"
          >
            <el-option
              v-for="plan in offlinePlans"
              :key="plan.id"
              :label="`${plan.name} · ${formatMoney(plan.price, plan.currency)}`"
              :value="plan.id"
            />
          </el-select>
        </el-form-item>
        <div class="form-grid">
          <el-form-item :label="t('memberships.fields.amount')" prop="amount">
            <el-input-number v-model="offlineOrderForm.amount" class="full-input" :min="0" :precision="2" :step="10" />
          </el-form-item>
          <el-form-item :label="t('memberships.fields.currency')" prop="currency">
            <el-input v-model="offlineOrderForm.currency" maxlength="10" />
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item :label="t('memberships.fields.paidAt')" prop="paidAt">
            <el-date-picker
              v-model="offlineOrderForm.paidAt"
              class="full-input"
              type="datetime"
              :placeholder="t('memberships.fields.paidAtPlaceholder')"
            />
          </el-form-item>
          <el-form-item :label="t('memberships.fields.offlineTradeNo')" prop="offlineTradeNo">
            <el-input v-model="offlineOrderForm.offlineTradeNo" maxlength="128" :placeholder="t('memberships.fields.offlineTradeNoPlaceholder')" />
          </el-form-item>
        </div>
        <el-form-item :label="t('memberships.fields.remark')" prop="remark">
          <el-input
            v-model="offlineOrderForm.remark"
            type="textarea"
            :rows="3"
            maxlength="1000"
            show-word-limit
            :placeholder="t('memberships.fields.remarkPlaceholder')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="offlineOrderDialogVisible = false">{{ t('memberships.cancel') }}</el-button>
        <el-button type="primary" :loading="offlineOrderSubmitting" @click="submitOfflineOrder">
          {{ t('memberships.submit') }}
        </el-button>
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
            <div class="detail-actions">
              <el-tag :type="orderStatusTag(selectedOrder.status)">
                {{ t(`memberships.orderStatus.${selectedOrder.status}`) }}
              </el-tag>
              <el-button
                v-if="selectedOrder.status === 'pending' && canUpdateOrders"
                type="danger"
                plain
                :icon="CircleClose"
                :loading="orderFailSubmitting"
                @click="markOrderFailed"
              >
                {{ t('memberships.actions.markFailed') }}
              </el-button>
            </div>
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
            <el-descriptions-item :label="t('memberships.columns.exception')" :span="2">
              <div v-if="selectedOrder.exceptionTypes?.length" class="exception-tags">
                <el-tag
                  v-for="exceptionType in selectedOrder.exceptionTypes"
                  :key="exceptionType"
                  :type="orderExceptionTag(exceptionType)"
                  effect="plain"
                >
                  {{ t(`memberships.orderExceptions.${exceptionType}`) }}
                </el-tag>
              </div>
              <span v-else>{{ t('common.empty') }}</span>
            </el-descriptions-item>
            <el-descriptions-item :label="t('memberships.latestCallback')" :span="2">
              <div v-if="selectedOrder.latestNotificationProcessStatus" class="callback-cell">
                <el-tag :type="notificationProcessTag(selectedOrder.latestNotificationProcessStatus)" effect="plain">
                  {{ t(`memberships.processStatus.${selectedOrder.latestNotificationProcessStatus}`) }}
                </el-tag>
                <span>{{ selectedOrder.latestNotificationResultCode || t('common.empty') }}</span>
                <span>{{ formatDate(selectedOrder.latestNotificationReceivedAt) }}</span>
              </div>
              <span v-else>{{ t('common.empty') }}</span>
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
          <el-alert
            v-if="latestOrderFailureReason"
            class="failure-reason-alert"
            type="warning"
            show-icon
            :closable="false"
            :title="t('memberships.failureReason')"
            :description="latestOrderFailureReason"
          />
          <section class="callback-section">
            <div class="section-header">
              <h3>{{ t('memberships.orderOperationLogsTitle') }}</h3>
              <el-button link type="primary" :loading="orderOperationLogLoading" @click="loadOrderOperationLogs(selectedOrder.id)">
                {{ t('common.refresh') }}
              </el-button>
            </div>
            <el-table
              v-loading="orderOperationLogLoading"
              :data="orderOperationLogs"
              row-key="id"
              border
              size="small"
              :empty-text="t('memberships.emptyOrderOperationLogs')"
            >
              <el-table-column :label="t('memberships.columns.createdAt')" min-width="160">
                <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
              </el-table-column>
              <el-table-column :label="t('memberships.columns.action')" min-width="170">
                <template #default="{ row }">{{ operationActionLabel(row.action) }}</template>
              </el-table-column>
              <el-table-column :label="t('memberships.columns.admin')" width="110">
                <template #default="{ row }">{{ row.adminUserId || t('common.empty') }}</template>
              </el-table-column>
              <el-table-column :label="t('memberships.columns.reason')" min-width="220" show-overflow-tooltip>
                <template #default="{ row }">{{ operationLogReason(row) || t('common.empty') }}</template>
              </el-table-column>
            </el-table>
          </section>
          <section class="callback-section">
            <div class="section-header">
              <h3>{{ t('memberships.orderCallbacksTitle') }}</h3>
              <el-button link type="primary" :loading="orderNotificationLoading" @click="loadOrderNotifications(selectedOrder.id)">
                {{ t('common.refresh') }}
              </el-button>
            </div>
            <el-table
              v-loading="orderNotificationLoading"
              :data="orderNotifications"
              row-key="id"
              border
              size="small"
              :empty-text="t('memberships.emptyOrderCallbacks')"
            >
              <el-table-column :label="t('memberships.receivedAt')" min-width="160">
                <template #default="{ row }">{{ formatDate(row.receivedAt) }}</template>
              </el-table-column>
              <el-table-column :label="t('memberships.processStatusLabel')" width="110">
                <template #default="{ row }">
                  <el-tag :type="notificationProcessTag(row.processStatus)">
                    {{ t(`memberships.processStatus.${row.processStatus}`) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="resultCode" :label="t('memberships.resultCode')" min-width="150" />
              <el-table-column prop="resultMessage" :label="t('memberships.resultMessage')" min-width="180" show-overflow-tooltip />
              <el-table-column :label="t('memberships.columns.actions')" width="90">
                <template #default="{ row }">
                  <el-button link type="primary" @click="openNotificationDetail(row)">
                    {{ t('memberships.actions.detail') }}
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </section>
        </template>
      </div>
    </el-drawer>

    <el-drawer v-model="notificationDrawerVisible" :title="t('memberships.notificationDetailTitle')" size="720px">
      <div class="detail-drawer">
        <template v-if="selectedNotification">
          <div class="detail-title">
            <div>
              <h2>{{ selectedNotification.resultCode || t('common.empty') }}</h2>
              <p>{{ selectedNotification.orderNo || selectedNotification.orderId || t('common.empty') }}</p>
            </div>
            <el-tag :type="notificationProcessTag(selectedNotification.processStatus)">
              {{ t(`memberships.processStatus.${selectedNotification.processStatus}`) }}
            </el-tag>
          </div>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="ID">{{ selectedNotification.id }}</el-descriptions-item>
            <el-descriptions-item :label="t('memberships.receivedAt')">
              {{ formatDate(selectedNotification.receivedAt) }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('memberships.columns.provider')">
              {{ t(`memberships.providers.${selectedNotification.provider}`) }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('memberships.providerTradeNo')">
              {{ selectedNotification.providerTradeNo || t('common.empty') }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('memberships.signatureValid')">
              {{ selectedNotification.signatureValid ? t('memberships.signature.valid') : t('memberships.signature.invalid') }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('memberships.resultCode')">
              {{ selectedNotification.resultCode || t('common.empty') }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('memberships.resultMessage')" :span="2">
              {{ selectedNotification.resultMessage || t('common.empty') }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('memberships.columns.user')">
              {{ selectedNotification.userEmail || selectedNotification.userId || t('common.empty') }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('memberships.columns.amount')">
              {{ formatMoney(selectedNotification.orderAmount, selectedNotification.currency) }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('memberships.orderStatusLabel')">
              {{ selectedNotification.orderStatus ? t(`memberships.orderStatus.${selectedNotification.orderStatus}`) : t('common.empty') }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('memberships.columns.planName')">
              {{ selectedNotification.planName || selectedNotification.planId || t('common.empty') }}
            </el-descriptions-item>
          </el-descriptions>
          <section class="payload-section">
            <h3>{{ t('memberships.notifyPayload') }}</h3>
            <pre>{{ formatPayload(selectedNotification.notifyPayload) }}</pre>
          </section>
        </template>
      </div>
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { CircleClose, Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  createAdminOfflinePaymentOrder,
  createAdminMembershipPlan,
  fetchAdminMembershipPlans,
  fetchAdminOrderExceptionSummary,
  fetchAdminPaymentNotifications,
  fetchAdminPaymentOrderDetail,
  fetchAdminPaymentOrderOperationLogs,
  fetchAdminPaymentOrders,
  markAdminPaymentOrderFailed,
  updateAdminMembershipPlan,
  updateAdminMembershipPlanStatus
} from '@/api/memberships'
import { useSessionStore } from '@/stores/session'
import type {
  AdminCreateOfflinePaymentOrderPayload,
  AdminMembershipPlan,
  AdminMembershipPlanQuery,
  AdminOrderExceptionSummary,
  AdminOperationLog,
  AdminPaymentNotification,
  AdminPaymentNotificationQuery,
  AdminPaymentOrder,
  AdminPaymentOrderQuery,
  MembershipDurationUnit,
  MembershipPlanStatus,
  PaymentClientType,
  PaymentNotificationProcessStatus,
  PaymentOrderExceptionType,
  PaymentProvider,
  PaymentOrderStatus
} from '@/types/api'

const { t, locale } = useI18n()
const session = useSessionStore()
const route = useRoute()

const activeTab = ref<'plans' | 'orders' | 'notifications'>('plans')
const planLoading = ref(false)
const orderLoading = ref(false)
const notificationLoading = ref(false)
const planSubmitting = ref(false)
const orderDetailLoading = ref(false)
const orderFailSubmitting = ref(false)
const offlineOrderSubmitting = ref(false)
const offlinePlansLoading = ref(false)
const orderNotificationLoading = ref(false)
const orderOperationLogLoading = ref(false)
const planDialogVisible = ref(false)
const offlineOrderDialogVisible = ref(false)
const orderDrawerVisible = ref(false)
const notificationDrawerVisible = ref(false)
const plans = ref<AdminMembershipPlan[]>([])
const offlinePlans = ref<AdminMembershipPlan[]>([])
const orders = ref<AdminPaymentOrder[]>([])
const notifications = ref<AdminPaymentNotification[]>([])
const orderNotifications = ref<AdminPaymentNotification[]>([])
const orderOperationLogs = ref<AdminOperationLog[]>([])
const orderExceptionSummary = ref<AdminOrderExceptionSummary>({
  allExceptions: 0,
  pendingTimeout: 0,
  callbackFailed: 0,
  amountMismatch: 0,
  providerMismatch: 0,
  membershipMissing: 0
})
const planTotal = ref(0)
const orderTotal = ref(0)
const notificationTotal = ref(0)
const editingPlan = ref<AdminMembershipPlan | null>(null)
const selectedOrder = ref<AdminPaymentOrder | null>(null)
const selectedNotification = ref<AdminPaymentNotification | null>(null)
const planFormRef = ref<FormInstance>()
const offlineOrderFormRef = ref<FormInstance>()

const activeLoading = computed(() => {
  if (activeTab.value === 'plans') {
    return planLoading.value
  }
  if (activeTab.value === 'orders') {
    return orderLoading.value
  }
  return notificationLoading.value
})
const canUpdateOrders = computed(() => {
  const permissions = session.profile?.permissions || []
  return permissions.includes('admin:*') || permissions.includes('admin:orders:update')
})
const latestOrderFailureReason = computed(() => {
  return orderOperationLogs.value
    .map(log => operationLogReason(log))
    .find(reason => Boolean(reason)) || ''
})
const orderExceptionOptions = computed(() => [
  {
    type: 'all' as PaymentOrderExceptionType,
    label: t('memberships.exceptionViewWithCount', {
      label: t('memberships.exceptionViews.allExceptions'),
      count: orderExceptionSummary.value.allExceptions
    })
  },
  {
    type: 'pending_timeout' as PaymentOrderExceptionType,
    label: t('memberships.exceptionViewWithCount', {
      label: t('memberships.orderExceptions.pending_timeout'),
      count: orderExceptionSummary.value.pendingTimeout
    })
  },
  {
    type: 'callback_failed' as PaymentOrderExceptionType,
    label: t('memberships.exceptionViewWithCount', {
      label: t('memberships.orderExceptions.callback_failed'),
      count: orderExceptionSummary.value.callbackFailed
    })
  },
  {
    type: 'amount_mismatch' as PaymentOrderExceptionType,
    label: t('memberships.exceptionViewWithCount', {
      label: t('memberships.orderExceptions.amount_mismatch'),
      count: orderExceptionSummary.value.amountMismatch
    })
  },
  {
    type: 'provider_mismatch' as PaymentOrderExceptionType,
    label: t('memberships.exceptionViewWithCount', {
      label: t('memberships.orderExceptions.provider_mismatch'),
      count: orderExceptionSummary.value.providerMismatch
    })
  },
  {
    type: 'membership_missing' as PaymentOrderExceptionType,
    label: t('memberships.exceptionViewWithCount', {
      label: t('memberships.orderExceptions.membership_missing'),
      count: orderExceptionSummary.value.membershipMissing
    })
  }
])

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
  clientType: '',
  exceptionType: ''
})

const notificationQuery = reactive<AdminPaymentNotificationQuery>({
  page: 1,
  pageSize: 20,
  keyword: '',
  provider: '',
  processStatus: '',
  resultCode: '',
  signatureValid: ''
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

const offlineOrderForm = reactive<{
  userKeyword: string
  planId: number | null
  amount: number
  currency: string
  paidAt: Date | null
  offlineTradeNo: string
  remark: string
}>({
  userKeyword: '',
  planId: null,
  amount: 0,
  currency: 'CNY',
  paidAt: new Date(),
  offlineTradeNo: '',
  remark: ''
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

const offlineOrderRules = computed<FormRules>(() => ({
  userKeyword: [
    { required: true, message: t('memberships.validation.userRequired'), trigger: 'blur' },
    { max: 128, message: t('memberships.validation.userTooLong'), trigger: 'blur' }
  ],
  planId: [{ required: true, message: t('memberships.validation.planRequired'), trigger: 'change' }],
  amount: [{ required: true, message: t('memberships.validation.amountRequired'), trigger: 'change' }],
  currency: [{ required: true, message: t('memberships.validation.currencyRequired'), trigger: 'blur' }],
  remark: [
    { required: true, message: t('memberships.validation.remarkRequired'), trigger: 'blur' },
    { max: 1000, message: t('memberships.validation.reasonTooLong'), trigger: 'blur' }
  ]
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
    const [result, summary] = await Promise.all([
      fetchAdminPaymentOrders(orderQuery),
      fetchAdminOrderExceptionSummary(orderQuery.pendingTimeoutMinutes)
    ])
    orders.value = result.records
    orderTotal.value = result.total
    orderExceptionSummary.value = summary
  } finally {
    orderLoading.value = false
  }
}

async function loadNotifications() {
  notificationLoading.value = true
  try {
    const result = await fetchAdminPaymentNotifications(notificationQuery)
    notifications.value = result.records
    notificationTotal.value = result.total
  } finally {
    notificationLoading.value = false
  }
}

function reloadActive() {
  if (activeTab.value === 'plans') {
    void loadPlans()
    return
  }
  if (activeTab.value === 'orders') {
    void loadOrders()
    return
  }
  void loadNotifications()
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

function handleOrderExceptionChange() {
  orderQuery.page = 1
  if (orderQuery.exceptionType) {
    orderQuery.status = ''
  }
  void loadOrders()
}

function resetOrderFilters() {
  orderQuery.keyword = ''
  orderQuery.status = ''
  orderQuery.provider = ''
  orderQuery.clientType = ''
  orderQuery.exceptionType = ''
  orderQuery.createdFrom = ''
  orderQuery.createdTo = ''
  orderQuery.pendingTimeoutMinutes = undefined
  orderQuery.page = 1
  void loadOrders()
}

function handleOrderPageSizeChange() {
  orderQuery.page = 1
  void loadOrders()
}

function searchNotifications() {
  notificationQuery.page = 1
  void loadNotifications()
}

function resetNotificationFilters() {
  notificationQuery.keyword = ''
  notificationQuery.provider = ''
  notificationQuery.processStatus = ''
  notificationQuery.resultCode = ''
  notificationQuery.signatureValid = ''
  notificationQuery.orderId = null
  notificationQuery.page = 1
  void loadNotifications()
}

function handleNotificationPageSizeChange() {
  notificationQuery.page = 1
  void loadNotifications()
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

async function loadOfflinePlans() {
  offlinePlansLoading.value = true
  try {
    const result = await fetchAdminMembershipPlans({
      page: 1,
      pageSize: 100,
      keyword: '',
      status: 'active'
    })
    offlinePlans.value = result.records
  } finally {
    offlinePlansLoading.value = false
  }
}

async function openOfflineOrderDialog() {
  offlineOrderForm.userKeyword = ''
  offlineOrderForm.planId = null
  offlineOrderForm.amount = 0
  offlineOrderForm.currency = 'CNY'
  offlineOrderForm.paidAt = new Date()
  offlineOrderForm.offlineTradeNo = ''
  offlineOrderForm.remark = ''
  offlineOrderDialogVisible.value = true
  await loadOfflinePlans()
  offlineOrderForm.planId = offlinePlans.value[0]?.id || null
  syncOfflinePlanDefaults()
  offlineOrderFormRef.value?.clearValidate()
}

function syncOfflinePlanDefaults() {
  const plan = offlinePlans.value.find(item => item.id === offlineOrderForm.planId)
  if (!plan) {
    return
  }
  offlineOrderForm.amount = Number(plan.price || 0)
  offlineOrderForm.currency = plan.currency || 'CNY'
}

async function submitOfflineOrder() {
  await offlineOrderFormRef.value?.validate()
  if (!offlineOrderForm.planId) {
    return
  }
  offlineOrderSubmitting.value = true
  try {
    const payload: AdminCreateOfflinePaymentOrderPayload = {
      userKeyword: offlineOrderForm.userKeyword.trim(),
      planId: offlineOrderForm.planId,
      amount: offlineOrderForm.amount,
      currency: offlineOrderForm.currency.trim().toUpperCase(),
      paidAt: offlineOrderForm.paidAt ? offlineOrderForm.paidAt.toISOString() : undefined,
      offlineTradeNo: offlineOrderForm.offlineTradeNo.trim() || undefined,
      remark: offlineOrderForm.remark.trim()
    }
    await createAdminOfflinePaymentOrder(payload)
    offlineOrderDialogVisible.value = false
    ElMessage.success(t('memberships.offlineOrderCreated'))
    await loadOrders()
  } finally {
    offlineOrderSubmitting.value = false
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
  orderNotifications.value = []
  orderOperationLogs.value = []
  try {
    selectedOrder.value = await fetchAdminPaymentOrderDetail(orderId)
    await Promise.all([loadOrderNotifications(orderId), loadOrderOperationLogs(orderId)])
  } finally {
    orderDetailLoading.value = false
  }
}

async function loadOrderNotifications(orderId: number) {
  orderNotificationLoading.value = true
  try {
    const result = await fetchAdminPaymentNotifications({
      page: 1,
      pageSize: 20,
      orderId
    })
    orderNotifications.value = result.records
  } finally {
    orderNotificationLoading.value = false
  }
}

async function loadOrderOperationLogs(orderId: number) {
  orderOperationLogLoading.value = true
  try {
    const result = await fetchAdminPaymentOrderOperationLogs(orderId, 1, 20)
    orderOperationLogs.value = result.records
  } finally {
    orderOperationLogLoading.value = false
  }
}

async function markOrderFailed() {
  if (!selectedOrder.value) {
    return
  }
  const order = selectedOrder.value
  const { value } = await ElMessageBox.prompt(
    t('memberships.failReasonPlaceholder'),
    t('memberships.markFailedTitle'),
    {
      confirmButtonText: t('memberships.actions.markFailed'),
      cancelButtonText: t('memberships.cancel'),
      inputType: 'textarea',
      inputValidator: value => {
        const reason = value.trim()
        if (!reason) {
          return t('memberships.validation.failReasonRequired')
        }
        return reason.length <= 1000 || t('memberships.validation.reasonTooLong')
      }
    }
  )
  orderFailSubmitting.value = true
  try {
    selectedOrder.value = await markAdminPaymentOrderFailed(order.id, { reason: value.trim() })
    ElMessage.success(t('memberships.orderMarkedFailed'))
    await Promise.all([loadOrders(), loadOrderNotifications(order.id), loadOrderOperationLogs(order.id)])
  } finally {
    orderFailSubmitting.value = false
  }
}

function openNotificationDetail(notification: AdminPaymentNotification) {
  selectedNotification.value = notification
  notificationDrawerVisible.value = true
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

function notificationProcessTag(status: PaymentNotificationProcessStatus) {
  if (status === 'handled') {
    return 'success'
  }
  if (status === 'failed') {
    return 'danger'
  }
  return 'info'
}

function orderExceptionTag(exceptionType: PaymentOrderExceptionType) {
  if (exceptionType === 'pending_timeout') {
    return 'warning'
  }
  if (exceptionType === 'membership_missing') {
    return 'danger'
  }
  if (exceptionType === 'amount_mismatch' || exceptionType === 'provider_mismatch') {
    return 'danger'
  }
  return 'info'
}

function operationActionLabel(action: string) {
  if (action === 'payment.order.offline_paid') {
    return t('memberships.operationActions.offlinePaid')
  }
  if (action === 'payment.order.mark_failed') {
    return t('memberships.operationActions.markFailed')
  }
  return action
}

function operationLogReason(log: AdminOperationLog) {
  const detail = parseObject(log.detail)
  const reason = detail.reason
  return typeof reason === 'string' ? reason : ''
}

function parseObject(value?: string | null): Record<string, unknown> {
  if (!value) {
    return {}
  }
  try {
    const result = JSON.parse(value)
    return result && typeof result === 'object' && !Array.isArray(result) ? result : {}
  } catch {
    return {}
  }
}

function formatDuration(unit: MembershipDurationUnit, value: number, days: number) {
  return `${t(`memberships.durationUnits.${unit}`)} ${value} / ${t('memberships.days', { days })}`
}

function formatMoney(amount?: number | null, currency?: string | null) {
  if (amount === null || amount === undefined || !currency) {
    return t('common.empty')
  }
  try {
    return new Intl.NumberFormat(locale.value, {
      style: 'currency',
      currency
    }).format(Number(amount || 0))
  } catch {
    return `${Number(amount || 0).toFixed(2)} ${currency}`
  }
}

function formatPayload(payload?: string | null) {
  if (!payload) {
    return t('common.empty')
  }
  try {
    return JSON.stringify(JSON.parse(payload), null, 2)
  } catch {
    return payload
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

function routeText(key: string) {
  const value = route.query[key]
  if (Array.isArray(value)) {
    return value[0] || ''
  }
  return value || ''
}

function routeNumber(key: string, fallback: number) {
  const value = Number(routeText(key))
  return Number.isInteger(value) && value > 0 ? value : fallback
}

function isMembershipTab(value: string): value is typeof activeTab.value {
  return value === 'plans' || value === 'orders' || value === 'notifications'
}

function isPlanStatus(value: string): value is MembershipPlanStatus {
  return value === 'active' || value === 'inactive'
}

function isOrderStatus(value: string): value is PaymentOrderStatus {
  return value === 'pending' || value === 'paid' || value === 'failed' || value === 'refunded'
}

function isProvider(value: string): value is PaymentProvider {
  return value === 'wechat_pay' || value === 'alipay' || value === 'offline'
}

function isClientType(value: string): value is PaymentClientType {
  return value === 'web' || value === 'mobile' || value === 'admin'
}

function isOrderExceptionType(value: string): value is PaymentOrderExceptionType {
  return (
    value === 'all' ||
    value === 'pending_timeout' ||
    value === 'callback_failed' ||
    value === 'amount_mismatch' ||
    value === 'provider_mismatch' ||
    value === 'membership_missing'
  )
}

function applyRouteQuery() {
  const tab = routeText('tab')
  const status = routeText('status')
  activeTab.value = isMembershipTab(tab) ? tab : 'plans'

  if (activeTab.value === 'plans') {
    planQuery.page = routeNumber('page', 1)
    planQuery.pageSize = routeNumber('pageSize', 20)
    planQuery.keyword = routeText('keyword')
    planQuery.status = isPlanStatus(status) ? status : ''
  }

  if (activeTab.value === 'orders') {
    const provider = routeText('provider')
    const clientType = routeText('clientType')
    const exceptionType = routeText('exceptionType')
    orderQuery.page = routeNumber('page', 1)
    orderQuery.pageSize = routeNumber('pageSize', 20)
    orderQuery.keyword = routeText('keyword')
    orderQuery.status = isOrderStatus(status) ? status : ''
    orderQuery.provider = isProvider(provider) ? provider : ''
    orderQuery.clientType = isClientType(clientType) ? clientType : ''
    orderQuery.exceptionType = isOrderExceptionType(exceptionType) ? exceptionType : ''
    orderQuery.createdFrom = routeText('createdFrom')
    orderQuery.createdTo = routeText('createdTo')
  }
}

watch(() => offlineOrderForm.planId, syncOfflinePlanDefaults)

onMounted(() => {
  applyRouteQuery()
  reloadActive()
})
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

.quick-view-bar {
  align-items: center;
  display: flex;
  gap: 12px;
  margin-bottom: 14px;
  overflow-x: auto;
  padding-bottom: 2px;
}

.quick-view-bar > span {
  color: #64748b;
  flex: 0 0 auto;
  font-size: 13px;
  font-weight: 600;
}

.quick-view-bar :deep(.el-radio-group) {
  flex-wrap: nowrap;
}

.filter-form,
.order-filter-form,
.notification-filter-form {
  display: grid;
  gap: 12px;
}

.filter-form {
  grid-template-columns: minmax(260px, 360px) 180px auto;
}

.order-filter-form {
  grid-template-columns: minmax(260px, 360px) 150px 150px 150px 170px auto;
}

.notification-filter-form {
  grid-template-columns: minmax(260px, 360px) 150px 150px 150px 180px auto;
}

.filter-actions {
  justify-content: flex-end;
}

.card-header,
.pagination-row,
.detail-title,
.detail-actions,
.section-header {
  align-items: center;
  display: flex;
  justify-content: space-between;
}

.detail-actions {
  gap: 10px;
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

.exception-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.callback-cell {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.callback-cell span {
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

.failure-reason-alert {
  margin-top: 18px;
}

.callback-section {
  display: grid;
  gap: 12px;
  margin-top: 18px;
}

.section-header h3 {
  color: #172033;
  font-size: 15px;
  margin: 0;
}

.payload-section {
  display: grid;
  gap: 10px;
  margin-top: 18px;
}

.payload-section h3 {
  color: #172033;
  font-size: 15px;
  margin: 0;
}

.payload-section pre {
  background: #0f172a;
  border-radius: 8px;
  color: #e2e8f0;
  font-family: "SFMono-Regular", Consolas, "Liberation Mono", monospace;
  font-size: 12px;
  line-height: 1.6;
  margin: 0;
  max-height: 320px;
  overflow: auto;
  padding: 14px;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 1100px) {
  .filter-form,
  .order-filter-form,
  .notification-filter-form {
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
  .notification-filter-form,
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
