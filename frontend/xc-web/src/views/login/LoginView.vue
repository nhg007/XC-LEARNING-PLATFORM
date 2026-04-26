<template>
  <main class="login-page">
    <section class="login-panel">
      <h1>登录</h1>
      <el-form label-position="top">
        <el-form-item label="邮箱">
          <el-input model-value="" placeholder="student@example.com" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input model-value="" type="password" show-password />
        </el-form-item>
        <el-button type="primary" class="login-button" @click="$router.push('/')">
          进入学习
        </el-button>
      </el-form>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  align-items: center;
  display: flex;
  justify-content: center;
  min-height: 100vh;
  padding: 24px;
}

.login-panel {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  max-width: 420px;
  padding: 32px;
  width: 100%;
}

.login-button {
  width: 100%;
}
</style>
