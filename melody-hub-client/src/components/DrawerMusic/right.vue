<script setup lang="ts">
import type { SongDetail } from '@/api/interface'
import { ref, inject, type Ref, computed, watch } from 'vue'
import { getSongDetail } from '@/api/system'
import { UserStore } from '@/stores/modules/user'

const songDetail = inject<Ref<SongDetail | null>>('songDetail')
const userStore = UserStore()


// 调试：监听歌曲详情变化，输出打赏相关信息
watch(() => songDetail.value, (newDetail) => {
  if (newDetail) {
    console.log('========== 前端歌曲详情调试信息 ==========')
    console.log('songId:', newDetail.songId)
    console.log('songName:', newDetail.songName)
    console.log('isOriginal:', newDetail.isOriginal)
    console.log('isRewardEnabled:', newDetail.isRewardEnabled)
    console.log('rewardQrUrl:', newDetail.rewardQrUrl)
    console.log('creatorName:', newDetail.creatorName)
    console.log('artistName:', newDetail.artistName)
    console.log('完整对象:', JSON.stringify(newDetail, null, 2))
    console.log('========================================')
  }
}, { immediate: true, deep: true })

const formatDate = (date: string) => {
  return new Date(date).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}
</script>

<template>
  <div class="h-full p-6 overflow-y-auto mr-16">
    <div v-if="songDetail" class="space-y-6">
      <!-- 歌曲信息 -->
      <div class="space-y-2">
        <h3 class="text-xl font-semibold text-primary-foreground">歌曲信息</h3>
        <div class="grid grid-cols-2 gap-4 text-sm text-muted-foreground">
          <div>
            <span class="text-primary-foreground">专辑：</span>
            {{ songDetail.album }}
          </div>
          <div>
            <span class="text-primary-foreground">发行时间：</span>
            {{ formatDate(songDetail.releaseTime) }}
          </div>
        </div>
      </div>

      <!-- 打赏收款码 -->
      <div v-if="songDetail.isRewardEnabled && songDetail.rewardQrUrl" class="space-y-3 mt-8">
        <h3 class="text-xl font-semibold text-primary-foreground">欢迎打赏</h3>
        <div class="flex flex-col items-center gap-3 p-4 bg-gray-50 dark:bg-gray-800 rounded-lg">
          <p class="text-sm text-muted-foreground text-center">
            <span v-if="songDetail.creatorName">支持 <strong>{{ songDetail.creatorName }}</strong> 的创作</span>
            <span v-else>支持原创音乐人</span>
          </p>
          <el-image
            :src="songDetail.rewardQrUrl"
            alt="收款码"
            fit="contain"
            class="reward-qr-code max-w-[200px] max-h-[200px] rounded-lg border border-gray-200 shadow-sm cursor-pointer hover:scale-105 transition-transform"
            :preview-src-list="[songDetail.rewardQrUrl]"
            preview-teleported
            @error="() => console.error('收款码加载失败:', songDetail.rewardQrUrl)"
          />
          <p class="text-xs text-gray-500 text-center">
            <icon-ri:information-line class="inline mr-1" />
            您的支持将鼓励更多原创音乐诞生
          </p>
        </div>
      </div>
      <!-- 调试信息：显示打赏状态 -->
      <div v-else-if="songDetail.isRewardEnabled" class="mt-4 text-xs text-gray-400">
        <!-- 已开启打赏但收款码未加载 -->
      </div>
    </div>
    <div v-else class="flex items-center justify-center h-full">
      <el-empty description="暂无歌曲信息" />
    </div>
  </div>
</template>

<style scoped>
.el-button {
  --el-button-hover-text-color: var(--el-color-primary);
  --el-button-hover-bg-color: transparent;
}

:deep(.el-input__wrapper) {
  border-radius: 8px;
}

:deep(.el-textarea__inner) {
  border-radius: 12px !important;
}
</style>
