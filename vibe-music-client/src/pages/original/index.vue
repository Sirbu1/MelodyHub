<template>
  <div class="original-container">
    <!-- 子页面切换 -->
    <el-tabs v-model="activeTab" class="original-tabs" @tab-change="handleTabChange">
      <el-tab-pane label="上传歌曲" name="upload">
        <!-- 上传歌曲组件 -->
        <div class="upload-section">
          <UploadSong @upload-success="handleUploadSuccess" />
        </div>
      </el-tab-pane>

      <el-tab-pane label="原创曲库" name="library">
        <!-- 原创曲库组件 -->
        <div class="library-section">
          <OriginalLibrary ref="libraryRef" />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 登录提示 -->
    <AuthTabs v-model="authVisible" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { UserStore } from '@/stores/modules/user'
import UploadSong from '@/components/UploadSong.vue'
import OriginalLibrary from '@/components/OriginalLibrary.vue'
import AuthTabs from '@/components/Auth/AuthTabs.vue'

const route = useRoute()
const userStore = UserStore()
const activeTab = ref('upload')
const authVisible = ref(false)
const libraryRef = ref()

// 根据路由设置默认标签页
const setActiveTabFromRoute = () => {
  if (route.path === '/original/library') {
    activeTab.value = 'library'
  } else if (route.path === '/original/upload') {
    activeTab.value = 'upload'
  } else {
    activeTab.value = 'upload' // 默认显示上传页面
  }
}

// 检查登录状态
onMounted(() => {
  setActiveTabFromRoute()
  if (!userStore.isLoggedIn) {
    authVisible.value = true
  }
})

// 监听路由变化
watch(() => route.path, () => {
  setActiveTabFromRoute()
})

// 处理标签页切换
const handleTabChange = (tabName: string) => {
  if (tabName === 'library' && libraryRef.value) {
    // 切换到曲库时刷新数据
    libraryRef.value.refreshData()
  }
}

// 处理上传成功
const handleUploadSuccess = () => {
  // 上传成功后可以切换到曲库页面查看新上传的歌曲
  activeTab.value = 'library'
  if (libraryRef.value) {
    libraryRef.value.refreshData()
  }
}
</script>

<style scoped>
.original-container {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.original-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

:deep(.el-tabs__header) {
  margin: 0;
  padding: 0 20px;
  flex-shrink: 0;
}

:deep(.el-tabs__content) {
  flex: 1;
  overflow: hidden;
  padding: 0;
}

:deep(.el-tab-pane) {
  height: 100%;
  overflow: hidden;
}

.upload-section {
  width: 100%;
  height: 100%;
  overflow-y: auto;
  overflow-x: hidden;
}

.library-section {
  width: 100%;
  height: 100%;
  overflow: hidden;
}
</style>
