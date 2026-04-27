import 'vue-router'

export interface AdminRouteMeta {
  title?: string
  icon?: string
  rank?: number
  requiresAuth?: boolean
  roles?: string[]
  permissions?: string[]
  showLink?: boolean
}

declare module 'vue-router' {
  interface RouteMeta extends AdminRouteMeta {}
}
