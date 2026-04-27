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
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import PageTabs from '@/components/PageTabs.vue'
import type { NotifyColor, NotifyPayload } from './utils/notify'

const route = useRoute()
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

onBeforeUnmount(() => {
  window.removeEventListener('xc:notify', handleNotify)
})
</script>
