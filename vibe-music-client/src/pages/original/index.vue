<template>
  <div class="original-container">
    <!-- 上传歌曲组件 -->
    <div class="upload-section">
      <UploadSong @upload-success="handleUploadSuccess" @show-login="handleShowLogin" />
    </div>

    <!-- 登录提示 -->
    <AuthTabs v-model="authVisible" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { UserStore } from '@/stores/modules/user'
import UploadSong from '@/components/UploadSong.vue'
import AuthTabs from '@/components/Auth/AuthTabs.vue'

const userStore = UserStore()
const authVisible = ref(false)

// 检查登录状态
onMounted(() => {
  if (!userStore.isLoggedIn) {
    authVisible.value = true
  }
})

// 处理上传成功
const handleUploadSuccess = () => {
  // 上传成功后可以显示提示信息，但不切换页面
  console.log('上传成功')
}

// 处理显示登录对话框
const handleShowLogin = () => {
  authVisible.value = true
}
</script>

<style scoped>
.original-container {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.upload-section {
  width: 100%;
  height: 100%;
  overflow-y: auto;
  overflow-x: hidden;
}
</style>
