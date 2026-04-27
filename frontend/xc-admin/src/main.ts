import { createPinia } from 'pinia'
import { createApp } from 'vue'
import App from './App.vue'
import { i18n } from './plugins/i18n'
import { setupElementPlus } from './plugins/elementPlus'
import router from './router'
import './styles/main.css'

const app = createApp(App)

app.use(createPinia())
app.use(i18n)
setupElementPlus(app)
app.use(router)
app.mount('#app')
