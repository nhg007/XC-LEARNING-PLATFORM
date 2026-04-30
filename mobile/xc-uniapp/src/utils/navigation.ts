import { getToken } from './storage'

export const routes = {
  home: '/pages/index/index',
  login: '/pages/login/login',
  features: '/pages/features/index',
  more: '/pages/more/index',
  vocab: '/pages/vocab/index',
  vocabFavorites: '/pages/vocab/favorites',
  vocabStudy: '/pages/vocab/study',
  practice: '/pages/practice/index',
  dialogue: '/pages/dialogue/index',
  matching: '/pages/matching/index',
  elimination: '/pages/elimination/index',
  membership: '/pages/membership/index',
  profile: '/pages/profile/index',
  classroom: '/pages/classroom/index',
  classroomDetail: '/pages/classroom/detail',
  records: '/pages/records/index'
} as const

const tabRoutes = new Set<string>([
  routes.home,
  routes.features,
  routes.more,
  routes.profile
])

export function openPage(url: string) {
  if (tabRoutes.has(url)) {
    return uni.switchTab({ url })
  }
  return uni.navigateTo({ url })
}

export function redirectToLogin() {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  if (currentPage?.route === routes.login.slice(1)) {
    return Promise.resolve()
  }
  return uni.redirectTo({ url: routes.login })
}

export function requireLogin() {
  if (getToken()) {
    return true
  }
  void redirectToLogin()
  return false
}

export function openVocabStudy(listId: number) {
  return uni.navigateTo({ url: `${routes.vocabStudy}?listId=${listId}` })
}

export function openClassroomDetail(classId: number) {
  return uni.navigateTo({ url: `${routes.classroomDetail}?id=${classId}` })
}
