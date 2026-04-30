import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiProxyTarget = env.VITE_API_PROXY_TARGET || env.VITE_API_BASE_URL?.replace(/\/api\/?$/, '')
  const devHost = env.VITE_DEV_HOST || '0.0.0.0'
  const devPort = Number(env.VITE_DEV_PORT || 8091)
  const base = env.VITE_BASE || (mode === 'prod' ? '/admin/' : '/')

  return {
    base,
    plugins: [vue()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    server: {
      host: devHost,
      port: devPort,
      proxy: apiProxyTarget
        ? {
            '/api': {
              target: apiProxyTarget,
              changeOrigin: true
            }
          }
        : undefined
    }
  }
})
