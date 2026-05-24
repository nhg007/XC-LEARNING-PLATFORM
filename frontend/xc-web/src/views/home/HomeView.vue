<template>
  <main class="app-shell">
    <header class="topbar">
      <div>
        <h1>{{ t('app.title') }}</h1>
        <p>{{ welcomeText }}</p>
      </div>
      <div class="top-actions">
        <LocaleSwitch />
        <v-btn class="offline-pay-action" prepend-icon="mdi-qrcode-scan" variant="tonal" @click="paymentDialogOpen = true">
          {{ t('membership.offlineTitle') }}
        </v-btn>
        <v-btn class="membership-action" prepend-icon="mdi-crown-outline" variant="tonal" @click="$router.push('/membership')">{{ t('common.membership') }}</v-btn>
        <v-btn prepend-icon="mdi-refresh" variant="tonal" :loading="loading" @click="loadHome">{{ t('common.refresh') }}</v-btn>
        <v-btn prepend-icon="mdi-logout" variant="text" @click="logout">{{ t('common.logout') }}</v-btn>
      </div>
    </header>

    <v-dialog v-model="paymentDialogOpen" max-width="760">
      <v-card class="home-payment-dialog" elevation="0">
        <div class="payment-dialog-head">
          <div>
            <span class="payment-kicker">{{ t('membership.offlineTitle') }}</span>
            <h2>{{ t('membership.footerTitle') }}</h2>
            <p>{{ t('membership.footerSubtitle') }}</p>
          </div>
          <v-btn aria-label="close" icon="mdi-close" size="small" variant="text" @click="paymentDialogOpen = false" />
        </div>

        <div class="payment-method-grid">
          <article v-for="method in offlinePaymentMethods" :key="method.key" class="payment-method-card">
            <div class="payment-method-title">
              <v-icon :icon="method.icon" size="20" />
              <span>{{ t(method.labelKey) }}</span>
            </div>
            <img :src="method.qrUrl" :alt="t(method.labelKey)" />
          </article>
        </div>

        <div class="payment-contact-panel">
          <div>
            <strong>{{ t('membership.offlineContactTitle') }}</strong>
            <p>{{ t('membership.offlineHint') }}</p>
          </div>
          <div class="payment-contact-grid">
            <button v-for="contact in offlineContacts" :key="contact.key" class="payment-contact-button" type="button" @click="copyOfflineContact(contact.value)">
              <span>
                <v-icon :icon="contact.icon" size="18" />
                {{ contact.label }}
              </span>
              <strong>{{ contact.value }}</strong>
              <v-icon icon="mdi-content-copy" size="16" />
            </button>
          </div>
        </div>
      </v-card>
    </v-dialog>

    <section class="status-band">
      <v-progress-linear v-if="loading" class="loading-line" color="primary" indeterminate />
      <div>
        <span class="label">{{ t('home.accessStatus') }}</span>
        <strong>{{ accessLabel }}</strong>
      </div>
      <div>
        <span class="label">{{ t('home.remainingTime') }}</span>
        <strong>{{ remainingLabel }}</strong>
      </div>
      <div>
        <span class="label">{{ t('home.currentStreak') }}</span>
        <strong>{{ t('units.days', { count: summary.currentStreakDays }) }}</strong>
      </div>
      <div>
        <span class="label">{{ t('home.accuracy') }}</span>
        <strong>{{ summary.overallAccuracyRate }}%</strong>
      </div>
    </section>

    <section class="feature-grid">
      <v-card
        class="feature-card"
        v-for="item in features"
        :key="item.key"
        :class="{ locked: item.requiresFullAccess && !membership.fullAccess }"
        elevation="0"
        @click="openFeature(item.key)"
      >
        <v-icon class="feature-icon" :icon="item.icon" />
        <span class="feature-status" :class="{ locked: item.requiresFullAccess && !membership.fullAccess }">
          {{ item.requiresFullAccess && !membership.fullAccess ? t('common.memberRequired') : t('common.available') }}
        </span>
        <h2>{{ item.title }}</h2>
        <p>{{ item.description }}</p>
      </v-card>
    </section>

    <section class="content-grid">
      <v-card class="panel" elevation="0">
        <div class="panel-title">{{ t('home.vocabLists') }}</div>
        <div v-if="vocabLists.length === 0" class="empty-state">{{ t('home.emptyVocabLists') }}</div>
        <v-table v-else density="comfortable">
          <thead>
            <tr>
              <th>{{ t('home.table.name') }}</th>
              <th>{{ t('home.table.level') }}</th>
              <th>{{ t('home.table.type') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in vocabLists" :key="item.id" class="clickable-row" @click="openVocabList(item)">
              <td>{{ item.name }}</td>
              <td>{{ item.level || '-' }}</td>
              <td>{{ item.listType }}</td>
            </tr>
          </tbody>
        </v-table>
      </v-card>
      <v-card class="panel" elevation="0">
        <div class="panel-title">{{ t('home.myClasses') }}</div>
        <div v-if="classes.length === 0" class="empty-state">{{ t('home.emptyClasses') }}</div>
        <v-table v-else density="comfortable">
          <thead>
            <tr>
              <th>{{ t('home.table.className') }}</th>
              <th>{{ t('home.table.owner') }}</th>
              <th>{{ t('home.table.status') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in classes" :key="item.id" class="clickable-row" @click="openClassrooms">
              <td>{{ item.name }}</td>
              <td>{{ item.ownerName || item.ownerContact || t('classroom.noOwner') }}</td>
              <td>{{ t(`classroom.statuses.${item.memberStatus}`) }}</td>
            </tr>
          </tbody>
        </v-table>
      </v-card>
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
import { fetchClassRooms, fetchLearningSummary, fetchMembershipStatus, fetchVocabLists } from '../../api/learning'
import { useSessionStore } from '../../stores/session'
import type { ClassRoom, LearningSummary, MembershipStatus, VocabList } from '../../types/api'
import { notifyInfo, notifySuccess, notifyWarning } from '../../utils/notify'

const router = useRouter()
const session = useSessionStore()
const { t } = useI18n()
const loading = ref(false)
const paymentDialogOpen = ref(false)
type HomeFeatureKey = 'vocab' | 'favorites' | 'practice' | 'matching' | 'elimination' | 'classroom' | 'records'
const featureIconMap: Record<HomeFeatureKey, string> = {
  vocab: 'mdi-book-open-page-variant-outline',
  favorites: 'mdi-star-outline',
  practice: 'mdi-pencil-outline',
  matching: 'mdi-vector-square',
  elimination: 'mdi-cards-outline',
  classroom: 'mdi-account-group-outline',
  records: 'mdi-chart-line'
}
const membership = ref<MembershipStatus>({
  accessLevel: 'free',
  fullAccess: false,
  trialEndsAt: null,
  membershipEndsAt: null,
  remainingSeconds: 0
})
const summary = ref<LearningSummary>({
  totalStudySeconds: 0,
  totalExerciseCount: 0,
  totalCorrectCount: 0,
  totalVocabReviewCount: 0,
  currentStreakDays: 0,
  longestStreakDays: 0,
  overallAccuracyRate: 0,
  lastStudyDate: null
})
const vocabLists = ref<VocabList[]>([])
const classes = ref<ClassRoom[]>([])
const offlinePaymentMethods = [
  {
    key: 'wechat',
    icon: 'mdi-wechat',
    labelKey: 'membership.providers.wechat_pay',
    qrUrl: wechatQrUrl
  },
  {
    key: 'alipay',
    icon: 'mdi-wallet-outline',
    labelKey: 'membership.providers.alipay',
    qrUrl: alipayQrUrl
  }
]
const offlineContacts = [
  {
    key: 'telegram',
    icon: 'mdi-send',
    label: 'Telegram',
    value: '+8618605454289'
  },
  {
    key: 'whatsapp',
    icon: 'mdi-whatsapp',
    label: 'WhatsApp',
    value: '+8619953594090'
  },
  {
    key: 'vk',
    icon: 'mdi-vk',
    label: 'VK',
    value: 'id305390341'
  }
]
const features = computed(() => [
  { key: 'vocab' as HomeFeatureKey, title: t('home.features.vocab.title'), description: t('home.features.vocab.description'), requiresFullAccess: false },
  { key: 'favorites' as HomeFeatureKey, title: t('home.features.favorites.title'), description: t('home.features.favorites.description'), requiresFullAccess: false },
  { key: 'practice' as HomeFeatureKey, title: t('home.features.practice.title'), description: t('home.features.practice.description'), requiresFullAccess: true },
  { key: 'matching' as HomeFeatureKey, title: t('home.features.matching.title'), description: t('home.features.matching.description'), requiresFullAccess: true },
  { key: 'elimination' as HomeFeatureKey, title: t('home.features.elimination.title'), description: t('home.features.elimination.description'), requiresFullAccess: true },
  { key: 'classroom' as HomeFeatureKey, title: t('home.features.classroom.title'), description: t('home.features.classroom.description'), requiresFullAccess: false },
  { key: 'records' as HomeFeatureKey, title: t('home.features.records.title'), description: t('home.features.records.description'), requiresFullAccess: false }
].map((item) => ({
  ...item,
  icon: featureIconMap[item.key]
})))

const welcomeText = computed(() => {
  const nickname = session.profile?.nickname || session.profile?.email || t('home.learner')
  return t('home.welcome', { name: nickname })
})
const accessLabel = computed(() => {
  if (membership.value.accessLevel === 'member') {
    return t('home.access.member')
  }
  if (membership.value.accessLevel === 'trial') {
    return t('home.access.trial')
  }
  return t('home.access.free')
})
const remainingLabel = computed(() => {
  if (!membership.value.remainingSeconds) {
    return t('units.zeroDays')
  }
  const days = Math.ceil(membership.value.remainingSeconds / 86400)
  return t('units.days', { count: days })
})

async function loadHome() {
  loading.value = true
  try {
    const [status, learning, vocab, classRooms] = await Promise.all([
      fetchMembershipStatus(),
      fetchLearningSummary(),
      fetchVocabLists(),
      fetchClassRooms()
    ])
    membership.value = status
    summary.value = learning
    vocabLists.value = vocab.records.slice(0, 6)
    classes.value = classRooms
  } finally {
    loading.value = false
  }
}

async function logout() {
  session.logout()
  await router.push('/login')
}

async function openFeature(key: string) {
  if (key === 'vocab') {
    await router.push('/vocab')
    return
  }
  if (key === 'practice') {
    if (!membership.value.fullAccess) {
      notifyWarning(t('home.featureLocked'))
      await router.push('/membership')
      return
    }
    await router.push('/practice')
    return
  }
  if (key === 'favorites') {
    await router.push('/favorites')
    return
  }
  if (key === 'classroom') {
    await router.push('/classrooms')
    return
  }
  if (key === 'matching') {
    if (!membership.value.fullAccess) {
      notifyWarning(t('home.featureLocked'))
      await router.push('/membership')
      return
    }
    await router.push('/matching')
    return
  }
  if (key === 'elimination') {
    if (!membership.value.fullAccess) {
      notifyWarning(t('home.featureLocked'))
      await router.push('/membership')
      return
    }
    await router.push('/elimination')
    return
  }
  if (key === 'records') {
    await router.push('/records')
    return
  }
  notifyInfo(t('common.comingSoon'))
}

async function openVocabList(row: VocabList) {
  await router.push(`/vocab/${row.id}`)
}

async function openClassrooms() {
  await router.push('/classrooms')
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

onMounted(loadHome)
</script>

<style scoped>
h1 {
  font-size: 32px;
  line-height: 1.2;
  margin: 0;
}

h2 {
  font-size: 18px;
  margin: 0 0 8px;
}

p {
  color: #64748b;
  margin: 8px 0 0;
}

.topbar {
  align-items: center;
  background: #142033;
  border: 1px solid #23324a;
  border-radius: 8px;
  color: #f8fafc;
  display: flex;
  gap: 24px;
  justify-content: space-between;
  margin-bottom: 22px;
  padding: 28px 30px;
}

.topbar p {
  color: #cbd5e1;
}

.top-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
}

.top-actions :deep(.v-btn) {
  border-radius: 4px;
  letter-spacing: 0;
}

.top-actions :deep(.v-btn--variant-text) {
  color: #f8fafc;
}

.offline-pay-action {
  background: linear-gradient(135deg, #10b981 0%, #0ea5e9 100%) !important;
  border: 1px solid rgba(236, 254, 255, 0.28);
  box-shadow: 0 8px 18px rgba(14, 165, 233, 0.18);
  color: #ffffff !important;
  font-weight: 900;
}

.offline-pay-action:hover {
  box-shadow: 0 10px 22px rgba(16, 185, 129, 0.24);
  filter: brightness(1.05);
}

.offline-pay-action :deep(.v-btn__prepend),
.offline-pay-action :deep(.v-btn__content) {
  color: #ffffff;
}

.membership-action {
  background: linear-gradient(135deg, #f59e0b 0%, #f97316 100%) !important;
  border: 1px solid rgba(255, 237, 213, 0.36);
  box-shadow: 0 8px 18px rgba(249, 115, 22, 0.16);
  color: #ffffff !important;
  font-weight: 900;
}

.membership-action:hover {
  box-shadow: 0 10px 22px rgba(245, 158, 11, 0.24);
  filter: brightness(1.04);
}

.membership-action :deep(.v-btn__prepend),
.membership-action :deep(.v-btn__content) {
  color: #ffffff;
}

.home-payment-dialog {
  border: 1px solid #dbe3ee;
  border-radius: 10px;
  color: #172033;
  padding: 22px;
}

.payment-dialog-head {
  align-items: flex-start;
  display: flex;
  gap: 18px;
  justify-content: space-between;
  margin-bottom: 18px;
}

.payment-dialog-head h2 {
  color: #172033;
  font-size: 24px;
  line-height: 1.25;
  margin: 4px 0 0;
}

.payment-dialog-head p {
  line-height: 1.65;
  margin-top: 8px;
}

.payment-kicker {
  color: #0f766e;
  font-size: 13px;
  font-weight: 900;
}

.payment-method-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.payment-method-card {
  background: #f8fafc;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  padding: 14px;
}

.payment-method-title {
  align-items: center;
  color: #0f766e;
  display: flex;
  font-weight: 900;
  gap: 8px;
  margin-bottom: 10px;
}

.payment-method-card img {
  background: #ffffff;
  border: 1px solid #e5edf7;
  border-radius: 6px;
  display: block;
  height: 240px;
  object-fit: contain;
  padding: 8px;
  width: 100%;
}

.payment-contact-panel {
  background: #f0fdfa;
  border: 1px solid #99f6e4;
  border-radius: 8px;
  display: grid;
  gap: 14px;
  grid-template-columns: minmax(180px, 0.8fr) minmax(0, 1.2fr);
  margin-top: 14px;
  padding: 14px;
}

.payment-contact-panel strong {
  color: #172033;
}

.payment-contact-panel p {
  font-size: 13px;
  line-height: 1.55;
}

.payment-contact-grid {
  display: grid;
  gap: 8px;
}

.payment-contact-button {
  align-items: center;
  background: #ffffff;
  border: 1px solid #dbe3ee;
  border-radius: 6px;
  color: #172033;
  cursor: pointer;
  display: grid;
  font: inherit;
  gap: 8px;
  grid-template-columns: 104px minmax(0, 1fr) 18px;
  min-height: 44px;
  padding: 8px 10px;
  text-align: left;
}

.payment-contact-button:hover {
  border-color: #2dd4bf;
}

.payment-contact-button span {
  align-items: center;
  color: #0f766e;
  display: inline-flex;
  font-weight: 800;
  gap: 6px;
}

.payment-contact-button strong {
  overflow-wrap: anywhere;
}

.status-band {
  background: #ffffff;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin-bottom: 20px;
  overflow: hidden;
  padding: 22px;
  position: relative;
}

.loading-line {
  left: 0;
  position: absolute;
  right: 0;
  top: 0;
}

.label {
  color: #64748b;
  display: block;
  font-size: 13px;
  margin-bottom: 8px;
}

.status-band strong {
  font-size: 26px;
  line-height: 1.15;
}

.feature-card {
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  cursor: pointer;
  min-height: 150px;
  padding: 22px 20px;
  position: relative;
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

.feature-card:hover {
  border-color: #9db8e8;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
  transform: translateY(-2px);
}

.feature-card h2 {
  padding-right: 92px;
}

.feature-icon {
  background: #ecfeff;
  border-radius: 6px;
  color: #0f7f75;
  font-size: 26px;
  height: 44px;
  margin-bottom: 16px;
  padding: 8px;
  width: 44px;
}

.feature-status {
  background: #ecfdf5;
  border: 1px solid #b7ebc9;
  border-radius: 4px;
  color: #15803d;
  font-size: 12px;
  line-height: 1.2;
  padding: 4px 8px;
  position: absolute;
  right: 14px;
  top: 14px;
}

.feature-status.locked {
  background: #fff7ed;
  border-color: #fed7aa;
  color: #c2410c;
}

.feature-card.locked {
  background: #fafafa;
}

.content-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(2, minmax(420px, 1fr));
  margin-top: 20px;
}

.panel {
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  padding: 20px;
}

.panel-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 14px;
}

.panel :deep(.v-table) {
  border: 1px solid #e5edf7;
  border-radius: 6px;
  overflow: hidden;
}

.panel :deep(.v-table__wrapper table thead),
.panel :deep(.v-table__wrapper table thead tr),
.panel :deep(.v-table__wrapper table thead tr th) {
  background: #f8fafc !important;
  background-color: #f8fafc !important;
}

.panel :deep(.v-table__wrapper table thead tr th) {
  border-bottom: 1px solid #e5edf7 !important;
  box-shadow: none !important;
  color: #475569 !important;
  font-size: 13px !important;
  font-weight: 800 !important;
}

.panel :deep(.v-table__wrapper table tbody tr td) {
  border-bottom: 1px solid #eef2f7;
}

.panel :deep(.v-table__wrapper table tbody tr:last-child td) {
  border-bottom: 0;
}

.panel :deep(.v-table__wrapper table tbody tr:hover) {
  background: #f8fbff;
}

.empty-state {
  align-items: center;
  color: #64748b;
  display: flex;
  justify-content: center;
  min-height: 128px;
}

.clickable-row {
  cursor: pointer;
}

@media (max-width: 900px) {
  .topbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .top-actions {
    justify-content: flex-start;
  }

  .status-band,
  .payment-contact-panel,
  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  h1 {
    font-size: 28px;
  }

  .topbar {
    padding: 22px 18px;
  }

  .payment-method-grid {
    grid-template-columns: 1fr;
  }

  .payment-method-card img {
    height: 220px;
  }

  .payment-contact-button {
    grid-template-columns: 1fr 18px;
  }

  .payment-contact-button strong {
    grid-column: 1 / 2;
  }

  .feature-card h2 {
    padding-right: 0;
  }

  .feature-status {
    display: inline-block;
    margin-bottom: 12px;
    position: static;
  }
}
</style>
