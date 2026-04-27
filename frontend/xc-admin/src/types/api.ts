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
  activeMembershipCount: number
  classCount: number
  todayStudyEventCount: number
  vocabListCount: number
  exerciseSetCount: number
}
