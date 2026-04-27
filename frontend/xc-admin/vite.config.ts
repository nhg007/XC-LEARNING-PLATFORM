import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiOrigin = env.VITE_API_BASE_URL?.replace(/\/api\/?$/, '')

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    server: apiOrigin
      ? {
          proxy: {
            '/api': {
              target: apiOrigin,
              changeOrigin: true
            }
          }
        }
      : undefined
  }
})
