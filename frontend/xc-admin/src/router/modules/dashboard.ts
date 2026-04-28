import type { RouteRecordRaw } from 'vue-router'

export default {
  path: '/dashboard',
  name: 'Dashboard',
  component: () => import('@/views/dashboard/DashboardView.vue'),
  meta: {
    title: 'menus.dashboard',
    icon: 'House',
    rank: 0,
    requiresAuth: true,
    roles: ['admin']
  }
} satisfies RouteRecordRaw
