<template>
  <view class="page">
    <view class="panel">
      <text class="title">汉语学习</text>
      <input v-model="form.account" class="input" type="text" placeholder="邮箱" />
      <input v-model="form.password" class="input" password placeholder="密码" />
      <button class="button" :loading="submitting" @click="submit">登录</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { login } from '../../api/auth'
import { setProfile, setToken } from '../../utils/storage'

const submitting = ref(false)
const form = reactive({
  account: '',
  password: ''
})

async function submit() {
  if (!form.account || !form.password) {
    void uni.showToast({ icon: 'none', title: '请输入邮箱和密码' })
    return
  }
  submitting.value = true
  try {
    const data = await login(form)
    setToken(data.accessToken)
    setProfile(data.profile)
    void uni.reLaunch({ url: '/pages/index/index' })
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.page {
  align-items: center;
  background: #f7f8fb;
  box-sizing: border-box;
  display: flex;
  min-height: 100vh;
  padding: 32rpx;
}

.panel {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8rpx;
  box-sizing: border-box;
  padding: 36rpx;
  width: 100%;
}

.title {
  display: block;
  font-size: 44rpx;
  font-weight: 700;
  margin-bottom: 32rpx;
}

.input {
  border: 1px solid #d1d5db;
  border-radius: 8rpx;
  box-sizing: border-box;
  height: 88rpx;
  margin-bottom: 22rpx;
  padding: 0 24rpx;
}

.button {
  background: #2563eb;
  color: #ffffff;
}
</style>
