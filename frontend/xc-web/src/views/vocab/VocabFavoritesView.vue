<template>
  <main class="app-shell">
    <header class="topbar">
      <div>
        <h1>{{ t('vocab.favoritesTitle') }}</h1>
        <p>{{ t('vocab.favoritesSubtitle') }}</p>
      </div>
      <div class="top-actions">
        <LocaleSwitch />
        <v-btn prepend-icon="mdi-refresh" variant="tonal" :loading="loading" @click="loadFavorites">{{ t('common.refresh') }}</v-btn>
      </div>
    </header>

    <v-card class="panel" elevation="0">
      <v-progress-linear v-if="loading" class="loading-line" color="primary" indeterminate />
      <div class="panel-head">
        <div>
          <span class="panel-kicker">{{ t('vocab.favorites') }}</span>
          <h2>{{ t('vocab.favoriteTotalCount', { count: total }) }}</h2>
        </div>
        <v-btn variant="outlined" prepend-icon="mdi-book-open-page-variant-outline" @click="$router.push('/vocab')">
          {{ t('vocab.browseLists') }}
        </v-btn>
      </div>

      <v-tabs v-model="activeGroup" class="favorite-tabs" color="primary">
        <v-tab value="vocab">{{ t('vocab.favoriteWords') }} ({{ wordTotal }})</v-tab>
        <v-tab value="sentences">{{ t('vocab.favoriteSentences') }} ({{ sentenceTotal }})</v-tab>
      </v-tabs>

      <div v-if="activeGroup === 'vocab' && !wordLoading && wordItems.length === 0" class="empty-state">
        {{ t('vocab.emptyFavoriteWords') }}
      </div>

      <div v-else-if="activeGroup === 'vocab'" class="favorites-grid">
        <v-card v-for="item in wordItems" :key="item.id" class="word-card" elevation="0">
          <div class="word-main">
            <div>
              <h3>{{ item.hanzi }}</h3>
              <p class="pinyin">{{ item.pinyin || '-' }}</p>
            </div>
            <v-btn
              color="warning"
              prepend-icon="mdi-star-off-outline"
              variant="tonal"
              :loading="removingId === item.id"
              @click="removeFavorite(item)"
            >
              {{ t('vocab.unfavorite') }}
            </v-btn>
          </div>
          <div class="meaning-grid">
            <div>
              <span>{{ t('common.russian') }}</span>
              <strong>{{ item.meaningRu || '-' }}</strong>
            </div>
            <div>
              <span>{{ t('common.english') }}</span>
              <strong>{{ item.meaningEn || '-' }}</strong>
            </div>
          </div>
          <p v-if="item.exampleSentence" class="example">{{ item.exampleSentence }}</p>
          <div class="word-actions">
            <v-btn variant="text" prepend-icon="mdi-volume-high" :disabled="!item.hanzi" @click="playPronunciation(item)">
              {{ t('vocab.playPronunciation') }}
            </v-btn>
            <v-btn v-if="item.vocabListId" variant="text" prepend-icon="mdi-arrow-right" @click="$router.push(`/vocab/${item.vocabListId}`)">
              {{ t('vocab.openList') }}
            </v-btn>
          </div>
        </v-card>
      </div>

      <div v-if="activeGroup === 'sentences' && !sentenceLoading && sentenceItems.length === 0" class="empty-state">
        {{ t('vocab.emptyFavoriteSentences') }}
      </div>

      <div v-else-if="activeGroup === 'sentences'" class="favorites-grid sentence-grid">
        <v-card v-for="item in sentenceItems" :key="item.id" class="word-card sentence-card" elevation="0">
          <div class="word-main">
            <div>
              <span class="set-label">{{ item.exerciseSetTitle || t('practice.title') }}</span>
              <h3 class="sentence-answer">{{ item.hanziAnswer }}</h3>
              <p v-if="item.pinyinPrompt" class="pinyin">{{ item.pinyinPrompt }}</p>
            </div>
            <v-btn
              color="warning"
              prepend-icon="mdi-star-off-outline"
              variant="tonal"
              :loading="removingSentenceId === item.id"
              @click="removeSentenceFavorite(item)"
            >
              {{ t('vocab.unfavorite') }}
            </v-btn>
          </div>
          <div class="meaning-grid">
            <div>
              <span>{{ t('common.russian') }}</span>
              <strong>{{ item.translationRu || '-' }}</strong>
            </div>
            <div>
              <span>{{ t('common.english') }}</span>
              <strong>{{ item.translationEn || '-' }}</strong>
            </div>
          </div>
          <div class="word-actions">
            <v-btn variant="text" prepend-icon="mdi-volume-high" :disabled="!item.hanziAnswer" @click="playSentence(item)">
              {{ t('vocab.playPronunciation') }}
            </v-btn>
            <v-btn variant="text" prepend-icon="mdi-arrow-right" @click="$router.push('/practice')">
              {{ t('practice.title') }}
            </v-btn>
          </div>
        </v-card>
      </div>

      <div v-if="activeTotal > 0" class="pager-bar">
        <span>{{ t('vocab.pageSummary', { page: activePage, pages: activePageCount, pageSize, total: activeTotal }) }}</span>
        <v-pagination
          v-model="activePage"
          class="pager"
          density="comfortable"
          :length="activePageCount"
          :total-visible="7"
          @update:model-value="loadActivePage"
        />
      </div>
    </v-card>
  </main>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import LocaleSwitch from '@/components/LocaleSwitch.vue'
import { useSpeechPlayer } from '@/composables/useSpeechPlayer'
import { fetchFavoriteSentenceExercises, unfavoriteSentenceExercise } from '../../api/exercise'
import { fetchFavoriteVocabItems, unfavoriteVocabItem } from '../../api/vocab'
import type { FavoriteSentenceExercise, VocabItem } from '../../types/api'
import { notifySuccess } from '../../utils/notify'

const { t } = useI18n()
const speech = useSpeechPlayer()
const pageSize = 10
const activeGroup = ref<'vocab' | 'sentences'>('vocab')
const wordPage = ref(1)
const sentencePage = ref(1)
const wordTotal = ref(0)
const sentenceTotal = ref(0)
const wordLoading = ref(false)
const sentenceLoading = ref(false)
const removingId = ref<number | null>(null)
const removingSentenceId = ref<number | null>(null)
const wordItems = ref<VocabItem[]>([])
const sentenceItems = ref<FavoriteSentenceExercise[]>([])
const loading = computed(() => wordLoading.value || sentenceLoading.value)
const total = computed(() => wordTotal.value + sentenceTotal.value)
const wordPageCount = computed(() => Math.max(1, Math.ceil(wordTotal.value / pageSize)))
const sentencePageCount = computed(() => Math.max(1, Math.ceil(sentenceTotal.value / pageSize)))
const activeTotal = computed(() => activeGroup.value === 'vocab' ? wordTotal.value : sentenceTotal.value)
const activePageCount = computed(() => activeGroup.value === 'vocab' ? wordPageCount.value : sentencePageCount.value)
const activePage = computed({
  get: () => activeGroup.value === 'vocab' ? wordPage.value : sentencePage.value,
  set: value => {
    if (activeGroup.value === 'vocab') {
      wordPage.value = value
    } else {
      sentencePage.value = value
    }
  }
})

async function loadFavorites() {
  await loadWordFavorites()
  try {
    await loadSentenceFavorites()
  } catch {
    sentenceItems.value = []
    sentenceTotal.value = 0
  }
}

async function loadWordFavorites() {
  wordLoading.value = true
  try {
    const result = await fetchFavoriteVocabItems(wordPage.value, pageSize)
    wordItems.value = result.records
    wordTotal.value = result.total
  } finally {
    wordLoading.value = false
  }
}

async function loadSentenceFavorites() {
  sentenceLoading.value = true
  try {
    const result = await fetchFavoriteSentenceExercises(sentencePage.value, pageSize)
    sentenceItems.value = result.records
    sentenceTotal.value = result.total
  } finally {
    sentenceLoading.value = false
  }
}

async function loadActivePage() {
  if (activeGroup.value === 'vocab') {
    await loadWordFavorites()
    return
  }
  await loadSentenceFavorites()
}

async function removeFavorite(item: VocabItem) {
  removingId.value = item.id
  try {
    await unfavoriteVocabItem(item.id)
    wordItems.value = wordItems.value.filter(record => record.id !== item.id)
    wordTotal.value = Math.max(0, wordTotal.value - 1)
    notifySuccess(t('vocab.unfavorited'))
    if (wordItems.value.length === 0 && wordPage.value > 1) {
      wordPage.value -= 1
      await loadWordFavorites()
    }
  } finally {
    removingId.value = null
  }
}

async function removeSentenceFavorite(item: FavoriteSentenceExercise) {
  removingSentenceId.value = item.id
  try {
    await unfavoriteSentenceExercise(item.id)
    sentenceItems.value = sentenceItems.value.filter(record => record.id !== item.id)
    sentenceTotal.value = Math.max(0, sentenceTotal.value - 1)
    notifySuccess(t('vocab.unfavorited'))
    if (sentenceItems.value.length === 0 && sentencePage.value > 1) {
      sentencePage.value -= 1
      await loadSentenceFavorites()
    }
  } finally {
    removingSentenceId.value = null
  }
}

function playPronunciation(item: VocabItem) {
  speech.playTargetText(item.hanzi, item.audioUrl)
}

function playSentence(item: FavoriteSentenceExercise) {
  speech.playTargetText(item.hanziAnswer, item.audioUrl)
}

onMounted(loadFavorites)
onBeforeUnmount(speech.stop)
</script>

<style scoped>
h1 {
  font-size: 34px;
  line-height: 1.2;
  margin: 0;
}

h2,
h3,
p {
  margin: 0;
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
  margin-top: 8px;
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

.panel {
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  overflow: hidden;
  padding: 24px;
  position: relative;
}

.loading-line {
  left: 0;
  position: absolute;
  right: 0;
  top: 0;
}

.panel-head,
.word-main,
.word-actions {
  align-items: center;
  display: flex;
  gap: 16px;
  justify-content: space-between;
}

.panel-kicker {
  color: #14796f;
  display: block;
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 8px;
}

.empty-state {
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  color: #64748b;
  margin-top: 20px;
  padding: 34px;
  text-align: center;
}

.favorite-tabs {
  margin-top: 18px;
}

.favorites-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 22px;
}

.word-card {
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  padding: 20px;
}

.word-card h3 {
  color: #102033;
  font-family: var(--xc-kai-font-family);
  font-size: 30px;
  line-height: 1.2;
}

.sentence-card h3 {
  font-size: 24px;
  line-height: 1.45;
}

.set-label {
  color: #64748b;
  display: block;
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 8px;
}

.pinyin {
  color: #2563eb;
  font-size: 18px;
  margin-top: 6px;
}

.meaning-grid {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 18px;
}

.meaning-grid div {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 12px;
}

.meaning-grid span {
  color: #64748b;
  display: block;
  font-size: 12px;
  margin-bottom: 6px;
}

.meaning-grid strong {
  color: #102033;
  font-size: 15px;
  line-height: 1.5;
}

.example {
  color: #64748b;
  line-height: 1.6;
  margin-top: 16px;
}

.word-actions {
  border-top: 1px solid #e2e8f0;
  justify-content: flex-end;
  margin-top: 16px;
  padding-top: 12px;
}

.word-actions :deep(.v-btn),
.panel-head :deep(.v-btn),
.word-main :deep(.v-btn) {
  border-radius: 4px;
  letter-spacing: 0;
}

.pager-bar {
  align-items: center;
  border-top: 1px solid #e2e8f0;
  color: #64748b;
  display: flex;
  font-size: 14px;
  gap: 16px;
  justify-content: space-between;
  margin-top: 22px;
  padding-top: 16px;
}

.pager {
  margin-left: auto;
}

@media (max-width: 900px) {
  .favorites-grid,
  .meaning-grid {
    grid-template-columns: 1fr;
  }

  .panel-head,
  .word-main,
  .word-actions,
  .pager-bar {
    align-items: stretch;
    flex-direction: column;
  }

  .pager {
    margin-left: 0;
  }
}
</style>
