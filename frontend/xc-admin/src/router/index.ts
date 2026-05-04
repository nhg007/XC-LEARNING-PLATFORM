import '@/router/types'
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { t } from '@/plugins/i18n'
import { hasRouteAccess } from '@/router/permission'
import { useSessionStore } from '@/stores/session'

const Layout = () => import('@/layout/index.vue')
const LoginView = () => import('@/views/login/LoginView.vue')
const Error403View = () => import('@/views/error/Error403View.vue')

const modules: Record<string, { default: RouteRecordRaw | RouteRecordRaw[] }> = import.meta.glob('./modules/**/*.ts', {
  eager: true
})

const moduleRoutes = Object.values(modules)
  .flatMap(module => Array.isArray(module.default) ? module.default : [module.default])
  .sort((a, b) => (a.meta?.rank ?? 999) - (b.meta?.rank ?? 999))

export const constantMenus = moduleRoutes.filter(route => route.meta?.showLink !== false)

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: moduleRoutes
  },
  {
    path: '/login',
    name: 'Login',
    component: LoginView,
    meta: {
      title: 'common.login'
    }
  },
  {
    path: '/error/403',
    name: 'Error403',
    component: Error403View,
    meta: {
      title: 'common.noPermission'
    }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior() {
    return { left: 0, top: 0 }
  }
})

router.beforeEach(async (to) => {
  const session = useSessionStore()

  if (to.path === '/login' && session.isLoggedIn) {
    return '/dashboard'
  }

  if (to.meta.requiresAuth && !session.isLoggedIn) {
    return {
      path: '/login',
      query: {
        redirect: to.fullPath
      }
    }
  }

  if (to.meta.requiresAuth && session.isLoggedIn && !session.profile) {
    try {
      await session.refreshProfile()
    } catch {
      session.logout()
      return '/login'
    }
  }

  if (to.meta.requiresAuth && !hasRouteAccess(to.meta, session.profile)) {
    return '/error/403'
  }

  if (to.meta.title) {
    document.title = `${t(to.meta.title)} | ${t('app.title')}`
  }

  return true
})

export default router
