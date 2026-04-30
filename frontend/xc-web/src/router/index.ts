import { createRouter, createWebHistory } from 'vue-router'
import { t } from '../plugins/i18n'
import { useSessionStore } from '../stores/session'

const HomeView = () => import('../views/home/HomeView.vue')
const LoginView = () => import('../views/login/LoginView.vue')
const PracticeView = () => import('../views/practice/PracticeView.vue')
const VocabStudyView = () => import('../views/vocab/VocabStudyView.vue')
const VocabFavoritesView = () => import('../views/vocab/VocabFavoritesView.vue')
const ClassroomsView = () => import('../views/classroom/ClassroomsView.vue')
const LearningRecordsView = () => import('../views/records/LearningRecordsView.vue')
const MembershipView = () => import('../views/membership/MembershipView.vue')
const MatchingGameView = () => import('../views/matching/MatchingGameView.vue')
const EliminationGameView = () => import('../views/matching/EliminationGameView.vue')
const DialoguePracticeView = () => import('../views/dialogue/DialoguePracticeView.vue')

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: HomeView, meta: { requiresAuth: true, title: 'app.title' } },
    { path: '/practice', component: PracticeView, meta: { requiresAuth: true, title: 'practice.title' } },
    { path: '/membership', component: MembershipView, meta: { requiresAuth: true, title: 'membership.title' } },
    { path: '/dialogue', component: DialoguePracticeView, meta: { requiresAuth: true, title: 'dialogue.title' } },
    { path: '/matching', component: MatchingGameView, meta: { requiresAuth: true, title: 'matching.title' } },
    { path: '/elimination', component: EliminationGameView, meta: { requiresAuth: true, title: 'elimination.title' } },
    { path: '/classrooms', component: ClassroomsView, meta: { requiresAuth: true, title: 'classroom.title' } },
    { path: '/records', component: LearningRecordsView, meta: { requiresAuth: true, title: 'records.title' } },
    { path: '/favorites', component: VocabFavoritesView, meta: { requiresAuth: true, title: 'vocab.favoritesTitle' } },
    { path: '/vocab/:listId', component: VocabStudyView, meta: { requiresAuth: true, title: 'vocab.title' } },
    { path: '/login', component: LoginView, meta: { title: 'login.title' } }
  ]
})

router.beforeEach((to) => {
  const session = useSessionStore()
  if (to.meta.requiresAuth && !session.isLoggedIn) {
    return '/login'
  }
  if (typeof to.meta.title === 'string') {
    document.title = `${t(to.meta.title)} | ${t('app.title')}`
  }
  return true
})

export default router
