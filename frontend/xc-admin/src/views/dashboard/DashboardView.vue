<template>
  <main class="shell">
    <aside class="sidebar">
      <div class="brand">XC Admin</div>
      <div class="nav-item active">后台首页</div>
      <div class="nav-item">用户管理</div>
      <div class="nav-item">会员订单</div>
      <div class="nav-item">班级管理</div>
      <div class="nav-item">内容管理</div>
      <div class="nav-item">统计报表</div>
    </aside>
    <section class="content">
      <div class="toolbar">
        <div>
          <h1>后台首页</h1>
          <p>首期管理后台骨架，后续按模块接入真实接口。</p>
        </div>
        <el-button @click="$router.push('/login')">退出</el-button>
      </div>

      <div class="metric-grid">
        <div class="metric-card" v-for="item in metrics" :key="item.label">
          <div>{{ item.label }}</div>
          <strong>{{ item.value }}</strong>
        </div>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
const metrics = [
  { label: '用户数', value: '-' },
  { label: '会员数', value: '-' },
  { label: '班级数', value: '-' },
  { label: '今日学习次数', value: '-' }
]
</script>

<style scoped>
h1 {
  font-size: 24px;
  margin: 0;
}

p {
  color: #64748b;
  margin: 8px 0 0;
}

strong {
  display: block;
  font-size: 28px;
  margin-top: 10px;
}
</style>
