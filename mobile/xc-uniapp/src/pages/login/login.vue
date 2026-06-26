<template>
  <view class="page">
    <view class="hero">
      <view class="hero-top">
        <view class="hero-copy">
          <text class="eyebrow">{{ t('login.eyebrow') }}</text>
          <text class="title">{{ t('login.title') }}</text>
          <text class="subtitle">{{ t('login.subtitle') }}</text>
        </view>
        <LanguageSwitch variant="hero" />
      </view>
      <view class="benefits">
        <text>{{ t('login.benefit.vocab') }}</text>
        <text>{{ t('login.benefit.practice') }}</text>
      </view>
    </view>

    <view class="panel">
      <view class="panel-head">
        <view>
          <text class="panel-title">{{ mode === 'login' ? t('login.formTitle') : t('login.registerTitle') }}</text>
          <text class="tip">{{ mode === 'login' ? t('login.tip') : t('login.registerTip') }}</text>
        </view>
      </view>

      <view class="auth-tabs">
        <button
          class="auth-tab"
          :class="{ active: mode === 'login' }"
          @click="switchMode('login')"
        >
          {{ t('login.loginTab') }}
        </button>
        <button
          class="auth-tab"
          :class="{ active: mode === 'register' }"
          @click="switchMode('register')"
        >
          {{ t('login.registerTab') }}
        </button>
      </view>

      <view v-if="errorMessage" class="error-card">
        <text>{{ errorMessage }}</text>
      </view>

      <view class="field">
        <text class="field-label">{{ t('login.email') }}</text>
        <input
          v-model="form.account"
          class="input"
          type="text"
          :placeholder="t('login.email')"
          confirm-type="next"
          @input="clearError"
        />
      </view>
      <view v-if="mode === 'register'" class="field">
        <text class="field-label">{{ t('login.nickname') }}</text>
        <input
          v-model="form.nickname"
          class="input"
          type="text"
          :placeholder="t('login.nicknamePlaceholder')"
          confirm-type="next"
          @input="clearError"
        />
      </view>
      <view class="field">
        <text class="field-label">{{ t('login.password') }}</text>
        <input
          v-model="form.password"
          class="input"
          password
          :placeholder="t('login.password')"
          confirm-type="done"
          @confirm="submit"
          @input="clearError"
        />
      </view>
      <button class="button" :loading="submitting" :disabled="!canSubmit || submitting" @click="submit">
        {{ mode === 'login' ? t('login.submit') : t('login.registerSubmit') }}
      </button>

      <view class="trust-list">
        <text>{{ t('login.syncProgress') }}</text>
        <text>{{ t('login.syncClass') }}</text>
        <text>{{ t('login.syncAccess') }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import LanguageSwitch from '../../components/LanguageSwitch.vue'
import { login, register } from '../../api/auth'
import { useI18n, setPageTitle } from '../../i18n'
import { getToken, setProfile, setToken } from '../../utils/storage'
import { openPage, routes } from '../../utils/navigation'

const { locale, t } = useI18n()
const mode = ref<'login' | 'register'>('login')
const submitting = ref(false)
const errorMessage = ref('')
const form = reactive({
  account: '',
  nickname: '',
  password: ''
})

const canSubmit = computed(() => {
  if (!form.account.trim() || !form.password) {
    return false
  }
  if (mode.value === 'register') {
    return form.password.length >= 8 && form.password.length <= 72 && form.nickname.length <= 100
  }
  return true
})

onShow(() => {
  setPageTitle('login.title')
  if (getToken()) {
    void openPage(routes.home)
  }
})

watch(locale, () => {
  setPageTitle('login.title')
})

async function submit() {
  if (!form.account.trim()) {
    errorMessage.value = t('login.accountRequired')
    return
  }
  if (mode.value === 'register' && !isValidEmail(form.account.trim())) {
    errorMessage.value = t('login.validEmail')
    return
  }
  if (!form.password) {
    errorMessage.value = t('login.passwordRequired')
    return
  }
  if (mode.value === 'register' && (form.password.length < 8 || form.password.length > 72)) {
    errorMessage.value = t('login.passwordLength')
    return
  }
  if (mode.value === 'register' && form.nickname.length > 100) {
    errorMessage.value = t('login.nicknameLength')
    return
  }
  submitting.value = true
  errorMessage.value = ''
  try {
    const data = mode.value === 'login'
      ? await login({
          account: form.account.trim(),
          password: form.password
        })
      : await register({
          email: form.account.trim(),
          nickname: form.nickname.trim() || undefined,
          password: form.password
        })
    setToken(data.accessToken)
    setProfile(data.profile)
    void openPage(routes.home)
  } catch {
    errorMessage.value = mode.value === 'login' ? t('login.failed') : t('login.registerFailed')
  } finally {
    submitting.value = false
  }
}

function switchMode(nextMode: 'login' | 'register') {
  mode.value = nextMode
  clearError()
}

function clearError() {
  errorMessage.value = ''
}

function isValidEmail(value: string) {
  return /.+@.+\..+/.test(value)
}
</script>

<style scoped>
.page {
  background: #eef5f7;
  box-sizing: border-box;
  min-height: 100vh;
  padding: 0 24rpx 36rpx;
}

.hero {
  background: #102033;
  border-bottom-left-radius: 32rpx;
  border-bottom-right-radius: 32rpx;
  color: #ffffff;
  margin: 0 -24rpx 22rpx;
  padding: 34rpx 24rpx 30rpx;
}

.hero-top {
  align-items: flex-start;
  display: flex;
  gap: 20rpx;
  justify-content: space-between;
}

.hero-copy {
  flex: 1;
  min-width: 0;
}

.eyebrow {
  color: #a7f3d0;
  display: block;
  font-size: 24rpx;
  font-weight: 700;
}

.title {
  display: block;
  font-size: 54rpx;
  font-weight: 800;
  letter-spacing: 0;
  line-height: 1.1;
  margin-top: 12rpx;
}

.subtitle {
  color: #cbd5e1;
  display: block;
  font-size: 28rpx;
  line-height: 1.55;
  margin-top: 18rpx;
}

.benefits {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 28rpx;
}

.benefits text {
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 999rpx;
  color: #e2e8f0;
  font-size: 23rpx;
  padding: 10rpx 16rpx;
}

.panel {
  background: var(--xc-surface);
  border: 1px solid var(--xc-border);
  border-radius: 24rpx;
  box-shadow: var(--xc-shadow-raised);
  box-sizing: border-box;
  margin-top: 22rpx;
  padding: 30rpx;
}

.panel-head {
  margin-bottom: 24rpx;
  text-align: center;
}

.panel-title {
  color: var(--xc-ink);
  display: block;
  font-size: 40rpx;
  font-weight: 900;
  line-height: 1.3;
}

.auth-tabs {
  background: #eef5f7;
  border: 1px solid var(--xc-border);
  border-radius: 18rpx;
  display: grid;
  gap: 8rpx;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-bottom: 22rpx;
  padding: 8rpx;
}

.auth-tab {
  background: transparent;
  border-radius: 14rpx;
  color: var(--xc-muted);
  font-size: 28rpx;
  font-weight: 850;
  line-height: 1;
  min-height: 72rpx;
  padding: 0;
}

.auth-tab::after {
  border: 0;
}

.auth-tab.active {
  background: var(--xc-primary);
  color: #ffffff;
}

.error-card {
  background: #fff7ed;
  border: 1px solid #fed7aa;
  border-radius: 14rpx;
  color: #9a3412;
  font-size: 24rpx;
  line-height: 1.45;
  margin-bottom: 22rpx;
  padding: 18rpx;
}

.field {
  margin-bottom: 22rpx;
}

.field-label {
  color: var(--xc-muted);
  display: block;
  font-size: 24rpx;
  font-weight: 850;
  margin-bottom: 10rpx;
}

.input {
  background: var(--xc-surface-raised);
  border: 1px solid var(--xc-border);
  border-radius: 18rpx;
  box-sizing: border-box;
  height: 88rpx;
  padding: 0 24rpx;
}

.button {
  background: var(--xc-primary);
  border-radius: 20rpx;
  box-shadow: 0 12rpx 24rpx rgba(13, 143, 117, 0.2);
  color: #ffffff;
  font-size: 30rpx;
  font-weight: 800;
  margin-top: 6rpx;
  min-height: 88rpx;
}

.button[disabled] {
  opacity: 0.58;
}

.tip {
  color: var(--xc-muted);
  display: block;
  font-size: 24rpx;
  line-height: 1.6;
  margin-top: 8rpx;
}

.trust-list {
  border-top: 1px solid var(--xc-border);
  color: var(--xc-muted);
  display: grid;
  font-size: 23rpx;
  gap: 8rpx;
  line-height: 1.45;
  margin-top: 24rpx;
  padding-top: 20rpx;
}
</style>
