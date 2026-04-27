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

export interface MembershipStatus {
  accessLevel: 'trial' | 'member' | 'free'
  fullAccess: boolean
  trialEndsAt: string | null
  membershipEndsAt: string | null
  remainingSeconds: number
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

export interface VocabItem {
  id: number
  vocabListId: number
  hanzi: string
  pinyin: string | null
  meaningEn: string | null
  meaningRu: string | null
  exampleSentence: string | null
  audioAssetId: number | null
  sortOrder: number
  favorite: boolean
}

export interface VocabProgress {
  vocabListId: number
  currentIndex: number
  lastVocabItemId: number | null
  reviewedCount: number
  totalCount: number
}

export interface FavoriteStatus {
  vocabItemId: number
  favorite: boolean
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
  sortOrder: number
  wordOptions: SentenceWordOption[]
}

export interface ExerciseCheckResult {
  attemptId: number
  exerciseId: number
  correct: boolean
  answerText: string
  standardAnswer: string
  firstMismatchIndex: number | null
  message: string
}

export interface ExerciseAnswer {
  exerciseId: number
  hanziAnswer: string
  explanation: string | null
  translationEn: string | null
  translationRu: string | null
}

export interface ClassRoom {
  id: number
  name: string
  description: string | null
  inviteCode: string
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
