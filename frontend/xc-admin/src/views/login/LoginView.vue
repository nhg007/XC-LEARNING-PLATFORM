<template>
  <main class="login-page">
    <section class="login-panel">
      <h1>XC 管理后台</h1>
      <el-form label-position="top">
        <el-form-item label="账号">
          <el-input model-value="" placeholder="admin@example.com" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input model-value="" placeholder="请输入密码" type="password" show-password />
        </el-form-item>
        <el-button type="primary" class="login-button" @click="$router.push('/dashboard')">
          登录
        </el-button>
      </el-form>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  align-items: center;
  background: #eef2f7;
  display: flex;
  min-height: 100vh;
  justify-content: center;
  padding: 24px;
}

.login-panel {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 18px 60px rgb(15 23 42 / 8%);
  max-width: 420px;
  padding: 32px;
  width: 100%;
}

.login-panel h1 {
  font-size: 24px;
  margin: 0 0 24px;
}

.login-button {
  width: 100%;
}
</style>
