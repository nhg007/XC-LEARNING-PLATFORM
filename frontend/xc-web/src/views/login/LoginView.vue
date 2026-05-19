<template>
  <main class="login-page">
    <div class="login-frame">
      <section class="brand-panel" aria-labelledby="student-login-title">
        <div class="brand-copy">
          <div class="brand-mark">{{ t('login.mark') }}</div>
          <p class="eyebrow">{{ t('login.eyebrow') }}</p>
          <h1 id="student-login-title">{{ t('login.title') }}</h1>
          <p class="subtitle">{{ t('login.subtitle') }}</p>
        </div>

        <div class="study-preview" aria-hidden="true">
          <div class="lesson-strip">
            <span>{{ t('login.previewLesson') }}</span>
            <strong>{{ t('login.previewLevel') }}</strong>
          </div>
          <div class="character-card">
            <span class="hanzi">{{ t('login.previewHanzi') }}</span>
            <span class="pinyin">{{ t('login.previewPinyin') }}</span>
            <span class="meaning">{{ t('login.previewMeaning') }}</span>
          </div>
          <dl class="preview-stats">
            <div v-for="item in previewStats" :key="item.label">
              <dt>{{ item.label }}</dt>
              <dd>{{ item.value }}</dd>
            </div>
          </dl>
        </div>

        <ul class="benefit-list">
          <li v-for="item in benefitItems" :key="item">
            <v-icon icon="mdi-check-circle" size="18" />
            <span>{{ item }}</span>
          </li>
        </ul>
      </section>

      <section class="login-panel" aria-label="student auth">
        <div class="locale-row">
          <LocaleSwitch variant="outlined" />
        </div>

        <div class="panel-heading">
          <h2>{{ t('login.panelTitle') }}</h2>
          <p>{{ t('login.panelSubtitle') }}</p>
        </div>

        <div class="auth-tabs" role="tablist">
          <button
            type="button"
            role="tab"
            :aria-selected="mode === 'login'"
            :class="{ active: mode === 'login' }"
            @click="mode = 'login'"
          >
            {{ t('login.loginTab') }}
          </button>
          <button
            type="button"
            role="tab"
            :aria-selected="mode === 'register'"
            :class="{ active: mode === 'register' }"
            @click="mode = 'register'"
          >
            {{ t('login.registerTab') }}
          </button>
        </div>

        <v-window v-model="mode" class="form-window">
          <v-window-item value="login">
            <v-form ref="loginFormRef" @submit.prevent="submitLogin">
              <v-text-field
                v-model="loginForm.account"
                autocomplete="username"
                :label="t('login.email')"
                :placeholder="t('login.emailPlaceholder')"
                prepend-inner-icon="mdi-email-outline"
                :rules="emailRules"
                color="primary"
                variant="outlined"
              />
              <v-text-field
                v-model="loginForm.password"
                autocomplete="current-password"
                :label="t('login.password')"
                :placeholder="t('login.passwordPlaceholder')"
                prepend-inner-icon="mdi-lock-outline"
                :rules="loginPasswordRules"
                color="primary"
                type="password"
                variant="outlined"
              />
              <v-btn block class="submit-button" color="primary" size="large" type="submit" :loading="submitting">
                {{ t('login.enter') }}
              </v-btn>
            </v-form>
          </v-window-item>

          <v-window-item value="register">
            <v-form ref="registerFormRef" @submit.prevent="submitRegister">
              <v-text-field
                v-model="registerForm.email"
                autocomplete="username"
                :label="t('login.email')"
                :placeholder="t('login.emailPlaceholder')"
                prepend-inner-icon="mdi-email-outline"
                :rules="emailRules"
                color="primary"
                variant="outlined"
              />
              <v-text-field
                v-model="registerForm.nickname"
                counter="100"
                :label="t('login.nickname')"
                :placeholder="t('login.nicknamePlaceholder')"
                maxlength="100"
                prepend-inner-icon="mdi-account-outline"
                :rules="nicknameRules"
                color="primary"
                variant="outlined"
              />
              <v-text-field
                v-model="registerForm.password"
                autocomplete="new-password"
                :label="t('login.password')"
                :placeholder="t('login.passwordPlaceholder')"
                prepend-inner-icon="mdi-lock-outline"
                :rules="registerPasswordRules"
                color="primary"
                type="password"
                variant="outlined"
              />
              <v-btn block class="submit-button" color="primary" size="large" type="submit" :loading="submitting">
                {{ t('login.registerStart') }}
              </v-btn>
            </v-form>
          </v-window-item>
        </v-window>

        <p class="trial-note">{{ t('login.trialNote') }}</p>
      </section>
    </div>
  </main>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import LocaleSwitch from '@/components/LocaleSwitch.vue'
import { useSessionStore } from '../../stores/session'
import { notifySuccess } from '../../utils/notify'

type VFormRef = {
  validate: () => Promise<{ valid: boolean }>
}

const router = useRouter()
const session = useSessionStore()
const { t } = useI18n()
const mode = ref<'login' | 'register'>('login')
const submitting = ref(false)
const loginFormRef = ref<VFormRef | null>(null)
const registerFormRef = ref<VFormRef | null>(null)
const demoEmail = import.meta.env.DEV ? String(import.meta.env.VITE_DEMO_STUDENT_EMAIL || '') : ''
const demoPassword = import.meta.env.DEV ? String(import.meta.env.VITE_DEMO_STUDENT_PASSWORD || '') : ''
const loginForm = reactive({
  account: demoEmail,
  password: demoPassword
})
const registerForm = reactive({
  email: '',
  nickname: '',
  password: ''
})
const emailRules = computed(() => [
  (value: string) => /.+@.+\..+/.test(value) || t('login.validEmail')
])
const loginPasswordRules = computed(() => [
  (value: string) => Boolean(value) || t('login.passwordRequired')
])
const registerPasswordRules = computed(() => [
  (value: string) => (value.length >= 8 && value.length <= 72) || t('login.passwordLength')
])
const nicknameRules = computed(() => [
  (value: string) => value.length <= 100 || t('login.nicknameLength')
])
const benefitItems = computed(() => [
  t('login.benefitListen'),
  t('login.benefitPractice'),
  t('login.benefitClass')
])
const previewStats = computed(() => [
  { label: t('login.statStreak'), value: t('login.statStreakValue') },
  { label: t('login.statAccuracy'), value: t('login.statAccuracyValue') },
  { label: t('login.statWords'), value: t('login.statWordsValue') }
])

async function submitLogin() {
  const valid = await loginFormRef.value?.validate()
  if (!valid?.valid) {
    return
  }
  submitting.value = true
  try {
    await session.login(loginForm)
    notifySuccess(t('login.loginSuccess'))
    await router.push('/')
  } finally {
    submitting.value = false
  }
}

async function submitRegister() {
  const valid = await registerFormRef.value?.validate()
  if (!valid?.valid) {
    return
  }
  submitting.value = true
  try {
    await session.register(registerForm)
    notifySuccess(t('login.trialStarted'))
    await router.push('/')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.login-page {
  align-items: center;
  background: #f4f6f8;
  box-sizing: border-box;
  display: flex;
  justify-content: center;
  min-height: 100vh;
  padding: 32px var(--xc-page-padding-x);
}

.login-frame {
  background: #ffffff;
  border: 1px solid #d8dee8;
  border-radius: 8px;
  box-sizing: border-box;
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(400px, 460px);
  max-width: var(--xc-page-max-width);
  min-height: 640px;
  overflow: hidden;
  width: 100%;
}

.brand-panel {
  background: #152033;
  color: #ffffff;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  padding: 44px;
}

.brand-copy {
  max-width: 480px;
}

.brand-mark {
  align-items: center;
  background: #ffffff;
  border-radius: 8px;
  color: #2563eb;
  display: flex;
  font-size: 28px;
  font-weight: 900;
  height: 58px;
  justify-content: center;
  margin-bottom: 24px;
  width: 58px;
}

.eyebrow {
  color: #8fb7ff;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0;
  margin: 0 0 10px;
}

h1,
h2,
p {
  margin: 0;
}

h1 {
  color: #ffffff;
  font-size: 44px;
  line-height: 1.15;
}

.subtitle {
  color: #cbd5e1;
  font-size: 17px;
  line-height: 1.75;
  margin-top: 16px;
}

.study-preview {
  align-self: end;
  display: grid;
  gap: 12px;
  margin: 40px 0 32px;
}

.lesson-strip,
.character-card,
.preview-stats {
  background: #223047;
  border: 1px solid #344156;
  border-radius: 8px;
}

.lesson-strip {
  align-items: center;
  display: flex;
  justify-content: space-between;
  padding: 12px 14px;
}

.lesson-strip span {
  color: #aeb9ca;
  font-size: 13px;
}

.lesson-strip strong {
  color: #fbbf24;
  font-size: 14px;
}

.character-card {
  display: grid;
  gap: 8px;
  justify-items: center;
  padding: 28px 18px;
}

.hanzi {
  color: #ffffff;
  font-family: var(--xc-hanzi-font-family);
  font-size: 64px;
  font-weight: 900;
  line-height: 1;
}

.pinyin {
  color: #7dd3fc;
  font-size: 18px;
  font-weight: 700;
}

.meaning {
  color: #cbd5e1;
  font-size: 15px;
}

.preview-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin: 0;
  overflow: hidden;
}

.preview-stats div {
  display: grid;
  gap: 4px;
  padding: 14px;
}

.preview-stats div + div {
  border-left: 1px solid #344156;
}

.preview-stats dt {
  color: #94a3b8;
  font-size: 12px;
}

.preview-stats dd {
  color: #ffffff;
  font-size: 22px;
  font-weight: 800;
  margin: 0;
}

.benefit-list {
  display: grid;
  gap: 12px;
  list-style: none;
  margin: 0;
  padding: 0;
}

.benefit-list li {
  align-items: center;
  color: #dbe3ef;
  display: flex;
  font-size: 14px;
  gap: 10px;
}

.benefit-list :deep(.v-icon) {
  color: #3bd1b5;
}

.login-panel {
  align-self: stretch;
  border-left: 1px solid #d8dee8;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 44px 36px;
  width: 100%;
}

.locale-row {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 28px;
}

.locale-row :deep(.v-btn) {
  border-radius: 4px;
  font-size: 13px;
  min-height: 34px;
  padding: 0 10px;
}

.panel-heading {
  margin-bottom: 24px;
}

.panel-heading h2 {
  color: #172033;
  font-size: 26px;
  line-height: 1.2;
  margin-bottom: 8px;
}

.panel-heading p,
.trial-note {
  color: #64748b;
  font-size: 14px;
  line-height: 1.6;
}

.auth-tabs {
  border: 1px solid #d8dee8;
  border-radius: 6px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  overflow: hidden;
}

.auth-tabs button {
  background: #ffffff;
  border: 0;
  color: #475569;
  cursor: pointer;
  font-size: 14px;
  font-weight: 700;
  height: 44px;
  line-height: 44px;
  padding: 0 12px;
}

.auth-tabs button + button {
  border-left: 1px solid #d8dee8;
}

.auth-tabs button.active {
  background: #eaf2ff;
  color: #2563eb;
}

.auth-tabs button:focus-visible {
  outline: 2px solid #2563eb;
  outline-offset: -2px;
}

.form-window {
  padding-top: 26px;
}

.form-window :deep(.v-field) {
  border-radius: 4px;
}

.submit-button {
  border-radius: 4px;
  font-weight: 800;
  margin-top: 6px;
}

.trial-note {
  margin-top: 20px;
  text-align: center;
}

@media (max-width: 820px) {
  .login-page {
    padding: 20px;
  }

  .login-frame {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .brand-panel {
    padding: 32px;
  }

  .login-panel {
    border-left: 0;
    border-top: 1px solid #d8dee8;
    padding: 32px;
  }

  h1 {
    font-size: 36px;
  }

  .subtitle {
    font-size: 16px;
  }

  .hanzi {
    font-size: 56px;
  }
}

@media (max-width: 520px) {
  .login-page {
    padding: 10px var(--xc-page-padding-x-mobile);
  }

  .brand-panel {
    padding: 24px;
  }

  .login-panel {
    padding: 24px;
  }

  .preview-stats {
    grid-template-columns: 1fr;
  }

  .preview-stats div + div {
    border-left: 0;
    border-top: 1px solid #344156;
  }
}
</style>
