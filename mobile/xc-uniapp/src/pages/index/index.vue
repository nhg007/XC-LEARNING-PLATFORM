<template>
  <view class="page">
    <view class="header">
      <text class="title">汉语学习</text>
      <text class="subtitle">移动端骨架，后续接入背单词、练习和班级统计。</text>
    </view>
    <view class="grid">
      <view class="card" v-for="item in features" :key="item">
        <text>{{ item }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
const features = ['背单词', '句子练习', '台词跟读', '连连看', '班级', '学习记录']
</script>

<style scoped>
.page {
  padding: 24rpx;
}

.header {
  margin-bottom: 28rpx;
}

.title {
  display: block;
  font-size: 44rpx;
  font-weight: 700;
}

.subtitle {
  color: #64748b;
  display: block;
  font-size: 26rpx;
  margin-top: 12rpx;
}

.grid {
  display: flex;
  flex-wrap: wrap;
  gap: 18rpx;
}

.card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8rpx;
  box-sizing: border-box;
  padding: 28rpx;
  width: calc(50% - 9rpx);
}
</style>
