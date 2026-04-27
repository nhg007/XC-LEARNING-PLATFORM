import { t } from '@/plugins/i18n'

export type NotifyColor = 'success' | 'warning' | 'info' | 'error'

export interface NotifyPayload {
  message: string
  color: NotifyColor
}

export function notify(message: string, color: NotifyColor = 'info') {
  window.dispatchEvent(new CustomEvent<NotifyPayload>('xc:notify', {
    detail: {
      message,
      color
    }
  }))
}

export function notifySuccess(message: string) {
  notify(message, 'success')
}

export function notifyWarning(message: string) {
  notify(message, 'warning')
}

export function notifyInfo(message: string) {
  notify(message, 'info')
}

export function confirmAction(message: string, title = t('common.confirm')) {
  return Promise.resolve(window.confirm(`${title}\n\n${message}`))
}
