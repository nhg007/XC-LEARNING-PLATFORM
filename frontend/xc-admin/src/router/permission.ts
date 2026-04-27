import type { AdminProfile } from '@/types/api'
import type { RouteMeta } from 'vue-router'

function matchPermission(required: string, granted: string) {
  if (granted === '*' || granted === required) {
    return true
  }
  if (granted.endsWith(':*')) {
    return required.startsWith(granted.slice(0, -1))
  }
  if (required.endsWith(':*')) {
    return granted.startsWith(required.slice(0, -1))
  }
  return false
}

export function hasRouteAccess(meta: RouteMeta, profile: AdminProfile | null) {
  if (!meta.roles?.length && !meta.permissions?.length) {
    return true
  }
  if (!profile) {
    return false
  }
  const roleAllowed = !meta.roles?.length || meta.roles.some(role => profile.roles.includes(role))
  const permissionAllowed = !meta.permissions?.length || meta.permissions.some(required =>
    profile.permissions.some(granted => matchPermission(required, granted))
  )
  return roleAllowed && permissionAllowed
}
