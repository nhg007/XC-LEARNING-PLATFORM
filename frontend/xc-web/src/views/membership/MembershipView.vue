<template>
  <main class="app-shell membership-page">
    <section class="membership-stage">
      <v-progress-linear v-if="loading" class="stage-loader" color="primary" indeterminate />

      <div class="stage-toolbar">
        <LocaleSwitch variant="tonal" />
        <v-btn class="toolbar-icon" :aria-label="t('common.back')" variant="tonal" @click="router.push('/')">
          <v-icon icon="mdi-arrow-left" size="20" />
          <v-tooltip activator="parent" location="bottom">{{ t('common.back') }}</v-tooltip>
        </v-btn>
        <v-btn class="toolbar-icon" :aria-label="t('common.refresh')" :loading="loading" variant="tonal" @click="loadPage">
          <v-icon icon="mdi-refresh" size="20" />
          <v-tooltip activator="parent" location="bottom">{{ t('common.refresh') }}</v-tooltip>
        </v-btn>
      </div>

      <div class="stage-content">
        <div class="hero-copy">
          <span class="hero-kicker">
            <v-icon icon="mdi-crown-outline" size="18" />
            {{ t('membership.title') }}
          </span>
          <h1>{{ t('membership.heroTitle') }}</h1>
          <p>{{ t('membership.subtitle') }}</p>

          <div class="benefit-strip">
            <span v-for="item in benefitItems" :key="item.label" class="benefit-item">
              <v-icon :icon="item.icon" size="18" />
              {{ item.label }}
            </span>
          </div>
        </div>

        <aside :class="['access-summary', accessTone]">
          <div class="summary-label">
            <v-icon :icon="accessIcon" size="22" />
            <span>{{ t('membership.currentAccess') }}</span>
          </div>
          <strong>{{ accessLabel }}</strong>
          <div class="summary-countdown">
            <span>{{ t('membership.remaining') }}</span>
            <b>{{ remainingLabel }}</b>
          </div>
        </aside>
      </div>
    </section>

    <section class="access-grid">
      <article v-for="item in accessItems" :key="item.label" class="access-item">
        <v-icon :icon="item.icon" size="22" />
        <div>
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </article>
    </section>

    <section class="offline-payment-panel">
      <div class="offline-heading">
        <div>
          <span class="offline-kicker">
            <v-icon icon="mdi-account-cash-outline" size="18" />
            {{ t('membership.offlineTitle') }}
          </span>
          <h2>{{ t('membership.offlineSubtitle') }}</h2>
          <p>{{ t('membership.offlineHint') }}</p>
        </div>
      </div>

      <div class="offline-grid">
        <article class="offline-card method-card">
          <div class="offline-card-head">
            <span>{{ t('membership.offlineMethodsTitle') }}</span>
          </div>
          <div class="offline-methods">
            <div v-for="method in offlinePaymentMethods" :key="method.key" :class="['offline-method', method.key]">
              <div class="method-main">
                <v-icon :icon="method.icon" size="24" />
                <strong>{{ method.label }}</strong>
              </div>
              <div v-if="method.qrUrl" class="qr-frame">
                <img :src="method.qrUrl" :alt="method.label" />
              </div>
              <div v-else class="qr-placeholder">
                <v-icon icon="mdi-qrcode-scan" size="28" />
                <span>{{ t('membership.offlineQrMissing') }}</span>
              </div>
            </div>
          </div>
        </article>

        <article class="offline-card contact-card">
          <div class="offline-card-head">
            <span>{{ t('membership.offlineContactTitle') }}</span>
          </div>
          <div class="contact-list">
            <div v-for="contact in offlinePaymentContacts" :key="contact.key" class="contact-row">
              <div>
                <span class="contact-label">
                  <v-icon :icon="contact.icon" size="18" />
                  {{ contact.label }}
                </span>
                <strong>{{ contact.value }}</strong>
              </div>
              <v-btn
                class="copy-contact"
                color="primary"
                size="small"
                variant="tonal"
                @click="copyOfflineContact(contact.value)"
              >
                <v-icon icon="mdi-content-copy" size="16" />
                {{ t('membership.copyContact') }}
              </v-btn>
            </div>
          </div>
        </article>
      </div>
    </section>

    <section v-if="showPurchaseSection" class="membership-layout">
      <section class="plans-area">
        <div class="section-heading">
          <div>
            <h2>{{ t('membership.plans') }}</h2>
            <p>{{ t('membership.plansHint') }}</p>
          </div>
        </div>

        <div v-if="plans.length === 0 && !loading" class="empty-state">
          <v-icon icon="mdi-package-variant-closed" size="30" />
          <span>{{ t('membership.emptyPlans') }}</span>
        </div>

        <div v-else class="plan-grid">
          <article
            v-for="plan in plans"
            :key="plan.id"
            :class="['plan-card', { featured: isFeaturedPlan(plan), active: isCurrentOrderPlan(plan) }]"
          >
            <div class="plan-topline">
              <span v-if="isFeaturedPlan(plan)" class="plan-badge">{{ t('membership.bestValue') }}</span>
              <v-icon icon="mdi-crown-outline" size="22" />
            </div>

            <div class="plan-title">
              <h3>{{ plan.name }}</h3>
              <span>{{ durationLabel(plan) }}</span>
            </div>

            <div class="price-block">
              <strong>{{ formatPrice(plan.price, plan.currency) }}</strong>
              <span>{{ t('membership.durationDays', { count: plan.durationDays }) }}</span>
            </div>

            <div class="plan-benefits">
              <span>
                <v-icon icon="mdi-check-circle-outline" size="18" />
                {{ t('membership.fullAccessBenefit') }}
              </span>
              <span>
                <v-icon icon="mdi-calendar-clock" size="18" />
                {{ t('membership.validFor', { count: plan.durationDays }) }}
              </span>
            </div>

            <div class="provider-actions">
              <v-btn
                class="pay-button wechat"
                prepend-icon="mdi-wechat"
                variant="flat"
                :loading="creatingPlanId === plan.id && creatingProvider === 'wechat_pay'"
                @click="createOrder(plan.id, 'wechat_pay')"
              >
                {{ t('membership.payWith', { provider: t('membership.providers.wechat_pay') }) }}
              </v-btn>
              <v-btn
                class="pay-button alipay"
                prepend-icon="mdi-wallet-outline"
                variant="tonal"
                :loading="creatingPlanId === plan.id && creatingProvider === 'alipay'"
                @click="createOrder(plan.id, 'alipay')"
              >
                {{ t('membership.payWith', { provider: t('membership.providers.alipay') }) }}
              </v-btn>
            </div>
          </article>
        </div>
      </section>

      <aside :class="['checkout-panel', orderStatusClass]">
        <div class="checkout-header">
          <div>
            <span>{{ t('membership.checkoutLabel') }}</span>
            <h2>{{ t('membership.orderTitle') }}</h2>
          </div>
          <v-icon icon="mdi-receipt-text-outline" size="28" />
        </div>

        <div v-if="!order" class="checkout-empty">
          <v-icon icon="mdi-qrcode-scan" size="40" />
          <strong>{{ t('membership.checkoutEmptyTitle') }}</strong>
          <p>{{ t('membership.noOrder') }}</p>
          <div class="method-row">
            <span><v-icon icon="mdi-wechat" size="17" />{{ t('membership.providers.wechat_pay') }}</span>
            <span><v-icon icon="mdi-wallet-outline" size="17" />{{ t('membership.providers.alipay') }}</span>
          </div>
        </div>

        <div v-else class="order-detail">
          <div class="order-status-card">
            <span>{{ t('membership.orderStatus') }}</span>
            <strong>{{ statusLabel(order.status) }}</strong>
          </div>

          <div class="order-lines">
            <div class="order-line">
              <span>{{ t('membership.orderPlan') }}</span>
              <strong>{{ order.planName || '-' }}</strong>
            </div>
            <div class="order-line">
              <span>{{ t('membership.orderAmount') }}</span>
              <strong>{{ formatPrice(order.amount, order.currency) }}</strong>
            </div>
            <div class="order-line">
              <span>{{ t('membership.orderProvider') }}</span>
              <strong>{{ t(`membership.providers.${order.provider}`) }}</strong>
            </div>
            <div class="order-line">
              <span>{{ t('membership.orderNo') }}</span>
              <strong>{{ order.orderNo }}</strong>
            </div>
          </div>

          <div v-if="order.paymentUrl" class="payment-box">
            <span>{{ t('membership.paymentLink') }}</span>
            <code>{{ order.paymentUrl }}</code>
          </div>

          <v-alert v-if="order.mockPayment && order.status === 'pending'" class="mock-alert" type="info" variant="tonal">
            {{ t('membership.mockNotice') }}
          </v-alert>

          <div class="order-actions">
            <v-btn
              v-if="order.mockPayment && order.status === 'pending'"
              color="primary"
              prepend-icon="mdi-check-circle-outline"
              :loading="paying"
              @click="markPaid"
            >
              {{ t('membership.simulatePaid') }}
            </v-btn>
            <v-btn prepend-icon="mdi-refresh" variant="tonal" :loading="orderLoading" @click="refreshOrder">
              {{ t('membership.refreshOrder') }}
            </v-btn>
          </div>
        </div>
      </aside>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import LocaleSwitch from '@/components/LocaleSwitch.vue'
import alipayQrUrl from '../../assets/payment/alipay.jpg'
import wechatQrUrl from '../../assets/payment/wechat.jpg'
import { createPaymentOrder, fetchMembershipPlans, fetchMembershipStatus, fetchPaymentOrder, simulatePayment } from '../../api/membership'
import type { MembershipPlan, MembershipStatus, PaymentOrder, PaymentOrderStatus, PaymentProvider } from '../../types/api'
import { notifySuccess, notifyWarning } from '../../utils/notify'

const router = useRouter()
const { t, locale } = useI18n()
const loading = ref(false)
const orderLoading = ref(false)
const paying = ref(false)
const creatingPlanId = ref<number | null>(null)
const creatingProvider = ref<PaymentProvider | null>(null)
const status = ref<MembershipStatus>({
  accessLevel: 'free',
  fullAccess: false,
  trialEndsAt: null,
  membershipEndsAt: null,
  remainingSeconds: 0
})
const plans = ref<MembershipPlan[]>([])
const order = ref<PaymentOrder | null>(null)
const showPurchaseSection = import.meta.env.VITE_ENABLE_MEMBERSHIP_PURCHASE === 'true'

type OfflineContactKey = 'telegram' | 'whatsapp' | 'vk'
type OfflineMethodKey = 'wechat' | 'alipay'

const offlinePaymentMethods = computed(() => [
  {
    key: 'wechat' as OfflineMethodKey,
    icon: 'mdi-wechat',
    label: t('membership.providers.wechat_pay'),
    qrUrl: wechatQrUrl
  },
  {
    key: 'alipay' as OfflineMethodKey,
    icon: 'mdi-wallet-outline',
    label: t('membership.providers.alipay'),
    qrUrl: alipayQrUrl
  }
])

const offlinePaymentContacts = computed(() => [
  {
    key: 'telegram' as OfflineContactKey,
    icon: 'mdi-send',
    label: 'Telegram',
    value: '+8618605454289'
  },
  {
    key: 'whatsapp' as OfflineContactKey,
    icon: 'mdi-whatsapp',
    label: 'WhatsApp',
    value: '+8619953594090'
  },
  {
    key: 'vk' as OfflineContactKey,
    icon: 'mdi-vk',
    label: 'VK',
    value: 'id305390341'
  }
].filter((item) => item.value))

const accessLabel = computed(() => {
  if (status.value.accessLevel === 'member') {
    return t('home.access.member')
  }
  if (status.value.accessLevel === 'trial') {
    return t('home.access.trial')
  }
  return t('home.access.free')
})

const remainingLabel = computed(() => {
  if (!status.value.remainingSeconds) {
    return t('units.zeroDays')
  }
  const days = Math.ceil(status.value.remainingSeconds / 86400)
  return t('units.days', { count: days })
})

const accessTone = computed(() => `access-${status.value.accessLevel}`)

const accessIcon = computed(() => {
  if (status.value.accessLevel === 'member') {
    return 'mdi-crown'
  }
  if (status.value.accessLevel === 'trial') {
    return 'mdi-clock-fast'
  }
  return 'mdi-lock-open-outline'
})

const benefitItems = computed(() => [
  { icon: 'mdi-pencil-ruler', label: t('membership.benefits.practice') },
  { icon: 'mdi-chart-line', label: t('membership.benefits.records') },
  { icon: 'mdi-account-group-outline', label: t('membership.benefits.classroom') }
])

const accessItems = computed(() => [
  { icon: 'mdi-shield-crown-outline', label: t('membership.accessLevel'), value: accessLabel.value },
  { icon: 'mdi-timer-sand', label: t('membership.remaining'), value: remainingLabel.value },
  { icon: 'mdi-calendar-start', label: t('membership.trialEndsAt'), value: formatOptionalDate(status.value.trialEndsAt) },
  { icon: 'mdi-calendar-check-outline', label: t('membership.membershipEndsAt'), value: formatOptionalDate(status.value.membershipEndsAt) }
])

const featuredPlanId = computed(() => {
  if (plans.value.length < 2) {
    return null
  }
  return plans.value.reduce((best, plan) => (plan.durationDays > best.durationDays ? plan : best), plans.value[0]).id
})

const orderStatusClass = computed(() => (order.value ? `checkout-${order.value.status}` : ''))

async function loadPage() {
  loading.value = true
  try {
    const [statusData, planData] = await Promise.all([
      fetchMembershipStatus(),
      showPurchaseSection ? fetchMembershipPlans() : Promise.resolve([])
    ])
    status.value = statusData
    plans.value = planData
    if (!showPurchaseSection) {
      order.value = null
    }
  } finally {
    loading.value = false
  }
}

async function createOrder(planId: number, provider: PaymentProvider) {
  creatingPlanId.value = planId
  creatingProvider.value = provider
  try {
    order.value = await createPaymentOrder({ planId, provider, clientType: 'web' })
    notifySuccess(t('membership.createSuccess'))
  } finally {
    creatingPlanId.value = null
    creatingProvider.value = null
  }
}

async function refreshOrder() {
  if (!order.value) {
    return
  }
  orderLoading.value = true
  try {
    order.value = await fetchPaymentOrder(order.value.orderNo)
    if (order.value.status === 'paid') {
      status.value = await fetchMembershipStatus()
    }
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
    order.value = await simulatePayment(order.value.orderNo)
    status.value = await fetchMembershipStatus()
    notifySuccess(t('membership.paidSuccess'))
  } finally {
    paying.value = false
  }
}

function isFeaturedPlan(plan: MembershipPlan) {
  return plan.id === featuredPlanId.value
}

function isCurrentOrderPlan(plan: MembershipPlan) {
  return order.value?.planId === plan.id
}

function formatPrice(price: number, currency: string) {
  return new Intl.NumberFormat(locale.value, {
    style: 'currency',
    currency,
    minimumFractionDigits: 2
  }).format(Number(price || 0))
}

function durationLabel(plan: MembershipPlan) {
  return t(`membership.duration.${plan.durationUnit}`, {
    count: plan.durationValue,
    days: plan.durationDays
  })
}

function statusLabel(value: PaymentOrderStatus) {
  return t(`membership.orderStatuses.${value}`)
}

function formatOptionalDate(value: string | null) {
  if (!value) {
    return '-'
  }
  return new Intl.DateTimeFormat(locale.value, {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

async function copyOfflineContact(value: string) {
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(value)
    } else {
      const textarea = document.createElement('textarea')
      textarea.value = value
      textarea.setAttribute('readonly', 'readonly')
      textarea.style.position = 'fixed'
      textarea.style.left = '-9999px'
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
    }
    notifySuccess(t('membership.contactCopied'))
  } catch {
    notifyWarning(t('membership.copyFailed'))
  }
}

onMounted(loadPage)
</script>

<style scoped>
.membership-page {
  --xc-page-max-width: 1120px;
  color: #172033;
}

h1,
h2,
h3,
p {
  margin: 0;
}

.membership-stage {
  background:
    linear-gradient(135deg, rgba(20, 184, 166, 0.16) 0%, rgba(20, 184, 166, 0) 42%),
    linear-gradient(90deg, #ffffff 0%, #f8fbff 100%);
  border: 1px solid #d7e2f1;
  border-radius: 8px;
  overflow: hidden;
  padding: 24px;
  position: relative;
}

.stage-loader {
  left: 0;
  position: absolute;
  right: 0;
  top: 0;
}

.stage-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
  margin-bottom: 22px;
}

.stage-toolbar :deep(.v-btn),
.provider-actions :deep(.v-btn),
.order-actions :deep(.v-btn) {
  border-radius: 6px;
  letter-spacing: 0;
}

.stage-toolbar .toolbar-icon {
  flex: 0 0 auto;
  min-width: 44px;
  padding: 0;
  width: 44px;
}

.stage-content {
  align-items: stretch;
  display: grid;
  gap: 24px;
  grid-template-columns: minmax(0, 1fr) 320px;
}

.hero-copy {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
}

.hero-kicker {
  align-items: center;
  color: #0f766e;
  display: inline-flex;
  font-size: 14px;
  font-weight: 800;
  gap: 8px;
  margin-bottom: 14px;
}

.hero-copy h1 {
  font-size: 40px;
  line-height: 1.15;
  max-width: 680px;
}

.hero-copy p {
  color: #52647c;
  font-size: 16px;
  line-height: 1.7;
  margin-top: 14px;
  max-width: 660px;
}

.benefit-strip {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin-top: 24px;
}

.benefit-item {
  align-items: center;
  background: rgba(255, 255, 255, 0.76);
  border: 1px solid #dbe7f4;
  border-radius: 6px;
  color: #334155;
  display: flex;
  font-size: 14px;
  font-weight: 700;
  gap: 8px;
  min-height: 42px;
  min-width: 0;
  padding: 10px 12px;
}

.benefit-item :deep(.v-icon) {
  color: #2563eb;
}

.access-summary {
  background: linear-gradient(135deg, #1f2937, #0f172a);
  border-radius: 8px;
  color: #f8fafc;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 220px;
  padding: 24px;
}

.access-summary.access-member {
  background: linear-gradient(135deg, #0f766e, #115e59 48%, #0f172a 100%);
}

.access-summary.access-trial {
  background: linear-gradient(135deg, #2563eb, #0f766e 100%);
}

.summary-label {
  align-items: center;
  display: flex;
  font-size: 14px;
  font-weight: 800;
  gap: 10px;
}

.access-summary strong {
  display: block;
  font-size: 38px;
  line-height: 1.1;
  margin: 22px 0;
  overflow-wrap: anywhere;
}

.summary-countdown {
  border-top: 1px solid rgba(255, 255, 255, 0.22);
  display: grid;
  gap: 6px;
  padding-top: 16px;
}

.summary-countdown span {
  color: rgba(255, 255, 255, 0.72);
  font-size: 13px;
}

.summary-countdown b {
  font-size: 22px;
  line-height: 1.1;
}

.access-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin: 18px 0 22px;
}

.access-item {
  align-items: flex-start;
  background: #ffffff;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  display: flex;
  gap: 12px;
  min-width: 0;
  padding: 16px;
}

.access-item :deep(.v-icon) {
  color: #0f766e;
  flex: 0 0 auto;
  margin-top: 2px;
}

.access-item span,
.checkout-header span,
.order-line span,
.payment-box span,
.order-status-card span {
  color: #64748b;
  display: block;
  font-size: 13px;
}

.access-item strong {
  display: block;
  font-size: 17px;
  line-height: 1.35;
  margin-top: 6px;
  overflow-wrap: anywhere;
}

.offline-payment-panel {
  background:
    linear-gradient(135deg, rgba(20, 184, 166, 0.12), rgba(37, 99, 235, 0.04)),
    #ffffff;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  margin-bottom: 22px;
  padding: 22px;
}

.offline-heading {
  align-items: flex-start;
  display: flex;
  justify-content: space-between;
  margin-bottom: 18px;
}

.offline-kicker {
  align-items: center;
  color: #0f766e;
  display: inline-flex;
  font-size: 13px;
  font-weight: 900;
  gap: 6px;
  margin-bottom: 8px;
}

.offline-heading h2 {
  color: #172033;
  font-size: 22px;
  line-height: 1.35;
}

.offline-heading p {
  color: #64748b;
  line-height: 1.65;
  margin-top: 8px;
  max-width: 760px;
}

.offline-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: minmax(0, 1.25fr) minmax(320px, 0.75fr);
}

.offline-card {
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid #dbe7f4;
  border-radius: 8px;
  padding: 16px;
}

.offline-card-head {
  align-items: center;
  color: #334155;
  display: flex;
  font-size: 14px;
  font-weight: 900;
  justify-content: space-between;
  margin-bottom: 12px;
}

.offline-methods {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.offline-method {
  background: #f8fafc;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  display: grid;
  gap: 12px;
  min-height: 170px;
  padding: 14px;
}

.offline-method.wechat {
  background: linear-gradient(135deg, rgba(34, 197, 94, 0.12), #ffffff);
}

.offline-method.alipay {
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.12), #ffffff);
}

.method-main,
.contact-label {
  align-items: center;
  display: flex;
  gap: 8px;
}

.method-main {
  color: #172033;
  font-size: 16px;
}

.method-main :deep(.v-icon) {
  color: #0f766e;
}

.offline-method.alipay .method-main :deep(.v-icon) {
  color: #1d4ed8;
}

.qr-frame,
.qr-placeholder {
  align-items: center;
  background: #ffffff;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  display: flex;
  justify-content: center;
  min-height: 108px;
  overflow: hidden;
}

.qr-frame img {
  display: block;
  max-height: 180px;
  max-width: 100%;
  object-fit: contain;
}

.qr-placeholder {
  color: #64748b;
  flex-direction: column;
  font-size: 13px;
  gap: 8px;
  padding: 18px;
  text-align: center;
}

.contact-list {
  display: grid;
  gap: 12px;
}

.contact-row {
  align-items: center;
  background: #ffffff;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  display: flex;
  gap: 12px;
  justify-content: space-between;
  min-width: 0;
  padding: 14px;
}

.contact-label {
  color: #64748b;
  font-size: 13px;
  font-weight: 800;
}

.contact-row strong {
  color: #172033;
  display: block;
  font-size: 17px;
  line-height: 1.25;
  margin-top: 4px;
  overflow-wrap: anywhere;
}

.copy-contact {
  flex: 0 0 auto;
  letter-spacing: 0;
}

.membership-layout {
  align-items: start;
  display: grid;
  gap: 22px;
  grid-template-columns: minmax(0, 1fr) 340px;
}

.plans-area,
.checkout-panel {
  background: #ffffff;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
}

.plans-area {
  padding: 24px;
}

.section-heading {
  align-items: flex-start;
  display: flex;
  justify-content: space-between;
  margin-bottom: 18px;
}

.section-heading h2,
.checkout-header h2 {
  font-size: 22px;
  line-height: 1.25;
}

.section-heading p {
  color: #64748b;
  line-height: 1.6;
  margin-top: 8px;
  max-width: 680px;
}

.plan-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.plan-card {
  background: #ffffff;
  border: 1px solid #d8e3ef;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-height: 360px;
  padding: 20px;
  transition:
    border-color 0.16s ease,
    box-shadow 0.16s ease,
    transform 0.16s ease;
}

.plan-card:hover {
  border-color: #94a3b8;
  box-shadow: 0 14px 34px rgba(30, 41, 59, 0.09);
  transform: translateY(-2px);
}

.plan-card.featured {
  border-color: #f59e0b;
  box-shadow: inset 0 0 0 1px rgba(245, 158, 11, 0.36);
}

.plan-card.active {
  border-color: #2563eb;
  box-shadow: inset 0 0 0 1px rgba(37, 99, 235, 0.4);
}

.plan-topline {
  align-items: center;
  color: #64748b;
  display: flex;
  height: 28px;
  justify-content: space-between;
}

.plan-badge {
  background: #fff7ed;
  border: 1px solid #fed7aa;
  border-radius: 4px;
  color: #b45309;
  font-size: 12px;
  font-weight: 800;
  padding: 4px 8px;
}

.plan-title h3 {
  font-size: 22px;
  line-height: 1.25;
  overflow-wrap: anywhere;
}

.plan-title span {
  color: #64748b;
  display: block;
  margin-top: 8px;
}

.price-block {
  border-bottom: 1px solid #e2e8f0;
  border-top: 1px solid #e2e8f0;
  display: grid;
  gap: 8px;
  padding: 18px 0;
}

.price-block strong {
  color: #1d4ed8;
  font-size: 34px;
  line-height: 1;
  overflow-wrap: anywhere;
}

.price-block span {
  color: #64748b;
  font-size: 13px;
}

.plan-benefits {
  display: grid;
  gap: 10px;
}

.plan-benefits span {
  align-items: flex-start;
  color: #334155;
  display: flex;
  font-size: 14px;
  gap: 8px;
  line-height: 1.4;
}

.plan-benefits :deep(.v-icon) {
  color: #16a34a;
  flex: 0 0 auto;
  margin-top: 1px;
}

.provider-actions {
  display: grid;
  gap: 10px;
  margin-top: auto;
}

.pay-button.wechat {
  background: #0f766e;
  color: #ffffff;
}

.pay-button.alipay {
  color: #1d4ed8;
}

.empty-state {
  align-items: center;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  color: #64748b;
  display: flex;
  flex-direction: column;
  gap: 10px;
  justify-content: center;
  min-height: 220px;
}

.checkout-panel {
  overflow: hidden;
  padding: 22px;
  position: sticky;
  top: 54px;
}

.checkout-header {
  align-items: flex-start;
  display: flex;
  justify-content: space-between;
  margin-bottom: 18px;
}

.checkout-header :deep(.v-icon) {
  color: #2563eb;
}

.checkout-empty {
  align-items: center;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  color: #334155;
  display: flex;
  flex-direction: column;
  min-height: 300px;
  padding: 28px 18px;
  text-align: center;
}

.checkout-empty :deep(.v-icon) {
  color: #0f766e;
}

.checkout-empty strong {
  font-size: 18px;
  line-height: 1.3;
  margin-top: 14px;
}

.checkout-empty p {
  color: #64748b;
  line-height: 1.6;
  margin-top: 10px;
}

.method-row {
  display: grid;
  gap: 8px;
  grid-template-columns: 1fr 1fr;
  margin-top: auto;
  width: 100%;
}

.method-row span {
  align-items: center;
  background: #ffffff;
  border: 1px solid #dbe3ee;
  border-radius: 6px;
  display: flex;
  font-size: 13px;
  font-weight: 700;
  gap: 6px;
  justify-content: center;
  min-height: 38px;
  min-width: 0;
}

.order-detail {
  display: grid;
  gap: 16px;
}

.order-status-card {
  background: #f8fafc;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  padding: 16px;
}

.checkout-pending .order-status-card {
  background: #fff7ed;
  border-color: #fed7aa;
}

.checkout-paid .order-status-card {
  background: #ecfdf5;
  border-color: #bbf7d0;
}

.checkout-failed .order-status-card,
.checkout-refunded .order-status-card {
  background: #fef2f2;
  border-color: #fecaca;
}

.order-status-card strong {
  color: #172033;
  display: block;
  font-size: 24px;
  line-height: 1.15;
  margin-top: 6px;
}

.order-lines {
  display: grid;
  gap: 12px;
}

.order-line {
  border-bottom: 1px solid #e2e8f0;
  display: grid;
  gap: 5px;
  padding-bottom: 12px;
}

.order-line strong {
  font-size: 15px;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.payment-box {
  background: #f8fafc;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  padding: 14px;
}

.payment-box code {
  color: #334155;
  display: block;
  font-size: 12px;
  margin-top: 8px;
  overflow-wrap: anywhere;
}

.mock-alert {
  border-radius: 8px;
}

.order-actions {
  display: grid;
  gap: 10px;
}

@media (max-width: 1080px) {
  .stage-content,
  .membership-layout,
  .offline-grid {
    grid-template-columns: 1fr;
  }

  .access-summary,
  .checkout-panel {
    position: static;
  }

  .plan-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 820px) {
  .hero-copy h1 {
    font-size: 32px;
  }

  .benefit-strip,
  .access-grid,
  .plan-grid,
  .offline-methods {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 620px) {
  .membership-stage,
  .plans-area,
  .checkout-panel,
  .offline-payment-panel {
    padding: 18px;
  }

  .stage-toolbar {
    justify-content: flex-start;
  }

  .hero-copy h1 {
    font-size: 28px;
  }

  .access-summary {
    min-height: 190px;
  }

  .method-row {
    grid-template-columns: 1fr;
  }

  .contact-row {
    align-items: stretch;
    flex-direction: column;
  }

  .copy-contact {
    width: 100%;
  }
}
</style>
