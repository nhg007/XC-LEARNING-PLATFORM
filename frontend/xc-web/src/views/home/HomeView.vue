<template>
  <main class="app-shell">
    <header class="topbar">
      <div>
        <h1>汉语学习网站</h1>
        <p>学习入口骨架，后续接入试用、会员和学习记录接口。</p>
      </div>
      <el-button type="primary" @click="$router.push('/login')">登录</el-button>
    </header>

    <section class="feature-grid">
      <div class="feature-card" v-for="item in features" :key="item.title">
        <h2>{{ item.title }}</h2>
        <p>{{ item.description }}</p>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
const features = [
  { title: '背单词', description: 'HSK、YCT、分类词汇和收藏夹。' },
  { title: '句子练习', description: '听录音排序、听写、拼音写句子。' },
  { title: '台词训练', description: '听台词、跟读录音、本地 ASR 识别。' },
  { title: '连连看', description: '汉字与英/俄释义匹配游戏。' },
  { title: '班级', description: '查看班级成员和学习统计。' },
  { title: '学习记录', description: '连续天数、正确率和排行榜。' }
]
</script>

<style scoped>
h1 {
  font-size: 28px;
  margin: 0;
}

h2 {
  font-size: 18px;
  margin: 0 0 8px;
}

p {
  color: #64748b;
  margin: 8px 0 0;
}
</style>
