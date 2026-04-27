<template>
  <view class="page">
    <view class="header">
      <text class="title">汉语学习</text>
      <text class="subtitle">{{ accessLabel }} · 连续学习 {{ summary.currentStreakDays }} 天</text>
    </view>
    <view class="summary">
      <view class="summary-item">
        <text class="label">剩余时间</text>
        <text class="value">{{ remainingLabel }}</text>
      </view>
      <view class="summary-item">
        <text class="label">正确率</text>
        <text class="value">{{ summary.overallAccuracyRate }}%</text>
      </view>
    </view>
    <view class="grid">
      <view class="card" v-for="item in features" :key="item" @click="openFeature(item)">
        <text>{{ item }}</text>
      </view>
    </view>
    <view class="section">
      <text class="section-title">词汇表</text>
      <view class="list-item" v-for="item in vocabLists" :key="item.id" @click="openVocab(item.id)">
        <text>{{ item.name }}</text>
        <text class="muted">{{ item.level || item.listType }}</text>
      </view>
      <text v-if="vocabLists.length === 0" class="muted">暂无词汇表</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { fetchLearningSummary, fetchMembershipStatus, fetchVocabLists } from '../../api/home'
import type { LearningSummary, MembershipStatus, VocabList } from '../../types/api'
import { getToken } from '../../utils/storage'

const features = ['背单词', '句子练习', '台词跟读', '连连看', '班级', '学习记录']
const membership = ref<MembershipStatus>({
  accessLevel: 'free',
  fullAccess: false,
  trialEndsAt: null,
  membershipEndsAt: null,
  remainingSeconds: 0
})
const summary = ref<LearningSummary>({
  totalStudySeconds: 0,
  totalExerciseCount: 0,
  totalCorrectCount: 0,
  totalVocabReviewCount: 0,
  currentStreakDays: 0,
  longestStreakDays: 0,
  overallAccuracyRate: 0,
  lastStudyDate: null
})
const vocabLists = ref<VocabList[]>([])

const accessLabel = computed(() => {
  if (membership.value.accessLevel === 'member') {
    return '会员'
  }
  if (membership.value.accessLevel === 'trial') {
    return '试用中'
  }
  return '免费'
})
const remainingLabel = computed(() => {
  const days = Math.ceil(membership.value.remainingSeconds / 86400)
  return `${days} 天`
})

async function loadHome() {
  if (!getToken()) {
    void uni.redirectTo({ url: '/pages/login/login' })
    return
  }
  const [status, learning, vocab] = await Promise.all([
    fetchMembershipStatus(),
    fetchLearningSummary(),
    fetchVocabLists()
  ])
  membership.value = status
  summary.value = learning
  vocabLists.value = vocab.records
}

onShow(loadHome)

function openVocab(id: number) {
  void uni.navigateTo({ url: `/pages/vocab/study?listId=${id}` })
}

function openFeature(name: string) {
  if (name === '背单词') {
    if (vocabLists.value.length > 0) {
      openVocab(vocabLists.value[0].id)
    }
    return
  }
  if (name === '句子练习') {
    if (!membership.value.fullAccess) {
      void uni.showToast({ icon: 'none', title: '当前功能需要试用或会员权限' })
      return
    }
    void uni.navigateTo({ url: '/pages/practice/index' })
    return
  }
  if (name === '班级') {
    void uni.navigateTo({ url: '/pages/classroom/index' })
    return
  }
  if (name === '学习记录') {
    void uni.navigateTo({ url: '/pages/records/index' })
    return
  }
  void uni.showToast({ icon: 'none', title: '功能建设中' })
}
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

.summary {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8rpx;
  display: flex;
  gap: 20rpx;
  margin-bottom: 28rpx;
  padding: 24rpx;
}

.summary-item {
  flex: 1;
}

.label {
  color: #64748b;
  display: block;
  font-size: 24rpx;
  margin-bottom: 10rpx;
}

.value {
  display: block;
  font-size: 34rpx;
  font-weight: 700;
}

.section {
  margin-top: 30rpx;
}

.section-title {
  display: block;
  font-size: 34rpx;
  font-weight: 700;
  margin-bottom: 18rpx;
}

.list-item {
  align-items: center;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8rpx;
  display: flex;
  justify-content: space-between;
  margin-bottom: 14rpx;
  padding: 22rpx;
}

.muted {
  color: #64748b;
  font-size: 24rpx;
}
</style>
