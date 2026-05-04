<template>
  <main class="app-shell">
    <header class="topbar elimination-hero">
      <div>
        <span class="eyebrow">{{ t('app.title') }}</span>
        <h1>{{ t('elimination.title') }}</h1>
        <p>{{ t('elimination.subtitle') }}</p>
      </div>
      <div class="top-actions">
        <LocaleSwitch />
        <v-btn prepend-icon="mdi-arrow-left" variant="text" @click="$router.push('/')">{{ t('common.back') }}</v-btn>
        <v-btn prepend-icon="mdi-refresh" variant="tonal" :loading="setupLoading || updating" @click="resetGame">
          {{ t('matching.newGame') }}
        </v-btn>
      </div>
    </header>

    <section v-if="!activeGame" class="setup-layout">
      <v-card class="panel player-panel" elevation="0">
        <v-progress-linear v-if="setupLoading" class="panel-loader" color="primary" indeterminate />
        <div class="avatar">{{ avatarText }}</div>
        <div>
          <div class="player-name">{{ displayName }}</div>
          <p>{{ t('elimination.playerBest') }} · {{ t('elimination.points') }}</p>
        </div>
      </v-card>

      <v-card class="panel setup-panel" elevation="0">
        <div class="panel-header">
          <div>
            <div class="panel-title">{{ t('elimination.setup') }}</div>
            <p>{{ t('elimination.setupDesc') }}</p>
          </div>
          <div class="setup-actions">
            <div class="reference-switch">
              <span>{{ t('common.referenceLanguage') }}</span>
              <div
                class="choice-row"
                :style="segmentStyle(meaningLanguageOptions.length, optionIndex(meaningLanguageOptions, meaningLanguage))"
              >
                <button
                  v-for="item in meaningLanguageOptions"
                  :key="item.value"
                  class="choice-button"
                  :class="{ active: meaningLanguage === item.value }"
                  type="button"
                  @click="meaningLanguage = item.value"
                >
                  {{ item.label }}
                </button>
              </div>
            </div>
            <div class="reference-switch">
              <span>{{ t('matching.pinyin') }}</span>
              <div
                class="choice-row"
                :style="segmentStyle(pinyinOptions.length, optionIndex(pinyinOptions, pinyinMode))"
              >
                <button
                  v-for="item in pinyinOptions"
                  :key="item.value"
                  class="choice-button"
                  :class="{ active: pinyinMode === item.value }"
                  type="button"
                  @click="pinyinMode = item.value"
                >
                  {{ item.label }}
                </button>
              </div>
            </div>
            <v-btn class="start-button" color="primary" prepend-icon="mdi-play-outline" :disabled="!canStart" :loading="starting" @click="startGame">
              {{ t('matching.start') }}
            </v-btn>
          </div>
        </div>

        <div class="settings-grid">
          <v-select
            v-model="sourceType"
            density="comfortable"
            hide-details
            item-title="label"
            item-value="value"
            :items="sourceOptions"
            :label="t('matching.source')"
            variant="outlined"
          />
          <v-select
            v-if="sourceType === 'vocab_list'"
            v-model="vocabListId"
            density="comfortable"
            hide-details
            item-title="displayName"
            item-value="id"
            :items="vocabLists"
            :label="t('matching.vocabList')"
            variant="outlined"
          />
        </div>

        <template v-if="stageGroups.length > 0">
          <div class="stage-list-head">{{ t('elimination.stageGroups') }}</div>
          <div class="stage-list stage-group-list">
            <button
              v-for="group in stageGroups"
              :key="group.code"
              class="stage-button"
              :class="{ selected: stageGroupCode === group.code }"
              type="button"
              @click="selectStageGroup(group.code)"
            >
              <strong>{{ stageGroupLabel(group) }}</strong>
              <span>{{ t('elimination.levelCount', { count: group.levels.length }) }}</span>
            </button>
          </div>
        </template>

        <div class="stage-list-head">{{ t('elimination.levels') }}</div>
        <div class="stage-list">
          <button
            v-for="(stage, index) in stages"
            :key="stage.code"
            class="stage-button"
            :class="{ selected: difficulty === stage.code, locked: !stage.unlocked, completed: stage.completed }"
            type="button"
            :disabled="!stage.unlocked"
            @click="selectLevel(stage)"
          >
            <strong>{{ levelLabel(index) }}</strong>
            <span>{{ levelMeta(stage) }}</span>
            <small v-if="!stage.unlocked">{{ t('elimination.locked') }}</small>
            <small v-else-if="stage.completed">{{ t('elimination.bestTime', { time: formatDuration(stage.bestElapsedSeconds || 0) }) }}</small>
          </button>
        </div>
      </v-card>
    </section>

    <section v-else class="game-layout">
      <v-card class="panel stat-panel" elevation="0">
        <div>
          <span>{{ t('elimination.cleared') }}</span>
          <strong>{{ matchedPairs }} / {{ activeGame.totalPairs }}</strong>
        </div>
        <div>
          <span>{{ t('matching.errors') }}</span>
          <strong>{{ wrongCount }}</strong>
        </div>
        <div>
          <span>{{ t('elimination.remaining') }}</span>
          <strong>{{ formatDuration(remainingSeconds) }}</strong>
        </div>
        <div>
          <span>{{ t('elimination.combo') }}</span>
          <strong>{{ comboCount }}</strong>
        </div>
      </v-card>

      <div class="hint-line">{{ t('elimination.boardHint') }}</div>

      <v-card class="panel board-panel" elevation="0">
        <TransitionGroup name="tile-pop" tag="div" class="board-grid">
          <button
            v-for="tile in visibleTiles"
            :key="tile.id"
            class="pop-card"
            :class="{ selected: tile.selected, matched: tile.matched, wrong: tile.wrong, meaning: tile.kind === 'meaning' }"
            type="button"
            :disabled="gameCompleted || gameFailed || resolving"
            @click="selectTile(tile.id)"
          >
            <strong>{{ tile.text }}</strong>
            <span v-if="tile.subtext">{{ tile.subtext }}</span>
          </button>
        </TransitionGroup>
      </v-card>

      <v-card v-if="gameCompleted" class="panel result-panel" elevation="0">
        <div>
          <div class="panel-title">{{ t('elimination.completedTitle') }}</div>
          <p>{{ t('elimination.completedSummary', { pairs: matchedPairs, wrong: wrongCount, time: formatDuration(elapsedSeconds) }) }}</p>
        </div>
        <div class="result-actions">
          <v-btn color="primary" prepend-icon="mdi-refresh" :loading="updating" @click="resetGame">{{ t('matching.newGame') }}</v-btn>
          <v-btn prepend-icon="mdi-chart-line" variant="tonal" @click="$router.push('/records')">{{ t('records.title') }}</v-btn>
        </div>
      </v-card>

      <v-card v-if="gameFailed" class="panel result-panel failed" elevation="0">
        <div>
          <div class="panel-title">{{ t('elimination.failedTitle') }}</div>
          <p>{{ t('elimination.failedSummary', { pairs: matchedPairs, total: activeGame.totalPairs }) }}</p>
        </div>
        <div class="result-actions">
          <v-btn color="primary" prepend-icon="mdi-refresh" :loading="updating" @click="resetGame">{{ t('matching.newGame') }}</v-btn>
          <v-btn prepend-icon="mdi-book-open-page-variant" variant="tonal" @click="$router.push('/vocab')">{{ t('vocab.title') }}</v-btn>
        </div>
      </v-card>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import LocaleSwitch from '@/components/LocaleSwitch.vue'
import { createMatchingGame, fetchMatchingStageGroups, updateMatchingGame } from '../../api/matching'
import { fetchVocabLists } from '../../api/vocab'
import { learningProfile, type LearningLanguage, type SupportedMeaningLanguage } from '../../config/learningProfile'
import { usePreferenceStore } from '../../stores/preferences'
import { useSessionStore } from '../../stores/session'
import type { MatchingDifficulty, MatchingGameCard, MatchingGameSession, MatchingSourceType, MatchingStage, MatchingStageGroup, VocabList } from '../../types/api'
import { notifySuccess, notifyWarning } from '../../utils/notify'

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

const router = useRouter()
const { locale, t } = useI18n()
const preferences = usePreferenceStore()
const session = useSessionStore()

const setupLoading = ref(false)
const starting = ref(false)
const updating = ref(false)
const resolving = ref(false)
const sourceType = ref<MatchingSourceType>('vocab_list')
const vocabListId = ref<number | null>(null)
const meaningLanguage = ref<SupportedMeaningLanguage>(learningProfile.meaningLanguages[0])
const pinyinMode = ref<'with' | 'without'>('with')
const difficulty = ref<MatchingDifficulty>('')
const vocabLists = ref<VocabListOption[]>([])
const stageGroups = ref<MatchingStageGroup[]>([])
const stageGroupCode = ref('')
const activeGame = ref<MatchingGameSession | null>(null)
const tiles = ref<Tile[]>([])
const selectedTileIds = ref<string[]>([])
const wrongCount = ref(0)
const matchedPairs = ref(0)
const comboCount = ref(0)
const elapsedSeconds = ref(0)
let timer: number | undefined

const visibleTiles = computed(() => tiles.value.filter(tile => !tile.matched))
const activeStageGroup = computed(() => stageGroups.value.find(item => item.code === stageGroupCode.value) || stageGroups.value[0] || null)
const stages = computed<MatchingStage[]>(() => activeStageGroup.value?.levels ?? [])
const selectedLevel = computed(() => stages.value.find(item => item.code === difficulty.value) || null)
const displayName = computed(() => session.profile?.nickname || session.profile?.email || t('home.learner'))
const avatarText = computed(() => displayName.value.slice(0, 1).toUpperCase())
const gameCompleted = computed(() => Boolean(activeGame.value && (activeGame.value.status === 'completed' || matchedPairs.value >= activeGame.value.totalPairs)))
const gameFailed = computed(() => activeGame.value?.status === 'failed')
const timeLimitSeconds = computed(() => activeGame.value?.timeLimitSeconds || selectedLevel.value?.timeLimitSeconds || 0)
const remainingSeconds = computed(() => timeLimitSeconds.value > 0 ? Math.max(0, timeLimitSeconds.value - elapsedSeconds.value) : elapsedSeconds.value)
const sourceOptions = computed(() => [
  { label: t('matching.sources.vocab_list'), value: 'vocab_list' },
  { label: t('matching.sources.favorites'), value: 'favorites' }
])
const meaningLanguageOptions = computed(() => learningProfile.meaningLanguages.map(value => ({
  label: languageLabel(value),
  value
})))
const pinyinOptions = computed(() => [
  { label: t('matching.withPinyin'), value: 'with' as const },
  { label: t('matching.withoutPinyin'), value: 'without' as const }
])
const canStart = computed(() => Boolean(difficulty.value) && Boolean(selectedLevel.value?.unlocked) && (sourceType.value === 'favorites' || Boolean(vocabListId.value)))

function optionIndex(options: Array<{ value: string }>, value: string) {
  const index = options.findIndex(item => item.value === value)
  return index >= 0 ? index : 0
}

function segmentStyle(count: number, index: number) {
  const safeCount = Math.max(count, 1)
  const safeIndex = Math.min(Math.max(index, 0), safeCount - 1)
  return {
    '--segment-width': `calc(100% / ${safeCount})`,
    '--segment-translate': `${safeIndex * 100}%`,
    gridTemplateColumns: `repeat(${safeCount}, minmax(0, 1fr))`
  }
}

async function loadSetup() {
  setupLoading.value = true
  try {
    const page = await fetchVocabLists(100)
    vocabLists.value = await buildVocabListOptions(page.records)
    if (!vocabListId.value && page.records.length > 0) {
      vocabListId.value = page.records[0].id
    }
    await loadStageGroups()
  } finally {
    setupLoading.value = false
  }
}

async function buildVocabListOptions(parentLists: VocabList[]): Promise<VocabListOption[]> {
  const childPages = await Promise.all(
    parentLists
      .filter(list => list.childCount > 0)
      .map(list => fetchVocabLists({ pageSize: 100, parentId: list.id }).then(page => [list, page.records] as const))
  )
  const childrenByParentId = new Map(childPages.map(([parent, children]) => [parent.id, children]))
  return parentLists.flatMap((list) => {
    const parentOption: VocabListOption = {
      ...list,
      displayName: list.childCount > 0 ? `${list.name} (${t('practice.scopeAll')})` : list.name
    }
    const childOptions = (childrenByParentId.get(list.id) || []).map(child => ({
      ...child,
      displayName: `${list.name} / ${child.name}`
    }))
    return [parentOption, ...childOptions]
  })
}

async function loadStageGroups() {
  const groupList = await fetchMatchingStageGroups({
    gameType: 'elimination',
    sourceType: sourceType.value,
    vocabListId: sourceType.value === 'vocab_list' ? vocabListId.value : null,
    meaningLanguage: meaningLanguage.value
  })
  stageGroups.value = groupList
  if ((!stageGroupCode.value || !groupList.some(item => item.code === stageGroupCode.value)) && groupList.length > 0) {
    stageGroupCode.value = groupList[0].code
  }
  const levelList = groupList.find(item => item.code === stageGroupCode.value)?.levels ?? groupList[0]?.levels ?? []
  if ((!difficulty.value || !levelList.some(item => item.code === difficulty.value)) && levelList.length > 0) {
    difficulty.value = (levelList.find(item => item.unlocked) ?? levelList[0]).code
  }
}

async function startGame() {
  if (sourceType.value === 'vocab_list' && !vocabListId.value) {
    notifyWarning(t('matching.selectListWarning'))
    return
  }
  if (!difficulty.value) {
    notifyWarning(t('matching.selectStageWarning'))
    return
  }
  if (!selectedLevel.value?.unlocked) {
    notifyWarning(t('elimination.lockedWarning'))
    return
  }
  starting.value = true
  try {
    await abandonCurrentGame()
    const nextGame = await createMatchingGame({
      gameType: 'elimination',
      sourceType: sourceType.value,
      vocabListId: sourceType.value === 'vocab_list' ? vocabListId.value : null,
      meaningLanguage: meaningLanguage.value,
      difficulty: difficulty.value
    })
    applyGame(nextGame)
  } finally {
    starting.value = false
  }
}

function applyGame(nextGame: MatchingGameSession) {
  activeGame.value = nextGame
  tiles.value = shuffle(buildTiles(nextGame.cards))
  selectedTileIds.value = []
  wrongCount.value = nextGame.wrongCount
  matchedPairs.value = nextGame.matchedPairs
  comboCount.value = 0
  elapsedSeconds.value = nextGame.elapsedSeconds
  resolving.value = false
  startTimer()
}

function buildTiles(cards: MatchingGameCard[]): Tile[] {
  return cards.flatMap(card => [
    {
      id: `${card.vocabItemId}-hanzi`,
      pairId: card.vocabItemId,
      kind: 'hanzi',
      text: card.hanzi,
      subtext: pinyinMode.value === 'with' ? card.pinyin || '' : '',
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
    if (activeGame.value && matchedPairs.value >= activeGame.value.totalPairs) {
      stopTimer()
      void completeGame()
    }
    return
  }
  resolving.value = true
  wrongCount.value += 1
  comboCount.value = 0
  first.wrong = true
  second.wrong = true
  window.setTimeout(() => {
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
  const game = activeGame.value
  if (!game || updating.value || game.status === 'completed') {
    return
  }
  updating.value = true
  try {
    stopTimer()
    activeGame.value = await updateMatchingGame(game.id, {
      matchedPairs: matchedPairs.value,
      wrongCount: wrongCount.value,
      elapsedSeconds: elapsedSeconds.value,
      status: 'completed'
    })
    notifySuccess(t('elimination.completedTitle'))
  } finally {
    updating.value = false
  }
}

async function failGame() {
  const game = activeGame.value
  if (!game || updating.value || game.status !== 'playing') {
    return
  }
  updating.value = true
  try {
    stopTimer()
    activeGame.value = await updateMatchingGame(game.id, {
      matchedPairs: matchedPairs.value,
      wrongCount: wrongCount.value,
      elapsedSeconds: elapsedSeconds.value,
      status: 'failed'
    })
  } catch {
    notifyWarning(t('matching.saveFailed'))
  } finally {
    updating.value = false
  }
}

async function abandonCurrentGame() {
  const game = activeGame.value
  if (!game || game.status !== 'playing' || matchedPairs.value >= game.totalPairs) {
    return
  }
  try {
    await updateMatchingGame(game.id, {
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
    stopTimer()
    activeGame.value = null
    tiles.value = []
    selectedTileIds.value = []
    wrongCount.value = 0
    matchedPairs.value = 0
    comboCount.value = 0
    elapsedSeconds.value = 0
    await loadStageGroups()
    difficulty.value = (stages.value.find(item => item.unlocked && !item.completed) ?? stages.value.find(item => item.unlocked) ?? stages.value[0])?.code || ''
  } finally {
    updating.value = false
  }
}

function startTimer() {
  stopTimer()
  timer = window.setInterval(() => {
    elapsedSeconds.value += 1
    if (timeLimitSeconds.value > 0 && elapsedSeconds.value >= timeLimitSeconds.value && !gameCompleted.value && !gameFailed.value) {
      void failGame()
    }
  }, 1000)
}

function stopTimer() {
  if (timer !== undefined) {
    window.clearInterval(timer)
    timer = undefined
  }
}

function languageLabel(value: LearningLanguage | SupportedMeaningLanguage) {
  return t(`common.studyLanguages.${value}`)
}

function selectStageGroup(code: string) {
  stageGroupCode.value = code
  difficulty.value = (stages.value.find(item => item.unlocked) ?? stages.value[0])?.code || ''
}

function selectLevel(stage: MatchingStage) {
  if (!stage.unlocked) {
    notifyWarning(t('elimination.lockedWarning'))
    return
  }
  difficulty.value = stage.code
}

function stageGroupLabel(stage: MatchingStageGroup) {
  const key = locale.value.startsWith('zh') ? 'zh' : locale.value.startsWith('ru') ? 'ru' : 'en'
  return stage.labels[key] || stage.labels.zh || stage.code
}

function levelLabel(index: number) {
  return t('elimination.levelNumber', { number: index + 1 })
}

function levelMeta(stage: MatchingStage) {
  return `${t('elimination.cards', { count: stage.cardCount })} · ${t('elimination.limit', { time: formatDuration(stage.timeLimitSeconds) })}`
}

function formatDuration(seconds: number) {
  const minutes = Math.floor(seconds / 60)
  const rest = seconds % 60
  return `${minutes}:${String(rest).padStart(2, '0')}`
}

function shuffle<T>(items: T[]) {
  const copy = [...items]
  for (let index = copy.length - 1; index > 0; index -= 1) {
    const target = Math.floor(Math.random() * (index + 1))
    ;[copy[index], copy[target]] = [copy[target], copy[index]]
  }
  return copy
}

watch(
  () => preferences.preference?.matchingMeaningLanguage,
  (value) => {
    if (value && learningProfile.meaningLanguages.includes(value)) {
      meaningLanguage.value = value
    }
  }
)

watch(meaningLanguage, (value) => {
  if (preferences.loaded && preferences.preference?.matchingMeaningLanguage !== value) {
    void preferences.save({ matchingMeaningLanguage: value })
  }
})

watch([sourceType, vocabListId, meaningLanguage], () => {
  if (!activeGame.value) {
    void loadStageGroups()
  }
})

onMounted(async () => {
  await preferences.load()
  if (preferences.preference && learningProfile.meaningLanguages.includes(preferences.preference.matchingMeaningLanguage)) {
    meaningLanguage.value = preferences.preference.matchingMeaningLanguage
  }
  await loadSetup()
})

onBeforeUnmount(() => {
  stopTimer()
  void abandonCurrentGame()
})
</script>

<style scoped>
h1 {
  font-size: 34px;
  line-height: 1.15;
  margin: 4px 0 0;
}

p {
  color: #64748b;
  margin: 8px 0 0;
}

.elimination-hero {
  background: #142033;
  border: 1px solid #23324a;
  border-radius: 8px;
  color: #f8fafc;
  margin-bottom: 22px;
  padding: 30px;
}

.elimination-hero p {
  color: #cbd5e1;
}

.eyebrow {
  color: #93c5fd;
  font-size: 13px;
  font-weight: 800;
}

.top-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
}

.top-actions :deep(.v-btn),
.panel-header :deep(.v-btn),
.result-actions :deep(.v-btn) {
  border-radius: 4px;
  letter-spacing: 0;
}

.top-actions :deep(.v-btn--variant-text) {
  color: #f8fafc;
}

.setup-layout,
.game-layout {
  display: grid;
  gap: 18px;
}

.panel {
  background: #ffffff;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  padding: 24px;
  position: relative;
}

.panel-loader {
  left: 0;
  position: absolute;
  right: 0;
  top: 0;
}

.player-panel {
  align-items: center;
  display: flex;
  gap: 16px;
}

.avatar {
  align-items: center;
  background: #2563eb;
  border-radius: 8px;
  color: #ffffff;
  display: flex;
  flex: 0 0 58px;
  font-size: 28px;
  font-weight: 900;
  height: 58px;
  justify-content: center;
}

.player-name {
  color: #0f172a;
  font-size: 22px;
  font-weight: 900;
}

.panel-header {
  align-items: flex-start;
  display: flex;
  gap: 16px;
  justify-content: space-between;
}

.setup-actions {
  align-items: center;
  display: flex;
  flex: 0 0 auto;
  gap: 12px;
}

.reference-switch {
  align-items: center;
  display: flex;
  gap: 10px;
  min-height: 40px;
}

.reference-switch > span {
  color: #64748b;
  font-size: 13px;
  font-weight: 800;
  line-height: 1;
  white-space: nowrap;
}

.choice-row {
  align-items: center;
  background: #eef2f7;
  border: 1px solid #dbe3ee;
  border-radius: 7px;
  display: grid;
  gap: 0;
  min-height: 40px;
  min-width: 148px;
  overflow: hidden;
  padding: 0;
  position: relative;
}

.choice-row::before {
  background: #2563eb;
  border-radius: 0;
  bottom: 0;
  box-shadow: 0 5px 12px rgba(37, 99, 235, 0.24);
  content: "";
  left: 0;
  position: absolute;
  top: 0;
  transform: translateX(var(--segment-translate, 0));
  transition: transform 0.22s ease;
  width: var(--segment-width, 50%);
}

.choice-button {
  align-items: center;
  appearance: none;
  background: transparent;
  border: 0;
  border-radius: 4px;
  color: #475569;
  cursor: pointer;
  display: flex;
  font-family: inherit;
  font-size: 14px;
  font-weight: 800;
  justify-content: center;
  letter-spacing: 0;
  min-height: 32px;
  padding: 0 14px;
  position: relative;
  transition: color 0.18s ease;
  z-index: 1;
}

.choice-button:hover {
  color: #1d4ed8;
}

.choice-button.active {
  color: #ffffff;
}

.panel-title {
  color: #0f172a;
  font-size: 20px;
  font-weight: 900;
}

.settings-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 20px;
}

.stage-list-head {
  color: #64748b;
  font-size: 13px;
  font-weight: 800;
  margin-top: 18px;
}

.stage-list {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  margin-top: 10px;
}

.stage-button {
  background: #f8fafc;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  color: #0f172a;
  cursor: pointer;
  min-height: 72px;
  padding: 14px 16px;
  text-align: left;
}

.stage-button strong,
.stage-button span {
  display: block;
}

.stage-button strong {
  color: #0f172a;
  font-size: 18px;
}

.stage-button span {
  color: #64748b;
  margin-top: 6px;
}

.stage-button small {
  color: #64748b;
  display: block;
  font-size: 12px;
  font-weight: 800;
  margin-top: 8px;
}

.stage-button.selected {
  background: #eff6ff;
  border-color: #2563eb;
  box-shadow: inset 0 0 0 1px #2563eb;
}

.stage-button.completed {
  border-color: #94a3b8;
}

.stage-button.locked {
  background: #f1f5f9;
  color: #94a3b8;
  cursor: not-allowed;
}

.stage-button.locked strong,
.stage-button.locked span {
  color: #94a3b8;
}

.stat-panel {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.stat-panel span {
  color: #64748b;
  display: block;
  font-size: 13px;
}

.stat-panel strong {
  color: #0f172a;
  display: block;
  font-size: 24px;
  line-height: 1.1;
  margin-top: 8px;
}

.hint-line {
  color: #2563eb;
  font-size: 14px;
  font-weight: 700;
}

.board-panel {
  background: #f5f9fc;
}

.board-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(auto-fit, minmax(136px, 1fr));
  position: relative;
}

.pop-card {
  background: #ffffff;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.06);
  color: #0f172a;
  cursor: pointer;
  min-height: 116px;
  padding: 16px 12px;
  text-align: center;
  transition: transform 0.16s ease, border-color 0.16s ease, background 0.16s ease;
}

.pop-card:hover:not(:disabled) {
  transform: translateY(-2px);
}

.pop-card strong,
.pop-card span {
  display: block;
}

.pop-card strong {
  color: #0f172a;
  font-size: 24px;
  line-height: 1.25;
}

.pop-card span {
  color: #64748b;
  font-size: 13px;
  margin-top: 8px;
}

.pop-card.meaning strong {
  font-size: 18px;
}

.pop-card.selected {
  background: #eff6ff;
  border-color: #2563eb;
  box-shadow: inset 0 0 0 1px #2563eb, 0 10px 22px rgba(37, 99, 235, 0.16);
}

.pop-card.matched {
  background: #f0f7ff;
  border-color: #60a5fa;
  box-shadow: inset 0 0 0 1px #60a5fa;
  opacity: 0.55;
}

.pop-card.wrong {
  background: #fff1f2;
  border-color: #ef4444;
  box-shadow: inset 0 0 0 1px #ef4444, 0 10px 22px rgba(239, 68, 68, 0.14);
}

.pop-card:disabled {
  cursor: default;
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
  z-index: 0;
}

.result-panel {
  align-items: center;
  display: flex;
  gap: 18px;
  justify-content: space-between;
}

.result-panel.failed {
  border-color: #f59e0b;
}

.result-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
}

@media (max-width: 900px) {
  .elimination-hero,
  .panel-header,
  .result-panel {
    align-items: flex-start;
    flex-direction: column;
  }

  .top-actions,
  .result-actions {
    justify-content: flex-start;
  }

  .setup-actions {
    flex-wrap: wrap;
    width: 100%;
  }

  .reference-switch {
    flex: 1 1 280px;
  }

  .settings-grid,
  .stat-panel {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  h1 {
    font-size: 28px;
  }

  .elimination-hero,
  .panel {
    padding: 20px 16px;
  }

  .board-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .setup-actions,
  .reference-switch {
    align-items: stretch;
    flex-direction: column;
  }

  .reference-switch > span {
    padding-top: 4px;
  }

  .choice-row,
  .start-button {
    width: 100%;
  }

  .pop-card {
    min-height: 100px;
  }
}
</style>
