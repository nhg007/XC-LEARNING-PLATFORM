<template>
  <view class="page">
    <view class="hero">
      <view class="hero-top">
        <view class="hero-copy">
          <text class="eyebrow">{{ t('app.title') }}</text>
          <text class="title">{{ t('matching.title') }}</text>
          <text class="subtitle">{{ t('matching.subtitle') }}</text>
        </view>
        <LanguageSwitch variant="hero" />
      </view>
    </view>

    <view v-if="setupLoading && !session" class="state-card">{{ t('common.loading') }}</view>

    <view v-else-if="setupError && !session" class="state-card error-card">
      <text>{{ setupError }}</text>
      <button class="plain-btn compact" size="mini" @click="prepareSetup(true)">{{ t('common.retry') }}</button>
    </view>

    <view v-else-if="!session" class="setup-card">
      <view class="setup-head">
        <view>
          <text class="section-title">{{ t('matching.setup') }}</text>
          <text class="muted">{{ t('matching.setupDesc') }}</text>
        </view>
        <text class="access-pill">{{ t('features.freeAccess') }}</text>
      </view>

      <view class="field">
        <text class="field-label">{{ t('matching.source') }}</text>
        <view class="segmented">
          <button class="segment" :class="{ active: sourceType === 'vocab_list' }" @click="sourceType = 'vocab_list'">{{ t('matching.sourceVocabList') }}</button>
          <button class="segment" :class="{ active: sourceType === 'favorites' }" @click="sourceType = 'favorites'">{{ t('matching.favorites') }}</button>
        </view>
      </view>

      <view v-if="sourceType === 'vocab_list'" class="field">
        <text class="field-label">{{ t('matching.vocabList') }}</text>
        <view v-if="vocabLists.length === 0" class="hint-card">{{ t('matching.emptyVocabLists') }}</view>
        <picker :range="vocabListNames" :value="selectedListIndex" @change="changeVocabList">
          <view class="picker-box">
            <text>{{ selectedListLabel }}</text>
          </view>
        </picker>
      </view>

      <view class="field">
        <text class="field-label">{{ t('matching.referenceLanguage') }}</text>
        <view class="segmented">
          <button class="segment" :class="{ active: meaningLanguage === 'ru' }" @click="meaningLanguage = 'ru'">{{ t('common.ru') }}</button>
          <button class="segment" :class="{ active: meaningLanguage === 'en' }" @click="meaningLanguage = 'en'">{{ t('common.en') }}</button>
        </view>
      </view>

      <view class="field">
        <text class="field-label">{{ t('matching.difficulty') }}</text>
        <view class="difficulty-grid">
          <button v-for="item in difficultyOptions" :key="item" class="difficulty-btn" :class="{ active: difficulty === item }" @click="difficulty = item">
            {{ item }}
          </button>
        </view>
      </view>

      <button class="primary-btn" :loading="starting" :disabled="!canStart" @click="startGame">{{ t('matching.start') }}</button>
    </view>

    <template v-else>
      <view class="status-card">
        <view>
          <text class="muted">{{ t('matching.matched') }}</text>
          <text class="status-value">{{ matchedPairs }} / {{ session.totalPairs }}</text>
        </view>
        <view>
          <text class="muted">{{ t('matching.wrong') }}</text>
          <text class="status-value">{{ wrongCount }}</text>
        </view>
        <view>
          <text class="muted">{{ t('matching.time') }}</text>
          <text class="status-value">{{ elapsedLabel }}</text>
        </view>
      </view>

      <view class="board" :class="{ compact: tiles.length > 14 }">
        <button
          v-for="tile in tiles"
          :key="tile.id"
          class="tile"
          :class="{ selected: tile.selected, matched: tile.matched, meaning: tile.kind === 'meaning' }"
          :disabled="tile.matched || gameCompleted"
          @click="selectTile(tile.id)"
        >
          <text class="tile-main">{{ tile.text }}</text>
          <text v-if="tile.subtext" class="tile-sub">{{ tile.subtext }}</text>
        </button>
      </view>

      <view v-if="gameCompleted" class="completed-card">
        <text class="completed-title">{{ t('matching.completedTitle') }}</text>
        <text class="muted">{{ t('matching.completedSummary', { wrong: wrongCount, time: elapsedLabel }) }}</text>
      </view>

      <view class="action-row">
        <button class="plain-btn" :disabled="updating" @click="resetGame">{{ t('matching.playAgain') }}</button>
        <button class="primary-btn" :disabled="updating" @click="goHome">{{ t('common.backHome') }}</button>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { onHide, onLoad, onPullDownRefresh, onShow, onUnload } from '@dcloudio/uni-app'
import LanguageSwitch from '../../components/LanguageSwitch.vue'
import { createMatchingGame, updateMatchingGame } from '../../api/matching'
import { fetchVocabLists } from '../../api/vocab'
import { usePreferences } from '../../composables/usePreferences'
import { applyTabBarLocale, setPageTitle, useI18n } from '../../i18n'
import type { MatchingDifficulty, MatchingGameCard, MatchingGameSession, MatchingSourceType, VocabList } from '../../types/api'
import { openPage, requireLogin, routes } from '../../utils/navigation'

interface Tile {
  id: string
  pairId: number
  kind: 'hanzi' | 'meaning'
  text: string
  subtext: string
  selected: boolean
  matched: boolean
}

const { locale, t } = useI18n()
const preferences = usePreferences()
const difficultyOptions: MatchingDifficulty[] = ['4x4', '7x7', '10x10']
const sourceType = ref<MatchingSourceType>('vocab_list')
const meaningLanguage = ref<'ru' | 'en'>(locale.value === 'en' ? 'en' : 'ru')
const difficulty = ref<MatchingDifficulty>('4x4')
const vocabLists = ref<VocabList[]>([])
const selectedListId = ref<number | null>(null)
const setupLoading = ref(false)
const setupError = ref('')
const starting = ref(false)
const updating = ref(false)
const session = ref<MatchingGameSession | null>(null)
const tiles = ref<Tile[]>([])
const selectedTileIds = ref<string[]>([])
const resolving = ref(false)
const wrongCount = ref(0)
const matchedPairs = ref(0)
const startedAt = ref(Date.now())
const clockNow = ref(Date.now())
let clockTimer: ReturnType<typeof setInterval> | null = null
let preferenceApplied = false

const vocabListNames = computed(() => vocabLists.value.map(item => item.name))
const selectedListIndex = computed(() => Math.max(0, vocabLists.value.findIndex(item => item.id === selectedListId.value)))
const selectedListLabel = computed(() => vocabLists.value.find(item => item.id === selectedListId.value)?.name || t('matching.chooseList'))
const canStart = computed(() => sourceType.value === 'favorites' || Boolean(selectedListId.value))
const gameCompleted = computed(() => Boolean(session.value && (session.value.status === 'completed' || matchedPairs.value >= session.value.totalPairs)))
const elapsedSeconds = computed(() => Math.max(0, Math.round((clockNow.value - startedAt.value) / 1000)))
const elapsedLabel = computed(() => t('common.seconds', { seconds: elapsedSeconds.value }))

onLoad(() => {
  setPageTitle('matching.title')
})

onShow(() => {
  applyTabBarLocale()
  setPageTitle('matching.title')
  if (!requireLogin()) {
    return
  }
  if (session.value?.status === 'playing') {
    startClock()
    return
  }
  void prepareSetup()
})

watch(locale, () => {
  setPageTitle('matching.title')
})

onPullDownRefresh(() => {
  const task = session.value ? Promise.resolve() : prepareSetup(true)
  void task.finally(() => uni.stopPullDownRefresh())
})

onHide(() => {
  stopClock()
})

onUnload(() => {
  stopClock()
  void abandonCurrentGame()
})

async function loadVocabLists() {
  const page = await fetchVocabLists(1, 50)
  vocabLists.value = page.records
  if ((!selectedListId.value || !page.records.some(item => item.id === selectedListId.value)) && page.records.length > 0) {
    selectedListId.value = page.records[0].id
  }
}

async function prepareSetup(force = false) {
  if (session.value) {
    return
  }
  setupLoading.value = true
  setupError.value = ''
  try {
    if (!preferenceApplied || force) {
      const preference = await preferences.loadPreference()
      meaningLanguage.value = preference.matchingMeaningLanguage
      preferenceApplied = true
    }
    await loadVocabLists()
  } catch {
    setupError.value = t('matching.setupLoadFailed')
  } finally {
    setupLoading.value = false
  }
}

function changeVocabList(event: { detail: { value: number } }) {
  selectedListId.value = vocabLists.value[event.detail.value]?.id || null
}

async function startGame() {
  if (!canStart.value) {
    void uni.showToast({ icon: 'none', title: t('matching.chooseList') })
    return
  }
  starting.value = true
  try {
    await abandonCurrentGame()
    const nextSession = await createMatchingGame({
      sourceType: sourceType.value,
      vocabListId: sourceType.value === 'vocab_list' ? selectedListId.value : null,
      meaningLanguage: meaningLanguage.value,
      difficulty: difficulty.value
    })
    applySession(nextSession)
  } catch {
    void uni.showToast({ icon: 'none', title: t('matching.startFailed') })
  } finally {
    starting.value = false
  }
}

function applySession(nextSession: MatchingGameSession) {
  session.value = nextSession
  wrongCount.value = nextSession.wrongCount
  matchedPairs.value = nextSession.matchedPairs
  selectedTileIds.value = []
  resolving.value = false
  tiles.value = shuffleTiles(buildTiles(nextSession.cards))
  startedAt.value = Date.now() - nextSession.elapsedSeconds * 1000
  clockNow.value = Date.now()
  startClock()
}

function buildTiles(cards: MatchingGameCard[]) {
  return cards.flatMap<Tile>((card) => [
    {
      id: `${card.vocabItemId}-hanzi`,
      pairId: card.vocabItemId,
      kind: 'hanzi',
      text: card.hanzi,
      subtext: card.pinyin || '',
      selected: false,
      matched: false
    },
    {
      id: `${card.vocabItemId}-meaning`,
      pairId: card.vocabItemId,
      kind: 'meaning',
      text: card.meaning,
      subtext: '',
      selected: false,
      matched: false
    }
  ])
}

function shuffleTiles(items: Tile[]) {
  const next = [...items]
  for (let index = next.length - 1; index > 0; index -= 1) {
    const target = Math.floor(Math.random() * (index + 1))
    const current = next[index]
    next[index] = next[target]
    next[target] = current
  }
  return next
}

function selectTile(tileId: string) {
  if (resolving.value || gameCompleted.value) {
    return
  }
  const tile = tiles.value.find(item => item.id === tileId)
  if (!tile || tile.matched || tile.selected) {
    return
  }
  tile.selected = true
  selectedTileIds.value.push(tile.id)
  if (selectedTileIds.value.length < 2) {
    return
  }
  const [first, second] = selectedTileIds.value
    .map(id => tiles.value.find(item => item.id === id))
    .filter(Boolean) as Tile[]
  if (!first || !second) {
    clearSelection()
    return
  }
  if (first.pairId === second.pairId && first.kind !== second.kind) {
    first.matched = true
    second.matched = true
    selectedTileIds.value = []
    matchedPairs.value += 1
    if (session.value && matchedPairs.value >= session.value.totalPairs) {
      stopClock()
      void completeGame()
    }
    return
  }
  resolving.value = true
  wrongCount.value += 1
  setTimeout(() => {
    first.selected = false
    second.selected = false
    selectedTileIds.value = []
    resolving.value = false
  }, 650)
}

function clearSelection() {
  selectedTileIds.value.forEach((id) => {
    const tile = tiles.value.find(item => item.id === id)
    if (tile) {
      tile.selected = false
    }
  })
  selectedTileIds.value = []
}

async function completeGame() {
  const current = session.value
  if (!current || updating.value) {
    return
  }
  updating.value = true
  try {
    const updated = await updateMatchingGame(current.id, {
      matchedPairs: matchedPairs.value,
      wrongCount: wrongCount.value,
      elapsedSeconds: elapsedSeconds.value,
      status: 'completed'
    })
    session.value = updated
    stopClock()
  } catch {
    void uni.showToast({ icon: 'none', title: t('matching.saveFailed') })
  } finally {
    updating.value = false
  }
}

async function abandonCurrentGame() {
  const current = session.value
  if (!current || current.status !== 'playing' || matchedPairs.value >= current.totalPairs) {
    return
  }
  try {
    await updateMatchingGame(current.id, {
      matchedPairs: matchedPairs.value,
      wrongCount: wrongCount.value,
      elapsedSeconds: elapsedSeconds.value,
      status: 'abandoned'
    })
  } catch {
    // Leaving a page should not block navigation or reset.
  }
}

async function resetGame() {
  if (updating.value) {
    return
  }
  updating.value = true
  try {
    await abandonCurrentGame()
    stopClock()
    session.value = null
    tiles.value = []
    selectedTileIds.value = []
    wrongCount.value = 0
    matchedPairs.value = 0
  } catch {
    void uni.showToast({ icon: 'none', title: t('matching.resetFailed') })
  } finally {
    updating.value = false
  }
}

function startClock() {
  stopClock()
  clockTimer = setInterval(() => {
    clockNow.value = Date.now()
  }, 1000)
}

function stopClock() {
  if (clockTimer) {
    clearInterval(clockTimer)
    clockTimer = null
  }
}

function goHome() {
  void openPage(routes.home)
}
</script>

<style scoped>
.page {
  background: #eef5f7;
  min-height: 100vh;
  padding: 0 24rpx 32rpx;
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

.setup-card,
.status-card,
.state-card,
.completed-card {
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
  padding: 34rpx 24rpx;
}

.error-card {
  background: #fff7ed;
  border-color: #fed7aa;
  color: #9a3412;
}

.setup-head {
  align-items: flex-start;
  display: flex;
  gap: 18rpx;
  justify-content: space-between;
}

.section-title {
  color: #102033;
  display: block;
  font-size: 34rpx;
  font-weight: 800;
}

.access-pill {
  background: #ccfbf1;
  border-radius: 999rpx;
  color: #14796f;
  flex: 0 0 auto;
  font-size: 22rpx;
  font-weight: 800;
  padding: 10rpx 16rpx;
}

.field {
  margin-top: 24rpx;
}

.field-label,
.muted {
  color: #64748b;
  display: block;
  font-size: 24rpx;
  line-height: 1.5;
}

.field-label {
  font-weight: 700;
  margin-bottom: 12rpx;
}

.segmented,
.difficulty-grid {
  display: grid;
  gap: 12rpx;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.difficulty-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.segment,
.difficulty-btn,
.plain-btn,
.primary-btn {
  border-radius: 14rpx;
  font-size: 27rpx;
  font-weight: 800;
  margin: 0;
  min-height: 78rpx;
}

.segment,
.difficulty-btn,
.plain-btn {
  background: #f8fafc;
  border: 1px solid #d7e2ea;
  color: #102033;
}

.segment.active,
.difficulty-btn.active,
.primary-btn {
  background: #14796f;
  border-color: #14796f;
  color: #ffffff;
}

.primary-btn {
  box-shadow: 0 12rpx 24rpx rgba(20, 121, 111, 0.18);
  margin-top: 26rpx;
  width: 100%;
}

.plain-btn.compact {
  width: 180rpx;
}

.hint-card {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 14rpx;
  color: #64748b;
  font-size: 24rpx;
  line-height: 1.45;
  margin-bottom: 14rpx;
  padding: 18rpx;
}

.picker-box {
  background: #f8fafc;
  border: 1px solid #d7e2ea;
  border-radius: 14rpx;
  box-sizing: border-box;
  color: #102033;
  font-size: 28rpx;
  font-weight: 700;
  min-height: 82rpx;
  padding: 22rpx;
}

.status-card {
  display: grid;
  gap: 12rpx;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.status-value {
  color: #102033;
  display: block;
  font-size: 34rpx;
  font-weight: 800;
  margin-top: 6rpx;
}

.board {
  display: grid;
  gap: 14rpx;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 18rpx;
}

.board.compact {
  gap: 12rpx;
}

.tile {
  align-items: center;
  background: #ffffff;
  border: 1px solid #d7e2ea;
  border-radius: 18rpx;
  box-shadow: 0 8rpx 24rpx rgba(15, 23, 42, 0.05);
  color: #102033;
  display: flex;
  flex-direction: column;
  justify-content: center;
  margin: 0;
  min-height: 120rpx;
  padding: 18rpx;
  text-align: center;
}

.board.compact .tile {
  min-height: 104rpx;
}

.tile.meaning {
  background: #f8fafc;
}

.tile.selected {
  border-color: #14796f;
  box-shadow: 0 10rpx 28rpx rgba(20, 121, 111, 0.18);
}

.tile.matched {
  background: #e9f7f5;
  border-color: #9fd8d0;
  color: #14796f;
}

.tile-main {
  display: block;
  font-size: 30rpx;
  font-weight: 800;
  line-height: 1.35;
  word-break: break-word;
}

.tile-sub {
  color: #64748b;
  display: block;
  font-size: 22rpx;
  line-height: 1.3;
  margin-top: 8rpx;
}

.completed-card {
  margin-top: 18rpx;
}

.completed-title {
  color: #14796f;
  display: block;
  font-size: 34rpx;
  font-weight: 800;
  margin-bottom: 8rpx;
}

.action-row {
  display: grid;
  gap: 14rpx;
  grid-template-columns: 1fr 1fr;
  margin-top: 18rpx;
}

.action-row .primary-btn {
  margin-top: 0;
}
</style>
