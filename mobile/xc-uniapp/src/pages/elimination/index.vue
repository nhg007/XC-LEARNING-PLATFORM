<template>
  <view class="page">
    <view class="hero">
      <view class="hero-top">
        <view class="hero-copy">
          <text class="eyebrow">{{ t('app.title') }}</text>
          <text class="title">{{ t('elimination.title') }}</text>
          <text class="subtitle">{{ t('elimination.subtitle') }}</text>
        </view>
        <LanguageSwitch variant="hero" />
      </view>
    </view>

    <view v-if="setupLoading && !session" class="state-card">{{ t('common.loading') }}</view>

    <view v-else-if="setupError && !session" class="state-card error-card">
      <text>{{ setupError }}</text>
      <button class="plain-btn compact" @click="prepareSetup(true)">{{ t('common.retry') }}</button>
    </view>

    <view v-else-if="!session" class="setup-panel">
      <view class="player-card">
        <view class="avatar">{{ avatarText }}</view>
        <view class="player-copy">
          <text class="player-name">{{ displayName }}</text>
          <text class="muted">{{ t('elimination.playerBest') }} · {{ t('elimination.points') }}</text>
        </view>
      </view>

      <view class="stage-card">
        <view class="stage-ribbon">{{ t('elimination.setup') }}</view>
        <text class="stage-desc">{{ t('elimination.setupDesc') }}</text>

        <view class="field">
          <text class="field-label">{{ t('matching.source') }}</text>
          <view class="segmented">
            <button class="segment" :class="{ active: sourceType === 'vocab_list' }" @click="sourceType = 'vocab_list'">
              {{ t('matching.sourceVocabList') }}
            </button>
            <button class="segment" :class="{ active: sourceType === 'favorites' }" @click="sourceType = 'favorites'">
              {{ t('matching.favorites') }}
            </button>
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
          <view class="level-stack">
            <button
              v-for="item in stageOptions"
              :key="item.code"
              class="level-btn"
              :class="{ active: difficulty === item.code }"
              @click="difficulty = item.code"
            >
              <text class="level-name">{{ difficultyLabel(item) }}</text>
              <text class="level-count">{{ t('elimination.cards', { count: item.cardCount }) }}</text>
            </button>
          </view>
        </view>

        <button class="primary-btn" :loading="starting" :disabled="!canStart" @click="startGame">
          {{ t('matching.start') }}
        </button>
      </view>
    </view>

    <template v-else>
      <view class="game-head">
        <view>
          <text class="muted">{{ t('elimination.cleared') }}</text>
          <text class="game-value">{{ matchedPairs }} / {{ session.totalPairs }}</text>
        </view>
        <view>
          <text class="muted">{{ t('matching.wrong') }}</text>
          <text class="game-value">{{ wrongCount }}</text>
        </view>
        <view>
          <text class="muted">{{ t('matching.time') }}</text>
          <text class="game-value">{{ elapsedLabel }}</text>
        </view>
        <view>
          <text class="muted">{{ t('elimination.combo') }}</text>
          <text class="game-value">{{ comboCount }}</text>
        </view>
      </view>

      <view class="hint-line">{{ t('elimination.boardHint') }}</view>

      <view class="card-board" :class="{ dense: tiles.length > 14 }">
        <button
          v-for="tile in tiles"
          :key="tile.id"
          class="pop-card"
          :class="{ selected: tile.selected, cleared: tile.matched, wrong: tile.wrong, meaning: tile.kind === 'meaning' }"
          :disabled="tile.matched || gameCompleted"
          @click="selectTile(tile.id)"
        >
          <text class="card-text">{{ tile.text }}</text>
          <text v-if="tile.subtext" class="card-sub">{{ tile.subtext }}</text>
        </button>
      </view>

      <view v-if="gameCompleted" class="completed-card">
        <text class="completed-title">{{ t('elimination.completedTitle') }}</text>
        <text class="muted">{{ t('elimination.completedSummary', { pairs: matchedPairs, wrong: wrongCount, time: elapsedLabel }) }}</text>
      </view>

      <view class="action-row">
        <button class="plain-btn" :disabled="updating" @click="resetGame">{{ t('matching.playAgain') }}</button>
        <button class="primary-btn flat" :disabled="updating" @click="goFeatures">{{ t('tab.features') }}</button>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { onHide, onLoad, onPullDownRefresh, onShow, onUnload } from '@dcloudio/uni-app'
import LanguageSwitch from '../../components/LanguageSwitch.vue'
import { createMatchingGame, fetchMatchingStages, updateMatchingGame } from '../../api/matching'
import { fetchVocabLists } from '../../api/vocab'
import { usePreferences } from '../../composables/usePreferences'
import { applyTabBarLocale, setPageTitle, useI18n } from '../../i18n'
import type { MatchingDifficulty, MatchingGameCard, MatchingGameSession, MatchingSourceType, MatchingStage, VocabList } from '../../types/api'
import { openPage, requireLogin, routes } from '../../utils/navigation'
import { getProfile } from '../../utils/storage'

interface Tile {
  id: string
  pairId: number
  kind: 'hanzi' | 'meaning'
  text: string
  subtext: string
  selected: boolean
  matched: boolean
  wrong: boolean
}

const { locale, t } = useI18n()
const preferences = usePreferences()
const sourceType = ref<MatchingSourceType>('vocab_list')
const meaningLanguage = ref<'ru' | 'en'>(locale.value === 'en' ? 'en' : 'ru')
const difficulty = ref<MatchingDifficulty>('')
const stageOptions = ref<MatchingStage[]>([])
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
const comboCount = ref(0)
const startedAt = ref(Date.now())
const clockNow = ref(Date.now())
let clockTimer: ReturnType<typeof setInterval> | null = null
let preferenceApplied = false

const profile = computed(() => getProfile())
const displayName = computed(() => profile.value?.nickname || profile.value?.email || t('profile.guest'))
const avatarText = computed(() => displayName.value.slice(0, 1).toUpperCase())
const vocabListNames = computed(() => vocabLists.value.map(item => item.name))
const selectedListIndex = computed(() => Math.max(0, vocabLists.value.findIndex(item => item.id === selectedListId.value)))
const selectedListLabel = computed(() => vocabLists.value.find(item => item.id === selectedListId.value)?.name || t('matching.chooseList'))
const canStart = computed(() => Boolean(difficulty.value) && (sourceType.value === 'favorites' || Boolean(selectedListId.value)))
const gameCompleted = computed(() => Boolean(session.value && (session.value.status === 'completed' || matchedPairs.value >= session.value.totalPairs)))
const elapsedSeconds = computed(() => Math.max(0, Math.round((clockNow.value - startedAt.value) / 1000)))
const elapsedLabel = computed(() => t('common.seconds', { seconds: elapsedSeconds.value }))

onLoad(() => {
  setPageTitle('elimination.title')
})

onShow(() => {
  applyTabBarLocale()
  setPageTitle('elimination.title')
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
  setPageTitle('elimination.title')
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

async function loadStages() {
  const stages = await fetchMatchingStages()
  stageOptions.value = stages
  if ((!difficulty.value || !stages.some(item => item.code === difficulty.value)) && stages.length > 0) {
    difficulty.value = stages[0].code
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
    await Promise.all([loadVocabLists(), loadStages()])
  } catch {
    setupError.value = t('matching.setupLoadFailed')
  } finally {
    setupLoading.value = false
  }
}

function changeVocabList(event: { detail: { value: number } }) {
  selectedListId.value = vocabLists.value[event.detail.value]?.id || null
}

function difficultyLabel(stage: MatchingStage) {
  const languageKey = locale.value.startsWith('ru') ? 'ru' : locale.value.startsWith('en') ? 'en' : 'zh'
  return stage.labels[languageKey] || stage.labels.zh || stage.code
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
  comboCount.value = 0
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
      matched: false,
      wrong: false
    },
    {
      id: `${card.vocabItemId}-meaning`,
      pairId: card.vocabItemId,
      kind: 'meaning',
      text: card.meaning,
      subtext: meaningLanguage.value.toUpperCase(),
      selected: false,
      matched: false,
      wrong: false
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
  const selectedTiles = selectedTileIds.value
    .map(id => tiles.value.find(item => item.id === id))
    .filter(Boolean) as Tile[]
  const [first, second] = selectedTiles
  if (!first || !second) {
    clearSelection()
    return
  }
  if (first.pairId === second.pairId && first.kind !== second.kind) {
    first.matched = true
    second.matched = true
    selectedTileIds.value = []
    matchedPairs.value += 1
    comboCount.value += 1
    if (session.value && matchedPairs.value >= session.value.totalPairs) {
      stopClock()
      void completeGame()
    }
    return
  }
  resolving.value = true
  wrongCount.value += 1
  comboCount.value = 0
  first.wrong = true
  second.wrong = true
  setTimeout(() => {
    first.selected = false
    second.selected = false
    first.wrong = false
    second.wrong = false
    selectedTileIds.value = []
    resolving.value = false
  }, 620)
}

function clearSelection() {
  selectedTileIds.value.forEach((id) => {
    const tile = tiles.value.find(item => item.id === id)
    if (tile) {
      tile.selected = false
      tile.wrong = false
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
    // Leaving the page should not block navigation.
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
    comboCount.value = 0
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

function goFeatures() {
  void openPage(routes.features)
}
</script>

<style scoped>
.page {
  background: #fff8e8;
  min-height: 100vh;
  padding: 0 24rpx 34rpx;
}

button::after {
  border: 0;
}

.hero {
  background: #102033;
  border-bottom-left-radius: 34rpx;
  border-bottom-right-radius: 34rpx;
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
  font-weight: 800;
}

.title {
  display: block;
  font-size: 48rpx;
  font-weight: 900;
  line-height: 1.15;
  margin-top: 10rpx;
}

.subtitle {
  color: #cbd5e1;
  display: block;
  font-size: 26rpx;
  line-height: 1.45;
  margin-top: 14rpx;
}

.state-card,
.player-card,
.stage-card,
.game-head,
.completed-card {
  background: #ffffff;
  border: 1px solid #ead9b8;
  border-radius: 26rpx;
  box-shadow: 0 12rpx 32rpx rgba(120, 72, 20, 0.08);
  box-sizing: border-box;
}

.state-card {
  color: #7c6b52;
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

.setup-panel {
  display: flex;
  flex-direction: column;
  gap: 22rpx;
}

.player-card {
  align-items: center;
  display: flex;
  gap: 20rpx;
  padding: 24rpx;
}

.avatar {
  align-items: center;
  background: #14796f;
  border-radius: 22rpx;
  color: #ffffff;
  display: flex;
  flex: 0 0 88rpx;
  font-size: 42rpx;
  font-weight: 900;
  height: 88rpx;
  justify-content: center;
}

.player-copy {
  flex: 1;
  min-width: 0;
}

.player-name {
  color: #102033;
  display: block;
  font-size: 34rpx;
  font-weight: 900;
  line-height: 1.25;
}

.stage-card {
  padding: 30rpx 24rpx 26rpx;
  position: relative;
}

.stage-ribbon {
  align-items: center;
  background: #f6b331;
  border-radius: 18rpx;
  box-shadow: 0 8rpx 0 #c78217;
  color: #ffffff;
  display: flex;
  font-size: 32rpx;
  font-weight: 900;
  height: 76rpx;
  justify-content: center;
  margin: 0 auto 28rpx;
  text-shadow: 0 2rpx 0 rgba(120, 72, 20, 0.35);
  width: 300rpx;
}

.stage-desc,
.muted,
.field-label {
  color: #7c6b52;
  display: block;
  font-size: 24rpx;
  line-height: 1.45;
}

.field {
  margin-top: 24rpx;
}

.field-label {
  font-weight: 800;
  margin-bottom: 12rpx;
}

.segmented {
  display: grid;
  gap: 12rpx;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.segment,
.plain-btn,
.primary-btn,
.level-btn {
  align-items: center;
  border-radius: 16rpx;
  box-sizing: border-box;
  display: flex;
  font-weight: 900;
  justify-content: center;
  line-height: 1.2;
  margin: 0;
  min-height: 76rpx;
  padding: 0 20rpx;
}

.segment,
.plain-btn,
.level-btn {
  background: #fffaf0;
  border: 1px solid #ead9b8;
  color: #102033;
}

.segment.active,
.level-btn.active,
.primary-btn {
  background: #14796f;
  border-color: #14796f;
  color: #ffffff;
}

.hint-card,
.picker-box {
  background: #fffaf0;
  border: 1px solid #ead9b8;
  border-radius: 16rpx;
  box-sizing: border-box;
  color: #7c6b52;
  font-size: 24rpx;
  line-height: 1.45;
  padding: 18rpx;
}

.picker-box {
  color: #102033;
  font-size: 28rpx;
  font-weight: 800;
  min-height: 80rpx;
  padding: 22rpx;
}

.level-stack {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.level-btn {
  align-items: center;
  box-shadow: 0 7rpx 0 #d5c4a8;
  justify-content: space-between;
  min-height: 82rpx;
}

.level-btn.active {
  box-shadow: 0 7rpx 0 #0f5f57;
}

.level-name {
  font-size: 30rpx;
}

.level-count {
  font-size: 23rpx;
  opacity: 0.82;
}

.primary-btn {
  box-shadow: 0 10rpx 0 #0f5f57;
  font-size: 30rpx;
  margin-top: 28rpx;
  width: 100%;
}

.primary-btn.flat {
  box-shadow: none;
  margin-top: 0;
}

.plain-btn.compact {
  width: 180rpx;
}

.game-head {
  display: grid;
  gap: 14rpx;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  padding: 22rpx;
}

.game-value {
  color: #102033;
  display: block;
  font-size: 30rpx;
  font-weight: 900;
  margin-top: 6rpx;
}

.hint-line {
  color: #7c6b52;
  font-size: 24rpx;
  line-height: 1.45;
  margin: 18rpx 4rpx 16rpx;
}

.card-board {
  display: grid;
  gap: 18rpx;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.card-board.dense {
  gap: 14rpx;
}

.pop-card {
  align-items: center;
  background: #fffdf7;
  border: 4rpx solid #f4ca73;
  border-radius: 22rpx;
  box-shadow: inset 0 0 0 6rpx #fff7df, 0 8rpx 0 #dca94d;
  box-sizing: border-box;
  color: #5d4327;
  display: flex;
  flex-direction: column;
  justify-content: center;
  margin: 0;
  min-height: 146rpx;
  padding: 18rpx 14rpx;
  text-align: center;
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.card-board.dense .pop-card {
  min-height: 118rpx;
}

.pop-card.meaning {
  background: #f7fbff;
  border-color: #8ec5ff;
  box-shadow: inset 0 0 0 6rpx #eef7ff, 0 8rpx 0 #5fa8e8;
  color: #164e8a;
}

.pop-card.selected {
  transform: translateY(4rpx);
  box-shadow: inset 0 0 0 6rpx #e5fbf7, 0 4rpx 0 #0f5f57;
  border-color: #14796f;
  color: #14796f;
}

.pop-card.wrong {
  border-color: #ef4444;
  color: #b91c1c;
}

.pop-card.cleared {
  opacity: 0;
  pointer-events: none;
  transform: scale(0.88);
}

.card-text {
  display: block;
  font-size: 30rpx;
  font-weight: 900;
  line-height: 1.25;
  word-break: break-word;
}

.card-sub {
  color: #7c6b52;
  display: block;
  font-size: 21rpx;
  font-weight: 700;
  line-height: 1.2;
  margin-top: 8rpx;
}

.completed-card {
  margin-top: 20rpx;
  padding: 24rpx;
}

.completed-title {
  color: #14796f;
  display: block;
  font-size: 34rpx;
  font-weight: 900;
  margin-bottom: 8rpx;
}

.action-row {
  display: grid;
  gap: 14rpx;
  grid-template-columns: 1fr 1fr;
  margin-top: 20rpx;
}
</style>
