<template>
  <main class="app-shell">
    <header class="topbar">
      <div>
        <h1>{{ t('vocab.title') }}</h1>
        <p>{{ progressLabel }}</p>
      </div>
      <div class="top-actions">
        <LocaleSwitch />
        <v-btn prepend-icon="mdi-arrow-left" variant="text" @click="$router.push('/')">{{ t('common.back') }}</v-btn>
        <v-btn prepend-icon="mdi-refresh" variant="tonal" :loading="loading" @click="loadPage">{{ t('common.refresh') }}</v-btn>
      </div>
    </header>

    <v-card v-if="childLists.length > 0" class="scope-panel" elevation="0">
      <v-tabs v-model="scopeTab" color="primary">
        <v-tab value="all">{{ t('vocab.scopeAll') }}</v-tab>
        <v-tab value="lessons">{{ t('vocab.scopeLessons') }}</v-tab>
      </v-tabs>
    </v-card>

    <section v-if="scopeTab === 'lessons' && childLists.length > 0" class="lesson-grid">
      <v-card v-for="lesson in childLists" :key="lesson.id" class="lesson-card" elevation="0" @click="openLesson(lesson.id)">
        <span class="lesson-meta">{{ lesson.level || currentList?.level || t('common.basic') }}</span>
        <h2>{{ lesson.name }}</h2>
        <p>{{ lesson.description || t('vocab.noDescription') }}</p>
        <div class="lesson-footer">
          <span>{{ t('vocab.wordCount', { count: lesson.totalActiveItemCount || lesson.activeItemCount }) }}</span>
          <v-btn append-icon="mdi-arrow-right" variant="text">{{ t('vocab.start') }}</v-btn>
        </div>
      </v-card>
    </section>

    <section v-else class="study-layout">
      <v-progress-linear v-if="loading" class="layout-loader" color="primary" indeterminate />
      <v-card class="word-card" :class="{ flipped }" elevation="0" rounded="lg" @click="flipped = !flipped">
        <template v-if="currentItem">
          <span class="status-badge" :class="{ learned: isCurrentLearned }">{{ currentStatusLabel }}</span>
          <div class="card-face">
            <span class="hanzi">{{ currentItem.hanzi }}</span>
            <span v-if="showPinyin" class="pinyin">{{ currentItem.pinyin || '-' }}</span>
          </div>
          <div class="card-back">
            <span class="meaning">{{ currentMeaning }}</span>
            <span v-if="currentItem.exampleSentence" class="example">{{ currentItem.exampleSentence }}</span>
          </div>
        </template>
        <div v-else class="empty-state">{{ t('vocab.empty') }}</div>
      </v-card>

      <v-card class="side-panel" elevation="0" rounded="lg">
        <v-btn
          block
          append-icon="mdi-chevron-right"
          class="source-switch-btn"
          color="primary"
          prepend-icon="mdi-bookshelf"
          variant="tonal"
          @click="router.push('/vocab')"
        >
          {{ t('vocab.changeList') }}
        </v-btn>
        <span class="control-label">{{ t('common.referenceLanguage') }}</span>
        <v-btn-toggle v-model="meaningLanguage" color="primary" density="comfortable" mandatory variant="outlined">
          <v-btn value="ru">{{ t('common.russian') }}</v-btn>
          <v-btn value="en">{{ t('common.english') }}</v-btn>
        </v-btn-toggle>

        <div class="button-grid">
          <v-btn :disabled="!currentItem" variant="tonal" @click="showPinyin = !showPinyin">
            {{ showPinyin ? t('vocab.hidePinyin') : t('vocab.showPinyin') }}
          </v-btn>
          <v-btn :disabled="!currentItem" variant="tonal" @click="flipped = !flipped">
            {{ flipped ? t('vocab.showHanzi') : t('vocab.showMeaning') }}
          </v-btn>
          <v-btn :color="currentItem?.favorite ? 'warning' : undefined" :disabled="!currentItem" variant="tonal" @click="toggleFavorite">
            {{ currentItem?.favorite ? t('vocab.unfavorite') : t('vocab.favorite') }}
          </v-btn>
          <v-btn
            :disabled="!currentItem"
            prepend-icon="mdi-volume-high"
            :loading="pronunciationLoading"
            variant="tonal"
            @click="playCurrentItemPronunciation"
          >
            {{ t('vocab.playPronunciation') }}
          </v-btn>
          <v-btn
            :disabled="!currentItem"
            prepend-icon="mdi-volume-high"
            :loading="meaningAudioLoading"
            variant="tonal"
            @click="playCurrentItemAudio"
          >
            {{ t('vocab.playAudio') }}
          </v-btn>
        </div>

        <div class="nav-row">
          <v-btn :disabled="currentIndex <= 0" variant="outlined" @click="previousCard">{{ t('vocab.previous') }}</v-btn>
          <v-btn color="primary" :disabled="!currentItem" :loading="saving" @click="nextCard">
            {{ t('vocab.markLearnedNext') }}
          </v-btn>
        </div>
      </v-card>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import LocaleSwitch from '@/components/LocaleSwitch.vue'
import { useSpeechPlayer } from '@/composables/useSpeechPlayer'
import {
  favoriteVocabItem,
  fetchVocabItems,
  fetchVocabList,
  fetchVocabLists,
  fetchVocabProgress,
  unfavoriteVocabItem,
  updateVocabProgress
} from '../../api/vocab'
import { usePreferenceStore } from '../../stores/preferences'
import type { VocabItem, VocabList, VocabProgress } from '../../types/api'
import { notifySuccess } from '../../utils/notify'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const preferences = usePreferenceStore()
const speech = useSpeechPlayer()
const vocabListId = ref(Number(route.params.listId))
const loading = ref(false)
const saving = ref(false)
const flipped = ref(false)
const showPinyin = ref(false)
const meaningLanguage = ref<'ru' | 'en'>('ru')
const currentIndex = ref(0)
const cardStartedAt = ref(Date.now())
const activeSpeechAction = ref<'pronunciation' | 'meaning' | null>(null)
const scopeTab = ref<'all' | 'lessons'>('all')
const currentList = ref<VocabList | null>(null)
const childLists = ref<VocabList[]>([])
const items = ref<VocabItem[]>([])
const progress = ref<VocabProgress>({
  vocabListId: vocabListId.value,
  currentIndex: 0,
  lastVocabItemId: null,
  reviewedCount: 0,
  learnedCount: 0,
  reviewingCount: 0,
  masteredCount: 0,
  totalCount: 0
})
const learnedStatuses = new Set(['learned', 'reviewing', 'mastered'])

const currentItem = computed(() => items.value[currentIndex.value])
const isCurrentLearned = computed(() => {
  const status = currentItem.value?.progressStatus
  return status ? learnedStatuses.has(status) : false
})
const currentStatusLabel = computed(() => {
  switch (currentItem.value?.progressStatus) {
    case 'mastered':
      return t('vocab.mastered')
    case 'reviewing':
      return t('vocab.reviewing')
    case 'learned':
      return t('vocab.learned')
    default:
      return t('vocab.unlearned')
  }
})
const currentMeaning = computed(() => {
  const item = currentItem.value
  if (!item) {
    return ''
  }
  return meaningLanguage.value === 'ru' ? item.meaningRu || '-' : item.meaningEn || '-'
})
const progressLabel = computed(() => {
  const prefix = currentList.value?.name ? `${currentList.value.name} · ` : ''
  if (items.value.length === 0) {
    return `${prefix}0/0`
  }
  return `${prefix}${currentIndex.value + 1}/${progress.value.totalCount || items.value.length}`
})
const pronunciationLoading = computed(() => activeSpeechAction.value === 'pronunciation' && speech.speaking.value)
const meaningAudioLoading = computed(() => activeSpeechAction.value === 'meaning' && speech.speaking.value)

async function loadCards() {
  loading.value = true
  try {
    const [itemPage, currentProgress] = await Promise.all([
      fetchVocabItems(vocabListId.value, 100),
      fetchVocabProgress(vocabListId.value)
    ])
    items.value = itemPage.records
    progress.value = currentProgress
    currentIndex.value = Math.min(currentProgress.currentIndex, Math.max(itemPage.records.length - 1, 0))
    flipped.value = false
    showPinyin.value = false
    resetCardTimer()
  } finally {
    loading.value = false
  }
}

function previousCard() {
  speech.stop()
  currentIndex.value = Math.max(0, currentIndex.value - 1)
  flipped.value = false
  showPinyin.value = false
  resetCardTimer()
}

async function nextCard() {
  const item = currentItem.value
  if (!item) {
    return
  }
  speech.stop()
  const nextIndex = Math.min(currentIndex.value + 1, Math.max(items.value.length - 1, 0))
  const reviewedCount = Math.max(progress.value.reviewedCount, currentIndex.value + 1)
  saving.value = true
  try {
    progress.value = await updateVocabProgress(vocabListId.value, {
      currentIndex: nextIndex,
      lastVocabItemId: item.id,
      reviewedCount,
      itemStatus: 'learned',
      durationSeconds: elapsedSeconds()
    })
    markItemLearned(item)
    currentIndex.value = nextIndex
    flipped.value = false
    showPinyin.value = false
    resetCardTimer()
  } finally {
    saving.value = false
  }
}

async function loadListContext() {
  const [detail, children] = await Promise.all([
    fetchVocabList(vocabListId.value),
    fetchVocabLists({ pageSize: 100, parentId: vocabListId.value })
  ])
  currentList.value = detail
  childLists.value = children.records
  if (childLists.value.length === 0) {
    scopeTab.value = 'all'
  }
}

async function loadPage() {
  loading.value = true
  try {
    await loadListContext()
    await loadCards()
  } finally {
    loading.value = false
  }
}

async function openLesson(id: number) {
  await router.push(`/vocab/${id}`)
}

function markItemLearned(item: VocabItem) {
  const now = new Date().toISOString()
  item.progressStatus = item.progressStatus === 'mastered' ? 'mastered' : 'learned'
  item.reviewCount = (item.reviewCount || 0) + 1
  item.learnedAt = item.learnedAt || now
  item.lastReviewedAt = now
}

function resetCardTimer() {
  cardStartedAt.value = Date.now()
}

function elapsedSeconds() {
  const seconds = Math.round((Date.now() - cardStartedAt.value) / 1000)
  return Math.min(Math.max(seconds, 1), 24 * 60 * 60)
}

async function toggleFavorite() {
  const item = currentItem.value
  if (!item) {
    return
  }
  const result = item.favorite ? await unfavoriteVocabItem(item.id) : await favoriteVocabItem(item.id)
  item.favorite = result.favorite
  notifySuccess(result.favorite ? t('vocab.favorited') : t('vocab.unfavorited'))
}

function playCurrentItemAudio() {
  if (!currentItem.value) {
    return
  }
  activeSpeechAction.value = 'meaning'
  speech.speakText(currentMeaning.value, meaningLanguage.value)
  if (!speech.speaking.value) {
    activeSpeechAction.value = null
  }
}

function playCurrentItemPronunciation() {
  const item = currentItem.value
  if (!item) {
    return
  }
  activeSpeechAction.value = 'pronunciation'
  speech.playTargetText(item.hanzi, item.audioUrl)
  if (!speech.speaking.value) {
    activeSpeechAction.value = null
  }
}

watch(
  () => speech.speaking.value,
  (speaking) => {
    if (!speaking) {
      activeSpeechAction.value = null
    }
  }
)

watch(
  () => preferences.preference?.vocabMeaningLanguage,
  (value) => {
    if (value) {
      meaningLanguage.value = value
    }
  }
)

watch(meaningLanguage, (value) => {
  if (preferences.loaded && preferences.preference?.vocabMeaningLanguage !== value) {
    void preferences.save({ vocabMeaningLanguage: value })
  }
})

onMounted(async () => {
  await preferences.load()
  if (preferences.preference?.vocabMeaningLanguage) {
    meaningLanguage.value = preferences.preference.vocabMeaningLanguage
  }
  await loadPage()
})

watch(
  () => route.params.listId,
  async (value) => {
    vocabListId.value = Number(value)
    scopeTab.value = 'all'
    progress.value = {
      vocabListId: vocabListId.value,
      currentIndex: 0,
      lastVocabItemId: null,
      reviewedCount: 0,
      learnedCount: 0,
      reviewingCount: 0,
      masteredCount: 0,
      totalCount: 0
    }
    await loadPage()
  }
)

onBeforeUnmount(speech.stop)
</script>

<style scoped>
h1 {
  font-size: 34px;
  line-height: 1.2;
  margin: 0;
}

p {
  color: #64748b;
  margin: 8px 0 0;
}

.topbar {
  background: #142033;
  border: 1px solid #23324a;
  border-radius: 8px;
  color: #f8fafc;
  margin-bottom: 22px;
  padding: 30px;
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

.study-layout {
  display: grid;
  gap: 18px;
  grid-template-columns: minmax(0, 1fr) 300px;
}

.scope-panel {
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  margin-bottom: 18px;
  padding: 0 12px;
}

.scope-panel :deep(.v-tab) {
  letter-spacing: 0;
}

.lesson-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.lesson-card {
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  cursor: pointer;
  display: grid;
  gap: 12px;
  min-height: 178px;
  padding: 20px;
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

.lesson-card:hover {
  border-color: #9db8e8;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
  transform: translateY(-2px);
}

.lesson-card h2 {
  color: #172033;
  font-size: 21px;
  line-height: 1.25;
  margin: 0;
}

.lesson-card p {
  margin: 0;
}

.lesson-meta {
  background: #eef4ff;
  border: 1px solid #c7d7fe;
  border-radius: 4px;
  color: #1d4ed8;
  display: inline-flex;
  font-size: 13px;
  font-weight: 700;
  justify-self: start;
  line-height: 1.2;
  padding: 5px 9px;
}

.lesson-footer {
  align-items: center;
  align-self: end;
  border-top: 1px solid #e5edf6;
  color: #475569;
  display: flex;
  justify-content: space-between;
  padding-top: 12px;
}

.layout-loader {
  grid-column: 1 / -1;
}

.word-card {
  align-items: center;
  border: 1px solid #e5e7eb;
  cursor: pointer;
  display: flex;
  justify-content: center;
  min-height: 430px;
  padding: 28px;
  position: relative;
}

.status-badge {
  background: #f8fafc;
  border: 1px solid #cbd5e1;
  border-radius: 4px;
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
  line-height: 1;
  padding: 8px 10px;
  position: absolute;
  right: 18px;
  top: 18px;
}

.status-badge.learned {
  background: #e8f7ef;
  border-color: #9bd7b4;
  color: #166534;
}

.card-face,
.card-back {
  align-items: center;
  display: flex;
  flex-direction: column;
  gap: 18px;
  text-align: center;
}

.card-back {
  display: none;
}

.word-card.flipped .card-face {
  display: none;
}

.word-card.flipped .card-back {
  display: flex;
}

.hanzi {
  font-family: "Kaiti SC", "STKaiti", "KaiTi", "楷体", "楷体_GB2312", serif;
  font-size: 88px;
  font-weight: 700;
  line-height: 1.1;
}

.pinyin {
  color: #2563eb;
  font-size: 28px;
}

.meaning {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.45;
}

.example {
  color: #64748b;
  line-height: 1.7;
  max-width: 620px;
}

.side-panel {
  border: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 18px;
}

.side-panel :deep(.v-btn) {
  letter-spacing: 0;
}

.source-switch-btn {
  border: 1px solid #b7cff8;
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.08);
}

.source-switch-btn :deep(.v-btn__content) {
  font-weight: 800;
}

.source-switch-btn :deep(.v-btn__append) {
  margin-left: auto;
}

.control-label {
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
}

.button-grid {
  display: grid;
  gap: 10px;
}

.empty-state {
  color: #64748b;
}

.nav-row {
  display: grid;
  gap: 10px;
  grid-template-columns: 1fr 1fr;
  margin-top: auto;
}

@media (max-width: 760px) {
  .topbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .top-actions {
    justify-content: flex-start;
  }

  .study-layout {
    grid-template-columns: 1fr;
  }

  .lesson-grid {
    grid-template-columns: 1fr;
  }

  .word-card {
    min-height: 320px;
  }

  .hanzi {
    font-size: 64px;
  }
}

@media (max-width: 560px) {
  h1 {
    font-size: 28px;
  }

  .topbar {
    padding: 20px 16px;
  }
}
</style>
