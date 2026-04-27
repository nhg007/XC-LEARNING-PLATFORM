<template>
  <main class="app-shell">
    <header class="topbar">
      <div>
        <h1>{{ t('app.title') }}</h1>
        <p>{{ welcomeText }}</p>
      </div>
      <div class="top-actions">
        <LocaleSwitch />
        <v-btn prepend-icon="mdi-refresh" variant="tonal" :loading="loading" @click="loadHome">{{ t('common.refresh') }}</v-btn>
        <v-btn prepend-icon="mdi-logout" variant="text" @click="logout">{{ t('common.logout') }}</v-btn>
      </div>
    </header>

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
        :key="item.title"
        :class="{ locked: item.requiresFullAccess && !membership.fullAccess }"
        elevation="0"
        rounded="lg"
        @click="openFeature(item.key)"
      >
        <v-chip class="feature-chip" size="small" :color="item.requiresFullAccess && !membership.fullAccess ? 'warning' : 'success'">
          {{ item.requiresFullAccess && !membership.fullAccess ? t('common.memberRequired') : t('common.available') }}
        </v-chip>
        <h2>{{ item.title }}</h2>
        <p>{{ item.description }}</p>
      </v-card>
    </section>

    <section class="content-grid">
      <v-card class="panel" elevation="0" rounded="lg">
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
      <v-card class="panel" elevation="0" rounded="lg">
        <div class="panel-title">{{ t('home.myClasses') }}</div>
        <div v-if="classes.length === 0" class="empty-state">{{ t('home.emptyClasses') }}</div>
        <v-table v-else density="comfortable">
          <thead>
            <tr>
              <th>{{ t('home.table.className') }}</th>
              <th>{{ t('home.table.role') }}</th>
              <th>{{ t('home.table.inviteCode') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in classes" :key="item.id" class="clickable-row" @click="openClassrooms">
              <td>{{ item.name }}</td>
              <td>{{ item.memberRole }}</td>
              <td>{{ item.inviteCode }}</td>
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
import { fetchClassRooms, fetchLearningSummary, fetchMembershipStatus, fetchVocabLists } from '../../api/learning'
import { useSessionStore } from '../../stores/session'
import type { ClassRoom, LearningSummary, MembershipStatus, VocabList } from '../../types/api'
import { notifyInfo, notifyWarning } from '../../utils/notify'

const router = useRouter()
const session = useSessionStore()
const { t } = useI18n()
const loading = ref(false)
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
const features = computed(() => [
  { key: 'vocab', title: t('home.features.vocab.title'), description: t('home.features.vocab.description'), requiresFullAccess: false },
  { key: 'practice', title: t('home.features.practice.title'), description: t('home.features.practice.description'), requiresFullAccess: true },
  { key: 'dialogue', title: t('home.features.dialogue.title'), description: t('home.features.dialogue.description'), requiresFullAccess: true },
  { key: 'matching', title: t('home.features.matching.title'), description: t('home.features.matching.description'), requiresFullAccess: true },
  { key: 'classroom', title: t('home.features.classroom.title'), description: t('home.features.classroom.description'), requiresFullAccess: false },
  { key: 'records', title: t('home.features.records.title'), description: t('home.features.records.description'), requiresFullAccess: false }
])

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
    vocabLists.value = vocab.records
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
  if (key === 'vocab' && vocabLists.value.length > 0) {
    await openVocabList(vocabLists.value[0])
    return
  }
  if (key === 'practice') {
    if (!membership.value.fullAccess) {
      notifyWarning(t('home.featureLocked'))
      return
    }
    await router.push('/practice')
    return
  }
  if (key === 'classroom') {
    await router.push('/classrooms')
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

onMounted(loadHome)
</script>

<style scoped>
h1 {
  font-size: 28px;
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

.top-actions {
  display: flex;
  gap: 10px;
}

.status-band {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  margin-bottom: 20px;
  padding: 18px;
  position: relative;
  overflow: hidden;
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
  font-size: 22px;
}

.feature-card {
  border: 1px solid #e5e7eb;
  cursor: pointer;
  min-height: 132px;
  padding: 18px;
  position: relative;
}

.feature-chip {
  position: absolute;
  right: 14px;
  top: 14px;
}

.feature-card.locked {
  background: #fafafa;
}

.content-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  margin-top: 20px;
}

.panel {
  border: 1px solid #e5e7eb;
  padding: 18px;
}

.panel-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 14px;
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
</style>
