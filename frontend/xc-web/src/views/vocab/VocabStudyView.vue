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
        <v-btn prepend-icon="mdi-refresh" variant="tonal" :loading="loading" @click="loadCards">{{ t('common.refresh') }}</v-btn>
      </div>
    </header>

    <section class="study-layout">
      <v-progress-linear v-if="loading" class="layout-loader" color="primary" indeterminate />
      <v-card class="word-card" :class="{ flipped }" elevation="0" rounded="lg" @click="flipped = !flipped">
        <template v-if="currentItem">
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
          <v-btn :disabled="!currentItem" prepend-icon="mdi-volume-high" :loading="speech.speaking.value" variant="tonal" @click="playCurrentItemPronunciation">{{ t('vocab.playPronunciation') }}</v-btn>
          <v-btn :disabled="!currentItem" prepend-icon="mdi-volume-high" :loading="speech.speaking.value" variant="tonal" @click="playCurrentItemAudio">{{ t('vocab.playAudio') }}</v-btn>
        </div>

        <div class="nav-row">
          <v-btn :disabled="currentIndex <= 0" variant="outlined" @click="previousCard">{{ t('vocab.previous') }}</v-btn>
          <v-btn color="primary" :disabled="!currentItem" :loading="saving" @click="nextCard">
            {{ t('vocab.next') }}
          </v-btn>
        </div>
      </v-card>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import LocaleSwitch from '@/components/LocaleSwitch.vue'
import { useSpeechPlayer } from '@/composables/useSpeechPlayer'
import {
  favoriteVocabItem,
  fetchVocabItems,
  fetchVocabProgress,
  unfavoriteVocabItem,
  updateVocabProgress
} from '../../api/vocab'
import { usePreferenceStore } from '../../stores/preferences'
import type { VocabItem, VocabProgress } from '../../types/api'
import { notifySuccess } from '../../utils/notify'

const route = useRoute()
const { t } = useI18n()
const preferences = usePreferenceStore()
const speech = useSpeechPlayer()
const vocabListId = Number(route.params.listId)
const loading = ref(false)
const saving = ref(false)
const flipped = ref(false)
const showPinyin = ref(false)
const meaningLanguage = ref<'ru' | 'en'>('ru')
const currentIndex = ref(0)
const items = ref<VocabItem[]>([])
const progress = ref<VocabProgress>({
  vocabListId,
  currentIndex: 0,
  lastVocabItemId: null,
  reviewedCount: 0,
  totalCount: 0
})

const currentItem = computed(() => items.value[currentIndex.value])
const currentMeaning = computed(() => {
  const item = currentItem.value
  if (!item) {
    return ''
  }
  return meaningLanguage.value === 'ru' ? item.meaningRu || '-' : item.meaningEn || '-'
})
const progressLabel = computed(() => {
  if (items.value.length === 0) {
    return '0/0'
  }
  return `${currentIndex.value + 1}/${progress.value.totalCount || items.value.length}`
})

async function loadCards() {
  loading.value = true
  try {
    const [itemPage, currentProgress] = await Promise.all([
      fetchVocabItems(vocabListId, 100),
      fetchVocabProgress(vocabListId)
    ])
    items.value = itemPage.records
    progress.value = currentProgress
    currentIndex.value = Math.min(currentProgress.currentIndex, Math.max(itemPage.records.length - 1, 0))
    flipped.value = false
    showPinyin.value = false
  } finally {
    loading.value = false
  }
}

function previousCard() {
  speech.stop()
  currentIndex.value = Math.max(0, currentIndex.value - 1)
  flipped.value = false
  showPinyin.value = false
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
    progress.value = await updateVocabProgress(vocabListId, {
      currentIndex: nextIndex,
      lastVocabItemId: item.id,
      reviewedCount
    })
    currentIndex.value = nextIndex
    flipped.value = false
    showPinyin.value = false
  } finally {
    saving.value = false
  }
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
  speech.speakText(currentMeaning.value, meaningLanguage.value)
}

function playCurrentItemPronunciation() {
  const item = currentItem.value
  if (!item) {
    return
  }
  speech.playTargetText(item.hanzi, item.audioUrl)
}

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
  await loadCards()
})

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
  font-size: 88px;
  font-weight: 800;
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
