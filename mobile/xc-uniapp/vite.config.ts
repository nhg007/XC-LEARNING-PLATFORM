import uniPlugin from '@dcloudio/vite-plugin-uni'
import { defineConfig } from 'vite'

const uni = typeof uniPlugin === 'function'
  ? uniPlugin
  : (uniPlugin as unknown as { default: typeof uniPlugin }).default

export default defineConfig({
  plugins: [uni()]
})
