import vue from '@vitejs/plugin-vue'
import { defineConfig, loadEnv } from 'vite'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiOrigin = (env.VITE_API_BASE_URL || 'http://localhost:8080/api').replace(/\/api\/?$/, '')

  return {
    plugins: [vue()],
    server: {
      proxy: {
        '/api': {
          target: apiOrigin,
          changeOrigin: true
        }
      }
    }
  }
})
