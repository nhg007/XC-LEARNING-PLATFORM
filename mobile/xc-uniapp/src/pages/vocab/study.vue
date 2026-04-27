<template>
  <view class="page">
    <view class="header">
      <text class="title">背单词</text>
      <text class="subtitle">{{ progressLabel }}</text>
    </view>

    <view class="card" @click="flipped = !flipped">
      <template v-if="currentItem">
        <view v-if="!flipped" class="face">
          <text class="hanzi">{{ currentItem.hanzi }}</text>
          <text v-if="showPinyin" class="pinyin">{{ currentItem.pinyin || '-' }}</text>
        </view>
        <view v-else class="face">
          <text class="meaning">{{ currentMeaning }}</text>
          <text v-if="currentItem.exampleSentence" class="example">{{ currentItem.exampleSentence }}</text>
        </view>
      </template>
      <text v-else class="muted">暂无词汇</text>
    </view>

    <view class="language-row">
      <button class="small-button" :class="{ active: meaningLanguage === 'ru' }" @click="meaningLanguage = 'ru'">俄语</button>
      <button class="small-button" :class="{ active: meaningLanguage === 'en' }" @click="meaningLanguage = 'en'">英语</button>
    </view>

    <view class="actions">
      <button @click="showPinyin = !showPinyin">{{ showPinyin ? '隐藏拼音' : '显示拼音' }}</button>
      <button @click="flipped = !flipped">{{ flipped ? '看汉字' : '看释义' }}</button>
      <button @click="toggleFavorite">{{ currentItem?.favorite ? '取消收藏' : '收藏' }}</button>
    </view>

    <view class="nav-row">
      <button :disabled="currentIndex <= 0" @click="previousCard">上一词</button>
      <button class="primary" :loading="saving" :disabled="!currentItem" @click="nextCard">下一词</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import {
  favoriteVocabItem,
  fetchVocabItems,
  fetchVocabProgress,
  unfavoriteVocabItem,
  updateVocabProgress
} from '../../api/vocab'
import type { VocabItem, VocabProgress } from '../../types/api'

const vocabListId = ref(0)
const saving = ref(false)
const flipped = ref(false)
const showPinyin = ref(false)
const meaningLanguage = ref<'ru' | 'en'>('ru')
const currentIndex = ref(0)
const items = ref<VocabItem[]>([])
const progress = ref<VocabProgress>({
  vocabListId: 0,
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
  const [itemPage, currentProgress] = await Promise.all([
    fetchVocabItems(vocabListId.value),
    fetchVocabProgress(vocabListId.value)
  ])
  items.value = itemPage.records
  progress.value = currentProgress
  currentIndex.value = Math.min(currentProgress.currentIndex, Math.max(itemPage.records.length - 1, 0))
  flipped.value = false
  showPinyin.value = false
}

function previousCard() {
  currentIndex.value = Math.max(0, currentIndex.value - 1)
  flipped.value = false
  showPinyin.value = false
}

async function nextCard() {
  const item = currentItem.value
  if (!item) {
    return
  }
  const nextIndex = Math.min(currentIndex.value + 1, Math.max(items.value.length - 1, 0))
  const reviewedCount = Math.max(progress.value.reviewedCount, currentIndex.value + 1)
  saving.value = true
  try {
    progress.value = await updateVocabProgress(vocabListId.value, {
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
  void uni.showToast({ icon: 'none', title: result.favorite ? '已收藏' : '已取消收藏' })
}

onLoad((query) => {
  vocabListId.value = Number(query?.listId || 0)
  if (!vocabListId.value) {
    void uni.navigateBack()
    return
  }
  void loadCards()
})
</script>

<style scoped>
.page {
  box-sizing: border-box;
  padding: 24rpx;
}

.header {
  margin-bottom: 24rpx;
}

.title {
  display: block;
  font-size: 44rpx;
  font-weight: 700;
}

.subtitle {
  color: #64748b;
  display: block;
  font-size: 26rpx;
  margin-top: 10rpx;
}

.card {
  align-items: center;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8rpx;
  box-sizing: border-box;
  display: flex;
  justify-content: center;
  min-height: 520rpx;
  padding: 36rpx;
}

.face {
  align-items: center;
  display: flex;
  flex-direction: column;
  gap: 22rpx;
  text-align: center;
}

.hanzi {
  font-size: 104rpx;
  font-weight: 800;
  line-height: 1.1;
}

.pinyin {
  color: #2563eb;
  font-size: 34rpx;
}

.meaning {
  font-size: 40rpx;
  font-weight: 700;
  line-height: 1.45;
}

.example,
.muted {
  color: #64748b;
  font-size: 26rpx;
  line-height: 1.7;
}

.language-row,
.actions,
.nav-row {
  display: flex;
  gap: 16rpx;
  margin-top: 22rpx;
}

.actions {
  flex-wrap: wrap;
}

button {
  border-radius: 8rpx;
  flex: 1;
  font-size: 28rpx;
}

.small-button {
  background: #ffffff;
}

.small-button.active,
.primary {
  background: #2563eb;
  color: #ffffff;
}
</style>
