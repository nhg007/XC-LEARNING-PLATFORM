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
            item-title="name"
            item-value="id"
            :items="vocabLists"
            :label="t('matching.vocabList')"
            variant="outlined"
          />
          <div class="language-control">
            <span>{{ t('common.referenceLanguage') }}</span>
            <v-btn-toggle v-model="meaningLanguage" class="segmented" color="primary" density="comfortable" mandatory variant="outlined">
              <v-btn v-for="item in meaningLanguageOptions" :key="item.value" :value="item.value">
                {{ item.label }}
              </v-btn>
            </v-btn-toggle>
          </div>
          <v-btn-toggle v-model="difficulty" class="segmented" color="primary" density="comfortable" mandatory variant="outlined">
            <v-btn v-for="item in difficultyOptions" :key="item.value" :value="item.value">{{ item.label }}</v-btn>
          </v-btn-toggle>
          <v-switch v-model="soundEnabled" color="primary" hide-details :label="t('matching.sound')" />
        </div>
        <div class="setup-actions">
          <v-btn color="primary" prepend-icon="mdi-play-outline" :loading="creating" @click="startGame">
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
import { createMatchingGame, updateMatchingGame } from '../../api/matching'
import { fetchVocabLists } from '../../api/vocab'
import { learningProfile, targetHasPronunciationGuide, type LearningLanguage, type SupportedMeaningLanguage } from '../../config/learningProfile'
import { usePreferenceStore } from '../../stores/preferences'
import type { MatchingDifficulty, MatchingGameCard, MatchingGameSession, MatchingSourceType, VocabList } from '../../types/api'
import { notifySuccess, notifyWarning } from '../../utils/notify'

const { t } = useI18n()
const preferences = usePreferenceStore()
const loading = ref(false)
const creating = ref(false)
const sourceType = ref<MatchingSourceType>('vocab_list')
const vocabListId = ref<number | null>(null)
const meaningLanguage = ref<SupportedMeaningLanguage>(learningProfile.meaningLanguages[0])
const difficulty = ref<MatchingDifficulty>('4x4')
const soundEnabled = ref(true)
const vocabLists = ref<VocabList[]>([])
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
const difficultyOptions = computed<Array<{ label: string; value: MatchingDifficulty }>>(() => [
  { label: t('matching.difficulties.4x4'), value: '4x4' },
  { label: t('matching.difficulties.7x7'), value: '7x7' },
  { label: t('matching.difficulties.10x10'), value: '10x10' }
])

async function loadLists() {
  loading.value = true
  try {
    const page = await fetchVocabLists(100)
    vocabLists.value = page.records
    if (!vocabListId.value && page.records.length > 0) {
      vocabListId.value = page.records[0].id
    }
  } finally {
    loading.value = false
  }
}

async function startGame() {
  if (sourceType.value === 'vocab_list' && !vocabListId.value) {
    notifyWarning(t('matching.selectListWarning'))
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
  return {
    selected: side === 'target' ? selectedTargetId.value === id : selectedMeaningId.value === id,
    matched: isMatched(id),
    wrong: selectedTargetId.value !== null && selectedMeaningId.value !== null && selectedTargetId.value !== selectedMeaningId.value
      && (selectedTargetId.value === id || selectedMeaningId.value === id)
  }
}

function languageLabel(value: LearningLanguage | SupportedMeaningLanguage) {
  return t(`common.studyLanguages.${value}`)
}

function targetText(card: MatchingGameCard) {
  return card.hanzi
}

function targetPronunciation(card: MatchingGameCard) {
  return targetHasPronunciationGuide() ? card.pinyin || '-' : ''
}

function difficultyLabel(value: MatchingDifficulty) {
  return t(`matching.difficulties.${value}`)
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
.setup-actions :deep(.v-btn),
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

.segmented {
  width: 100%;
}

.language-control {
  display: grid;
  gap: 6px;
}

.language-control > span {
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
}

.segmented :deep(.v-btn) {
  flex: 1 1 0;
}

.setup-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
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

  .result-actions {
    justify-content: flex-start;
  }
}
</style>
