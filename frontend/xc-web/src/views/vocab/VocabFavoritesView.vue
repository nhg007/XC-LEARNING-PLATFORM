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
          <h2>{{ t('vocab.favoriteCount', { count: total }) }}</h2>
        </div>
        <v-btn variant="outlined" prepend-icon="mdi-book-open-page-variant-outline" @click="$router.push('/')">
          {{ t('vocab.browseLists') }}
        </v-btn>
      </div>

      <div v-if="!loading && items.length === 0" class="empty-state">
        {{ t('vocab.emptyFavorites') }}
      </div>

      <div v-else class="favorites-grid">
        <v-card v-for="item in items" :key="item.id" class="word-card" elevation="0">
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
            <v-btn variant="text" prepend-icon="mdi-arrow-right" @click="$router.push(`/vocab/${item.vocabListId}`)">
              {{ t('vocab.openList') }}
            </v-btn>
          </div>
        </v-card>
      </div>

      <v-pagination
        v-if="pageCount > 1"
        v-model="page"
        class="pager"
        density="comfortable"
        :length="pageCount"
        @update:model-value="loadFavorites"
      />
    </v-card>
  </main>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import LocaleSwitch from '@/components/LocaleSwitch.vue'
import { useSpeechPlayer } from '@/composables/useSpeechPlayer'
import { fetchFavoriteVocabItems, unfavoriteVocabItem } from '../../api/vocab'
import type { VocabItem } from '../../types/api'
import { notifySuccess } from '../../utils/notify'

const { t } = useI18n()
const speech = useSpeechPlayer()
const page = ref(1)
const pageSize = 20
const total = ref(0)
const loading = ref(false)
const removingId = ref<number | null>(null)
const items = ref<VocabItem[]>([])
const pageCount = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

async function loadFavorites() {
  loading.value = true
  try {
    const result = await fetchFavoriteVocabItems(page.value, pageSize)
    items.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

async function removeFavorite(item: VocabItem) {
  removingId.value = item.id
  try {
    await unfavoriteVocabItem(item.id)
    items.value = items.value.filter(record => record.id !== item.id)
    total.value = Math.max(0, total.value - 1)
    notifySuccess(t('vocab.unfavorited'))
    if (items.value.length === 0 && page.value > 1) {
      page.value -= 1
      await loadFavorites()
    }
  } finally {
    removingId.value = null
  }
}

function playPronunciation(item: VocabItem) {
  speech.playTargetText(item.hanzi, item.audioUrl)
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
  font-size: 30px;
  line-height: 1.2;
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

.pager {
  margin-top: 22px;
}

@media (max-width: 900px) {
  .favorites-grid,
  .meaning-grid {
    grid-template-columns: 1fr;
  }

  .panel-head,
  .word-main,
  .word-actions {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
