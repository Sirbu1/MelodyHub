<script setup lang="ts">
import { ref, reactive, watch, onMounted, onActivated, onUnmounted, onBeforeUnmount } from 'vue'
import { getForumPosts, addForumPostWithFile, deleteForumPost, likeForumPost, cancelLikeForumPost } from '@/api/system'
import { ElNotification, ElMessageBox } from 'element-plus'
import { UserStore } from '@/stores/modules/user'
import userAvatar from '@/assets/user.jpg'

const router = useRouter()
const route = useRoute()
const userStore = UserStore()

// 帖子列表
const posts = ref<any[]>([])
// 搜索关键词
const searchKeyword = ref('')
// 当前模块：'discussion'-交流，'requirement'-需求
const activeModule = ref<'discussion' | 'requirement'>('discussion')
// 是否显示发帖对话框
const showPostDialog = ref(false)
// 新帖子表单
const newPost = reactive({
  title: '',
  content: '',
  type: 0, // 0-交流，1-需求
  // 需求相关字段
  requirementType: '',
  timeRequirement: '',
  budget: '',
  styleDescription: '',
  referenceAttachmentFile: null as File | null
})
// 参考附件文件引用
const referenceAttachmentInput = ref<HTMLInputElement | null>(null)
// 提交中
const submitting = ref(false)

// 分页组件状态
const currentPage = ref(1)
const pageSize = ref(10)
const state = reactive({
  size: 'default',
  disabled: false,
  background: false,
  layout: 'total, sizes, prev, pager, next, jumper',
  total: 0,
  pageSizes: [10, 20, 30, 50],
})

// 监听分页变化
const handleSizeChange = () => {
  getPosts()
}
const handleCurrentChange = () => {
  getPosts()
}

// 获取帖子列表
const getPosts = async () => {
  try {
    const params = {
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      keyword: searchKeyword.value || null,
      type: activeModule.value === 'discussion' ? 0 : 1, // 0-交流，1-需求
    }
    const res = await getForumPosts(params)
    if (res.code === 0) {
      posts.value = res.data.items
      state.total = res.data.total
    } else {
      ElNotification({
        type: 'error',
        message: '获取帖子列表失败',
        duration: 2000,
      })
    }
  } catch (error) {
    ElNotification({
      type: 'error',
      message: '获取帖子列表失败',
      duration: 2000,
    })
  }
}

// 处理搜索
const handleSearch = () => {
  currentPage.value = 1
  getPosts()
}

// 处理搜索框按下回车
const handleKeyPress = (e: KeyboardEvent) => {
  if (e.key === 'Enter') {
    handleSearch()
  }
}

// 切换模块
const switchModule = (module: 'discussion' | 'requirement') => {
  activeModule.value = module
  currentPage.value = 1
  newPost.type = module === 'discussion' ? 0 : 1
  getPosts()
}

// 打开发帖对话框
const openPostDialog = () => {
  if (!userStore.userInfo?.token) {
    ElNotification({
      type: 'warning',
      message: '请先登录后再发帖',
      duration: 2000,
    })
    return
  }
  // 根据当前模块设置类型
  newPost.type = activeModule.value === 'discussion' ? 0 : 1
  showPostDialog.value = true
}

// 关闭发帖对话框
const closePostDialog = () => {
  showPostDialog.value = false
  newPost.title = ''
  newPost.content = ''
  newPost.requirementType = ''
  newPost.timeRequirement = ''
  newPost.budget = ''
  newPost.styleDescription = ''
  newPost.referenceAttachmentFile = null
  if (referenceAttachmentInput.value) {
    referenceAttachmentInput.value.value = ''
  }
}

// 处理参考附件文件选择
const handleReferenceAttachmentChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  if (target.files && target.files[0]) {
    newPost.referenceAttachmentFile = target.files[0]
  }
}

// 提交帖子
const submitPost = async () => {
  if (!newPost.title.trim()) {
    ElNotification({
      type: 'warning',
      message: activeModule.value === 'discussion' ? '请输入帖子标题' : '请输入需求标题',
      duration: 2000,
    })
    return
  }
  if (!newPost.content.trim()) {
    ElNotification({
      type: 'warning',
      message: activeModule.value === 'discussion' ? '请输入帖子内容' : '请输入需求内容',
      duration: 2000,
    })
    return
  }

  // 如果是需求，验证必填字段
  if (activeModule.value === 'requirement') {
    if (!newPost.requirementType.trim()) {
      ElNotification({
        type: 'warning',
        message: '请选择需求类型',
        duration: 2000,
      })
      return
    }
  }

  submitting.value = true
  try {
    // 统一使用FormData上传
    const formData = new FormData()
    formData.append('title', newPost.title.trim())
    formData.append('content', newPost.content.trim())
    formData.append('type', String(newPost.type))
    
    // 如果是需求，添加需求相关字段
    if (activeModule.value === 'requirement') {
      formData.append('requirementType', newPost.requirementType.trim())
      if (newPost.timeRequirement.trim()) {
        formData.append('timeRequirement', newPost.timeRequirement.trim())
      }
      if (newPost.budget.trim()) {
        formData.append('budget', newPost.budget.trim())
      }
      if (newPost.styleDescription.trim()) {
        formData.append('styleDescription', newPost.styleDescription.trim())
      }
    }
    
    // 附件上传（交流和需求都可以）
    if (newPost.referenceAttachmentFile) {
      formData.append('referenceAttachmentFile', newPost.referenceAttachmentFile)
    }
    
    const res = await addForumPostWithFile(formData)
    if (res.code === 0) {
      ElNotification({
        type: 'success',
        message: (activeModule.value === 'discussion' ? '发帖成功' : '发布需求成功') + '，您的帖子已提交审核，审核通过后将在论坛中显示。',
        duration: 3000,
      })
      closePostDialog()
      getPosts()
    } else {
      ElNotification({
        type: 'error',
        message: res.message || (activeModule.value === 'discussion' ? '发帖失败' : '发布需求失败'),
        duration: 2000,
      })
    }
  } catch (error) {
    ElNotification({
      type: 'error',
      message: activeModule.value === 'discussion' ? '发帖失败' : '发布需求失败',
      duration: 2000,
    })
  } finally {
    submitting.value = false
  }
}

// 跳转到帖子详情
const goToPost = (postId: number) => {
  router.push(`/forum/${postId}`)
}

// 点赞帖子
const handleLike = async (post: any, e: Event) => {
  e.stopPropagation()
  if (!userStore.userInfo?.token) {
    ElNotification({
      type: 'warning',
      message: '请先登录后再点赞',
      duration: 2000,
    })
    return
  }

  try {
    const res = post.liked 
      ? await cancelLikeForumPost(post.postId)
      : await likeForumPost(post.postId)
    
    if (res.code === 0) {
      post.liked = !post.liked
      post.likeCount = post.liked ? post.likeCount + 1 : post.likeCount - 1
    }
  } catch (error) {
    console.error('点赞失败', error)
  }
}

// 删除帖子
const handleDelete = async (post: any, e: Event) => {
  e.stopPropagation()
  
  try {
    await ElMessageBox.confirm('确定要删除这篇帖子吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    
    const res = await deleteForumPost(post.postId)
    if (res.code === 0) {
      ElNotification({
        type: 'success',
        message: '删除成功',
        duration: 2000,
      })
      getPosts()
    } else {
      ElNotification({
        type: 'error',
        message: res.message || '删除失败',
        duration: 2000,
      })
    }
  } catch (error) {
    // 用户取消删除
  }
}

// 格式化时间
const formatTime = (time: string) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`
  
  return time.substring(0, 10)
}

// 判断是否是当前用户的帖子
const isOwnPost = (post: any) => {
  return userStore.userInfo?.userId === post.userId
}

// 更新指定帖子的浏览量
const updatePostViewCount = (postId: number, viewCount: number) => {
  const post = posts.value.find(p => p.postId === postId)
  if (post) {
    post.viewCount = viewCount
  }
}

// 从路由参数中获取更新的浏览量并更新列表
const checkAndUpdateViewCount = () => {
  // 检查是否有从详情页返回时传递的浏览量更新
  if (route.query.updatedPostId && route.query.updatedViewCount) {
    const postId = Number(route.query.updatedPostId)
    const viewCount = Number(route.query.updatedViewCount)
    if (postId && !isNaN(postId) && viewCount && !isNaN(viewCount)) {
      updatePostViewCount(postId, viewCount)
      // 清除查询参数
      router.replace({ query: {} })
    }
  }
}

// 定时刷新间隔（30秒）
const REFRESH_INTERVAL = 30000
// 定时器引用
let refreshTimer: number | null = null

// 启动定时刷新（用于检测审核通过的帖子）
const startAutoRefresh = () => {
  // 清除已存在的定时器
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
  // 每30秒自动刷新一次，以检测新审核通过的帖子
  refreshTimer = window.setInterval(() => {
    // 只在页面可见时刷新
    if (document.visibilityState === 'visible') {
      getPosts()
    }
  }, REFRESH_INTERVAL)
}

// 停止定时刷新
const stopAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
  refreshTimer = null
}

// 监听页面可见性变化
const handleVisibilityChange = () => {
  if (document.visibilityState === 'visible') {
    // 页面变为可见时，立即刷新一次，然后启动定时刷新
    getPosts()
    startAutoRefresh()
  } else {
    // 页面不可见时，停止定时刷新以节省资源
    stopAutoRefresh()
  }
}

// 监听路由变化，当从详情页返回时更新浏览量和刷新列表
watch(() => route.path, (newPath, oldPath) => {
  // 如果从详情页返回到列表页
  if (newPath === '/forum' && oldPath && oldPath.startsWith('/forum/')) {
    checkAndUpdateViewCount()
    // 刷新列表以显示最新数据（如新回复数、点赞数等）
    getPosts()
  }
})

onMounted(() => {
  // 根据路由参数设置模块
  if (route.query.module === 'requirement') {
    activeModule.value = 'requirement'
    newPost.type = 1
  } else {
    activeModule.value = 'discussion'
    newPost.type = 0
  }
  getPosts()
  checkAndUpdateViewCount()
  // 启动定时刷新
  startAutoRefresh()
  // 监听页面可见性变化
  document.addEventListener('visibilitychange', handleVisibilityChange)
})

// 当页面被激活时（从详情页返回），检查并更新浏览量，同时刷新列表
onActivated(() => {
  checkAndUpdateViewCount()
  // 刷新列表以显示最新数据
  getPosts()
  // 启动定时刷新
  startAutoRefresh()
})

// 组件卸载时清理
onBeforeUnmount(() => {
  stopAutoRefresh()
  document.removeEventListener('visibilitychange', handleVisibilityChange)
})
</script>

<template>
  <div class="flex flex-col h-full flex-1 overflow-hidden bg-background px-4 py-2">
    <!-- 导航栏：切换模块 -->
    <div class="py-4 border-b border-border">
      <div class="flex items-center gap-2">
        <button
          @click="switchModule('discussion')"
          :class="[
            'px-6 py-2 rounded-lg text-sm font-medium transition-all duration-300',
            activeModule === 'discussion'
              ? 'bg-primary text-primary-foreground shadow-md'
              : 'bg-transparent text-muted-foreground hover:text-foreground hover:bg-accent'
          ]"
        >
          <icon-mdi:forum-outline class="inline mr-2" />
          交流
        </button>
        <button
          @click="switchModule('requirement')"
          :class="[
            'px-6 py-2 rounded-lg text-sm font-medium transition-all duration-300',
            activeModule === 'requirement'
              ? 'bg-primary text-primary-foreground shadow-md'
              : 'bg-transparent text-muted-foreground hover:text-foreground hover:bg-accent'
          ]"
        >
          <icon-mdi:lightbulb-outline class="inline mr-2" />
          需求
        </button>
      </div>
    </div>

    <!-- 头部：搜索和发帖按钮 -->
    <div class="py-4">
      <div class="flex flex-col sm:flex-row gap-4 items-center justify-between">
        <div class="relative flex-grow max-w-md">
          <icon-mdi:magnify
            class="lucide lucide-search absolute left-3 top-1/2 transform -translate-y-1/2 text-muted-foreground" />
          <input 
            v-model="searchKeyword" 
            @keydown="handleKeyPress"
            class="flex h-10 w-full rounded-lg border border-input transform duration-300 bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-0 pl-10"
            :placeholder="activeModule === 'discussion' ? '搜索帖子...' : '搜索需求...'" 
            type="search" 
          />
        </div>
        <button 
          @click="openPostDialog"
          class="inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-lg text-sm font-medium transition-colors bg-primary text-primary-foreground hover:bg-primary/90 h-10 px-6 shadow-md hover:shadow-lg"
        >
          <icon-mdi:plus class="w-5 h-5" />
          {{ activeModule === 'discussion' ? '发布帖子' : '发布需求' }}
        </button>
      </div>
    </div>

    <!-- 帖子列表 -->
    <div class="flex-1 overflow-y-auto">
      <div class="space-y-4">
        <div 
          v-for="post in posts" 
          :key="post.postId"
          @click="goToPost(post.postId)"
          class="bg-card rounded-xl border border-border p-5 cursor-pointer transition-all duration-300 hover:shadow-lg hover:border-primary/30 group"
        >
          <!-- 帖子头部 -->
          <div class="flex items-start justify-between mb-3">
            <div class="flex items-center gap-3">
              <el-avatar 
                :size="42" 
                :src="post.userAvatar || userAvatar"
                class="ring-2 ring-primary/20"
              />
              <div>
                <div class="font-medium text-foreground">{{ post.username || '匿名用户' }}</div>
                <div class="text-xs text-muted-foreground">{{ formatTime(post.createTime) }}</div>
              </div>
            </div>
            <!-- 置顶标签 -->
            <div v-if="post.isTop === 1" class="px-2 py-1 bg-primary/10 text-primary text-xs rounded-full font-medium">
              置顶
            </div>
          </div>

          <!-- 帖子标题 -->
          <h3 class="text-lg font-semibold text-foreground mb-2 line-clamp-2 group-hover:text-primary transition-colors">
            {{ post.title }}
          </h3>

          <!-- 帖子内容预览 -->
          <p class="text-muted-foreground text-sm line-clamp-3 mb-4">
            {{ post.content }}
          </p>

          <!-- 帖子底部统计 -->
          <div class="flex items-center justify-between text-sm text-muted-foreground">
            <div class="flex items-center gap-5">
              <span class="flex items-center gap-1.5">
                <icon-mdi:eye-outline class="w-4 h-4" />
                {{ post.viewCount || 0 }}
              </span>
              <span class="flex items-center gap-1.5">
                <icon-mdi:comment-outline class="w-4 h-4" />
                {{ post.replyCount || 0 }}
              </span>
              <button 
                @click="handleLike(post, $event)"
                class="flex items-center gap-1.5 hover:text-primary transition-colors"
                :class="{ 'text-primary': post.liked }"
              >
                <icon-mdi:thumb-up v-if="post.liked" class="w-4 h-4" />
                <icon-mdi:thumb-up-outline v-else class="w-4 h-4" />
                {{ post.likeCount || 0 }}
              </button>
            </div>
            <!-- 删除按钮（仅作者可见） -->
            <button 
              v-if="isOwnPost(post)"
              @click="handleDelete(post, $event)"
              class="flex items-center gap-1 text-muted-foreground hover:text-red-500 transition-colors"
            >
              <icon-mdi:delete-outline class="w-4 h-4" />
              删除
            </button>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-if="posts.length === 0" class="flex flex-col items-center justify-center py-20 text-muted-foreground">
          <icon-mdi:forum-outline v-if="activeModule === 'discussion'" class="w-16 h-16 mb-4 opacity-50" />
          <icon-mdi:lightbulb-outline v-else class="w-16 h-16 mb-4 opacity-50" />
          <p class="text-lg">{{ activeModule === 'discussion' ? '暂无帖子' : '暂无需求' }}</p>
          <p class="text-sm mt-1">{{ activeModule === 'discussion' ? '成为第一个发帖的人吧！' : '成为第一个发布需求的人吧！' }}</p>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <nav class="mx-auto flex w-full justify-center mt-3">
      <el-pagination 
        v-model:page-size="pageSize" 
        v-model:currentPage="currentPage" 
        v-bind="state"
        @size-change="handleSizeChange" 
        @current-change="handleCurrentChange" 
        class="mb-3" 
      />
    </nav>

    <!-- 发帖对话框 -->
    <el-dialog 
      v-model="showPostDialog" 
      :title="activeModule === 'discussion' ? '发布新帖子' : '发布新需求'" 
      width="600px"
      :close-on-click-modal="false"
      class="forum-dialog"
    >
      <div class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-foreground mb-2">
            {{ activeModule === 'discussion' ? '帖子标题' : '需求标题' }}
            <span v-if="activeModule === 'requirement'" class="text-red-500">*</span>
          </label>
          <input 
            v-model="newPost.title"
            class="flex h-10 w-full rounded-lg border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-0"
            :placeholder="activeModule === 'discussion' ? '请输入帖子标题（最多200字）' : '请输入需求标题（最多200字）'"
            maxlength="200"
          />
        </div>

        <!-- 需求类型（仅需求模块显示） -->
        <div v-if="activeModule === 'requirement'">
          <label class="block text-sm font-medium text-foreground mb-2">
            需求类型 <span class="text-red-500">*</span>
          </label>
          <select 
            v-model="newPost.requirementType"
            class="flex h-10 w-full rounded-lg border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-0"
          >
            <option value="">请选择需求类型</option>
            <option value="作曲">作曲</option>
            <option value="编曲">编曲</option>
            <option value="混音">混音</option>
            <option value="作词">作词</option>
            <option value="录音">录音</option>
            <option value="母带">母带</option>
            <option value="其他">其他</option>
          </select>
        </div>

        <!-- 时间要求（仅需求模块显示） -->
        <div v-if="activeModule === 'requirement'">
          <label class="block text-sm font-medium text-foreground mb-2">
            时间要求
          </label>
          <input 
            v-model="newPost.timeRequirement"
            class="flex h-10 w-full rounded-lg border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-0"
            placeholder="例如：1周内、1个月内等"
            maxlength="100"
          />
        </div>

        <!-- 预算（仅需求模块显示） -->
        <div v-if="activeModule === 'requirement'">
          <label class="block text-sm font-medium text-foreground mb-2">
            预算
          </label>
          <input 
            v-model="newPost.budget"
            class="flex h-10 w-full rounded-lg border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-0"
            placeholder="例如：500-1000元、面议等"
            maxlength="50"
          />
        </div>

        <!-- 风格描述（仅需求模块显示） -->
        <div v-if="activeModule === 'requirement'">
          <label class="block text-sm font-medium text-foreground mb-2">
            风格描述
          </label>
          <textarea 
            v-model="newPost.styleDescription"
            class="flex min-h-[100px] w-full rounded-lg border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-0 resize-none"
            placeholder="请描述您期望的音乐风格、参考作品等..."
            maxlength="2000"
          />
        </div>

        <!-- 附件上传（交流和需求都可以上传） -->
        <div>
          <label class="block text-sm font-medium text-foreground mb-2">
            {{ activeModule === 'discussion' ? '附件' : '参考附件' }}
            <span class="text-xs text-muted-foreground">（可选）</span>
          </label>
          <input 
            ref="referenceAttachmentInput"
            type="file"
            @change="handleReferenceAttachmentChange"
            accept=".mp3,.wav,.jpg,.jpeg,.png,.pdf,.doc,.docx"
            class="flex h-10 w-full rounded-lg border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-0"
          />
          <p class="text-xs text-muted-foreground mt-1">支持音频、图片、文档等格式</p>
          <p v-if="newPost.referenceAttachmentFile" class="text-xs text-primary mt-1">
            已选择：{{ newPost.referenceAttachmentFile.name }}
          </p>
        </div>

        <div>
          <label class="block text-sm font-medium text-foreground mb-2">
            {{ activeModule === 'discussion' ? '帖子内容' : '需求内容' }}
            <span class="text-red-500">*</span>
          </label>
          <textarea 
            v-model="newPost.content"
            class="flex min-h-[200px] w-full rounded-lg border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-0 resize-none"
            :placeholder="activeModule === 'discussion' ? '请输入帖子内容...' : '请详细描述您的需求...'"
            maxlength="10000"
          />
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-3">
          <button 
            @click="closePostDialog"
            class="inline-flex items-center justify-center rounded-lg text-sm font-medium transition-colors border border-input bg-background hover:bg-accent hover:text-accent-foreground h-10 px-4"
          >
            取消
          </button>
          <button 
            @click="submitPost"
            :disabled="submitting"
            class="inline-flex items-center justify-center rounded-lg text-sm font-medium transition-colors bg-primary text-primary-foreground hover:bg-primary/90 h-10 px-6 disabled:opacity-50"
          >
            {{ submitting ? '发布中...' : '发布' }}
          </button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.line-clamp-3 {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>

