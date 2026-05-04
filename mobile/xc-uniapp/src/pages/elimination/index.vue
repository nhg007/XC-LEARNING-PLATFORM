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
        <view class="setup-heading">
          <view>
            <text class="setup-kicker">{{ t('elimination.setupKicker') }}</text>
            <text class="setup-title">{{ t('elimination.setup') }}</text>
          </view>
          <text class="setup-badge">{{ selectedLevel ? levelMeta(selectedLevel) : t('elimination.points') }}</text>
        </view>
        <text class="stage-desc">{{ t('elimination.setupDesc') }}</text>

        <view class="setup-section">
          <view class="section-head">
            <text class="section-index">1</text>
            <text class="field-label">{{ t('elimination.contentSettings') }}</text>
          </view>
          <view class="segmented">
            <button class="segment" :class="{ active: sourceType === 'vocab_list' }" @click="sourceType = 'vocab_list'">
              {{ t('matching.sourceVocabList') }}
            </button>
            <button class="segment" :class="{ active: sourceType === 'favorites' }" @click="sourceType = 'favorites'">
              {{ t('matching.favorites') }}
            </button>
          </view>

          <view v-if="sourceType === 'vocab_list'" class="nested-field">
            <text class="field-label subtle">{{ t('matching.vocabList') }}</text>
            <view v-if="vocabLists.length === 0" class="hint-card">{{ t('matching.emptyVocabLists') }}</view>
            <picker :range="vocabListNames" :value="selectedListIndex" @change="changeVocabList">
              <view class="picker-box">
                <text>{{ selectedListLabel }}</text>
              </view>
            </picker>
          </view>
        </view>

        <view class="setup-section">
          <view class="section-head">
            <text class="section-index">2</text>
            <text class="field-label">{{ t('elimination.displaySettings') }}</text>
          </view>
          <view class="setting-grid">
            <view>
              <text class="field-label subtle">{{ t('matching.referenceLanguage') }}</text>
              <view class="segmented">
                <button class="segment" :class="{ active: meaningLanguage === 'ru' }" @click="meaningLanguage = 'ru'">{{ t('common.ru') }}</button>
                <button class="segment" :class="{ active: meaningLanguage === 'en' }" @click="meaningLanguage = 'en'">{{ t('common.en') }}</button>
              </view>
            </view>
            <view>
              <text class="field-label subtle">{{ t('matching.pinyin') }}</text>
              <button class="toggle-switch" :class="{ active: showPinyin }" @click="showPinyin = !showPinyin">
                <view class="toggle-knob"></view>
              </button>
            </view>
          </view>
        </view>

        <view class="setup-section">
          <view class="section-head">
            <text class="section-index">3</text>
            <text class="field-label">{{ t('elimination.stageSelect') }}</text>
          </view>
          <button v-if="activeStageGroup" class="stage-group-trigger" @click="stageGroupPickerOpen = true">
            <view class="stage-trigger-copy">
              <text class="stage-trigger-label">{{ t('elimination.currentStageGroup') }}</text>
              <text class="stage-trigger-title">{{ stageGroupLabel(activeStageGroup) }}</text>
              <text class="stage-trigger-meta">{{ stageGroupProgressText(activeStageGroup) }} · {{ t('elimination.levelCount', { count: activeStageGroup.levels.length }) }}</text>
            </view>
            <text class="stage-trigger-action">{{ t('elimination.changeStageGroup') }}</text>
          </button>
          <view v-else class="hint-card">{{ t('elimination.noStageGroups') }}</view>

          <view class="level-stack">
            <button
              v-for="(item, index) in stageOptions"
              :key="item.code"
              class="level-btn"
              :class="{ active: difficulty === item.code, locked: !item.unlocked, completed: item.completed }"
              :disabled="!item.unlocked"
              @click="selectLevel(item)"
            >
              <view class="level-main">
                <text class="level-name">{{ levelLabel(index) }}</text>
                <text class="level-count">{{ levelMeta(item) }}</text>
              </view>
              <text v-if="!item.unlocked" class="level-note">{{ t('elimination.locked') }}</text>
              <text v-else-if="item.completed" class="level-note">{{ t('elimination.bestTime', { time: formatTime(item.bestElapsedSeconds || 0) }) }}</text>
              <text v-else class="level-note">{{ t('elimination.levelReady') }}</text>
            </button>
          </view>
        </view>

        <view class="selection-summary">
          <view>
            <text class="summary-label">{{ t('elimination.readyStage') }}</text>
            <text class="summary-title">{{ activeStageGroup ? stageGroupLabel(activeStageGroup) : '-' }} · {{ selectedLevel ? levelLabel(selectedLevelIndex) : '-' }}</text>
          </view>
          <text class="summary-meta">{{ selectedLevel ? levelMeta(selectedLevel) : '-' }}</text>
        </view>

        <button class="primary-btn" :loading="starting" :disabled="!canStart" @click="startGame">
          {{ t('matching.start') }}
        </button>
      </view>

      <view v-if="stageGroupPickerOpen" class="sheet-mask" @click="stageGroupPickerOpen = false">
        <view class="stage-sheet" @click.stop>
          <view class="sheet-handle"></view>
          <view class="sheet-head">
            <view>
              <text class="sheet-title">{{ t('elimination.stageGroupSheetTitle') }}</text>
              <text class="sheet-subtitle">{{ t('elimination.stageGroupSheetDesc') }}</text>
            </view>
            <button class="sheet-close" @click="stageGroupPickerOpen = false">{{ t('common.cancel') }}</button>
          </view>
          <scroll-view scroll-y class="stage-sheet-list">
            <button
              v-for="item in stageGroups"
              :key="item.code"
              class="stage-sheet-item"
              :class="{ active: stageGroupCode === item.code }"
              @click="selectStageGroupFromPicker(item.code)"
            >
              <view class="stage-sheet-main">
                <view>
                  <text class="stage-sheet-title">{{ stageGroupLabel(item) }}</text>
                  <text class="stage-sheet-meta">{{ stageGroupProgressText(item) }} · {{ t('elimination.levelCount', { count: item.levels.length }) }}</text>
                </view>
                <text class="stage-sheet-state">{{ stageGroupCode === item.code ? t('elimination.selectedStageGroup') : t('elimination.chooseStageGroup') }}</text>
              </view>
              <view class="progress-track">
                <view class="progress-fill" :style="{ width: `${stageGroupProgressPercent(item)}%` }"></view>
              </view>
            </button>
          </scroll-view>
        </view>
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
          <text class="muted">{{ t('elimination.remaining') }}</text>
          <text class="game-value">{{ remainingLabel }}</text>
        </view>
        <view>
          <text class="muted">{{ t('elimination.combo') }}</text>
          <text class="game-value">{{ comboCount }}</text>
        </view>
      </view>

      <view class="hint-line">{{ t('elimination.boardHint') }}</view>

      <TransitionGroup name="tile-pop" tag="view" class="card-board" :class="{ dense: visibleTiles.length > 14 }">
        <button
          v-for="tile in visibleTiles"
          :key="tile.id"
          class="pop-card"
          :class="{ selected: tile.selected, cleared: tile.matched, wrong: tile.wrong, meaning: tile.kind === 'meaning' }"
          :disabled="gameCompleted || gameFailed"
          @click="selectTile(tile.id)"
        >
          <text class="card-text">{{ tile.text }}</text>
          <text v-if="tile.subtext" class="card-sub">{{ tile.subtext }}</text>
        </button>
      </TransitionGroup>

      <view v-if="gameCompleted" class="completed-card">
        <text class="completed-title">{{ t('elimination.completedTitle') }}</text>
        <text class="muted">{{ t('elimination.completedSummary', { pairs: matchedPairs, wrong: wrongCount, time: elapsedLabel }) }}</text>
      </view>

      <view v-if="gameFailed" class="completed-card failed">
        <text class="completed-title">{{ t('elimination.failedTitle') }}</text>
        <text class="muted">{{ t('elimination.failedSummary', { pairs: matchedPairs, total: session.totalPairs }) }}</text>
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
import { createMatchingGame, fetchMatchingStageGroups, updateMatchingGame } from '../../api/matching'
import { fetchVocabLists } from '../../api/vocab'
import { usePreferences } from '../../composables/usePreferences'
import { applyTabBarLocale, setPageTitle, useI18n } from '../../i18n'
import type { MatchingDifficulty, MatchingGameCard, MatchingGameSession, MatchingSourceType, MatchingStage, MatchingStageGroup, VocabList } from '../../types/api'
import { openPage, requireLogin, routes } from '../../utils/navigation'
import { getProfile } from '../../utils/storage'

type VocabListOption = VocabList & { displayName: string }

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
const showPinyin = ref(true)
const difficulty = ref<MatchingDifficulty>('')
const stageGroups = ref<MatchingStageGroup[]>([])
const stageGroupCode = ref('')
const stageGroupPickerOpen = ref(false)
const vocabLists = ref<VocabListOption[]>([])
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
const vocabListNames = computed(() => vocabLists.value.map(item => item.displayName))
const selectedListIndex = computed(() => Math.max(0, vocabLists.value.findIndex(item => item.id === selectedListId.value)))
const selectedListLabel = computed(() => vocabLists.value.find(item => item.id === selectedListId.value)?.displayName || t('matching.chooseList'))
const activeStageGroup = computed(() => stageGroups.value.find(item => item.code === stageGroupCode.value) || stageGroups.value[0] || null)
const stageOptions = computed<MatchingStage[]>(() => activeStageGroup.value?.levels ?? [])
const selectedLevel = computed(() => stageOptions.value.find(item => item.code === difficulty.value) || null)
const selectedLevelIndex = computed(() => Math.max(0, stageOptions.value.findIndex(item => item.code === difficulty.value)))
const canStart = computed(() => Boolean(difficulty.value) && Boolean(selectedLevel.value?.unlocked) && (sourceType.value === 'favorites' || Boolean(selectedListId.value)))
const gameCompleted = computed(() => Boolean(session.value && (session.value.status === 'completed' || matchedPairs.value >= session.value.totalPairs)))
const gameFailed = computed(() => session.value?.status === 'failed')
const timeLimitSeconds = computed(() => session.value?.timeLimitSeconds || selectedLevel.value?.timeLimitSeconds || 0)
const elapsedSeconds = computed(() => Math.max(0, Math.round((clockNow.value - startedAt.value) / 1000)))
const elapsedLabel = computed(() => t('common.seconds', { seconds: elapsedSeconds.value }))
const remainingSeconds = computed(() => timeLimitSeconds.value > 0 ? Math.max(0, timeLimitSeconds.value - elapsedSeconds.value) : elapsedSeconds.value)
const remainingLabel = computed(() => t('common.seconds', { seconds: remainingSeconds.value }))
const visibleTiles = computed(() => tiles.value.filter(tile => !tile.matched))

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

watch([sourceType, selectedListId, meaningLanguage], () => {
  if (!session.value) {
    stageGroupPickerOpen.value = false
    void loadStages()
  }
})

onPullDownRefresh(() => {
  const task = session.value ? Promise.resolve() : prepareSetup(true)
  void task.finally(() => uni.stopPullDownRefresh())
})

onHide(() => {
  stageGroupPickerOpen.value = false
  stopClock()
})

onUnload(() => {
  stopClock()
  void abandonCurrentGame()
})

async function loadVocabLists() {
  const page = await fetchVocabLists(1, 50)
  const options = await buildVocabListOptions(page.records)
  vocabLists.value = options
  if ((!selectedListId.value || !options.some(item => item.id === selectedListId.value)) && options.length > 0) {
    selectedListId.value = options[0].id
  }
}

async function buildVocabListOptions(parentLists: VocabList[]): Promise<VocabListOption[]> {
  const childPages = await Promise.all(
    parentLists
      .filter(list => list.childCount > 0)
      .map(list => fetchVocabLists({ page: 1, pageSize: 100, parentId: list.id }).then(page => [list, page.records] as const))
  )
  const childrenByParentId = new Map(childPages.map(([parent, children]) => [parent.id, children]))
  return parentLists.flatMap((list) => {
    const parentOption: VocabListOption = {
      ...list,
      displayName: list.childCount > 0 ? `${list.name} (${t('vocab.scopeAll')})` : list.name
    }
    const childOptions = (childrenByParentId.get(list.id) || []).map(child => ({
      ...child,
      displayName: `${list.name} / ${child.name}`
    }))
    return [parentOption, ...childOptions]
  })
}

async function loadStages() {
  const groups = await fetchMatchingStageGroups({
    gameType: 'elimination',
    sourceType: sourceType.value,
    vocabListId: sourceType.value === 'vocab_list' ? selectedListId.value : null,
    meaningLanguage: meaningLanguage.value
  })
  stageGroups.value = groups
  if ((!stageGroupCode.value || !groups.some(item => item.code === stageGroupCode.value)) && groups.length > 0) {
    stageGroupCode.value = groups[0].code
  }
  const levels = groups.find(item => item.code === stageGroupCode.value)?.levels ?? groups[0]?.levels ?? []
  if ((!difficulty.value || !levels.some(item => item.code === difficulty.value)) && levels.length > 0) {
    difficulty.value = (levels.find(item => item.unlocked) ?? levels[0]).code
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

function levelLabel(index: number) {
  return t('elimination.levelNumber', { number: index + 1 })
}

function formatTime(seconds: number) {
  const minutes = Math.floor(seconds / 60)
  const rest = seconds % 60
  return `${minutes}:${String(rest).padStart(2, '0')}`
}

function selectStageGroup(code: string) {
  stageGroupCode.value = code
  difficulty.value = (stageOptions.value.find(item => item.unlocked) ?? stageOptions.value[0])?.code || ''
}

function selectStageGroupFromPicker(code: string) {
  selectStageGroup(code)
  stageGroupPickerOpen.value = false
}

function stageGroupLabel(stage: MatchingStageGroup) {
  const key = locale.value === 'ru' ? 'ru' : locale.value === 'en' ? 'en' : 'zh'
  return stage.labels[key] || stage.labels.zh || stage.code
}

function stageGroupCompletedCount(stage: MatchingStageGroup) {
  return stage.levels.filter(level => level.completed).length
}

function stageGroupProgressText(stage: MatchingStageGroup) {
  return t('elimination.stageGroupProgress', {
    completed: stageGroupCompletedCount(stage),
    total: stage.levels.length
  })
}

function stageGroupProgressPercent(stage: MatchingStageGroup) {
  if (stage.levels.length === 0) {
    return 0
  }
  return Math.round((stageGroupCompletedCount(stage) / stage.levels.length) * 100)
}

function selectLevel(level: MatchingStage) {
  if (!level.unlocked) {
    void uni.showToast({ icon: 'none', title: t('elimination.lockedWarning') })
    return
  }
  difficulty.value = level.code
}

function levelMeta(level: MatchingStage) {
  return `${t('elimination.cards', { count: level.cardCount })} · ${t('elimination.limit', { time: formatTime(level.timeLimitSeconds) })}`
}

async function startGame() {
  if (!canStart.value) {
    void uni.showToast({ icon: 'none', title: selectedLevel.value && !selectedLevel.value.unlocked ? t('elimination.lockedWarning') : t('matching.chooseList') })
    return
  }
  starting.value = true
  try {
    await abandonCurrentGame()
    const nextSession = await createMatchingGame({
      gameType: 'elimination',
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
      subtext: showPinyin.value ? card.pinyin || '' : '',
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
  if (resolving.value || gameCompleted.value || gameFailed.value) {
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

async function failGame() {
  const current = session.value
  if (!current || updating.value || current.status !== 'playing') {
    return
  }
  updating.value = true
  try {
    stopClock()
    session.value = await updateMatchingGame(current.id, {
      matchedPairs: matchedPairs.value,
      wrongCount: wrongCount.value,
      elapsedSeconds: elapsedSeconds.value,
      status: 'failed'
    })
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
    await loadStages()
    difficulty.value = (stageOptions.value.find(item => item.unlocked && !item.completed) ?? stageOptions.value.find(item => item.unlocked) ?? stageOptions.value[0])?.code || ''
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
    if (timeLimitSeconds.value > 0 && elapsedSeconds.value >= timeLimitSeconds.value && !gameCompleted.value && !gameFailed.value) {
      void failGame()
    }
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
  color: #8bd6ce;
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
  background: #fffdf6;
  border: 1px solid #eadfbe;
  border-radius: 26rpx;
  box-shadow: 0 12rpx 32rpx rgba(120, 89, 32, 0.1);
  box-sizing: border-box;
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
  background: #16897d;
  border-radius: 22rpx;
  box-shadow: inset 0 -6rpx 0 rgba(0, 0, 0, 0.12);
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

.setup-heading {
  align-items: flex-start;
  display: flex;
  gap: 18rpx;
  justify-content: space-between;
}

.setup-kicker {
  color: #16897d;
  display: block;
  font-size: 22rpx;
  font-weight: 900;
  line-height: 1.2;
}

.setup-title {
  color: #102033;
  display: block;
  font-size: 38rpx;
  font-weight: 900;
  line-height: 1.2;
  margin-top: 8rpx;
}

.setup-badge {
  background: #fff3c7;
  border: 1px solid #f2cf72;
  border-radius: 999rpx;
  color: #825a0f;
  flex: 0 0 auto;
  font-size: 21rpx;
  font-weight: 900;
  line-height: 1.25;
  max-width: 260rpx;
  padding: 12rpx 18rpx;
  text-align: right;
}

.stage-desc,
.muted,
.field-label {
  color: #6f6758;
  display: block;
  font-size: 24rpx;
  line-height: 1.45;
}

.stage-desc {
  margin-top: 16rpx;
}

.field-label {
  font-weight: 800;
  margin-bottom: 12rpx;
}

.field-label.subtle {
  color: #8a8170;
  font-size: 22rpx;
}

.setup-section {
  background: #fffaf0;
  border: 1px solid #f0e2be;
  border-radius: 22rpx;
  box-sizing: border-box;
  margin-top: 20rpx;
  padding: 20rpx;
}

.section-head {
  align-items: center;
  display: flex;
  gap: 12rpx;
  margin-bottom: 16rpx;
}

.section-head .field-label {
  margin-bottom: 0;
}

.section-index {
  align-items: center;
  background: #102033;
  border-radius: 999rpx;
  color: #ffffff;
  display: flex;
  flex: 0 0 36rpx;
  font-size: 20rpx;
  font-weight: 900;
  height: 36rpx;
  justify-content: center;
}

.nested-field {
  margin-top: 18rpx;
}

.setting-grid {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.segmented {
  display: grid;
  gap: 12rpx;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.segment,
.plain-btn,
.primary-btn,
.stage-group-trigger,
.stage-sheet-item,
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
.stage-group-trigger,
.stage-sheet-item,
.level-btn {
  background: #fffdf6;
  border: 1px solid #eadfbe;
  color: #102033;
}

.segment.active,
.stage-sheet-item.active,
.level-btn.active,
.primary-btn {
  background: #16897d;
  border-color: #16897d;
  color: #ffffff;
}

.toggle-switch {
  align-items: center;
  background: #d7cfbd;
  border: 0;
  border-radius: 999rpx;
  box-sizing: border-box;
  display: flex;
  height: 60rpx;
  margin: 0;
  padding: 6rpx;
  position: relative;
  transition: background 0.2s ease;
  width: 104rpx;
}

.toggle-switch::after {
  border: 0;
}

.toggle-knob {
  background: #ffffff;
  border-radius: 999rpx;
  box-shadow: 0 4rpx 10rpx rgba(120, 89, 32, 0.18);
  height: 48rpx;
  transform: translateX(0);
  transition: transform 0.2s ease, background 0.2s ease;
  width: 48rpx;
}

.toggle-switch.active {
  background: #8aa4ee;
}

.toggle-switch.active .toggle-knob {
  transform: translateX(44rpx);
}

.hint-card,
.picker-box {
  background: #fffdf6;
  border: 1px solid #eadfbe;
  border-radius: 16rpx;
  box-sizing: border-box;
  color: #64748b;
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

.stage-group-trigger {
  align-items: center;
  gap: 18rpx;
  justify-content: space-between;
  min-height: 112rpx;
  padding: 18rpx 20rpx;
  text-align: left;
  width: 100%;
}

.stage-trigger-copy {
  flex: 1;
  min-width: 0;
}

.stage-trigger-label {
  color: #8a8170;
  display: block;
  font-size: 21rpx;
  font-weight: 800;
  line-height: 1.3;
}

.stage-trigger-title {
  color: #102033;
  display: block;
  font-size: 30rpx;
  font-weight: 900;
  line-height: 1.25;
  margin-top: 8rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stage-trigger-meta {
  color: #6f6758;
  display: block;
  font-size: 22rpx;
  font-weight: 800;
  line-height: 1.35;
  margin-top: 8rpx;
}

.stage-trigger-action {
  color: #16897d;
  flex: 0 0 auto;
  font-size: 24rpx;
  font-weight: 900;
}

.sheet-mask {
  background: rgba(15, 23, 42, 0.42);
  bottom: 0;
  left: 0;
  position: fixed;
  right: 0;
  top: 0;
  z-index: 60;
}

.stage-sheet {
  background: #fffdf6;
  border-top-left-radius: 30rpx;
  border-top-right-radius: 30rpx;
  bottom: 0;
  box-shadow: 0 -18rpx 36rpx rgba(15, 23, 42, 0.18);
  box-sizing: border-box;
  left: 0;
  max-height: 78vh;
  padding: 16rpx 24rpx calc(24rpx + env(safe-area-inset-bottom));
  position: absolute;
  right: 0;
}

.sheet-handle {
  background: #dacaa5;
  border-radius: 999rpx;
  height: 8rpx;
  margin: 0 auto 20rpx;
  width: 88rpx;
}

.sheet-head {
  align-items: flex-start;
  display: flex;
  gap: 18rpx;
  justify-content: space-between;
  margin-bottom: 18rpx;
}

.sheet-title {
  color: #102033;
  display: block;
  font-size: 34rpx;
  font-weight: 900;
  line-height: 1.25;
}

.sheet-subtitle {
  color: #6f6758;
  display: block;
  font-size: 23rpx;
  font-weight: 700;
  line-height: 1.4;
  margin-top: 8rpx;
}

.sheet-close {
  align-items: center;
  background: #f3eee1;
  border: 1px solid #e3d7b9;
  border-radius: 999rpx;
  box-sizing: border-box;
  color: #6f6758;
  display: flex;
  flex: 0 0 auto;
  font-size: 23rpx;
  font-weight: 900;
  height: 58rpx;
  justify-content: center;
  line-height: 58rpx;
  margin: 0;
  min-height: 58rpx;
  padding: 0 22rpx;
}

.stage-sheet-list {
  max-height: 56vh;
}

.stage-sheet-item {
  background: #fffaf0;
  border-color: #eadfbe;
  box-shadow: 0 6rpx 0 #d7c49d;
  display: flex;
  flex-direction: column;
  gap: 14rpx;
  margin: 0 0 16rpx;
  min-height: 118rpx;
  padding: 18rpx;
  text-align: left;
  width: 100%;
}

.stage-sheet-item.active {
  box-shadow: 0 6rpx 0 #0f6f66;
}

.stage-sheet-main {
  align-items: flex-start;
  display: flex;
  gap: 18rpx;
  justify-content: space-between;
  width: 100%;
}

.stage-sheet-title {
  display: block;
  font-size: 29rpx;
  font-weight: 900;
  line-height: 1.3;
}

.stage-sheet-meta {
  display: block;
  font-size: 22rpx;
  font-weight: 800;
  line-height: 1.35;
  margin-top: 8rpx;
  opacity: 0.76;
}

.stage-sheet-state {
  flex: 0 0 auto;
  font-size: 22rpx;
  font-weight: 900;
  line-height: 1.3;
  opacity: 0.82;
  text-align: right;
}

.progress-track {
  background: rgba(16, 32, 51, 0.12);
  border-radius: 999rpx;
  height: 10rpx;
  overflow: hidden;
  width: 100%;
}

.progress-fill {
  background: #f5b52b;
  border-radius: 999rpx;
  height: 100%;
}

.level-stack {
  display: flex;
  flex-direction: column;
  gap: 14rpx;
  margin-top: 18rpx;
}

.level-btn {
  align-items: center;
  box-shadow: 0 7rpx 0 #d7c49d;
  gap: 14rpx;
  justify-content: space-between;
  min-height: 96rpx;
  padding: 14rpx 18rpx;
  text-align: left;
}

.level-btn.active {
  box-shadow: 0 7rpx 0 #0f6f66;
}

.level-btn.completed {
  border-color: #c6b78f;
}

.level-btn.locked {
  background: #f3eee1;
  color: #8a8170;
  opacity: 0.82;
}

.level-main {
  flex: 1;
  min-width: 0;
}

.level-name {
  display: block;
  font-size: 30rpx;
  line-height: 1.25;
}

.level-count {
  display: block;
  font-size: 23rpx;
  line-height: 1.35;
  margin-top: 8rpx;
  opacity: 0.82;
}

.level-note {
  flex: 0 0 auto;
  font-size: 22rpx;
  max-width: 150rpx;
  opacity: 0.7;
  text-align: right;
}

.selection-summary {
  align-items: flex-start;
  background: #102033;
  border-radius: 20rpx;
  box-sizing: border-box;
  color: #ffffff;
  display: flex;
  gap: 18rpx;
  justify-content: space-between;
  margin-top: 22rpx;
  padding: 20rpx;
}

.summary-label {
  color: #8bd6ce;
  display: block;
  font-size: 21rpx;
  font-weight: 900;
  line-height: 1.3;
}

.summary-title {
  display: block;
  font-size: 27rpx;
  font-weight: 900;
  line-height: 1.35;
  margin-top: 6rpx;
}

.summary-meta {
  color: #e7d7aa;
  flex: 0 0 auto;
  font-size: 22rpx;
  font-weight: 800;
  line-height: 1.35;
  max-width: 190rpx;
  text-align: right;
}

.primary-btn {
  box-shadow: 0 10rpx 0 #0f6f66;
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
  color: #6f6758;
  font-size: 24rpx;
  line-height: 1.45;
  margin: 18rpx 4rpx 16rpx;
}

.card-board {
  display: grid;
  gap: 18rpx;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  position: relative;
}

.card-board.dense {
  gap: 14rpx;
}

.pop-card {
  align-items: center;
  background: #fffdf6;
  border: 2rpx solid #f0d688;
  border-radius: 22rpx;
  box-shadow: 0 8rpx 0 #d5a636, 0 14rpx 24rpx rgba(128, 93, 28, 0.12);
  box-sizing: border-box;
  color: #5b3f20;
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
  background: #f4f9ff;
  border-color: #c9e8f8;
  box-shadow: 0 8rpx 0 #48aee8, 0 14rpx 24rpx rgba(72, 174, 232, 0.12);
  color: #164e8a;
}

.pop-card.selected {
  transform: translateY(4rpx);
  background: #fff7dc;
  box-shadow: 0 4rpx 0 #c98911, inset 0 0 0 2rpx #f5b52b;
  border-color: #f5b52b;
  color: #102033;
}

.pop-card.meaning.selected {
  background: #edf7ff;
  box-shadow: 0 4rpx 0 #318ccb, inset 0 0 0 2rpx #48aee8;
  border-color: #48aee8;
  color: #164e8a;
}

.pop-card.wrong {
  background: #fff1f2;
  border-color: #ef4444;
  color: #b91c1c;
}

.pop-card.cleared {
  opacity: 0;
  pointer-events: none;
  transform: scale(0.88);
}

.tile-pop-move {
  transition: transform 0.68s cubic-bezier(0.18, 1.18, 0.28, 1);
}

.tile-pop-enter-active,
.tile-pop-leave-active {
  transition: opacity 0.24s ease, transform 0.34s cubic-bezier(0.2, 1.15, 0.34, 1);
}

.tile-pop-enter-from {
  opacity: 0;
  transform: scale(0.92);
}

.tile-pop-leave-to {
  opacity: 0;
  transform: scale(0.72);
}

.tile-pop-leave-active {
  pointer-events: none;
  position: absolute;
  width: calc((100% - 18rpx) / 2);
  z-index: 0;
}

.card-board.dense .tile-pop-leave-active {
  width: calc((100% - 14rpx) / 2);
}

.card-text {
  display: block;
  font-size: 30rpx;
  font-weight: 900;
  line-height: 1.25;
  word-break: break-word;
}

.card-sub {
  color: #6f6758;
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

.completed-card.failed {
  border-color: #eab308;
}

.completed-title {
  color: #16897d;
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
