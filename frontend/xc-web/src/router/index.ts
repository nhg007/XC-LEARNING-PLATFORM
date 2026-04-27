import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/home/HomeView.vue'
import LoginView from '../views/login/LoginView.vue'
import PracticeView from '../views/practice/PracticeView.vue'
import VocabStudyView from '../views/vocab/VocabStudyView.vue'
import ClassroomsView from '../views/classroom/ClassroomsView.vue'
import LearningRecordsView from '../views/records/LearningRecordsView.vue'
import { t } from '../plugins/i18n'
import { useSessionStore } from '../stores/session'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: HomeView, meta: { requiresAuth: true, title: 'app.title' } },
    { path: '/practice', component: PracticeView, meta: { requiresAuth: true, title: 'practice.title' } },
    { path: '/classrooms', component: ClassroomsView, meta: { requiresAuth: true, title: 'classroom.title' } },
    { path: '/records', component: LearningRecordsView, meta: { requiresAuth: true, title: 'records.title' } },
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
