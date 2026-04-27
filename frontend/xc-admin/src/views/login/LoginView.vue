<template>
  <main class="login-page">
    <section class="login-copy">
      <div class="brand-lockup">
        <div class="brand-logo">XC</div>
        <span>{{ t('app.shortTitle') }}</span>
      </div>
      <h1>{{ t('login.title') }}</h1>
      <p>{{ t('login.subtitle') }}</p>
    </section>

    <el-card class="login-panel" shadow="always">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large" @submit.prevent="submit">
        <el-form-item :label="t('common.account')" prop="account">
          <el-input v-model="form.account" autocomplete="username" :placeholder="t('login.accountPlaceholder')">
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item :label="t('common.password')" prop="password">
          <el-input
            v-model="form.password"
            autocomplete="current-password"
            :placeholder="t('login.passwordPlaceholder')"
            show-password
            type="password"
          >
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-button type="primary" native-type="submit" class="login-button" :loading="submitting">
          {{ t('common.login') }}
        </el-button>
      </el-form>
    </el-card>
  </main>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { useSessionStore } from '@/stores/session'

const route = useRoute()
const router = useRouter()
const session = useSessionStore()
const { t } = useI18n()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const form = reactive({
  account: '',
  password: ''
})
const rules = computed<FormRules<typeof form>>(() => ({
  account: [{ required: true, message: t('login.accountRequired'), trigger: 'blur' }],
  password: [{ required: true, message: t('login.passwordRequired'), trigger: 'blur' }]
}))

async function submit() {
  const valid = await formRef.value?.validate()
  if (!valid) {
    return
  }
  submitting.value = true
  try {
    await session.login(form)
    ElMessage.success(t('login.success'))
    await router.push(typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.login-page {
  align-items: center;
  background:
    radial-gradient(circle at 18% 16%, rgb(37 99 235 / 12%), transparent 28%),
    linear-gradient(135deg, #f8fafc 0%, #eef2ff 100%);
  display: grid;
  gap: 56px;
  grid-template-columns: minmax(0, 460px) minmax(360px, 420px);
  justify-content: center;
  min-height: 100vh;
  padding: 32px;
}

.login-copy {
  display: grid;
  gap: 18px;
}

.brand-lockup {
  align-items: center;
  color: #172033;
  display: flex;
  font-size: 18px;
  font-weight: 800;
  gap: 12px;
}

.brand-logo {
  align-items: center;
  background: #2563eb;
  border-radius: 10px;
  color: #ffffff;
  display: flex;
  font-size: 18px;
  height: 42px;
  justify-content: center;
  width: 42px;
}

.login-copy h1 {
  color: #172033;
  font-size: 42px;
  line-height: 1.15;
  margin: 0;
}

.login-copy p {
  color: #64748b;
  font-size: 18px;
  line-height: 1.7;
  margin: 0;
}

.login-panel {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 8px;
}

.login-button {
  width: 100%;
}

@media (max-width: 860px) {
  .login-page {
    grid-template-columns: 1fr;
  }
}
</style>
