export interface ApiResponse<T> {
  success: boolean
  code: string
  message: string
  data: T
  traceId: string
  timestamp: string
}

export interface AdminProfile {
  id: number
  username: string
  displayName: string | null
  status: string
  roles: string[]
  permissions: string[]
}

export interface AuthToken<TProfile> {
  tokenType: string
  accessToken: string
  profile: TProfile
}

export interface AdminDashboardSummary {
  userCount: number
  activeUserCount: number
  disabledUserCount: number
  trialUserCount: number
  todayNewUserCount: number
  activeMembershipCount: number
  activePlanCount: number
  todayOrderCount: number
  pendingOrderCount: number
  paidOrderCount: number
  todayPaidAmount: number
  classCount: number
  classMemberCount: number
  pendingClassMemberCount: number
  todayActiveClassCount: number
  todayStudyEventCount: number
  vocabListCount: number
  inactiveVocabListCount: number
  vocabItemCount: number
  exerciseSetCount: number
  inactiveExerciseSetCount: number
  sentenceExerciseCount: number
  videoMaterialCount: number
  inactiveVideoMaterialCount: number
  dialogueLineCount: number
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  pageSize: number
}

export type UserStatus = 'active' | 'disabled' | 'deleted'

export type AdminAccessLevel = 'member' | 'trial' | 'free'

export interface AdminLearningSummary {
  totalStudySeconds: number
  totalExerciseCount: number
  totalCorrectCount: number
  totalVocabReviewCount: number
  currentStreakDays: number
  longestStreakDays: number
  overallAccuracyRate: number
  lastStudyDate: string | null
}

export interface AdminMembershipRecord {
  id: number
  planId: number | null
  planName: string | null
  startedAt: string
  endsAt: string
  source: string
  adjustedByAdminId: number | null
  adjustReason: string | null
  status: string
  createdAt: string
}

export interface AdminUserListItem {
  id: number
  email: string
  nickname: string | null
  status: UserStatus
  accessLevel: AdminAccessLevel
  fullAccess: boolean
  trialEndsAt: string | null
  membershipEndsAt: string | null
  totalStudySeconds: number
  totalExerciseCount: number
  overallAccuracyRate: number
  currentStreakDays: number
  createdAt: string
  lastLoginAt: string | null
}

export interface AdminUserDetail {
  id: number
  email: string
  nickname: string | null
  status: UserStatus
  accessLevel: AdminAccessLevel
  fullAccess: boolean
  trialStartedAt: string | null
  trialEndsAt: string | null
  membershipEndsAt: string | null
  lastLoginAt: string | null
  createdAt: string
  learningSummary: AdminLearningSummary
  memberships: AdminMembershipRecord[]
}

export interface AdminUserQuery {
  page: number
  pageSize: number
  keyword?: string
  status?: UserStatus | ''
}

export interface AdminUpdateUserStatusPayload {
  status: Extract<UserStatus, 'active' | 'disabled'>
  reason?: string
}

export interface AdminAdjustMembershipPayload {
  action: 'grant' | 'cancel'
  startedAt?: string
  endsAt?: string
  reason: string
}

export type MembershipPlanStatus = 'active' | 'inactive'

export type MembershipDurationUnit = 'day' | 'month' | 'custom'

export interface AdminMembershipPlan {
  id: number
  name: string
  durationDays: number
  durationUnit: MembershipDurationUnit
  durationValue: number
  price: number
  currency: string
  status: MembershipPlanStatus
  createdAt: string
  updatedAt: string
}

export interface AdminMembershipPlanQuery {
  page: number
  pageSize: number
  keyword?: string
  status?: MembershipPlanStatus | ''
}

export interface AdminMembershipPlanPayload {
  name: string
  durationUnit: MembershipDurationUnit
  durationValue: number
  price: number
  currency: string
  status?: MembershipPlanStatus
}

export interface AdminUpdateMembershipPlanStatusPayload {
  status: MembershipPlanStatus
  reason?: string
}

export type PaymentOrderStatus = 'pending' | 'paid' | 'failed' | 'refunded'

export type PaymentProvider = 'wechat_pay' | 'alipay'

export type PaymentClientType = 'web' | 'mobile' | 'admin'

export interface AdminPaymentOrder {
  id: number
  orderNo: string
  userId: number
  userEmail: string | null
  userNickname: string | null
  planId: number
  planName: string | null
  amount: number
  currency: string
  provider: PaymentProvider
  clientType: PaymentClientType
  paymentUrl: string | null
  providerTradeNo: string | null
  status: PaymentOrderStatus
  paidAt: string | null
  createdAt: string
  updatedAt: string
}

export interface AdminPaymentOrderQuery {
  page: number
  pageSize: number
  keyword?: string
  status?: PaymentOrderStatus | ''
  provider?: PaymentProvider | ''
  clientType?: PaymentClientType | ''
  createdFrom?: string
  createdTo?: string
}

export type ClassRoomStatus = 'active' | 'archived' | 'deleted'

export type ClassMemberRole = 'teacher' | 'member'

export type ClassMemberStatus = 'invited' | 'pending_teacher_review' | 'active' | 'rejected' | 'left' | 'removed'

export interface AdminClassMemberStats {
  userId: number
  email: string | null
  nickname: string | null
  memberRole: ClassMemberRole
  studySeconds: number
  exerciseCount: number
  correctCount: number
  vocabReviewCount: number
  dialogueCount: number
  matchingGameCount: number
  accuracyRate: number
  lastStudyAt: string | null
}

export interface AdminClassMember {
  id: number
  classId: number
  userId: number
  email: string | null
  nickname: string | null
  userStatus: UserStatus | null
  memberRole: ClassMemberRole
  status: ClassMemberStatus
  invitedByUserId: number | null
  reviewedByUserId: number | null
  reviewedAt: string | null
  joinedAt: string | null
  removedAt: string | null
  createdAt: string
}

export interface AdminClassRoomListItem {
  id: number
  name: string
  description: string | null
  inviteCode: string | null
  status: ClassRoomStatus
  ownerUserId: number
  ownerEmail: string | null
  ownerNickname: string | null
  activeMemberCount: number
  pendingMemberCount: number
  totalStudySeconds: number
  totalExerciseCount: number
  totalCorrectCount: number
  accuracyRate: number
  lastStudyAt: string | null
  createdAt: string
  updatedAt: string
}

export interface AdminClassRoomDetail extends AdminClassRoomListItem {
  removedMemberCount: number
  members: AdminClassMember[]
  stats: AdminClassMemberStats[]
}

export interface AdminClassRoomQuery {
  page: number
  pageSize: number
  keyword?: string
  status?: ClassRoomStatus | ''
}

export interface AdminUpdateClassRoomStatusPayload {
  status: Extract<ClassRoomStatus, 'active' | 'archived'>
  reason?: string
}

export interface AdminRemoveClassMemberPayload {
  reason?: string
}

export type VocabListType = 'HSK' | 'YCT' | 'category' | 'professional' | 'custom'

export type ContentStatus = 'active' | 'inactive'

export interface AdminVocabList {
  id: number
  name: string
  listType: VocabListType
  level: string | null
  description: string | null
  sortOrder: number
  status: ContentStatus
  activeItemCount: number
  inactiveItemCount: number
  createdAt: string
  updatedAt: string
}

export interface AdminVocabListQuery {
  page: number
  pageSize: number
  keyword?: string
  listType?: VocabListType | ''
  level?: string
  status?: ContentStatus | ''
}

export interface AdminVocabListPayload {
  name: string
  listType: VocabListType
  level?: string | null
  description?: string | null
  sortOrder: number
  status?: ContentStatus
}

export interface AdminVocabItem {
  id: number
  vocabListId: number
  vocabListName: string | null
  listType: VocabListType | null
  level: string | null
  hanzi: string
  pinyin: string | null
  meaningEn: string | null
  meaningRu: string | null
  exampleSentence: string | null
  audioAssetId: number | null
  audioUrl: string | null
  sortOrder: number
  status: ContentStatus
  createdAt: string
  updatedAt: string
}

export interface AdminVocabItemQuery {
  page: number
  pageSize: number
  vocabListId?: number | null
  keyword?: string
  status?: ContentStatus | ''
}

export interface AdminVocabItemPayload {
  vocabListId: number
  hanzi: string
  pinyin?: string | null
  meaningEn?: string | null
  meaningRu?: string | null
  exampleSentence?: string | null
  audioAssetId?: number | null
  sortOrder: number
  status?: ContentStatus
}

export interface AdminUpdateContentStatusPayload {
  status: ContentStatus
  reason?: string
}

export type MediaType = 'audio' | 'image' | 'video'

export type MediaLanguage = 'zh' | 'ru' | 'en'

export interface AdminMediaAsset {
  id: number
  mediaType: MediaType
  url: string
  language: MediaLanguage | null
  durationMs: number | null
  fileSize: number | null
  createdAt: string
  updatedAt: string
}

export interface AdminMediaAssetQuery {
  page: number
  pageSize: number
  keyword?: string
  mediaType?: MediaType | ''
  language?: MediaLanguage | ''
}

export type ExerciseType = 'audio_order' | 'audio_dictation' | 'pinyin_dictation' | 'translation_order'

export interface AdminExerciseSet {
  id: number
  title: string
  exerciseType: ExerciseType
  level: string | null
  status: ContentStatus
  activeExerciseCount: number
  inactiveExerciseCount: number
  createdAt: string
  updatedAt: string
}

export interface AdminExerciseSetQuery {
  page: number
  pageSize: number
  keyword?: string
  exerciseType?: ExerciseType | ''
  level?: string
  status?: ContentStatus | ''
}

export interface AdminExerciseSetPayload {
  title: string
  exerciseType: ExerciseType
  level?: string | null
  status?: ContentStatus
}

export interface AdminSentenceWordOption {
  id: number
  exerciseId: number
  wordText: string
  correctOrder: number
  createdAt: string
  updatedAt: string
}

export interface AdminSentenceWordOptionPayload {
  wordText: string
  correctOrder: number
}

export interface AdminSentenceExercise {
  id: number
  exerciseSetId: number
  exerciseSetTitle: string | null
  exerciseType: ExerciseType
  hanziAnswer: string
  pinyinPrompt: string | null
  translationEn: string | null
  translationRu: string | null
  audioZhAssetId: number | null
  audioUrl: string | null
  explanation: string | null
  sortOrder: number
  status: ContentStatus
  createdAt: string
  updatedAt: string
  wordOptions: AdminSentenceWordOption[]
}

export interface AdminSentenceExerciseQuery {
  page: number
  pageSize: number
  exerciseSetId?: number | null
  keyword?: string
  exerciseType?: ExerciseType | ''
  status?: ContentStatus | ''
}

export interface AdminSentenceExercisePayload {
  exerciseSetId: number
  exerciseType: ExerciseType
  hanziAnswer: string
  pinyinPrompt?: string | null
  translationEn?: string | null
  translationRu?: string | null
  audioZhAssetId?: number | null
  explanation?: string | null
  sortOrder: number
  status?: ContentStatus
  wordOptions?: AdminSentenceWordOptionPayload[]
}

export type VideoMaterialType = 'drama' | 'short_video' | 'cartoon'

export interface AdminVideoMaterial {
  id: number
  title: string
  materialType: VideoMaterialType
  description: string | null
  coverAssetId: number | null
  coverUrl: string | null
  status: ContentStatus
  lineCount: number
  createdAt: string
  updatedAt: string
}

export interface AdminVideoMaterialQuery {
  page: number
  pageSize: number
  keyword?: string
  materialType?: VideoMaterialType | ''
  status?: ContentStatus | ''
}

export interface AdminVideoMaterialPayload {
  title: string
  materialType: VideoMaterialType
  description?: string | null
  coverAssetId?: number | null
  status?: ContentStatus
}

export interface AdminDialogueLine {
  id: number
  materialId: number
  materialTitle: string | null
  lineNo: number
  hanziText: string
  pinyinText: string | null
  translationEn: string | null
  translationRu: string | null
  audioAssetId: number | null
  audioUrl: string | null
  startMs: number | null
  endMs: number | null
  createdAt: string
  updatedAt: string
}

export interface AdminDialogueLineQuery {
  page: number
  pageSize: number
  materialId?: number | null
  keyword?: string
}

export interface AdminDialogueLinePayload {
  materialId: number
  lineNo: number
  hanziText: string
  pinyinText?: string | null
  translationEn?: string | null
  translationRu?: string | null
  audioAssetId?: number | null
  startMs?: number | null
  endMs?: number | null
}

export interface AdminDialogueLineVocab {
  id: number
  dialogueLineId: number
  materialId: number | null
  materialTitle: string | null
  lineNo: number | null
  lineHanziText: string | null
  vocabItemId: number | null
  vocabItemHanzi: string | null
  wordText: string
  pinyin: string | null
  meaningEn: string | null
  meaningRu: string | null
  explanation: string | null
  createdAt: string
  updatedAt: string
}

export interface AdminDialogueLineVocabQuery {
  page: number
  pageSize: number
  dialogueLineId?: number | null
  materialId?: number | null
  keyword?: string
}

export interface AdminDialogueLineVocabPayload {
  dialogueLineId: number
  vocabItemId?: number | null
  wordText: string
  pinyin?: string | null
  meaningEn?: string | null
  meaningRu?: string | null
  explanation?: string | null
}

export interface AdminPermission {
  id: number
  permissionCode: string
  permissionName: string
  moduleName: string
}

export interface AdminRole {
  id: number
  roleCode: string
  roleName: string
  description: string | null
  permissions: AdminPermission[]
  createdAt: string
  updatedAt: string
}

export interface AdminUpdateRolePermissionsPayload {
  permissionIds: number[]
}

export interface AdminRolePayload {
  roleCode: string
  roleName: string
  description?: string | null
}

export interface AdminAccount {
  id: number
  username: string
  displayName: string | null
  status: 'active' | 'disabled'
  lastLoginAt: string | null
  roles: AdminRole[]
  createdAt: string
  updatedAt: string
}

export interface AdminAccountQuery {
  page: number
  pageSize: number
  keyword?: string
  status?: 'active' | 'disabled' | ''
}

export interface AdminCreateAccountPayload {
  username: string
  displayName?: string | null
  password: string
  status: 'active' | 'disabled'
  roleIds: number[]
}

export interface AdminUpdateAccountPayload {
  displayName?: string | null
  status: 'active' | 'disabled'
}

export interface AdminUpdateAccountRolesPayload {
  roleIds: number[]
}

export interface AdminResetAccountPasswordPayload {
  password: string
}

export type SystemConfigGroup = 'payment' | 'asr' | 'membership' | 'upload'

export interface AdminSystemConfig {
  id: number
  configKey: string
  configValue: string | null
  configGroup: SystemConfigGroup
  description: string | null
  updatedBy: number | null
  createdAt: string
  updatedAt: string
}

export interface AdminSystemConfigQuery {
  page: number
  pageSize: number
  configGroup?: SystemConfigGroup | ''
  keyword?: string
}

export interface AdminUpdateSystemConfigPayload {
  configValue?: string | null
  description?: string | null
}

export interface AdminOperationLog {
  id: number
  adminUserId: number | null
  action: string
  targetType: string
  targetId: number | null
  detail: string | null
  ipAddress: string | null
  createdAt: string
  updatedAt: string
}

export interface AdminOperationLogQuery {
  page: number
  pageSize: number
  adminUserId?: number | null
  action?: string
  targetType?: string
  targetId?: number | null
  createdFrom?: string
  createdTo?: string
  keyword?: string
}

export interface AdminLearningReportSummary {
  dateFrom: string
  dateTo: string
  activeUserCount: number
  studySeconds: number
  exerciseCount: number
  correctCount: number
  vocabReviewCount: number
  dialogueCount: number
  matchingGameCount: number
  accuracyRate: number
}

export interface AdminDailyLearningReport {
  statDate: string
  activeUserCount: number
  studySeconds: number
  exerciseCount: number
  correctCount: number
  vocabReviewCount: number
  dialogueCount: number
  matchingGameCount: number
  accuracyRate: number
}

export interface AdminUserLearningReport {
  userId: number
  email: string | null
  nickname: string | null
  status: string | null
  studySeconds: number
  exerciseCount: number
  correctCount: number
  vocabReviewCount: number
  dialogueCount: number
  matchingGameCount: number
  accuracyRate: number
  lastStudyDate: string | null
}

export interface AdminLearningReport {
  summary: AdminLearningReportSummary
  dailyStats: AdminDailyLearningReport[]
  users: PageResult<AdminUserLearningReport>
}

export interface AdminLearningReportQuery {
  page: number
  pageSize: number
  dateFrom?: string
  dateTo?: string
  keyword?: string
}

export type LeaderboardPeriodType = 'daily' | 'weekly' | 'monthly' | 'all'

export type LeaderboardMetricType = 'streak' | 'accuracy' | 'vocab_count' | 'game_score'

export interface AdminLeaderboardEntry {
  id: number
  periodType: LeaderboardPeriodType
  periodStart: string
  metricType: LeaderboardMetricType
  userId: number
  email: string | null
  nickname: string | null
  scoreValue: number
  rankNo: number
  generatedAt: string
}

export interface AdminLeaderboardQuery {
  page: number
  pageSize: number
  periodType?: LeaderboardPeriodType | ''
  periodStart?: string
  metricType?: LeaderboardMetricType | ''
}
