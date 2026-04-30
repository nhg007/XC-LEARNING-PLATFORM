<template>
  <main class="app-shell dialogue-shell">
    <header class="topbar dialogue-hero">
      <div>
        <h1>{{ t('dialogue.title') }}</h1>
        <p>{{ activeMaterial ? activeMaterial.title : t('dialogue.subtitle') }}</p>
      </div>
      <div class="top-actions">
        <LocaleSwitch />
        <v-btn prepend-icon="mdi-arrow-left" variant="text" @click="$router.push('/')">{{ t('common.back') }}</v-btn>
        <v-btn prepend-icon="mdi-refresh" variant="tonal" :loading="loading" @click="reload">{{ t('common.refresh') }}</v-btn>
      </div>
    </header>

    <section v-if="!activeMaterial" class="material-toolbar">
      <div class="material-nav" role="tablist">
        <button
          v-for="item in materialTypeOptions"
          :key="item.value || 'all'"
          class="material-nav-button"
          :class="{ active: materialType === item.value }"
          role="tab"
          :aria-selected="materialType === item.value"
          type="button"
          @click="setMaterialType(item.value)"
        >
          <v-icon :icon="item.icon" />
          <span>{{ item.label }}</span>
        </button>
      </div>
    </section>

    <section v-if="!activeMaterial" class="material-grid">
      <v-progress-linear v-if="loading" class="grid-loader" color="primary" indeterminate />
      <v-card
        v-for="item in materials"
        :key="item.id"
        class="material-card"
        elevation="0"
        @click="selectMaterial(item)"
      >
        <div class="material-cover">
          <img v-if="item.coverUrl" :src="item.coverUrl" alt="" />
          <v-icon v-else icon="mdi-movie-open-play-outline" />
        </div>
        <div class="material-body">
          <span>{{ typeLabel(item.materialType) }}</span>
          <h2>{{ item.title }}</h2>
          <p>{{ item.description || t('dialogue.noDescription') }}</p>
          <strong>{{ t('dialogue.lineCount', { count: item.lineCount }) }}</strong>
        </div>
      </v-card>
      <div v-if="materials.length === 0 && !loading" class="empty-state">{{ t('dialogue.noMaterials') }}</div>
    </section>

    <section v-else class="dialogue-workspace">
      <aside class="line-rail">
        <div class="rail-head">
          <span>{{ typeLabel(activeMaterial.materialType) }}</span>
          <strong>{{ activeMaterial.title }}</strong>
        </div>
        <button
          v-for="(line, index) in lines"
          :key="line.id"
          class="line-item"
          :class="{ active: index === lineIndex }"
          type="button"
          @click="selectLine(index)"
        >
          <span>{{ line.lineNo }}</span>
          <strong>{{ line.pinyinText || line.hanziText }}</strong>
        </button>
        <v-btn block class="change-material" variant="tonal" @click="changeMaterial">{{ t('dialogue.changeMaterial') }}</v-btn>
      </aside>

      <section class="practice-column">
        <v-progress-linear v-if="loading" class="layout-loader" color="primary" indeterminate />
        <v-card class="line-panel" elevation="0">
          <template v-if="currentLine">
            <div class="line-meta">
              <span>{{ t('dialogue.currentLine', { current: lineIndex + 1, total: lines.length }) }}</span>
              <div class="language-control">
                <span>{{ t('common.referenceLanguage') }}</span>
                <v-btn-toggle v-model="meaningLanguage" color="primary" density="compact" mandatory variant="outlined">
                  <v-btn value="ru">{{ t('common.russian') }}</v-btn>
                  <v-btn value="en">{{ t('common.english') }}</v-btn>
                </v-btn-toggle>
              </div>
            </div>

            <div class="prompt-card">
              <div>
                <span>{{ t('dialogue.translationPrompt') }}</span>
                <strong>{{ translationText || t('dialogue.noTranslation') }}</strong>
              </div>
              <v-btn prepend-icon="mdi-volume-high" :loading="speech.speaking.value" variant="tonal" @click="playLineAudio">
                {{ t('dialogue.playAudio') }}
              </v-btn>
            </div>

            <v-tabs v-model="mode" class="mode-tabs" color="primary" density="comfortable">
              <v-tab value="order">{{ t('dialogue.modes.order') }}</v-tab>
              <v-tab value="dictation">{{ t('dialogue.modes.dictation') }}</v-tab>
              <v-tab value="shadow">{{ t('dialogue.modes.shadow') }}</v-tab>
            </v-tabs>

            <div v-if="mode === 'order'" class="order-practice">
              <div class="selected-zone">
                <button v-for="(word, index) in selectedWords" :key="`${word}-${index}`" type="button" @click="removeWord(index)">
                  {{ word }}
                </button>
                <span v-if="selectedWords.length === 0">{{ t('dialogue.selectWordsHint') }}</span>
              </div>
              <div class="word-options">
                <button v-for="(word, index) in availableWords" :key="`${word}-${index}`" type="button" @click="selectWord(word)">
                  {{ word }}
                </button>
              </div>
            </div>

            <v-textarea
              v-else-if="mode === 'dictation'"
              v-model="answerText"
              auto-grow
              counter="4000"
              :label="t('dialogue.answerPlaceholder')"
              maxlength="4000"
              rows="7"
              variant="outlined"
            />

            <div v-else class="shadow-box">
              <span>{{ currentLine.pinyinText }}</span>
              <strong>{{ currentLine.hanziText }}</strong>
              <p>{{ t('dialogue.shadowHint') }}</p>
            </div>

            <div v-if="result" class="result-box" :class="{ ok: result.correct }">
              <strong>{{ result.message }}</strong>
              <span>{{ t('dialogue.standardAnswer', { answer: result.standardAnswer }) }}</span>
            </div>

            <div class="actions-row">
              <v-btn color="primary" :loading="submitting" @click="submitAnswer">
                {{ mode === 'shadow' ? t('dialogue.completeShadow') : t('dialogue.submit') }}
              </v-btn>
              <v-btn variant="tonal" @click="showAnalysis">{{ t('dialogue.showAnswer') }}</v-btn>
              <v-btn :disabled="lineIndex <= 0" variant="tonal" @click="previousLine">{{ t('dialogue.previous') }}</v-btn>
              <v-btn :disabled="lineIndex >= lines.length - 1" variant="tonal" @click="nextLine">{{ t('dialogue.next') }}</v-btn>
            </div>
          </template>
          <div v-else class="empty-state">{{ t('dialogue.noLines') }}</div>
        </v-card>

        <v-card class="analysis-panel" elevation="0">
          <div class="panel-title">{{ t('dialogue.analysis') }}</div>
          <template v-if="analysis">
            <div class="answer-line">
              <strong>{{ analysis.hanziText }}</strong>
              <span>{{ analysis.pinyinText }}</span>
              <p>{{ meaningLanguage === 'ru' ? analysis.translationRu : analysis.translationEn }}</p>
            </div>
            <div v-if="analysis.vocabItems.length > 0" class="vocab-grid">
              <div v-for="item in analysis.vocabItems" :key="item.id" class="vocab-card">
                <strong>{{ item.wordText }}</strong>
                <span>{{ item.pinyin }}</span>
                <p>{{ meaningLanguage === 'ru' ? item.meaningRu : item.meaningEn }}</p>
                <small v-if="item.explanation">{{ item.explanation }}</small>
              </div>
            </div>
            <div v-else class="empty-state compact">{{ t('dialogue.noVocabAnalysis') }}</div>
          </template>
          <div v-else class="empty-state compact">{{ t('dialogue.analysisHint') }}</div>
        </v-card>
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import LocaleSwitch from '@/components/LocaleSwitch.vue'
import { useSpeechPlayer } from '@/composables/useSpeechPlayer'
import { checkDialogueLine, fetchDialogueLineAnalysis, fetchDialogueLines, fetchVideoMaterials } from '../../api/dialogue'
import { usePreferenceStore } from '../../stores/preferences'
import type { DialogueLine, DialogueLineAnalysis, DialogueLineCheckResult, VideoMaterial, VideoMaterialType } from '../../types/api'
import { notifyWarning } from '../../utils/notify'

type DialogueMode = 'order' | 'dictation' | 'shadow'

const { t } = useI18n()
const preferences = usePreferenceStore()
const speech = useSpeechPlayer()
const loading = ref(false)
const submitting = ref(false)
const materialType = ref<VideoMaterialType | ''>('')
const materials = ref<VideoMaterial[]>([])
const activeMaterial = ref<VideoMaterial | null>(null)
const lines = ref<DialogueLine[]>([])
const lineIndex = ref(0)
const mode = ref<DialogueMode>('order')
const meaningLanguage = ref<'ru' | 'en'>('ru')
const selectedWords = ref<string[]>([])
const answerText = ref('')
const analysis = ref<DialogueLineAnalysis | null>(null)
const result = ref<DialogueLineCheckResult | null>(null)
const startedAt = ref(Date.now())

const materialTypeOptions = computed<Array<{ icon: string; label: string; value: VideoMaterialType | '' }>>(() => [
  { icon: 'mdi-apps', label: t('dialogue.types.all'), value: '' },
  { icon: 'mdi-television-classic', label: t('dialogue.types.drama'), value: 'drama' },
  { icon: 'mdi-video-outline', label: t('dialogue.types.short_video'), value: 'short_video' },
  { icon: 'mdi-palette-outline', label: t('dialogue.types.cartoon'), value: 'cartoon' }
])
const currentLine = computed(() => lines.value[lineIndex.value])
const translationText = computed(() => {
  const line = currentLine.value
  if (!line) {
    return ''
  }
  return meaningLanguage.value === 'ru' ? line.translationRu || '' : line.translationEn || ''
})
const availableWords = computed(() => {
  const line = currentLine.value
  if (!line) {
    return []
  }
  const usedCounts = selectedWords.value.reduce<Record<string, number>>((counts, word) => {
    counts[word] = (counts[word] || 0) + 1
    return counts
  }, {})
  const seenCounts: Record<string, number> = {}
  return line.wordOptions.filter((word) => {
    const seen = seenCounts[word] || 0
    seenCounts[word] = seen + 1
    return seen >= (usedCounts[word] || 0)
  })
})

async function loadMaterials() {
  loading.value = true
  try {
    const page = await fetchVideoMaterials(materialType.value)
    materials.value = page.records
  } finally {
    loading.value = false
  }
}

async function setMaterialType(type: VideoMaterialType | '') {
  if (materialType.value === type) {
    return
  }
  materialType.value = type
  await loadMaterials()
}

async function selectMaterial(material: VideoMaterial) {
  activeMaterial.value = material
  loading.value = true
  try {
    lines.value = await fetchDialogueLines(material.id)
    lineIndex.value = 0
    resetPractice()
  } finally {
    loading.value = false
  }
}

async function reload() {
  if (activeMaterial.value) {
    await selectMaterial(activeMaterial.value)
    return
  }
  await loadMaterials()
}

function selectLine(index: number) {
  lineIndex.value = index
  resetPractice()
}

function changeMaterial() {
  activeMaterial.value = null
  lines.value = []
  resetPractice()
}

function selectWord(word: string) {
  selectedWords.value.push(word)
  result.value = null
}

function removeWord(index: number) {
  selectedWords.value.splice(index, 1)
  result.value = null
}

async function submitAnswer() {
  const line = currentLine.value
  if (!line) {
    return
  }
  if (mode.value === 'order' && selectedWords.value.length === 0) {
    notifyWarning(t('dialogue.selectWordsWarning'))
    return
  }
  if (mode.value === 'dictation' && !answerText.value.trim()) {
    notifyWarning(t('dialogue.answerRequired'))
    return
  }
  submitting.value = true
  try {
    result.value = await checkDialogueLine(line.id, {
      answerText: mode.value === 'dictation' ? answerText.value : mode.value === 'shadow' ? line.hanziText : undefined,
      orderedWords: mode.value === 'order' ? selectedWords.value : undefined,
      showedAnswer: Boolean(analysis.value) || mode.value === 'shadow',
      translationLanguage: meaningLanguage.value,
      durationSeconds: elapsedSeconds()
    })
    analysis.value = await fetchDialogueLineAnalysis(line.id)
  } finally {
    submitting.value = false
  }
}

async function showAnalysis() {
  const line = currentLine.value
  if (!line) {
    return
  }
  analysis.value = await fetchDialogueLineAnalysis(line.id)
}

function playLineAudio() {
  const line = currentLine.value
  if (!line) {
    return
  }
  speech.playTargetText(line.hanziText, line.audioUrl)
}

function previousLine() {
  if (lineIndex.value > 0) {
    selectLine(lineIndex.value - 1)
  }
}

function nextLine() {
  if (lineIndex.value < lines.value.length - 1) {
    selectLine(lineIndex.value + 1)
  }
}

function resetPractice() {
  speech.stop()
  selectedWords.value = []
  answerText.value = ''
  analysis.value = null
  result.value = null
  startedAt.value = Date.now()
}

function elapsedSeconds() {
  return Math.max(0, Math.round((Date.now() - startedAt.value) / 1000))
}

function typeLabel(type: string) {
  return t(`dialogue.types.${type}`)
}

watch(
  () => preferences.preference?.translationLanguage,
  (value) => {
    if (value) {
      meaningLanguage.value = value
    }
  }
)

watch(meaningLanguage, (value) => {
  if (preferences.loaded && preferences.preference?.translationLanguage !== value) {
    void preferences.save({ translationLanguage: value })
  }
})

onMounted(async () => {
  await preferences.load()
  if (preferences.preference?.translationLanguage) {
    meaningLanguage.value = preferences.preference.translationLanguage
  }
  await loadMaterials()
})

onBeforeUnmount(speech.stop)
</script>

<style scoped>
.dialogue-hero {
  background: #142033;
  border: 1px solid #23324a;
  border-radius: 8px;
  color: #f8fafc;
  padding: 28px 30px;
}

.dialogue-hero h1 {
  font-size: 32px;
  line-height: 1.2;
  margin: 0;
}

.dialogue-hero p {
  color: #cbd5e1;
  margin: 8px 0 0;
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

.top-actions :deep(.v-btn--variant-text) {
  color: #f8fafc;
}

.material-toolbar {
  align-items: center;
  background: #ffffff;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  display: flex;
  justify-content: flex-start;
  margin-bottom: 18px;
  padding: 12px 16px;
}

.material-nav {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.material-nav-button {
  align-items: center;
  appearance: none;
  background: #ffffff;
  border: 1px solid transparent;
  border-radius: 6px;
  color: #475569;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  font-family: inherit;
  gap: 5px;
  justify-content: center;
  letter-spacing: 0;
  min-height: 58px;
  min-width: 88px;
  padding: 8px 14px;
  transition: background 0.16s ease, border-color 0.16s ease, box-shadow 0.16s ease, color 0.16s ease;
}

.material-nav-button :deep(.v-icon) {
  font-size: 20px;
}

.material-nav-button span {
  font-size: 13px;
  font-weight: 800;
  line-height: 1;
}

.material-nav-button:hover {
  background: #f8fbff;
  border-color: #c8d8f0;
  color: #1d4ed8;
}

.material-nav-button.active {
  background: #e8f1ff;
  border-color: #93b4e8;
  box-shadow: inset 0 0 0 1px rgba(37, 99, 235, 0.12);
  color: #1d4ed8;
}

.material-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  position: relative;
}

.grid-loader,
.layout-loader {
  left: 0;
  position: absolute;
  right: 0;
  top: 0;
}

.material-card {
  background: #ffffff;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  cursor: pointer;
  display: grid;
  grid-template-columns: 112px 1fr;
  min-height: 168px;
  overflow: hidden;
}

.material-card:hover {
  border-color: #2563eb;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08);
}

.material-cover {
  align-items: center;
  background: #1f2f46;
  color: #bfdbfe;
  display: flex;
  font-size: 42px;
  justify-content: center;
}

.material-cover img {
  height: 100%;
  object-fit: cover;
  width: 100%;
}

.material-body {
  padding: 18px;
}

.material-body span,
.rail-head span,
.line-meta span,
.prompt-card span {
  color: #64748b;
  display: block;
  font-size: 13px;
}

.material-body h2 {
  font-size: 20px;
  line-height: 1.25;
  margin: 8px 0;
}

.material-body p {
  color: #64748b;
  line-height: 1.7;
  margin: 0 0 12px;
}

.material-body strong {
  color: #1d4ed8;
  font-size: 14px;
}

.dialogue-workspace {
  display: grid;
  gap: 18px;
  grid-template-columns: 260px 1fr;
}

.line-rail,
.line-panel,
.analysis-panel {
  background: #ffffff;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
}

.line-rail {
  align-self: start;
  padding: 14px;
}

.rail-head {
  border-bottom: 1px solid #e5edf7;
  margin-bottom: 12px;
  padding: 4px 4px 12px;
}

.rail-head strong {
  display: block;
  font-size: 18px;
  line-height: 1.35;
  margin-top: 4px;
}

.line-item {
  background: #ffffff;
  border: 1px solid #e5edf7;
  border-radius: 6px;
  cursor: pointer;
  display: grid;
  gap: 8px;
  grid-template-columns: 28px 1fr;
  margin-bottom: 8px;
  padding: 12px;
  text-align: left;
  width: 100%;
}

.line-item:hover {
  background: #f2f7ff;
}

.line-item.active {
  background: #e8f1ff;
  border-color: #2563eb;
  box-shadow: inset 3px 0 0 #2563eb;
}

.line-item span {
  color: #1d4ed8;
  font-weight: 800;
}

.line-item strong {
  font-size: 14px;
  line-height: 1.45;
}

.change-material {
  margin-top: 10px;
}

.practice-column {
  display: grid;
  gap: 16px;
  position: relative;
}

.line-panel,
.analysis-panel {
  padding: 18px;
}

.line-meta {
  align-items: center;
  display: flex;
  gap: 16px;
  justify-content: space-between;
  margin-bottom: 14px;
}

.language-control {
  align-items: center;
  display: flex;
  gap: 8px;
}

.language-control > span {
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
}

.line-meta :deep(.v-btn) {
  border-radius: 4px;
  letter-spacing: 0;
}

.prompt-card {
  align-items: center;
  background: #f8fafc;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  display: grid;
  gap: 16px;
  grid-template-columns: 1fr auto;
  margin-bottom: 14px;
  padding: 16px;
}

.prompt-card strong {
  display: block;
  font-size: 20px;
  line-height: 1.5;
  margin-top: 6px;
}

.prompt-card audio {
  max-width: 230px;
}

.mode-tabs {
  border-bottom: 1px solid #dbe3ee;
  margin-bottom: 18px;
}

.mode-tabs :deep(.v-tab) {
  border-radius: 0;
  letter-spacing: 0;
}

.order-practice {
  display: grid;
  gap: 14px;
}

.selected-zone,
.word-options {
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-height: 82px;
  padding: 12px;
}

.selected-zone {
  background: #f8fbff;
}

.selected-zone span {
  align-self: center;
  color: #94a3b8;
}

.selected-zone button,
.word-options button {
  border-radius: 4px;
  cursor: pointer;
  font-size: 18px;
  min-height: 42px;
  min-width: 42px;
  padding: 8px 12px;
}

.selected-zone button {
  background: #dbeafe;
  border: 1px solid #93c5fd;
  color: #1e3a8a;
}

.word-options button {
  background: #ffffff;
  border: 1px solid #cbd5e1;
  color: #0f172a;
}

.word-options button:hover {
  background: #eef4ff;
  border-color: #2563eb;
}

.shadow-box {
  background: #f8fafc;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  padding: 18px;
}

.shadow-box span {
  color: #2563eb;
  display: block;
  font-size: 16px;
  margin-bottom: 8px;
}

.shadow-box strong {
  display: block;
  font-size: 34px;
  line-height: 1.35;
}

.shadow-box p {
  color: #64748b;
  line-height: 1.7;
  margin: 12px 0 0;
}

.result-box {
  background: #fff7ed;
  border: 1px solid #fdba74;
  border-radius: 8px;
  color: #9a3412;
  display: grid;
  gap: 6px;
  margin-top: 14px;
  padding: 12px;
}

.result-box.ok {
  background: #ecfdf5;
  border-color: #86efac;
  color: #166534;
}

.actions-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}

.actions-row :deep(.v-btn) {
  border-radius: 4px;
  letter-spacing: 0;
}

.panel-title {
  font-size: 18px;
  font-weight: 800;
  margin-bottom: 12px;
}

.answer-line {
  background: #f8fafc;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  padding: 14px;
}

.answer-line strong {
  display: block;
  font-size: 24px;
  line-height: 1.35;
}

.answer-line span {
  color: #2563eb;
  display: block;
  margin-top: 6px;
}

.answer-line p {
  color: #475569;
  line-height: 1.7;
  margin: 8px 0 0;
}

.vocab-grid {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 12px;
}

.vocab-card {
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  padding: 12px;
}

.vocab-card strong {
  display: block;
  font-size: 18px;
}

.vocab-card span {
  color: #2563eb;
  display: block;
  margin-top: 4px;
}

.vocab-card p,
.vocab-card small {
  color: #64748b;
  display: block;
  line-height: 1.6;
  margin: 6px 0 0;
}

.empty-state {
  background: #ffffff;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  color: #64748b;
  padding: 28px;
  text-align: center;
}

.empty-state.compact {
  background: #f8fafc;
  padding: 18px;
}

@media (max-width: 860px) {
  .material-grid,
  .dialogue-workspace,
  .prompt-card,
  .vocab-grid {
    grid-template-columns: 1fr;
  }

  .prompt-card audio {
    max-width: 100%;
    width: 100%;
  }
}
</style>
