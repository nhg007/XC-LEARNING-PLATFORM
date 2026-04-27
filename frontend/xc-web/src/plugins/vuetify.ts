import '@mdi/font/css/materialdesignicons.css'
import 'vuetify/styles'
import { createVuetify } from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import { aliases, mdi } from 'vuetify/iconsets/mdi'

const vuetify = createVuetify({
  components,
  directives,
  icons: {
    defaultSet: 'mdi',
    aliases,
    sets: {
      mdi
    }
  },
  theme: {
    defaultTheme: 'xcLight',
    themes: {
      xcLight: {
        dark: false,
        colors: {
          background: '#f6f8fb',
          surface: '#ffffff',
          primary: '#2563eb',
          secondary: '#14b8a6',
          accent: '#f59e0b',
          error: '#dc2626',
          warning: '#d97706',
          info: '#0284c7',
          success: '#16a34a'
        }
      }
    }
  }
})

export default vuetify
