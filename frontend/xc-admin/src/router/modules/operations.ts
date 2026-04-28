import type { RouteRecordRaw } from 'vue-router'

const moduleRoutes: RouteRecordRaw[] = [
  {
    path: '/users',
    name: 'Users',
    component: () => import('@/views/users/UserManagementView.vue'),
    meta: {
      title: 'menus.users',
      icon: 'User',
      rank: 10,
      requiresAuth: true,
      roles: ['admin'],
      permissions: ['admin:users:read']
    }
  },
  {
    path: '/memberships',
    name: 'Memberships',
    component: () => import('@/views/memberships/MembershipManagementView.vue'),
    meta: {
      title: 'menus.memberships',
      icon: 'Wallet',
      rank: 20,
      requiresAuth: true,
      roles: ['admin'],
      permissions: ['admin:memberships:read', 'admin:orders:read']
    }
  },
  {
    path: '/classrooms',
    name: 'Classrooms',
    component: () => import('@/views/classrooms/ClassroomManagementView.vue'),
    meta: {
      title: 'menus.classrooms',
      icon: 'School',
      rank: 30,
      requiresAuth: true,
      roles: ['admin'],
      permissions: ['admin:classrooms:read']
    }
  },
  {
    path: '/content',
    name: 'Content',
    component: () => import('@/views/content/ContentManagementView.vue'),
    meta: {
      title: 'menus.content',
      icon: 'Collection',
      rank: 40,
      requiresAuth: true,
      roles: ['admin'],
      permissions: ['admin:content:read']
    }
  },
  {
    path: '/reports',
    name: 'Reports',
    component: () => import('@/views/reports/ReportsManagementView.vue'),
    meta: {
      title: 'menus.reports',
      icon: 'DataAnalysis',
      rank: 50,
      requiresAuth: true,
      roles: ['admin'],
      permissions: ['admin:report:read']
    }
  },
  {
    path: '/system',
    name: 'System',
    component: () => import('@/views/system/SystemManagementView.vue'),
    meta: {
      title: 'menus.system',
      icon: 'Setting',
      rank: 60,
      requiresAuth: true,
      roles: ['admin'],
      permissions: ['admin:system:read', 'admin:audit:read']
    }
  }
]

export default moduleRoutes
