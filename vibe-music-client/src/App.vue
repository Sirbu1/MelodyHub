<script setup lang="ts">
import { onMounted } from 'vue'
import DefaultLayout from '@/layout/index.vue'
import { UserStore } from '@/stores/modules/user'

const userStore = UserStore()

// 应用启动时验证用户信息
onMounted(async () => {
  // 如果有token，验证并同步用户信息
  if (userStore.userInfo?.token) {
    await userStore.validateAndSyncUserInfo()
  }
})
</script>

<template>
  <DefaultLayout />
</template>
