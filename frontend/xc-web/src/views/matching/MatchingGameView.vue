<template>
  <main class="app-shell">
    <header class="topbar matching-hero">
      <div>
        <h1>{{ t('matching.title') }}</h1>
        <p>{{ matchingSubtitle }}</p>
      </div>
      <div class="top-actions">
        <LocaleSwitch />
        <v-btn prepend-icon="mdi-arrow-left" variant="text" @click="$router.push('/')">{{ t('common.back') }}</v-btn>
        <v-btn prepend-icon="mdi-refresh" variant="tonal" :loading="loading" @click="resetGame">{{ t('matching.newGame') }}</v-btn>
      </div>
    </header>

    <section v-if="!activeGame" class="setup-layout">
      <v-card class="panel setup-panel" elevation="0">
        <v-progress-linear v-if="loading" class="panel-loader" color="primary" indeterminate />
        <div class="panel-title">{{ t('matching.settings') }}</div>
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
        <div class="option-strip">
          <div class="option-group">
            <span class="option-label">{{ t('common.referenceLanguage') }}</span>
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
          <div class="option-group">
            <span class="option-label">{{ t('matching.pinyin') }}</span>
            <button
              class="text-switch"
              :class="{ active: pinyinMode === 'with' }"
              type="button"
              @click="togglePinyinMode"
            >
              <span class="switch-option">{{ t('matching.pinyinOff') }}</span>
              <span class="switch-option">{{ t('matching.pinyinOn') }}</span>
              <span class="switch-thumb">{{ pinyinMode === 'with' ? t('matching.pinyinOn') : t('matching.pinyinOff') }}</span>
            </button>
          </div>
          <div class="option-group">
            <span class="option-label">{{ t('matching.difficulty') }}</span>
            <div
              class="choice-row"
              :style="segmentStyle(difficultyOptions.length, optionIndex(difficultyOptions, difficulty))"
            >
              <button
                v-for="item in difficultyOptions"
                :key="item.value"
                class="choice-button"
                :class="{ active: difficulty === item.value }"
                type="button"
                @click="difficulty = item.value"
              >
                {{ item.label }}
              </button>
            </div>
          </div>
          <div class="option-group sound-group">
            <span class="option-label">{{ t('matching.sound') }}</span>
            <v-switch v-model="soundEnabled" class="sound-switch" color="primary" hide-details inset />
          </div>
          <v-btn class="start-button" color="primary" prepend-icon="mdi-play-outline" :disabled="!canStart" :loading="creating" @click="startGame">
            {{ t('matching.start') }}
          </v-btn>
        </div>
      </v-card>

      <v-card class="panel rules-panel" elevation="0">
        <div class="panel-title">{{ t('matching.rulesTitle') }}</div>
        <div class="rule-grid">
          <div>
            <strong>{{ t('matching.rulePairs') }}</strong>
            <span>{{ t('matching.rulePairsText', { target: targetLanguageLabel, reference: referenceLanguageLabel }) }}</span>
          </div>
          <div>
            <strong>{{ t('matching.ruleStats') }}</strong>
            <span>{{ t('matching.ruleStatsText') }}</span>
          </div>
        </div>
      </v-card>
    </section>

    <section v-else class="game-layout">
      <v-card class="panel stat-panel" elevation="0">
        <div>
          <span>{{ t('matching.progress') }}</span>
          <strong>{{ matchedCount }}/{{ activeGame.totalPairs }}</strong>
        </div>
        <div>
          <span>{{ t('matching.errors') }}</span>
          <strong>{{ wrongCount }}</strong>
        </div>
        <div>
          <span>{{ t('matching.elapsed') }}</span>
          <strong>{{ formatDuration(elapsedSeconds) }}</strong>
        </div>
        <div>
          <span>{{ t('matching.difficulty') }}</span>
          <strong>{{ difficultyLabel(activeGame.difficulty) }}</strong>
        </div>
      </v-card>

      <div class="board">
        <v-card class="panel board-column" elevation="0">
          <div class="panel-title">{{ t('matching.targetColumn', { language: targetLanguageLabel }) }}</div>
          <button
            v-for="card in targetCards"
            :key="`target-${card.vocabItemId}`"
            class="match-card target-card"
            :class="cardClass(card.vocabItemId, 'target')"
            type="button"
            :disabled="isMatched(card.vocabItemId) || activeGame.status === 'completed'"
            @click="selectTarget(card.vocabItemId)"
          >
            <strong>{{ targetText(card) }}</strong>
            <span v-if="targetPronunciation(card)">{{ targetPronunciation(card) }}</span>
          </button>
        </v-card>

        <v-card class="panel board-column" elevation="0">
          <div class="panel-title">{{ t('matching.referenceColumn', { language: referenceLanguageLabel }) }}</div>
          <button
            v-for="card in meaningCards"
            :key="`meaning-${card.vocabItemId}`"
            class="match-card"
            :class="cardClass(card.vocabItemId, 'meaning')"
            type="button"
            :disabled="isMatched(card.vocabItemId) || activeGame.status === 'completed'"
            @click="selectMeaning(card.vocabItemId)"
          >
            <span>{{ card.meaning }}</span>
          </button>
        </v-card>
      </div>

      <v-card v-if="activeGame.status === 'completed'" class="panel result-panel" elevation="0">
        <div>
          <div class="panel-title">{{ t('matching.completedTitle') }}</div>
          <p>{{ t('matching.completedText', { time: formatDuration(elapsedSeconds), errors: wrongCount }) }}</p>
        </div>
        <div class="result-actions">
          <v-btn color="primary" prepend-icon="mdi-refresh" @click="resetGame">{{ t('matching.newGame') }}</v-btn>
          <v-btn prepend-icon="mdi-chart-line" variant="tonal" @click="$router.push('/records')">{{ t('records.title') }}</v-btn>
        </div>
      </v-card>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import LocaleSwitch from '@/components/LocaleSwitch.vue'
import { createMatchingGame, fetchMatchingStages, updateMatchingGame } from '../../api/matching'
import { fetchVocabLists } from '../../api/vocab'
import { learningProfile, targetHasPronunciationGuide, type LearningLanguage, type SupportedMeaningLanguage } from '../../config/learningProfile'
import { usePreferenceStore } from '../../stores/preferences'
import type { MatchingDifficulty, MatchingGameCard, MatchingGameSession, MatchingSourceType, MatchingStage, VocabList } from '../../types/api'
import { notifySuccess, notifyWarning } from '../../utils/notify'

type VocabListOption = VocabList & { displayName: string }

const { locale, t } = useI18n()
const preferences = usePreferenceStore()
const loading = ref(false)
const creating = ref(false)
const sourceType = ref<MatchingSourceType>('vocab_list')
const vocabListId = ref<number | null>(null)
const meaningLanguage = ref<SupportedMeaningLanguage>(learningProfile.meaningLanguages[0])
const pinyinMode = ref<'with' | 'without'>('with')
const difficulty = ref<MatchingDifficulty>('')
const soundEnabled = ref(true)
const vocabLists = ref<VocabListOption[]>([])
const stages = ref<MatchingStage[]>([])
const activeGame = ref<MatchingGameSession | null>(null)
const targetCards = ref<MatchingGameCard[]>([])
const meaningCards = ref<MatchingGameCard[]>([])
const selectedTargetId = ref<number | null>(null)
const selectedMeaningId = ref<number | null>(null)
const matchedIds = ref<Set<number>>(new Set())
const wrongCount = ref(0)
const elapsedSeconds = ref(0)
let timer: number | undefined

const matchedCount = computed(() => matchedIds.value.size)
const targetLanguageLabel = computed(() => languageLabel(learningProfile.targetLanguage))
const referenceLanguageLabel = computed(() => languageLabel(meaningLanguage.value))
const meaningLanguageOptions = computed(() => learningProfile.meaningLanguages.map(value => ({
  label: languageLabel(value),
  value
})))
const matchingSubtitle = computed(() => {
  const params = { target: targetLanguageLabel.value, reference: referenceLanguageLabel.value }
  return activeGame.value ? t('matching.playingSubtitle', params) : t('matching.subtitle', params)
})
const sourceOptions = computed(() => [
  { label: t('matching.sources.vocab_list'), value: 'vocab_list' },
  { label: t('matching.sources.favorites'), value: 'favorites' }
])
const difficultyOptions = computed<Array<{ label: string; value: MatchingDifficulty }>>(() => stages.value.map(stage => ({
  label: stageLabel(stage),
  value: stage.code
})))
const canStart = computed(() => Boolean(difficulty.value) && (sourceType.value === 'favorites' || Boolean(vocabListId.value)))

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

function togglePinyinMode() {
  pinyinMode.value = pinyinMode.value === 'with' ? 'without' : 'with'
}

async function loadLists() {
  loading.value = true
  try {
    const [page, stageList] = await Promise.all([fetchVocabLists(100), fetchMatchingStages()])
    vocabLists.value = await buildVocabListOptions(page.records)
    stages.value = stageList
    if (!vocabListId.value && page.records.length > 0) {
      vocabListId.value = page.records[0].id
    }
    if ((!difficulty.value || !stageList.some(item => item.code === difficulty.value)) && stageList.length > 0) {
      difficulty.value = stageList[0].code
    }
  } finally {
    loading.value = false
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

async function startGame() {
  if (sourceType.value === 'vocab_list' && !vocabListId.value) {
    notifyWarning(t('matching.selectListWarning'))
    return
  }
  if (!difficulty.value) {
    notifyWarning(t('matching.selectStageWarning'))
    return
  }
  creating.value = true
  try {
    const game = await createMatchingGame({
      sourceType: sourceType.value,
      vocabListId: sourceType.value === 'vocab_list' ? vocabListId.value : null,
      meaningLanguage: meaningLanguage.value,
      difficulty: difficulty.value
    })
    setGame(game)
  } finally {
    creating.value = false
  }
}

function setGame(game: MatchingGameSession) {
  activeGame.value = game
  matchedIds.value = new Set()
  selectedTargetId.value = null
  selectedMeaningId.value = null
  wrongCount.value = game.wrongCount
  elapsedSeconds.value = game.elapsedSeconds
  targetCards.value = shuffle(game.cards)
  meaningCards.value = shuffle(game.cards)
  startTimer()
}

function selectTarget(id: number) {
  selectedTargetId.value = selectedTargetId.value === id ? null : id
  checkSelection()
}

function selectMeaning(id: number) {
  selectedMeaningId.value = selectedMeaningId.value === id ? null : id
  checkSelection()
}

function checkSelection() {
  if (!selectedTargetId.value || !selectedMeaningId.value) {
    return
  }
  if (selectedTargetId.value === selectedMeaningId.value) {
    const next = new Set(matchedIds.value)
    next.add(selectedTargetId.value)
    matchedIds.value = next
    playTone(660)
    selectedTargetId.value = null
    selectedMeaningId.value = null
    if (activeGame.value && matchedIds.value.size === activeGame.value.totalPairs) {
      void completeGame()
    }
    return
  }
  wrongCount.value += 1
  playTone(220)
  window.setTimeout(() => {
    selectedTargetId.value = null
    selectedMeaningId.value = null
  }, 220)
}

async function completeGame() {
  const game = activeGame.value
  if (!game || game.status === 'completed') {
    return
  }
  stopTimer()
  activeGame.value = await updateMatchingGame(game.id, {
    matchedPairs: matchedIds.value.size,
    wrongCount: wrongCount.value,
    elapsedSeconds: elapsedSeconds.value,
    status: 'completed'
  })
  notifySuccess(t('matching.completedToast'))
}

function resetGame() {
  stopTimer()
  activeGame.value = null
  targetCards.value = []
  meaningCards.value = []
  matchedIds.value = new Set()
  selectedTargetId.value = null
  selectedMeaningId.value = null
  wrongCount.value = 0
  elapsedSeconds.value = 0
}

function startTimer() {
  stopTimer()
  timer = window.setInterval(() => {
    elapsedSeconds.value += 1
  }, 1000)
}

function stopTimer() {
  if (timer !== undefined) {
    window.clearInterval(timer)
    timer = undefined
  }
}

function isMatched(id: number) {
  return matchedIds.value.has(id)
}

function cardClass(id: number, side: 'target' | 'meaning') {
  const wrongSelection = selectedTargetId.value !== null
    && selectedMeaningId.value !== null
    && selectedTargetId.value !== selectedMeaningId.value
  return {
    selected: side === 'target' ? selectedTargetId.value === id : selectedMeaningId.value === id,
    matched: isMatched(id),
    wrong: wrongSelection && (side === 'target' ? selectedTargetId.value === id : selectedMeaningId.value === id)
  }
}

function languageLabel(value: LearningLanguage | SupportedMeaningLanguage) {
  return t(`common.studyLanguages.${value}`)
}

function targetText(card: MatchingGameCard) {
  return card.hanzi
}

function targetPronunciation(card: MatchingGameCard) {
  return targetHasPronunciationGuide() && pinyinMode.value === 'with' ? card.pinyin || '-' : ''
}

function difficultyLabel(value: MatchingDifficulty) {
  const stage = stages.value.find(item => item.code === value)
  return stage ? stageLabel(stage) : value
}

function stageLabel(stage: MatchingStage) {
  const key = locale.value.startsWith('zh') ? 'zh' : locale.value.startsWith('ru') ? 'ru' : 'en'
  return stage.labels[key] || stage.labels.zh || stage.code
}

function formatDuration(seconds: number) {
  const minutes = Math.floor(seconds / 60)
  const rest = seconds % 60
  return `${minutes}:${String(rest).padStart(2, '0')}`
}

function shuffle<T>(items: T[]) {
  const copy = [...items]
  for (let i = copy.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[copy[i], copy[j]] = [copy[j], copy[i]]
  }
  return copy
}

function playTone(frequency: number) {
  if (!soundEnabled.value || typeof window === 'undefined') {
    return
  }
  const audioWindow = window as Window & { webkitAudioContext?: typeof AudioContext }
  const AudioContextClass = window.AudioContext || audioWindow.webkitAudioContext
  if (!AudioContextClass) {
    return
  }
  const audioContext = new AudioContextClass()
  const oscillator = audioContext.createOscillator()
  const gain = audioContext.createGain()
  oscillator.frequency.value = frequency
  oscillator.type = 'sine'
  gain.gain.value = 0.04
  oscillator.connect(gain)
  gain.connect(audioContext.destination)
  oscillator.start()
  oscillator.stop(audioContext.currentTime + 0.08)
}

watch(
  () => preferences.preference?.matchingMeaningLanguage,
  (value) => {
    if (value && learningProfile.meaningLanguages.includes(value)) {
      meaningLanguage.value = value
    }
  }
)

watch(
  () => preferences.preference?.soundEnabled,
  (value) => {
    if (value !== undefined) {
      soundEnabled.value = value
    }
  }
)

watch(meaningLanguage, (value) => {
  if (preferences.loaded && preferences.preference?.matchingMeaningLanguage !== value) {
    void preferences.save({ matchingMeaningLanguage: value })
  }
})

watch(soundEnabled, (value) => {
  if (preferences.loaded && preferences.preference?.soundEnabled !== value) {
    void preferences.save({ soundEnabled: value })
  }
})

onMounted(async () => {
  await preferences.load()
  if (preferences.preference) {
    if (learningProfile.meaningLanguages.includes(preferences.preference.matchingMeaningLanguage)) {
      meaningLanguage.value = preferences.preference.matchingMeaningLanguage
    }
    soundEnabled.value = preferences.preference.soundEnabled
  }
  await loadLists()
})

onBeforeUnmount(stopTimer)
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

.matching-hero {
  background: #142033;
  border: 1px solid #23324a;
  border-radius: 8px;
  color: #f8fafc;
  margin-bottom: 22px;
  padding: 30px;
}

.matching-hero p {
  color: #cbd5e1;
}

.top-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
}

.top-actions :deep(.v-btn),
.option-strip :deep(.v-btn),
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
  gap: 20px;
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

.panel-title {
  font-size: 20px;
  font-weight: 800;
}

.settings-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 18px;
}

.option-strip {
  align-items: end;
  display: grid;
  gap: 14px;
  grid-template-columns: minmax(180px, 0.9fr) minmax(160px, 0.8fr) minmax(240px, 1.25fr) auto auto;
  margin-top: 16px;
}

.option-group {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.option-label {
  color: #64748b;
  font-size: 13px;
  font-weight: 800;
  line-height: 1;
}

.choice-row {
  align-items: center;
  background: #eef2f7;
  border: 1px solid #dbe3ee;
  border-radius: 7px;
  display: grid;
  gap: 0;
  min-height: 40px;
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

.text-switch {
  align-items: center;
  appearance: none;
  background: #eef2f7;
  border: 1px solid #dbe3ee;
  border-radius: 999px;
  cursor: pointer;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  height: 40px;
  min-width: 92px;
  overflow: hidden;
  padding: 3px;
  position: relative;
}

.text-switch .switch-option {
  color: #64748b;
  font-size: 13px;
  font-weight: 900;
  line-height: 1;
  position: relative;
  text-align: center;
  z-index: 1;
}

.text-switch .switch-thumb {
  align-items: center;
  background: #94a3b8;
  border-radius: 999px;
  bottom: 3px;
  box-shadow: 0 5px 12px rgba(15, 23, 42, 0.16);
  color: #ffffff;
  display: flex;
  font-size: 13px;
  font-weight: 900;
  justify-content: center;
  left: 3px;
  position: absolute;
  top: 3px;
  transform: translateX(0);
  transition: transform 0.2s ease, background 0.2s ease;
  width: calc(50% - 3px);
  z-index: 2;
}

.text-switch.active .switch-thumb {
  background: #2563eb;
  transform: translateX(calc(100% + 0px));
}

.sound-group {
  justify-items: center;
  min-width: 76px;
}

.sound-switch {
  height: 40px;
}

.sound-switch :deep(.v-selection-control) {
  min-height: 40px;
}

.start-button {
  align-self: end;
  min-height: 40px;
}

.rule-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 18px;
}

.rule-grid > div {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 16px;
}

.rule-grid strong,
.rule-grid span {
  display: block;
}

.rule-grid span {
  color: #64748b;
  margin-top: 8px;
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
  display: block;
  font-size: 24px;
  line-height: 1.1;
  margin-top: 8px;
}

.board {
  display: grid;
  gap: 18px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.board-column {
  display: grid;
  gap: 12px;
}

.match-card {
  background: #f8fafc;
  border: 1px solid #dbe3ee;
  border-radius: 6px;
  color: #1f2937;
  cursor: pointer;
  min-height: 76px;
  padding: 14px 16px;
  text-align: left;
  transition: background 0.16s ease, border-color 0.16s ease, transform 0.16s ease;
}

.match-card:hover:not(:disabled) {
  background: #eef4ff;
  border-color: #9db8e8;
}

.match-card strong,
.match-card span {
  display: block;
}

.match-card strong {
  font-size: 28px;
  line-height: 1.1;
}

.match-card span {
  color: #64748b;
  margin-top: 6px;
}

.match-card.selected {
  background: #dbeafe;
  border-color: #2563eb;
}

.match-card.matched {
  background: #dcfce7;
  border-color: #22c55e;
  color: #166534;
}

.match-card.wrong {
  background: #fee2e2;
  border-color: #ef4444;
}

.match-card:disabled {
  cursor: default;
}

.result-panel {
  align-items: center;
  display: flex;
  gap: 18px;
  justify-content: space-between;
}

.result-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
}

@media (max-width: 900px) {
  .settings-grid,
  .option-strip,
  .rule-grid,
  .stat-panel,
  .board {
    grid-template-columns: 1fr;
  }

  .matching-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .top-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 720px) {
  h1 {
    font-size: 28px;
  }

  .matching-hero,
  .panel {
    padding: 20px 16px;
  }

  .result-panel {
    align-items: stretch;
    flex-direction: column;
  }

  .start-button,
  .result-actions {
    justify-content: flex-start;
    width: 100%;
  }
}
</style>
