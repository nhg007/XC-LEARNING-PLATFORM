import { createPinia } from 'pinia'
import { createApp } from 'vue'
import App from './App.vue'
import { i18n, getStoredLocale } from './plugins/i18n'
import vuetify from './plugins/vuetify'
import router from './router'
import './styles/main.css'

document.documentElement.lang = getStoredLocale()

createApp(App).use(createPinia()).use(i18n).use(router).use(vuetify).mount('#app')
