<template>
  <view class="page">
    <view class="hero">
      <view class="hero-copy">
        <text class="eyebrow">{{ t('app.title') }}</text>
        <text class="title">{{ t('vocab.favoritesTitle') }}</text>
        <text class="subtitle">{{ t('vocab.favoritesSubtitle') }}</text>
      </view>
      <LanguageSwitch variant="hero" />
    </view>

    <view class="summary-card">
      <view>
        <text class="summary-label">{{ t('vocab.favorites') }}</text>
        <text class="summary-title">{{ t('vocab.favoriteTotalCount', { count: total }) }}</text>
      </view>
      <button class="outline-btn" size="mini" @click="openPage(routes.vocab)">{{ t('vocab.browseLists') }}</button>
    </view>

    <view class="group-tabs">
      <button class="group-tab" :class="{ active: activeGroup === 'vocab' }" @click="activeGroup = 'vocab'">
        {{ t('vocab.favoriteWords') }} ({{ wordTotal }})
      </button>
      <button class="group-tab" :class="{ active: activeGroup === 'sentences' }" @click="activeGroup = 'sentences'">
        {{ t('vocab.favoriteSentences') }} ({{ sentenceTotal }})
      </button>
    </view>

    <view v-if="loading && activeItemsEmpty" class="state">{{ t('common.loading') }}</view>
    <view v-else-if="error" class="state">
      <text>{{ error }}</text>
      <button class="plain-btn" size="mini" @click="loadFavorites">{{ t('common.retry') }}</button>
    </view>
    <view v-else-if="activeGroup === 'vocab' && wordItems.length === 0" class="state">{{ t('vocab.emptyFavoriteWords') }}</view>
    <view v-else-if="activeGroup === 'sentences' && sentenceItems.length === 0" class="state">{{ t('vocab.emptyFavoriteSentences') }}</view>

    <view v-else-if="activeGroup === 'vocab'" class="favorite-list">
      <view v-for="item in wordItems" :key="item.id" class="favorite-card">
        <view class="word-top">
          <view>
            <text class="hanzi">{{ item.hanzi }}</text>
            <text class="pinyin">{{ item.pinyin || '-' }}</text>
          </view>
          <button class="remove-btn" size="mini" :loading="removingId === item.id" @click="removeFavorite(item)">
            {{ t('vocab.unfavorite') }}
          </button>
        </view>

        <view class="meaning-grid">
          <view class="meaning-box">
            <text class="meaning-label">{{ t('common.ru') }}</text>
            <text class="meaning-text">{{ item.meaningRu || '-' }}</text>
          </view>
          <view class="meaning-box">
            <text class="meaning-label">{{ t('common.en') }}</text>
            <text class="meaning-text">{{ item.meaningEn || '-' }}</text>
          </view>
        </view>

        <text v-if="item.exampleSentence" class="example">{{ item.exampleSentence }}</text>

        <view class="card-footer">
          <text class="muted">#{{ item.id }}</text>
          <button class="text-btn" size="mini" @click="openVocabStudy(item.vocabListId)">{{ t('vocab.openList') }}</button>
        </view>
      </view>
    </view>

    <view v-else class="favorite-list">
      <view v-for="item in sentenceItems" :key="item.id" class="favorite-card">
        <view class="word-top">
          <view>
            <text class="set-label">{{ item.exerciseSetTitle || t('practice.title') }}</text>
            <text class="hanzi sentence">{{ item.hanziAnswer }}</text>
            <text v-if="item.pinyinPrompt" class="pinyin">{{ item.pinyinPrompt }}</text>
          </view>
          <button class="remove-btn" size="mini" :loading="removingSentenceId === item.id" @click="removeSentenceFavorite(item)">
            {{ t('vocab.unfavorite') }}
          </button>
        </view>

        <view class="meaning-grid">
          <view class="meaning-box">
            <text class="meaning-label">{{ t('common.ru') }}</text>
            <text class="meaning-text">{{ item.translationRu || '-' }}</text>
          </view>
          <view class="meaning-box">
            <text class="meaning-label">{{ t('common.en') }}</text>
            <text class="meaning-text">{{ item.translationEn || '-' }}</text>
          </view>
        </view>

        <view class="card-footer">
          <text class="muted">#{{ item.id }}</text>
          <button class="text-btn" size="mini" @click="openPage(routes.practice)">{{ t('practice.title') }}</button>
        </view>
      </view>
    </view>

    <button v-if="canLoadMore" class="plain-btn load-more" :loading="loadingMore" @click="loadMore">{{ t('vocab.loadMore') }}</button>
    <text v-else-if="!activeItemsEmpty" class="end-text">{{ t('vocab.noMoreFavorites') }}</text>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import LanguageSwitch from '../../components/LanguageSwitch.vue'
import { fetchFavoriteSentenceExercises, unfavoriteSentenceExercise } from '../../api/exercise'
import { fetchFavoriteVocabItems, unfavoriteVocabItem } from '../../api/vocab'
import { applyTabBarLocale, setPageTitle, useI18n } from '../../i18n'
import type { FavoriteSentenceExercise, VocabItem } from '../../types/api'
import { openPage, openVocabStudy, requireLogin, routes } from '../../utils/navigation'

const { locale, t } = useI18n()
const pageSize = 20
const activeGroup = ref<'vocab' | 'sentences'>('vocab')
const wordPage = ref(1)
const sentencePage = ref(1)
const wordTotal = ref(0)
const sentenceTotal = ref(0)
const loading = ref(false)
const loadingMore = ref(false)
const error = ref('')
const removingId = ref<number | null>(null)
const removingSentenceId = ref<number | null>(null)
const wordItems = ref<VocabItem[]>([])
const sentenceItems = ref<FavoriteSentenceExercise[]>([])
const total = computed(() => wordTotal.value + sentenceTotal.value)
const activeItemsEmpty = computed(() => activeGroup.value === 'vocab' ? wordItems.value.length === 0 : sentenceItems.value.length === 0)
const canLoadMore = computed(() => activeGroup.value === 'vocab' ? wordItems.value.length < wordTotal.value : sentenceItems.value.length < sentenceTotal.value)

onShow(() => {
  applyTabBarLocale()
  setPageTitle('vocab.favoritesTitle')
  if (!requireLogin()) {
    return
  }
  void loadFavorites()
})

watch(locale, () => {
  applyTabBarLocale()
  setPageTitle('vocab.favoritesTitle')
})

onPullDownRefresh(() => {
  void loadFavorites().finally(() => uni.stopPullDownRefresh())
})

onReachBottom(() => {
  if (canLoadMore.value && !loadingMore.value) {
    void loadMore()
  }
})

async function loadFavorites() {
  loading.value = true
  error.value = ''
  wordPage.value = 1
  sentencePage.value = 1
  try {
    const wordResult = await fetchFavoriteVocabItems(1, pageSize)
    wordItems.value = wordResult.records
    wordTotal.value = wordResult.total
    try {
      const sentenceResult = await fetchFavoriteSentenceExercises(1, pageSize)
      sentenceItems.value = sentenceResult.records
      sentenceTotal.value = sentenceResult.total
    } catch {
      sentenceItems.value = []
      sentenceTotal.value = 0
    }
  } catch {
    error.value = t('common.requestFailed')
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  if (loadingMore.value || !canLoadMore.value) {
    return
  }
  loadingMore.value = true
  try {
    if (activeGroup.value === 'vocab') {
      const nextPage = wordPage.value + 1
      const result = await fetchFavoriteVocabItems(nextPage, pageSize)
      wordItems.value = wordItems.value.concat(result.records)
      wordTotal.value = result.total
      wordPage.value = nextPage
    } else {
      const nextPage = sentencePage.value + 1
      const result = await fetchFavoriteSentenceExercises(nextPage, pageSize)
      sentenceItems.value = sentenceItems.value.concat(result.records)
      sentenceTotal.value = result.total
      sentencePage.value = nextPage
    }
  } catch {
    void uni.showToast({ icon: 'none', title: t('common.requestFailed') })
  } finally {
    loadingMore.value = false
  }
}

async function removeFavorite(item: VocabItem) {
  removingId.value = item.id
  try {
    await unfavoriteVocabItem(item.id)
    wordItems.value = wordItems.value.filter(record => record.id !== item.id)
    wordTotal.value = Math.max(0, wordTotal.value - 1)
    void uni.showToast({ icon: 'none', title: t('vocab.unfavorited') })
    if (wordItems.value.length === 0 && wordPage.value > 1) {
      wordPage.value -= 1
      await loadFavorites()
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
    void uni.showToast({ icon: 'none', title: t('vocab.unfavorited') })
    if (sentenceItems.value.length === 0 && sentencePage.value > 1) {
      sentencePage.value -= 1
      await loadFavorites()
    }
  } finally {
    removingSentenceId.value = null
  }
}
</script>

<style scoped>
.page {
  background: #eef5f7;
  min-height: 100vh;
  padding: 0 24rpx 36rpx;
}

.hero {
  align-items: flex-start;
  background: #102033;
  border-bottom-left-radius: 32rpx;
  border-bottom-right-radius: 32rpx;
  box-sizing: border-box;
  color: #ffffff;
  display: flex;
  justify-content: space-between;
  margin: 0 -24rpx 22rpx;
  padding: 30rpx 24rpx 28rpx;
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
  line-height: 1.55;
  margin-top: 14rpx;
}

.summary-card,
.favorite-card {
  background: #ffffff;
  border: 1px solid #d7e2ea;
  border-radius: 18rpx;
  box-shadow: 0 10rpx 30rpx rgba(15, 23, 42, 0.05);
  box-sizing: border-box;
}

.summary-card {
  align-items: center;
  display: flex;
  justify-content: space-between;
  margin-bottom: 18rpx;
  padding: 24rpx;
}

.summary-label,
.meaning-label,
.muted {
  color: #64748b;
  font-size: 24rpx;
}

.summary-title {
  color: #102033;
  display: block;
  font-size: 34rpx;
  font-weight: 700;
  margin-top: 6rpx;
}

.group-tabs {
  background: #ffffff;
  border: 1px solid #d7e2ea;
  border-radius: 18rpx;
  display: grid;
  gap: 8rpx;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-bottom: 18rpx;
  padding: 8rpx;
}

.group-tab {
  background: transparent;
  border: 0;
  color: #64748b;
  font-size: 26rpx;
  font-weight: 800;
  margin: 0;
}

.group-tab.active {
  background: #e6f4ff;
  color: #14796f;
}

.favorite-list {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.favorite-card {
  padding: 24rpx;
}

.word-top,
.card-footer {
  align-items: center;
  display: flex;
  gap: 16rpx;
  justify-content: space-between;
}

.hanzi {
  color: #102033;
  display: block;
  font-size: 46rpx;
  font-weight: 800;
  line-height: 1.2;
}

.hanzi.sentence {
  font-size: 34rpx;
  line-height: 1.45;
}

.set-label {
  color: #64748b;
  display: block;
  font-size: 23rpx;
  font-weight: 700;
  margin-bottom: 8rpx;
}

.pinyin {
  color: #2563eb;
  display: block;
  font-size: 28rpx;
  margin-top: 6rpx;
}

.meaning-grid {
  display: grid;
  gap: 14rpx;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 20rpx;
}

.meaning-box {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 14rpx;
  padding: 16rpx;
}

.meaning-text {
  color: #102033;
  display: block;
  font-size: 27rpx;
  font-weight: 600;
  line-height: 1.45;
  margin-top: 6rpx;
}

.example {
  color: #64748b;
  display: block;
  font-size: 25rpx;
  line-height: 1.55;
  margin-top: 18rpx;
}

.card-footer {
  border-top: 1px solid #e2e8f0;
  margin-top: 20rpx;
  padding-top: 16rpx;
}

button {
  align-items: center;
  border-radius: 12rpx;
  display: flex;
  justify-content: center;
  line-height: 1;
  min-height: 64rpx;
  padding: 0 22rpx;
}

.outline-btn,
.plain-btn,
.text-btn,
.remove-btn {
  background: #ffffff;
  border: 1px solid #d2dce6;
  color: #102033;
  font-size: 25rpx;
  font-weight: 700;
  margin: 0;
}

.remove-btn {
  color: #b45309;
  flex-shrink: 0;
}

.text-btn {
  border-color: transparent;
  color: #14796f;
  font-weight: 700;
}

.state {
  align-items: center;
  background: #ffffff;
  border: 1px dashed #cbd5e1;
  border-radius: 18rpx;
  color: #64748b;
  display: flex;
  flex-direction: column;
  gap: 14rpx;
  padding: 42rpx 24rpx;
  text-align: center;
}

.load-more,
.end-text {
  display: block;
  margin-top: 22rpx;
  text-align: center;
}

.end-text {
  color: #64748b;
  font-size: 24rpx;
}
</style>
