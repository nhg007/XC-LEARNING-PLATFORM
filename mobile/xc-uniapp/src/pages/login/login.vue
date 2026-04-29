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
        <text>{{ t('login.benefit.dialogue') }}</text>
      </view>
    </view>

    <view class="panel">
      <view class="panel-head">
        <view>
          <text class="panel-title">{{ t('login.formTitle') }}</text>
          <text class="tip">{{ t('login.tip') }}</text>
        </view>
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
      <button class="button" :loading="submitting" :disabled="!canSubmit || submitting" @click="submit">{{ t('login.submit') }}</button>

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
import { login } from '../../api/auth'
import { useI18n, setPageTitle } from '../../i18n'
import { getToken, setProfile, setToken } from '../../utils/storage'
import { openPage, routes } from '../../utils/navigation'

const { locale, t } = useI18n()
const submitting = ref(false)
const errorMessage = ref('')
const form = reactive({
  account: '',
  password: ''
})

const canSubmit = computed(() => form.account.trim().length > 0 && form.password.length > 0)

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
  if (!form.password) {
    errorMessage.value = t('login.passwordRequired')
    return
  }
  submitting.value = true
  errorMessage.value = ''
  try {
    const data = await login({
      account: form.account.trim(),
      password: form.password
    })
    setToken(data.accessToken)
    setProfile(data.profile)
    void openPage(routes.home)
  } catch {
    errorMessage.value = t('login.failed')
  } finally {
    submitting.value = false
  }
}

function clearError() {
  errorMessage.value = ''
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
  background: rgba(255, 255, 255, 0.12);
  border-radius: 999rpx;
  color: #e2e8f0;
  font-size: 23rpx;
  padding: 10rpx 16rpx;
}

.panel {
  background: #ffffff;
  border: 1px solid #d7e2ea;
  border-radius: 24rpx;
  box-shadow: 0 12rpx 36rpx rgba(15, 23, 42, 0.06);
  box-sizing: border-box;
  margin-top: 22rpx;
  padding: 28rpx;
}

.panel-head {
  margin-bottom: 24rpx;
}

.panel-title {
  color: #102033;
  display: block;
  font-size: 34rpx;
  font-weight: 800;
  line-height: 1.3;
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
  color: #475569;
  display: block;
  font-size: 24rpx;
  font-weight: 700;
  margin-bottom: 10rpx;
}

.input {
  background: #f8fafc;
  border: 1px solid #cbd5e1;
  border-radius: 14rpx;
  box-sizing: border-box;
  height: 88rpx;
  padding: 0 24rpx;
}

.button {
  background: #14796f;
  border-radius: 14rpx;
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
  color: #64748b;
  display: block;
  font-size: 24rpx;
  line-height: 1.6;
  margin-top: 8rpx;
}

.trust-list {
  border-top: 1px solid #e2e8f0;
  color: #475569;
  display: grid;
  font-size: 23rpx;
  gap: 8rpx;
  line-height: 1.45;
  margin-top: 24rpx;
  padding-top: 20rpx;
}
</style>
