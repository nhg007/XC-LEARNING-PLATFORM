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

export type AdminSortDirection = 'asc' | 'desc'

export interface AdminTableSortQuery {
  sortBy?: string
  sortDirection?: AdminSortDirection | ''
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

export interface AdminUserQuery extends AdminTableSortQuery {
  page: number
  pageSize: number
  keyword?: string
  status?: UserStatus | ''
  accessLevel?: AdminAccessLevel | ''
  createdFrom?: string
  createdTo?: string
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

export interface AdminMembershipPlanQuery extends AdminTableSortQuery {
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

export type PaymentProvider = 'wechat_pay' | 'alipay' | 'offline'

export type PaymentClientType = 'web' | 'mobile' | 'admin'

export type PaymentOrderExceptionType =
  | 'all'
  | 'pending_timeout'
  | 'callback_failed'
  | 'amount_mismatch'
  | 'provider_mismatch'
  | 'membership_missing'

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
  exceptionTypes: PaymentOrderExceptionType[]
  latestNotificationProcessStatus: PaymentNotificationProcessStatus | null
  latestNotificationResultCode: string | null
  latestNotificationResultMessage: string | null
  latestNotificationReceivedAt: string | null
  paidAt: string | null
  createdAt: string
  updatedAt: string
}

export interface AdminPaymentOrderQuery extends AdminTableSortQuery {
  page: number
  pageSize: number
  keyword?: string
  status?: PaymentOrderStatus | ''
  provider?: PaymentProvider | ''
  clientType?: PaymentClientType | ''
  exceptionType?: PaymentOrderExceptionType | ''
  pendingTimeoutMinutes?: number
  createdFrom?: string
  createdTo?: string
}

export interface AdminOrderExceptionSummary {
  allExceptions: number
  pendingTimeout: number
  callbackFailed: number
  amountMismatch: number
  providerMismatch: number
  membershipMissing: number
}

export interface AdminFailPaymentOrderPayload {
  reason: string
}

export interface AdminCreateOfflinePaymentOrderPayload {
  userKeyword: string
  planId: number
  amount?: number
  currency?: string
  paidAt?: string
  offlineTradeNo?: string
  remark: string
}

export type PaymentNotificationProcessStatus = 'handled' | 'ignored' | 'failed'

export interface AdminPaymentNotification {
  id: number
  orderId: number | null
  orderNo: string | null
  userId: number | null
  userEmail: string | null
  userNickname: string | null
  planId: number | null
  planName: string | null
  orderAmount: number | null
  currency: string | null
  orderStatus: PaymentOrderStatus | null
  provider: PaymentProvider
  providerTradeNo: string | null
  signatureValid: boolean | null
  handled: boolean | null
  processStatus: PaymentNotificationProcessStatus
  resultCode: string | null
  resultMessage: string | null
  notifyPayload: string | null
  receivedAt: string
  createdAt: string
  updatedAt: string
}

export interface AdminPaymentNotificationQuery extends AdminTableSortQuery {
  page: number
  pageSize: number
  keyword?: string
  provider?: PaymentProvider | ''
  processStatus?: PaymentNotificationProcessStatus | ''
  resultCode?: string
  signatureValid?: boolean | ''
  orderId?: number | null
  receivedFrom?: string
  receivedTo?: string
}

export type VocabListType = 'HSK' | 'YCT' | 'category' | 'professional' | 'custom'

export type ContentStatus = 'active' | 'inactive'

export interface AdminVocabList {
  id: number
  name: string
  parentId: number | null
  parentName: string | null
  listType: VocabListType
  level: string | null
  description: string | null
  sortOrder: number
  status: ContentStatus
  childCount: number
  activeItemCount: number
  inactiveItemCount: number
  createdAt: string
  updatedAt: string
}

export interface AdminVocabListQuery extends AdminTableSortQuery {
  page: number
  pageSize: number
  keyword?: string
  parentId?: number | null
  rootOnly?: boolean
  listType?: VocabListType | ''
  level?: string
  status?: ContentStatus | ''
}

export interface AdminVocabListPayload {
  name: string
  parentId?: number | null
  listType: VocabListType
  level?: string | null
  description?: string | null
  sortOrder: number
  status?: ContentStatus
}

export interface VocabStrokeOrderAsset {
  id?: number | null
  mediaAssetId: number
  title?: string | null
  url?: string | null
  sortOrder: number
}

export interface AdminVocabItem {
  id: number
  vocabListId: number | null
  vocabListName: string | null
  vocabListIds?: number[]
  vocabListNames?: string[]
  listType: VocabListType | null
  level: string | null
  vocabListStatus: ContentStatus | null
  hanzi: string
  pinyin: string | null
  meaningEn: string | null
  meaningRu: string | null
  exampleSentence: string | null
  audioAssetId: number | null
  audioUrl: string | null
  strokeOrderAssetId: number | null
  strokeOrderUrl: string | null
  strokeOrderAssets?: VocabStrokeOrderAsset[]
  sortOrder: number
  status: ContentStatus
  createdAt: string
  updatedAt: string
}

export interface AdminVocabItemQuery extends AdminTableSortQuery {
  page: number
  pageSize: number
  vocabListId?: number | null
  keyword?: string
  status?: ContentStatus | ''
  hasAudio?: boolean | null
}

export interface AdminVocabItemPayload {
  vocabListId?: number | null
  vocabListIds?: number[]
  hanzi: string
  pinyin?: string | null
  meaningEn?: string | null
  meaningRu?: string | null
  exampleSentence?: string | null
  audioAssetId?: number | null
  strokeOrderAssetId?: number | null
  strokeOrderAssets?: Array<{
    mediaAssetId: number
    title?: string | null
    sortOrder?: number
  }>
  sortOrder: number
  status?: ContentStatus
}

export interface AdminUpdateContentStatusPayload {
  status: ContentStatus
  reason?: string
}

export interface AdminBatchUpdateContentStatusPayload {
  ids: number[]
  status: ContentStatus
  reason?: string
}

export interface AdminBatchUpdateContentAssignmentsPayload {
  ids: number[]
  targetIds: number[]
}

export interface AdminBatchContentStatusResult {
  requestedCount: number
  successCount: number
  errors: string[]
}

export type MediaType = 'audio' | 'image' | 'video'

export type MediaLanguage = 'zh' | 'ru' | 'en'

export interface AdminMediaAsset {
  id: number
  mediaType: MediaType
  originalFilename: string | null
  url: string
  language: MediaLanguage | null
  durationMs: number | null
  fileSize: number | null
  status: ContentStatus
  createdAt: string
  updatedAt: string
}

export interface AdminMediaAssetQuery extends AdminTableSortQuery {
  page: number
  pageSize: number
  keyword?: string
  mediaType?: MediaType | ''
  language?: MediaLanguage | ''
  status?: ContentStatus | ''
}

export interface AdminMediaAssetBinding {
  targetId: number
  mediaAssetId: number
}

export interface AdminBatchBindMediaAssetPayload {
  bindings: AdminMediaAssetBinding[]
}

export interface AdminBatchBindMediaAssetResult {
  requestedCount: number
  successCount: number
  errors: string[]
}

export type AdminContentImportType =
  | 'vocab-lists'
  | 'vocab-items'
  | 'exercise-sets'
  | 'sentence-exercises'
  | 'video-materials'
  | 'dialogue-lines'
  | 'dialogue-line-vocab'

export interface AdminContentImportResult {
  importType: AdminContentImportType
  requestedCount: number
  successCount: number
  errors: string[]
}

export interface AdminContentImportTemplate {
  filename: string
  content: string
}

export type ExerciseType = 'audio_order' | 'audio_dictation' | 'pinyin_dictation' | 'translation_order'

export interface AdminExerciseSet {
  id: number
  title: string
  parentId: number | null
  parentTitle: string | null
  exerciseType: ExerciseType
  level: string | null
  status: ContentStatus
  childCount: number
  activeExerciseCount: number
  inactiveExerciseCount: number
  createdAt: string
  updatedAt: string
}

export interface AdminExerciseSetQuery extends AdminTableSortQuery {
  page: number
  pageSize: number
  keyword?: string
  parentId?: number | null
  rootOnly?: boolean
  exerciseType?: ExerciseType | ''
  level?: string
  status?: ContentStatus | ''
}

export interface AdminExerciseSetPayload {
  title: string
  parentId?: number | null
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
  exerciseSetId: number | null
  exerciseSetTitle: string | null
  exerciseSetIds?: number[]
  exerciseSetTitles?: string[]
  exerciseSetStatus: ContentStatus | null
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

export interface AdminSentenceExerciseQuery extends AdminTableSortQuery {
  page: number
  pageSize: number
  exerciseSetId?: number | null
  keyword?: string
  exerciseType?: ExerciseType | ''
  status?: ContentStatus | ''
  hasAudio?: boolean | null
}

export interface AdminSentenceExercisePayload {
  exerciseSetId?: number | null
  exerciseSetIds?: number[]
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
  parentId: number | null
  parentTitle: string | null
  materialType: VideoMaterialType
  description: string | null
  coverAssetId: number | null
  coverUrl: string | null
  status: ContentStatus
  childCount: number
  lineCount: number
  createdAt: string
  updatedAt: string
}

export interface AdminVideoMaterialQuery extends AdminTableSortQuery {
  page: number
  pageSize: number
  keyword?: string
  parentId?: number | null
  rootOnly?: boolean
  materialType?: VideoMaterialType | ''
  status?: ContentStatus | ''
  hasCover?: boolean | null
}

export interface AdminVideoMaterialPayload {
  title: string
  parentId?: number | null
  materialType: VideoMaterialType
  description?: string | null
  coverAssetId?: number | null
  status?: ContentStatus
}

export interface AdminDialogueLine {
  id: number
  materialId: number
  materialTitle: string | null
  materialStatus: ContentStatus | null
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

export interface AdminDialogueLineQuery extends AdminTableSortQuery {
  page: number
  pageSize: number
  materialId?: number | null
  keyword?: string
  hasAudio?: boolean | null
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
  materialStatus: ContentStatus | null
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

export interface AdminDialogueLineVocabQuery extends AdminTableSortQuery {
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

export interface AdminAccountQuery extends AdminTableSortQuery {
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

export type SystemConfigGroup = 'payment' | 'asr' | 'membership' | 'upload' | 'learning'

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

export interface AdminSystemConfigQuery extends AdminTableSortQuery {
  page: number
  pageSize: number
  configGroup?: SystemConfigGroup | ''
  keyword?: string
}

export interface AdminUpdateSystemConfigPayload {
  configValue?: string | null
  description?: string | null
}

export interface AdminMatchingStage {
  code: string
  labels: Record<'zh' | 'en' | 'ru', string>
  pairCount: number
  cardCount: number
  timeLimitSeconds: number
  enabled: boolean
  sortOrder: number
  unlocked?: boolean
  completed?: boolean
  bestElapsedSeconds?: number | null
}

export interface AdminMatchingStageGroup {
  code: string
  labels: Record<'zh' | 'en' | 'ru', string>
  enabled: boolean
  sortOrder: number
  levels: AdminMatchingStage[]
}

export interface AdminUpdateMatchingStagesPayload {
  stages: Array<{
    code: string
    labels: Record<'zh' | 'en' | 'ru', string>
    enabled: boolean
    sortOrder: number
    levels: Array<{
      code: string
      labels: Record<'zh' | 'en' | 'ru', string>
      pairCount: number
      timeLimitSeconds: number
      enabled: boolean
      sortOrder: number
    }>
  }>
}

export interface AdminRuntimeSettings {
  membership: {
    trialDays: number
  }
  payment: {
    mockEnabled: boolean
    wechatPayEnabled: boolean
    wechatPayPaymentUrlPrefix: string
    wechatPayNotifySecretConfigured: boolean
    alipayEnabled: boolean
    alipayPaymentUrlPrefix: string
    alipayNotifySecretConfigured: boolean
  }
  asr: {
    provider: 'mock' | 'http'
    workerEnabled: boolean
    engineName: string
    serviceUrl: string
    servicePath: string
    timeoutMs: number
    batchSize: number
    initialDelayMs: number
    pollDelayMs: number
    mockRecognizedText: string
  }
  upload: {
    maxFileSize: string
    maxRequestSize: string
    audioExtensions: string
    imageExtensions: string
    videoExtensions: string
  }
}

export interface AdminUpdateRuntimeSettingsPayload {
  membership: {
    trialDays: number
  }
  payment: {
    mockEnabled: boolean
    wechatPayEnabled: boolean
    wechatPayPaymentUrlPrefix: string
    wechatPayNotifySecret?: string | null
    clearWechatPayNotifySecret?: boolean
    alipayEnabled: boolean
    alipayPaymentUrlPrefix: string
    alipayNotifySecret?: string | null
    clearAlipayNotifySecret?: boolean
  }
  asr: AdminRuntimeSettings['asr']
  upload: AdminRuntimeSettings['upload']
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

export interface AdminOperationLogQuery extends AdminTableSortQuery {
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

export interface AdminLearningReportQuery extends AdminTableSortQuery {
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

export interface AdminLeaderboardQuery extends AdminTableSortQuery {
  page: number
  pageSize: number
  periodType?: LeaderboardPeriodType | ''
  periodStart?: string
  metricType?: LeaderboardMetricType | ''
}
