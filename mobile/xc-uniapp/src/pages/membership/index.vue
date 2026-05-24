<template>
  <view class="page">
    <view class="hero">
      <view class="hero-top">
        <view class="hero-copy">
          <text class="eyebrow">{{ t('app.title') }}</text>
          <text class="title">{{ t('membership.title') }}</text>
          <text class="subtitle">{{ t('membership.subtitle') }}</text>
        </view>
        <LanguageSwitch variant="hero" />
      </view>
    </view>

    <view v-if="pageError" class="state-card error-card">
      <text>{{ pageError }}</text>
      <button class="secondary-button compact" size="mini" @click="loadPage">{{ t('common.retry') }}</button>
    </view>

    <template v-else>
      <view class="access-card">
        <view class="section-head">
          <text class="section-title">{{ t('membership.currentAccess') }}</text>
          <text class="access-pill">{{ accessLabel }}</text>
        </view>
        <view class="access-grid">
          <view class="metric">
            <text class="metric-label">{{ t('membership.remaining') }}</text>
            <text class="metric-value">{{ remainingLabel }}</text>
          </view>
          <view class="metric">
            <text class="metric-label">{{ t('membership.trialEndsAt') }}</text>
            <text class="metric-value small">{{ formatOptionalDate(status.trialEndsAt) }}</text>
          </view>
          <view class="metric">
            <text class="metric-label">{{ t('membership.membershipEndsAt') }}</text>
            <text class="metric-value small">{{ formatOptionalDate(status.membershipEndsAt) }}</text>
          </view>
        </view>
        <view class="benefit-list">
          <text>{{ t('membership.benefitPractice') }}</text>
          <text>{{ t('membership.benefitMatching') }}</text>
        </view>
      </view>

      <view class="offline-payment-card">
        <view class="section-head">
          <view>
            <text class="section-title">{{ t('membership.offlineTitle') }}</text>
            <text class="hint">{{ t('membership.offlineSubtitle') }}</text>
          </view>
          <text class="offline-pill">{{ t('membership.offlineBadge') }}</text>
        </view>

        <view class="offline-method-grid">
          <view v-for="method in offlinePaymentMethods" :key="method.key" class="offline-method" :class="`method-${method.key}`">
            <view class="method-copy">
              <text class="method-mark">{{ method.mark }}</text>
              <view>
                <text class="method-name">{{ method.label }}</text>
                <text class="method-hint">{{ method.qrUrl ? t('membership.qrPreviewHint') : t('membership.offlineQrMissing') }}</text>
              </view>
            </view>
            <image
              v-if="method.qrUrl"
              class="offline-qr"
              mode="aspectFit"
              show-menu-by-longpress
              :src="method.qrUrl"
              @click="previewPaymentQr(method.qrUrl)"
            />
          </view>
        </view>

        <text class="offline-note">{{ t('membership.offlineHint') }}</text>

        <view class="offline-contacts">
          <view v-for="contact in offlinePaymentContacts" :key="contact.key" class="contact-row">
            <view>
              <text class="contact-name">{{ contact.label }}</text>
              <text class="contact-value">{{ contact.value }}</text>
            </view>
            <button class="secondary-button contact-copy" size="mini" @click="copyOfflineContact(contact.value)">
              {{ t('membership.copyContact') }}
            </button>
          </view>
        </view>
      </view>

      <view v-if="showPurchasePanel" class="section">
        <view class="section-head">
          <text class="section-title">{{ t('membership.plans') }}</text>
          <text v-if="loading" class="muted">{{ t('common.loading') }}</text>
        </view>
        <text class="hint">{{ t('membership.plansHint') }}</text>

        <view v-if="loading && plans.length === 0" class="state-card">{{ t('common.loading') }}</view>

        <view v-else-if="plans.length === 0" class="empty-card">
          <text>{{ t('membership.emptyPlans') }}</text>
        </view>

        <view v-for="plan in plans" :key="plan.id" class="plan-card" :class="{ featured: isFeaturedPlan(plan) }">
          <view class="plan-head">
            <view>
              <text class="plan-name">{{ plan.name }}</text>
              <text class="plan-duration">{{ durationLabel(plan) }}</text>
            </view>
            <view class="price-box">
              <text v-if="isFeaturedPlan(plan)" class="featured-pill">{{ t('membership.bestValue') }}</text>
              <text class="price">{{ formatPrice(plan.price, plan.currency) }}</text>
            </view>
          </view>
          <view class="benefits">
            <text>{{ t('membership.fullAccessBenefit') }}</text>
            <text>{{ t('membership.validFor', { count: plan.durationDays }) }}</text>
          </view>
          <view class="pay-grid">
            <button
              v-for="provider in providers"
              :key="provider"
              class="pay-button"
              :class="`provider-${provider}`"
              :loading="isCreating(plan.id, provider)"
              :disabled="creating || orderLoading || paying"
              @click="createOrder(plan.id, provider)"
            >
              <image class="pay-icon" mode="aspectFit" :src="providerIcon(provider)" />
              <text class="sr-only">{{ t('membership.payWith', { provider: providerLabel(provider) }) }}</text>
            </button>
          </view>
        </view>
      </view>

      <view v-if="showPurchasePanel" class="order-card" :class="order ? `status-${order.status}` : ''">
        <view class="section-head">
          <text class="section-title">{{ t('membership.orderTitle') }}</text>
          <text v-if="order" class="order-status">{{ statusLabel(order.status) }}</text>
        </view>

        <view v-if="orderLoading && !order" class="order-empty">
          <text>{{ t('common.loading') }}</text>
        </view>

        <view v-else-if="!order" class="order-empty">
          <text>{{ t('membership.noOrder') }}</text>
        </view>

        <template v-else>
          <view class="order-list">
            <view class="order-row">
              <text>{{ t('membership.orderNo') }}</text>
              <text class="row-value">{{ order.orderNo }}</text>
            </view>
            <view class="order-row">
              <text>{{ t('membership.orderPlan') }}</text>
              <text class="row-value">{{ order.planName || '-' }}</text>
            </view>
            <view class="order-row">
              <text>{{ t('membership.orderAmount') }}</text>
              <text class="row-value">{{ formatPrice(order.amount, order.currency) }}</text>
            </view>
            <view class="order-row">
              <text>{{ t('membership.orderProvider') }}</text>
              <text class="row-value">{{ providerLabel(order.provider) }}</text>
            </view>
            <view class="order-row">
              <text>{{ t('membership.orderStatus') }}</text>
              <text class="row-value">{{ statusLabel(order.status) }}</text>
            </view>
          </view>

          <view v-if="order.paymentUrl" class="payment-link">
            <text class="payment-label">{{ t('membership.paymentLink') }}</text>
            <text class="payment-url">{{ order.paymentUrl }}</text>
            <button class="secondary-button" @click="copyPaymentLink">{{ t('membership.copyPaymentLink') }}</button>
          </view>

          <text v-if="order.mockPayment && order.status === 'pending'" class="mock-notice">{{ t('membership.mockNotice') }}</text>

          <view v-if="orderNotice" class="order-notice" :class="`notice-${order.status}`">
            <text>{{ orderNotice }}</text>
            <text v-if="polling && order.status === 'pending'" class="polling-label">{{ t('membership.autoRefreshing') }}</text>
          </view>

          <view class="order-actions" :class="{ single: !(order.mockPayment && order.status === 'pending') }">
            <button class="secondary-button" :loading="orderLoading" :disabled="paying" @click="refreshOrder">
              {{ t('membership.refreshOrder') }}
            </button>
            <button
              v-if="order.mockPayment && order.status === 'pending'"
              class="primary-button"
              :loading="paying"
              :disabled="orderLoading"
              @click="markPaid"
            >
              {{ t('membership.simulatePaid') }}
            </button>
          </view>
        </template>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { onHide, onLoad, onPullDownRefresh, onShow, onUnload } from '@dcloudio/uni-app'
import LanguageSwitch from '../../components/LanguageSwitch.vue'
import { createPaymentOrder, fetchMembershipPlans, fetchMembershipStatus, fetchPaymentOrder, simulatePayment } from '../../api/membership'
import { applyTabBarLocale, setPageTitle, useI18n } from '../../i18n'
import type { MembershipPlan, MembershipStatus, PaymentOrder, PaymentOrderStatus, PaymentProvider } from '../../types/api'
import { requireLogin } from '../../utils/navigation'
import { clearLatestPaymentOrderNo, getLatestPaymentOrderNo, setLatestPaymentOrderNo } from '../../utils/storage'

const { locale, t } = useI18n()
const providers: PaymentProvider[] = ['wechat_pay', 'alipay']
const staticBase = normalizeAssetBase(import.meta.env.BASE_URL)
const showPurchasePanel = false
const providerIcons: Record<PaymentProvider, string> = {
  wechat_pay: `${staticBase}static/payment/wechat-pay.svg`,
  alipay: `${staticBase}static/payment/alipay.svg`
}

type OfflineContactKey = 'telegram' | 'whatsapp' | 'vk'
type OfflineMethodKey = 'wechat' | 'alipay'

const loading = ref(false)
const pageError = ref('')
const creating = ref(false)
const orderLoading = ref(false)
const paying = ref(false)
const polling = ref(false)
const creatingPlanId = ref<number | null>(null)
const creatingProvider = ref<PaymentProvider | null>(null)
const plans = ref<MembershipPlan[]>([])
const order = ref<PaymentOrder | null>(null)
const status = ref<MembershipStatus>({
  accessLevel: 'free',
  fullAccess: false,
  trialEndsAt: null,
  membershipEndsAt: null,
  remainingSeconds: 0
})

const accessLabel = computed(() => {
  if (status.value.accessLevel === 'member') {
    return t('home.member')
  }
  if (status.value.accessLevel === 'trial') {
    return t('home.trial')
  }
  return t('home.free')
})

const remainingLabel = computed(() => {
  const days = Math.ceil(status.value.remainingSeconds / 86400)
  return t('common.days', { days: Math.max(0, days) })
})

const featuredPlanId = computed(() => {
  if (plans.value.length < 2) {
    return null
  }
  return plans.value.reduce((best, plan) => (plan.durationDays > best.durationDays ? plan : best), plans.value[0]).id
})

const orderNotice = computed(() => {
  if (!order.value) {
    return ''
  }
  if (order.value.status === 'pending') {
    return t('membership.pendingNotice')
  }
  if (order.value.status === 'paid') {
    return t('membership.paidNotice')
  }
  if (order.value.status === 'refunded') {
    return t('membership.refundedNotice')
  }
  return t('membership.failedNotice')
})

const offlinePaymentMethods = computed(() => [
  {
    key: 'wechat' as OfflineMethodKey,
    label: t('membership.provider.wechat_pay'),
    mark: '微',
    qrUrl: `${staticBase}static/payment/wechat.jpg`
  },
  {
    key: 'alipay' as OfflineMethodKey,
    label: t('membership.provider.alipay'),
    mark: '支',
    qrUrl: `${staticBase}static/payment/alipay.jpg`
  }
])

const offlinePaymentContacts = computed(() => [
  {
    key: 'telegram' as OfflineContactKey,
    label: 'Telegram',
    value: '+8618605454289'
  },
  {
    key: 'whatsapp' as OfflineContactKey,
    label: 'WhatsApp',
    value: '+8619953594090'
  },
  {
    key: 'vk' as OfflineContactKey,
    label: 'VK',
    value: 'id305390341'
  }
].filter((item) => item.value))

let orderPollTimer: ReturnType<typeof setInterval> | null = null
let orderPollFailureCount = 0

onLoad(() => {
  setPageTitle('membership.title')
})

onShow(() => {
  applyTabBarLocale()
  setPageTitle('membership.title')
  if (!requireLogin()) {
    return
  }
  void loadPage()
})

watch(locale, () => {
  setPageTitle('membership.title')
})

onPullDownRefresh(() => {
  void loadPage().finally(() => uni.stopPullDownRefresh())
})

onHide(() => {
  stopOrderPolling()
})

onUnload(() => {
  stopOrderPolling()
})

async function loadPage() {
  loading.value = true
  pageError.value = ''
  try {
    if (showPurchasePanel) {
      const [membershipStatus, membershipPlans] = await Promise.all([
        fetchMembershipStatus(),
        fetchMembershipPlans()
      ])
      status.value = membershipStatus
      plans.value = membershipPlans
      await restoreLatestOrder()
      return
    }
    status.value = await fetchMembershipStatus()
    plans.value = []
    order.value = null
    stopOrderPolling()
  } catch {
    pageError.value = t('membership.loadFailed')
  } finally {
    loading.value = false
  }
}

async function createOrder(planId: number, provider: PaymentProvider) {
  creating.value = true
  creatingPlanId.value = planId
  creatingProvider.value = provider
  try {
    handleOrderUpdated(await createPaymentOrder({ planId, provider, clientType: 'mobile' }))
    void uni.showToast({ icon: 'success', title: t('membership.createSuccess') })
  } catch {
    void uni.showToast({ icon: 'none', title: t('membership.createFailed') })
  } finally {
    creating.value = false
    creatingPlanId.value = null
    creatingProvider.value = null
  }
}

async function refreshOrder() {
  if (!order.value) {
    return
  }
  try {
    await syncCurrentOrder(true)
  } catch {
    void uni.showToast({ icon: 'none', title: t('membership.refreshFailed') })
  }
}

async function syncCurrentOrder(showLoading = false) {
  if (!order.value) {
    return
  }
  if (showLoading) {
    orderLoading.value = true
  }
  try {
    handleOrderUpdated(await fetchPaymentOrder(order.value.orderNo, { silent: !showLoading }))
    orderPollFailureCount = 0
  } catch {
    if (!showLoading) {
      orderPollFailureCount += 1
      if (orderPollFailureCount >= 3) {
        stopOrderPolling()
      }
    } else {
      throw new Error('refresh failed')
    }
  } finally {
    if (showLoading) {
      orderLoading.value = false
    }
  }
}

async function restoreLatestOrder() {
  const orderNo = getLatestPaymentOrderNo()
  if (!orderNo || order.value?.orderNo === orderNo) {
    syncOrderPolling()
    return
  }
  orderLoading.value = true
  try {
    handleOrderUpdated(await fetchPaymentOrder(orderNo))
  } catch {
    clearLatestPaymentOrderNo()
    stopOrderPolling()
  } finally {
    orderLoading.value = false
  }
}

async function markPaid() {
  if (!order.value) {
    return
  }
  paying.value = true
  try {
    handleOrderUpdated(await simulatePayment(order.value.orderNo))
    void uni.showToast({ icon: 'success', title: t('membership.paidSuccess') })
  } catch {
    void uni.showToast({ icon: 'none', title: t('membership.paidFailed') })
  } finally {
    paying.value = false
  }
}

function handleOrderUpdated(nextOrder: PaymentOrder) {
  orderPollFailureCount = 0
  order.value = nextOrder
  if (nextOrder.status === 'pending') {
    setLatestPaymentOrderNo(nextOrder.orderNo)
  } else {
    clearLatestPaymentOrderNo()
  }
  if (nextOrder.status === 'paid') {
    void refreshMembershipStatus()
  }
  syncOrderPolling()
}

async function refreshMembershipStatus() {
  try {
    status.value = await fetchMembershipStatus()
  } catch {
    // Payment success already updated the order; the next page refresh can recover status.
  }
}

function syncOrderPolling() {
  if (order.value?.status === 'pending') {
    startOrderPolling()
    return
  }
  stopOrderPolling()
}

function startOrderPolling() {
  if (orderPollTimer) {
    polling.value = true
    return
  }
  polling.value = true
  orderPollTimer = setInterval(() => {
    void syncCurrentOrder(false)
  }, 8000)
}

function stopOrderPolling() {
  if (orderPollTimer) {
    clearInterval(orderPollTimer)
    orderPollTimer = null
  }
  polling.value = false
}

function isCreating(planId: number, provider: PaymentProvider) {
  return creating.value && creatingPlanId.value === planId && creatingProvider.value === provider
}

function isFeaturedPlan(plan: MembershipPlan) {
  return plan.id === featuredPlanId.value
}

function providerLabel(provider: PaymentProvider) {
  return t(`membership.provider.${provider}`)
}

function providerIcon(provider: PaymentProvider) {
  return providerIcons[provider]
}

function normalizeAssetBase(value: string | undefined) {
  const base = value || '/'
  return base.endsWith('/') ? base : `${base}/`
}

function statusLabel(value: PaymentOrderStatus) {
  return t(`membership.status.${value}`)
}

function durationLabel(plan: MembershipPlan) {
  return t(`membership.duration.${plan.durationUnit}`, {
    count: plan.durationValue,
    days: plan.durationDays
  })
}

function formatPrice(price: number, currency: string) {
  const amount = Number(price || 0).toFixed(2)
  if (currency === 'CNY') {
    return `¥${amount}`
  }
  return `${amount} ${currency}`
}

function formatOptionalDate(value: string | null) {
  if (!value) {
    return '-'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return '-'
  }
  const month = pad(date.getMonth() + 1)
  const day = pad(date.getDate())
  const hour = pad(date.getHours())
  const minute = pad(date.getMinutes())
  return `${date.getFullYear()}/${month}/${day} ${hour}:${minute}`
}

function pad(value: number) {
  return String(value).padStart(2, '0')
}

function copyPaymentLink() {
  if (!order.value?.paymentUrl) {
    return
  }
  uni.setClipboardData({
    data: order.value.paymentUrl,
    success() {
      void uni.showToast({ icon: 'success', title: t('membership.paymentLinkCopied') })
    }
  })
}

function copyOfflineContact(value: string) {
  uni.setClipboardData({
    data: value,
    success() {
      void uni.showToast({ icon: 'success', title: t('membership.contactCopied') })
    }
  })
}

function previewPaymentQr(url: string) {
  if (!url) {
    return
  }
  uni.previewImage({
    current: url,
    urls: [url]
  })
}
</script>

<style scoped>
.page {
  background: #eef5f7;
  min-height: 100vh;
  padding: 0 24rpx 36rpx;
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

.hero-top {
  align-items: flex-start;
  display: flex;
  gap: 20rpx;
  justify-content: space-between;
}

.hero-copy {
  flex: 1;
  min-width: 0;
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
  font-weight: 700;
  line-height: 1.15;
  margin-top: 10rpx;
}

.subtitle {
  color: #cbd5e1;
  display: block;
  font-size: 26rpx;
  line-height: 1.5;
  margin-top: 14rpx;
}

.access-card,
.offline-payment-card,
.plan-card,
.order-card,
.state-card,
.empty-card {
  background: #ffffff;
  border: 1px solid #d7e2ea;
  border-radius: 24rpx;
  box-shadow: 0 12rpx 36rpx rgba(15, 23, 42, 0.06);
  box-sizing: border-box;
  padding: 24rpx;
}

.state-card {
  color: #64748b;
  display: flex;
  flex-direction: column;
  font-size: 26rpx;
  gap: 18rpx;
  margin-top: 18rpx;
  padding: 34rpx 24rpx;
}

.error-card {
  background: #fff7ed;
  border-color: #fed7aa;
  color: #9a3412;
}

.section {
  margin-top: 28rpx;
}

.section-head {
  align-items: center;
  display: flex;
  gap: 16rpx;
  justify-content: space-between;
}

.section-title {
  color: #102033;
  display: block;
  font-size: 34rpx;
  font-weight: 800;
}

.access-pill,
.featured-pill,
.order-status {
  align-items: center;
  border-radius: 999rpx;
  display: flex;
  font-size: 22rpx;
  font-weight: 800;
  justify-content: center;
  line-height: 1;
  min-height: 46rpx;
  padding: 0 18rpx;
}

.access-pill {
  background: #ccfbf1;
  color: #14796f;
}

.access-grid {
  display: grid;
  gap: 14rpx;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-top: 22rpx;
}

.benefit-list {
  border-top: 1px solid #e2e8f0;
  color: #475569;
  display: grid;
  font-size: 24rpx;
  gap: 10rpx;
  line-height: 1.45;
  margin-top: 22rpx;
  padding-top: 18rpx;
}

.offline-payment-card {
  margin-top: 22rpx;
}

.offline-pill {
  align-items: center;
  background: #e8fbf5;
  border: 1px solid #bdebdc;
  border-radius: 999rpx;
  color: #14796f;
  display: flex;
  font-size: 22rpx;
  font-weight: 900;
  min-height: 48rpx;
  padding: 0 18rpx;
}

.offline-method-grid {
  display: grid;
  gap: 16rpx;
  grid-template-columns: 1fr;
  margin-top: 22rpx;
}

.offline-method {
  background: #f8fafc;
  border: 1px solid #dbe5ee;
  border-radius: 20rpx;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  min-width: 0;
  padding: 16rpx;
}

.method-wechat {
  background: linear-gradient(180deg, #f0fdf7, #ffffff);
  border-color: #bdebdc;
}

.method-alipay {
  background: linear-gradient(180deg, #eff6ff, #ffffff);
  border-color: #bdd7ff;
}

.method-copy {
  align-items: center;
  display: flex;
  gap: 14rpx;
  min-width: 0;
}

.method-mark {
  align-items: center;
  border-radius: 16rpx;
  color: #ffffff;
  display: flex;
  flex: 0 0 58rpx;
  font-size: 28rpx;
  font-weight: 900;
  height: 58rpx;
  justify-content: center;
  width: 58rpx;
}

.method-wechat .method-mark {
  background: #13a86f;
}

.method-alipay .method-mark {
  background: #1677ff;
}

.method-name {
  color: #102033;
  display: block;
  font-size: 27rpx;
  font-weight: 900;
  line-height: 1.25;
}

.method-hint {
  color: #64748b;
  display: block;
  font-size: 22rpx;
  line-height: 1.35;
  margin-top: 4rpx;
}

.offline-qr {
  background: #ffffff;
  border-radius: 16rpx;
  display: block;
  height: 560rpx;
  width: 100%;
}

.offline-note {
  background: #f8fafc;
  border-radius: 16rpx;
  color: #475569;
  display: block;
  font-size: 24rpx;
  line-height: 1.55;
  margin-top: 18rpx;
  padding: 18rpx;
}

.offline-contacts {
  display: grid;
  gap: 12rpx;
  margin-top: 18rpx;
}

.contact-row {
  align-items: center;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 18rpx;
  box-sizing: border-box;
  display: flex;
  gap: 16rpx;
  justify-content: space-between;
  padding: 16rpx;
}

.contact-name {
  color: #64748b;
  display: block;
  font-size: 22rpx;
  font-weight: 800;
}

.contact-value {
  color: #102033;
  display: block;
  font-size: 28rpx;
  font-weight: 900;
  line-height: 1.25;
  margin-top: 4rpx;
  word-break: break-all;
}

.contact-copy {
  align-items: center;
  box-sizing: border-box;
  display: flex;
  flex: 0 0 150rpx;
  height: 64rpx;
  justify-content: center;
  line-height: 64rpx;
  margin: 0;
  min-height: 64rpx;
  padding: 0;
  text-align: center;
}

.metric {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 16rpx;
  box-sizing: border-box;
  min-height: 132rpx;
  padding: 18rpx;
}

.metric-label,
.muted,
.hint,
.order-empty,
.payment-label,
.mock-notice {
  color: #64748b;
  display: block;
  font-size: 24rpx;
  line-height: 1.5;
}

.metric-value {
  color: #102033;
  display: block;
  font-size: 34rpx;
  font-weight: 800;
  line-height: 1.25;
  margin-top: 8rpx;
  word-break: break-word;
}

.metric-value.small {
  font-size: 24rpx;
}

.hint {
  margin-top: 10rpx;
}

.empty-card {
  color: #64748b;
  font-size: 26rpx;
  margin-top: 18rpx;
  text-align: center;
}

.plan-card {
  margin-top: 18rpx;
}

.plan-card.featured {
  border-color: #8dd7cf;
}

.plan-head {
  align-items: flex-start;
  display: flex;
  gap: 18rpx;
  justify-content: space-between;
}

.plan-name {
  color: #102033;
  display: block;
  font-size: 34rpx;
  font-weight: 800;
  line-height: 1.25;
}

.plan-duration {
  color: #64748b;
  display: block;
  font-size: 24rpx;
  margin-top: 8rpx;
}

.price-box {
  align-items: flex-end;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.featured-pill {
  background: #102033;
  color: #ffffff;
}

.price {
  color: #14796f;
  display: block;
  font-size: 42rpx;
  font-weight: 900;
  line-height: 1.1;
}

.benefits {
  border-top: 1px solid #e2e8f0;
  color: #475569;
  display: grid;
  font-size: 24rpx;
  gap: 8rpx;
  line-height: 1.45;
  margin-top: 22rpx;
  padding-top: 18rpx;
}

.pay-grid {
  align-items: center;
  display: flex;
  gap: 22rpx;
  justify-content: flex-start;
  margin-top: 20rpx;
}

.pay-button,
.primary-button,
.secondary-button {
  align-items: center;
  border-radius: 14rpx;
  box-sizing: border-box;
  display: flex;
  font-size: 27rpx;
  font-weight: 800;
  line-height: 1;
  margin: 0;
  min-height: 80rpx;
  padding: 0 18rpx;
}

.pay-button,
.primary-button {
  background: #14796f;
  border: 1px solid #14796f;
  color: #ffffff;
}

.pay-button {
  justify-content: center;
  min-height: 56rpx;
  min-width: 56rpx;
  padding: 0;
}

.primary-button,
.secondary-button {
  justify-content: center;
}

.pay-button.provider-wechat_pay,
.pay-button.provider-alipay {
  background: transparent;
  border-color: transparent;
  color: inherit;
}

.pay-icon {
  display: block;
  height: 48rpx;
  width: 48rpx;
}

.sr-only {
  height: 1px;
  overflow: hidden;
  position: absolute;
  width: 1px;
}

.secondary-button {
  background: #f8fafc;
  border: 1px solid #cbd5e1;
  color: #102033;
}

.secondary-button.compact {
  min-height: 68rpx;
  width: 180rpx;
}

.secondary-button.contact-copy {
  align-items: center;
  display: flex;
  flex: 0 0 150rpx;
  height: 64rpx;
  justify-content: center;
  line-height: 64rpx;
  min-height: 64rpx;
  padding: 0;
  text-align: center;
}

.secondary-button.contact-copy::after {
  border: 0;
}

.pay-button[disabled],
.primary-button[disabled],
.secondary-button[disabled] {
  opacity: 0.62;
}

.order-card {
  margin-top: 28rpx;
}

.status-pending .order-status {
  background: #fef3c7;
  color: #92400e;
}

.status-paid .order-status {
  background: #d1fae5;
  color: #047857;
}

.status-failed .order-status,
.status-refunded .order-status {
  background: #fee2e2;
  color: #b91c1c;
}

.order-empty {
  background: #f8fafc;
  border-radius: 16rpx;
  margin-top: 18rpx;
  padding: 24rpx;
  text-align: center;
}

.order-list {
  display: grid;
  gap: 12rpx;
  margin-top: 18rpx;
}

.order-row {
  align-items: flex-start;
  border-bottom: 1px solid #e2e8f0;
  color: #64748b;
  display: flex;
  font-size: 24rpx;
  gap: 18rpx;
  justify-content: space-between;
  line-height: 1.45;
  padding-bottom: 12rpx;
}

.row-value {
  color: #102033;
  flex: 1;
  font-weight: 800;
  text-align: right;
  word-break: break-word;
}

.payment-link {
  background: #f8fafc;
  border-radius: 16rpx;
  margin-top: 18rpx;
  padding: 18rpx;
}

.payment-url {
  color: #102033;
  display: block;
  font-size: 24rpx;
  font-weight: 700;
  line-height: 1.45;
  margin-top: 8rpx;
  word-break: break-all;
}

.payment-link .secondary-button {
  margin-top: 16rpx;
  width: 100%;
}

.mock-notice {
  background: #ecfeff;
  border-radius: 14rpx;
  color: #0f766e;
  margin-top: 18rpx;
  padding: 16rpx;
}

.order-notice {
  border-radius: 14rpx;
  color: #475569;
  display: block;
  font-size: 24rpx;
  line-height: 1.5;
  margin-top: 18rpx;
  padding: 16rpx;
}

.notice-pending {
  background: #fffbeb;
  color: #92400e;
}

.notice-paid {
  background: #ecfdf5;
  color: #047857;
}

.notice-failed,
.notice-refunded {
  background: #fef2f2;
  color: #b91c1c;
}

.polling-label {
  color: currentColor;
  display: block;
  font-size: 22rpx;
  font-weight: 800;
  margin-top: 8rpx;
}

.order-actions {
  display: grid;
  gap: 14rpx;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 20rpx;
}

.order-actions.single {
  grid-template-columns: 1fr;
}

.order-actions.single .secondary-button {
  grid-column: 1 / -1;
}
</style>
