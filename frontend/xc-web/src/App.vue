<template>
  <v-app>
    <PageTabs v-if="route.meta.requiresAuth" />
    <RouterView />
    <v-snackbar v-model="visible" :color="color" location="top right" timeout="2600">
      {{ message }}
    </v-snackbar>
  </v-app>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import PageTabs from '@/components/PageTabs.vue'
import { usePreferenceStore } from './stores/preferences'
import { useSessionStore } from './stores/session'
import type { NotifyColor, NotifyPayload } from './utils/notify'

const route = useRoute()
const session = useSessionStore()
const preferences = usePreferenceStore()
const visible = ref(false)
const message = ref('')
const color = ref<NotifyColor>('info')

function handleNotify(event: Event) {
  const detail = (event as CustomEvent<NotifyPayload>).detail
  message.value = detail.message
  color.value = detail.color
  visible.value = true
}

onMounted(() => {
  window.addEventListener('xc:notify', handleNotify)
})

watch(
  () => [session.token, route.meta.requiresAuth],
  () => {
    if (session.isLoggedIn && route.meta.requiresAuth) {
      void preferences.load()
    } else if (!session.isLoggedIn) {
      preferences.reset()
    }
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  window.removeEventListener('xc:notify', handleNotify)
})
</script>
