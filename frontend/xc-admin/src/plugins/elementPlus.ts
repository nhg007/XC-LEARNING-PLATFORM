import ElementPlus from 'element-plus'
import type { App } from 'vue'
import 'element-plus/dist/index.css'

export function setupElementPlus(app: App) {
  app.use(ElementPlus)
}
