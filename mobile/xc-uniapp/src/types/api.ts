export interface ApiResponse<T> {
  success: boolean
  code: string
  message: string
  data: T
  traceId: string
  timestamp: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  pageSize: number
}

export interface UserProfile {
  id: number
  email: string
  nickname: string | null
  status: string
  trialStartedAt: string | null
  trialEndsAt: string | null
}

export interface AuthToken<TProfile> {
  tokenType: string
  accessToken: string
  profile: TProfile
}

export interface UserPreference {
  uiLanguage: 'zh' | 'en' | 'ru'
  translationLanguage: 'ru' | 'en'
  vocabMeaningLanguage: 'ru' | 'en'
  matchingMeaningLanguage: 'ru' | 'en'
  soundEnabled: boolean
}

export type UpdateUserPreferencePayload = Partial<UserPreference>

export interface MembershipStatus {
  accessLevel: 'trial' | 'member' | 'free'
  fullAccess: boolean
  trialEndsAt: string | null
  membershipEndsAt: string | null
  remainingSeconds: number
}

export interface MembershipPlan {
  id: number
  name: string
  durationDays: number
  durationUnit: 'day' | 'month' | 'custom'
  durationValue: number
  price: number
  currency: string
}

export type PaymentProvider = 'wechat_pay' | 'alipay'
export type PaymentOrderStatus = 'pending' | 'paid' | 'failed' | 'refunded'

export interface PaymentOrder {
  id: number
  orderNo: string
  planId: number
  planName: string | null
  amount: number
  currency: string
  provider: PaymentProvider
  clientType: string
  paymentUrl: string | null
  providerTradeNo: string | null
  status: PaymentOrderStatus
  paidAt: string | null
  createdAt: string
  updatedAt: string
  mockPayment: boolean
}

export interface CreatePaymentOrderPayload {
  planId: number
  provider: PaymentProvider
  clientType: 'mobile'
}

export interface LearningSummary {
  totalStudySeconds: number
  totalExerciseCount: number
  totalCorrectCount: number
  totalVocabReviewCount: number
  currentStreakDays: number
  longestStreakDays: number
  overallAccuracyRate: number
  lastStudyDate: string | null
}

export interface VocabList {
  id: number
  name: string
  listType: string
  level: string | null
  description: string | null
  sortOrder: number
}

export type LearningProgressStatus = 'learning' | 'learned' | 'reviewing' | 'mastered'
export type VocabItemProgressStatus = LearningProgressStatus
export type SentenceProgressStatus = LearningProgressStatus

export interface VocabItem {
  id: number
  vocabListId: number
  hanzi: string
  pinyin: string | null
  meaningEn: string | null
  meaningRu: string | null
  exampleSentence: string | null
  audioAssetId: number | null
  audioUrl: string | null
  sortOrder: number
  favorite: boolean
  progressStatus: VocabItemProgressStatus | null
  reviewCount: number
  learnedAt: string | null
  lastReviewedAt: string | null
  nextReviewAt: string | null
}

export interface VocabProgress {
  vocabListId: number
  currentIndex: number
  lastVocabItemId: number | null
  reviewedCount: number
  learnedCount: number
  reviewingCount: number
  masteredCount: number
  totalCount: number
}

export interface FavoriteStatus {
  vocabItemId: number
  favorite: boolean
}

export type MatchingSourceType = 'vocab_list' | 'favorites'
export type MatchingGameType = 'matching' | 'elimination'
export type MatchingDifficulty = string
export type MatchingStatus = 'playing' | 'completed' | 'abandoned' | 'failed'

export interface MatchingStage {
  code: MatchingDifficulty
  labels: Record<'zh' | 'en' | 'ru', string>
  pairCount: number
  cardCount: number
  timeLimitSeconds: number
  enabled: boolean
  sortOrder: number
  unlocked: boolean
  completed: boolean
  bestElapsedSeconds: number | null
}

export interface MatchingStageGroup {
  code: string
  labels: Record<'zh' | 'en' | 'ru', string>
  enabled: boolean
  sortOrder: number
  levels: MatchingStage[]
}

export interface MatchingGameCard {
  vocabItemId: number
  hanzi: string
  pinyin: string | null
  meaning: string
}

export interface MatchingGameSession {
  id: number
  gameType: MatchingGameType
  sourceType: MatchingSourceType
  vocabListId: number | null
  meaningLanguage: 'ru' | 'en'
  difficulty: MatchingDifficulty
  totalPairs: number
  matchedPairs: number
  wrongCount: number
  elapsedSeconds: number
  timeLimitSeconds: number | null
  status: MatchingStatus
  createdAt: string
  completedAt: string | null
  cards: MatchingGameCard[]
}

export interface ExerciseSet {
  id: number
  title: string
  exerciseType: string
  level: string | null
}

export interface SentenceWordOption {
  id: number
  wordText: string
}

export interface SentenceExercise {
  id: number
  exerciseSetId: number
  exerciseType: string
  pinyinPrompt: string | null
  translationEn: string | null
  translationRu: string | null
  audioZhAssetId: number | null
  audioUrl: string | null
  sortOrder: number
  wordOptions: SentenceWordOption[]
  progressStatus: SentenceProgressStatus | null
  attemptCount: number
  correctCount: number
  learnedAt: string | null
  lastPracticedAt: string | null
  lastCorrectAt: string | null
  nextReviewAt: string | null
}

export interface ExerciseCheckResult {
  attemptId: number
  exerciseId: number
  correct: boolean
  answerText: string
  standardAnswer: string
  firstMismatchIndex: number | null
  message: string
  progressStatus: SentenceProgressStatus
  attemptCount: number
  correctCount: number
  learnedAt: string | null
  lastPracticedAt: string | null
  lastCorrectAt: string | null
  nextReviewAt: string | null
}

export interface ExerciseAnswer {
  exerciseId: number
  hanziAnswer: string
  explanation: string | null
  translationEn: string | null
  translationRu: string | null
  audioUrl: string | null
}

export type VideoMaterialType = 'drama' | 'short_video' | 'cartoon'

export interface VideoMaterial {
  id: number
  title: string
  materialType: VideoMaterialType
  description: string | null
  coverAssetId: number | null
  coverUrl: string | null
  lineCount: number
}

export interface DialogueLine {
  id: number
  materialId: number
  lineNo: number
  hanziText: string
  pinyinText: string | null
  translationEn: string | null
  translationRu: string | null
  audioAssetId: number | null
  audioUrl: string | null
  startMs: number | null
  endMs: number | null
  wordOptions: string[]
}

export interface DialogueLineVocab {
  id: number
  vocabItemId: number | null
  wordText: string
  pinyin: string | null
  meaningEn: string | null
  meaningRu: string | null
  explanation: string | null
}

export interface DialogueLineAnalysis {
  lineId: number
  hanziText: string
  pinyinText: string | null
  translationEn: string | null
  translationRu: string | null
  vocabItems: DialogueLineVocab[]
}

export interface DialogueLineCheckResult {
  eventId: number
  lineId: number
  correct: boolean
  answerText: string
  standardAnswer: string
  firstMismatchIndex: number | null
  message: string
}

export interface SpeechRecord {
  id: number
  dialogueLineId: number
  audioAssetId: number
  audioUrl: string | null
  asrJobId: number | null
  asrStatus: 'pending' | 'processing' | 'succeeded' | 'failed' | null
  recognizedText: string | null
  compareResult: string | null
  score: number | null
  errorMessage: string | null
  createdAt: string
  updatedAt: string
}

export interface ClassRoom {
  id: number
  name: string
  description: string | null
  inviteCode: string
  teacherName: string | null
  teacherContact: string | null
  status: string
  memberRole: string
  memberStatus: string
}

export interface ClassRoomDetail extends ClassRoom {
  activeMemberCount: number
  pendingMemberCount: number
}

export interface ClassMember {
  id: number
  classId: number
  userId: number
  email: string | null
  nickname: string | null
  memberRole: string
  status: string
  invitedByUserId: number | null
  reviewedByUserId: number | null
  reviewedAt: string | null
  joinedAt: string | null
  removedAt: string | null
}

export interface ClassMemberStats {
  userId: number
  email: string | null
  nickname: string | null
  memberRole: string
  studySeconds: number
  exerciseCount: number
  correctCount: number
  vocabReviewCount: number
  dialogueCount: number
  matchingGameCount: number
  accuracyRate: number
  lastStudyAt: string | null
}

export interface StudyEvent {
  id: number
  eventType: string
  targetId: number | null
  result: string | null
  durationSeconds: number
  occurredAt: string
}

export interface DailyStat {
  statDate: string
  studySeconds: number
  exerciseCount: number
  correctCount: number
  vocabReviewCount: number
  dialogueCount: number
  matchingGameCount: number
  accuracyRate: number
}
