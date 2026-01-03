<script setup lang="ts">
import { ref, watch, onMounted, onActivated, onBeforeUnmount } from 'vue'
import { 
  getForumPostDetail, 
  getForumReplies, 
  addForumReply, 
  deleteForumPost, 
  deleteForumReply,
  likeForumPost,
  cancelLikeForumPost,
  likeForumReply,
  cancelLikeForumReply,
  updateForumPostAcceptStatus,
  applyOrder,
  acceptOrder,
  rejectOrder,
  completeOrder
} from '@/api/system'
import { ElNotification, ElMessageBox } from 'element-plus'
import { UserStore } from '@/stores/modules/user'
import userAvatar from '@/assets/user.jpg'

const route = useRoute()
const router = useRouter()
const userStore = UserStore()

// 帖子详情
const post = ref<any>(null)
// 帖子详情（包含接单申请列表）
const postDetailWithOrders = ref<any>(null)
// 接单申请列表（仅需求发布者可见）
const orderApplications = ref<any[]>([])
// 当前用户是否已申请接单
const hasApplied = ref(false)
// 当前用户的接单信息（如果是接单者）
const myOrder = ref<any>(null)
// 回复列表
const replies = ref<any[]>([])
// 加载状态
const loading = ref(true)
// 回复内容
const replyContent = ref('')
// 回复中的父回复
const replyTo = ref<any>(null)
// 提交中
const submitting = ref(false)

// 分页
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

// 获取帖子详情
const getPostDetail = async () => {
  try {
    const postId = Number(route.params.id)
    // 如果 postId 无效，直接返回
    if (!postId || isNaN(postId)) {
      return
    }
    
    // 先清空旧数据，避免显示上一个帖子的数据
    orderApplications.value = []
    hasApplied.value = false
    myOrder.value = null
    postDetailWithOrders.value = null
    post.value = null
    
    const res = await getForumPostDetail(postId)
    if (res.code === 0) {
      // 检查返回的数据结构
      if (res.data && res.data.postDetail) {
        // 新格式：包含接单申请列表
        // 后端已经按 postId 过滤了，直接使用返回的数据
        postDetailWithOrders.value = res.data
        post.value = res.data.postDetail
        orderApplications.value = res.data.orderApplications || []
        hasApplied.value = res.data.hasApplied || false
        myOrder.value = res.data.myOrder || null
        
        // 调试日志
        console.log('帖子详情加载完成:', {
          postId: post.value.postId,
          userId: post.value.userId,
          currentUserId: userStore.userInfo?.userId,
          isOwnContent: isOwnContent(post.value.userId),
          orderApplicationsCount: orderApplications.value.length,
          orderApplications: orderApplications.value
        })
      } else {
        // 旧格式：直接是帖子详情
        post.value = res.data
        postDetailWithOrders.value = null
        orderApplications.value = []
        hasApplied.value = false
        myOrder.value = null
      }
    } else {
      ElNotification({
        type: 'error',
        message: '帖子不存在或已被删除',
        duration: 2000,
      })
      router.push('/forum')
    }
  } catch (error) {
    // 如果是路由切换导致的错误，不显示错误提示
    if (route.path === '/forum') {
      return
    }
    ElNotification({
      type: 'error',
      message: '获取帖子详情失败',
      duration: 2000,
    })
  }
}

// 获取回复列表
const getReplies = async () => {
  try {
    const postId = Number(route.params.id)
    // 如果 postId 无效，直接返回
    if (!postId || isNaN(postId)) {
      loading.value = false
      return
    }
    const res = await getForumReplies({
      postId,
      pageNum: currentPage.value,
      pageSize: pageSize.value,
    })
    if (res.code === 0) {
      replies.value = res.data.items
      total.value = res.data.total
    }
  } catch (error) {
    // 如果是路由切换导致的错误，不显示错误提示
    if (route.path === '/forum') {
      loading.value = false
      return
    }
    console.error('获取回复列表失败', error)
  } finally {
    loading.value = false
  }
}

// 发布回复
const submitReply = async () => {
  if (!userStore.userInfo?.token) {
    ElNotification({
      type: 'warning',
      message: '请先登录后再回复',
      duration: 2000,
    })
    return
  }

  // 检查用户积分
  const userScore = userStore.userInfo?.score ?? 100
  if (userScore <= 0) {
    ElNotification({
      type: 'error',
      message: '当前账号无发布权限，积分不足（积分为0时无法发帖、发歌、回复）',
      duration: 3000,
    })
    return
  }

  if (!replyContent.value.trim()) {
    ElNotification({
      type: 'warning',
      message: '请输入回复内容',
      duration: 2000,
    })
    return
  }

  submitting.value = true
  try {
    const postId = Number(route.params.id)
    const data: any = {
      postId,
      content: replyContent.value.trim(),
    }
    
    if (replyTo.value) {
      data.parentId = replyTo.value.replyId
    }

    const res = await addForumReply(data)
    if (res.code === 0) {
      ElNotification({
        type: 'success',
        message: '回复成功，您的回复已提交审核，审核通过后将在帖子中显示。',
        duration: 2000,
      })
      replyContent.value = ''
      replyTo.value = null
      getReplies()
      // 更新帖子回复数
      if (post.value) {
        post.value.replyCount = (post.value.replyCount || 0) + 1
      }
    } else {
      ElNotification({
        type: 'error',
        message: res.message || '回复失败',
        duration: 2000,
      })
    }
  } catch (error) {
    ElNotification({
      type: 'error',
      message: '回复失败',
      duration: 2000,
    })
  } finally {
    submitting.value = false
  }
}

// 回复某人
const handleReplyTo = (reply: any) => {
  if (!userStore.userInfo?.token) {
    ElNotification({
      type: 'warning',
      message: '请先登录后再回复',
      duration: 2000,
    })
    return
  }
  
  // 检查用户积分
  const userScore = userStore.userInfo?.score ?? 100
  if (userScore <= 0) {
    ElNotification({
      type: 'error',
      message: '当前账号无发布权限，积分不足（积分为0时无法发帖、发歌、回复）',
      duration: 3000,
    })
    return
  }
  
  replyTo.value = reply
  // 滚动到回复框
  document.querySelector('.reply-input-area')?.scrollIntoView({ behavior: 'smooth' })
}

// 取消回复
const cancelReplyTo = () => {
  replyTo.value = null
}

// 点赞帖子
const handleLikePost = async () => {
  if (!userStore.userInfo?.token) {
    ElNotification({
      type: 'warning',
      message: '请先登录后再点赞',
      duration: 2000,
    })
    return
  }

  try {
    const res = post.value.liked 
      ? await cancelLikeForumPost(post.value.postId)
      : await likeForumPost(post.value.postId)
    
    if (res.code === 0) {
      post.value.liked = !post.value.liked
      post.value.likeCount = post.value.liked ? post.value.likeCount + 1 : post.value.likeCount - 1
    }
  } catch (error) {
    console.error('点赞失败', error)
  }
}

// 点赞回复
const handleLikeReply = async (reply: any) => {
  if (!userStore.userInfo?.token) {
    ElNotification({
      type: 'warning',
      message: '请先登录后再点赞',
      duration: 2000,
    })
    return
  }

  try {
    const res = reply.liked 
      ? await cancelLikeForumReply(reply.replyId)
      : await likeForumReply(reply.replyId)
    
    if (res.code === 0) {
      reply.liked = !reply.liked
      reply.likeCount = reply.liked ? reply.likeCount + 1 : reply.likeCount - 1
    }
  } catch (error) {
    console.error('点赞失败', error)
  }
}

// 删除帖子
const handleDeletePost = async () => {
  try {
    await ElMessageBox.confirm('确定要删除这篇帖子吗？删除后不可恢复', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    
    const res = await deleteForumPost(post.value.postId)
    if (res.code === 0) {
      ElNotification({
        type: 'success',
        message: '删除成功',
        duration: 2000,
      })
      router.push('/forum')
    } else {
      ElNotification({
        type: 'error',
        message: res.message || '删除失败',
        duration: 2000,
      })
    }
  } catch (error) {
    // 用户取消
  }
}

// 删除回复
const handleDeleteReply = async (reply: any) => {
  try {
    await ElMessageBox.confirm('确定要删除这条回复吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    
    const res = await deleteForumReply(reply.replyId)
    if (res.code === 0) {
      ElNotification({
        type: 'success',
        message: '删除成功',
        duration: 2000,
      })
      getReplies()
      // 更新帖子回复数
      if (post.value) {
        post.value.replyCount = Math.max(0, (post.value.replyCount || 0) - 1)
      }
    } else {
      ElNotification({
        type: 'error',
        message: res.message || '删除失败',
        duration: 2000,
      })
    }
  } catch (error) {
    // 用户取消
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
  
  return time.substring(0, 16).replace('T', ' ')
}

// 判断是否是当前用户
const isOwnContent = (userId: number) => {
  return userStore.userInfo?.userId === userId
}

// 申请接单
const handleApplyOrder = async () => {
  if (!post.value || !userStore.userInfo?.token) {
    ElNotification({
      type: 'warning',
      message: '请先登录',
      duration: 2000,
    })
    return
  }

  try {
    const res = await applyOrder(post.value.postId)
    if (res.code === 0) {
      hasApplied.value = true
      ElNotification({
        type: 'success',
        message: res.message || '接单申请已提交，等待需求发布者同意',
        duration: 3000,
      })
      // 重新获取帖子详情
      await getPostDetail()
      // 重新启动定时刷新，根据新的接单状态调整刷新间隔（申请后状态变为待同意，需要频繁刷新）
      startAutoRefresh()
    } else {
      ElNotification({
        type: 'error',
        message: res.message || '申请接单失败',
        duration: 2000,
      })
    }
  } catch (error: any) {
    ElNotification({
      type: 'error',
      message: error.response?.data?.message || '申请接单失败',
      duration: 2000,
    })
  }
}

// 同意接单
const handleAcceptOrder = async (orderId: number) => {
  try {
    await ElMessageBox.confirm('确定同意该用户的接单申请吗？', '确认同意', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info',
    })

    const res = await acceptOrder(orderId)
    if (res.code === 0) {
      ElNotification({
        type: 'success',
        message: res.message || '已同意接单',
        duration: 2000,
      })
      // 重新获取帖子详情
      await getPostDetail()
      // 重新启动定时刷新，根据新的接单状态调整刷新间隔
      startAutoRefresh()
    } else {
      ElNotification({
        type: 'error',
        message: res.message || '同意接单失败',
        duration: 2000,
      })
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElNotification({
        type: 'error',
        message: error.response?.data?.message || '同意接单失败',
        duration: 2000,
      })
    }
  }
}

// 拒绝接单
const handleRejectOrder = async (orderId: number) => {
  if (!orderId) {
    ElNotification({
      type: 'error',
      message: '接单ID无效',
      duration: 2000,
    })
    return
  }

  try {
    await ElMessageBox.confirm('确定拒绝该用户的接单申请吗？', '确认拒绝', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })

    console.log('拒绝接单，orderId:', orderId)
    const res = await rejectOrder(orderId)
    console.log('拒绝接单响应:', res)
    
    if (res.code === 0) {
      ElNotification({
        type: 'success',
        message: res.message || '已拒绝接单申请',
        duration: 2000,
      })
      // 重新获取帖子详情
      await getPostDetail()
      // 重新启动定时刷新，根据新的接单状态调整刷新间隔
      startAutoRefresh()
    } else {
      ElNotification({
        type: 'error',
        message: res.message || '拒绝接单失败',
        duration: 2000,
      })
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('拒绝接单错误:', error)
      ElNotification({
        type: 'error',
        message: error.response?.data?.message || error.message || '拒绝接单失败',
        duration: 2000,
      })
    }
  }
}

// 标记为已完成
const handleCompleteOrder = async (orderId: number) => {
  try {
    await ElMessageBox.confirm('确定标记该接单为已完成吗？', '确认完成', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info',
    })

    const res = await completeOrder(orderId)
    if (res.code === 0) {
      ElNotification({
        type: 'success',
        message: res.message || '已标记为已完成',
        duration: 2000,
      })
      // 重新获取帖子详情
      await getPostDetail()
      // 重新启动定时刷新，根据新的接单状态调整刷新间隔
      startAutoRefresh()
    } else {
      ElNotification({
        type: 'error',
        message: res.message || '标记失败',
        duration: 2000,
      })
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElNotification({
        type: 'error',
        message: error.response?.data?.message || '标记失败',
        duration: 2000,
      })
    }
  }
}

// 获取文件名
const getFileName = (url: string) => {
  if (!url) return '附件'
  try {
    const urlObj = new URL(url)
    const pathname = urlObj.pathname
    const fileName = pathname.substring(pathname.lastIndexOf('/') + 1)
    // 移除UUID前缀（如果有）
    const parts = fileName.split('-')
    if (parts.length > 1) {
      return parts.slice(1).join('-')
    }
    return fileName || '附件'
  } catch {
    return url.substring(url.lastIndexOf('/') + 1) || '附件'
  }
}

// 判断是否是图片文件
const isImageFile = (url: string) => {
  if (!url) return false
  const imageExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.webp', '.bmp']
  const lowerUrl = url.toLowerCase()
  return imageExtensions.some(ext => lowerUrl.includes(ext))
}

// 判断是否是音频文件
const isAudioFile = (url: string) => {
  if (!url) return false
  const audioExtensions = ['.mp3', '.wav', '.ogg', '.m4a', '.flac', '.aac']
  const lowerUrl = url.toLowerCase()
  return audioExtensions.some(ext => lowerUrl.includes(ext))
}

// 处理图片加载错误
const handleImageError = (event: Event) => {
  const img = event.target as HTMLImageElement
  img.style.display = 'none'
}

// 返回列表
const goBack = () => {
  // 如果帖子已加载，传递更新后的浏览量
  if (post.value) {
    router.push({
      path: '/forum',
      query: {
        updatedPostId: post.value.postId,
        updatedViewCount: post.value.viewCount
      }
    })
  } else {
    router.push('/forum')
  }
}

// 分页变化
const handlePageChange = () => {
  getReplies()
}

// 加载数据
const loadData = () => {
  loading.value = true
  getPostDetail()
  getReplies()
}

// 监听路由参数变化
watch(() => route.params.id, (newId, oldId) => {
  // 只有当新ID有效且与旧ID不同，且当前路由仍然是帖子详情页时才加载数据
  if (newId !== oldId && newId !== undefined && newId !== null && route.path.startsWith('/forum/') && route.path !== '/forum') {
    // 重置状态
    post.value = null
    postDetailWithOrders.value = null
    orderApplications.value = []
    hasApplied.value = false
    myOrder.value = null
    replies.value = []
    currentPage.value = 1
    replyContent.value = ''
    replyTo.value = null
    // 重新加载数据
    loadData()
  }
})

// 定时刷新间隔（30秒）
const REFRESH_INTERVAL = 30000
// 接单状态检测刷新间隔（5秒）- 当有待同意的接单时使用
const ORDER_STATUS_REFRESH_INTERVAL = 5000
// 定时器引用
let refreshTimer: number | null = null
// 当前使用的刷新间隔（用于跟踪）
let currentRefreshInterval: number | null = null

// 检查是否有待同意的接单
const hasPendingOrder = () => {
  // 如果当前用户已申请接单，且状态为待同意（0），需要频繁刷新
  const hasPending = hasApplied.value && myOrder.value && myOrder.value.status === 0
  console.log('检查待同意接单状态:', {
    hasApplied: hasApplied.value,
    myOrder: myOrder.value,
    status: myOrder.value?.status,
    hasPending
  })
  return hasPending
}

// 启动定时刷新（用于检测审核通过的回复和接单状态变化）
const startAutoRefresh = () => {
  // 清除已存在的定时器
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
  
  // 刷新函数
  const doRefresh = async () => {
    // 只在页面可见时刷新
    if (document.visibilityState === 'visible') {
      try {
        console.log('开始自动刷新...')
        // 同时刷新回复和帖子详情
        await Promise.all([getReplies(), getPostDetail()])
        console.log('自动刷新完成')
        
        // 刷新后重新检查状态，决定是否需要调整刷新间隔
        const shouldUseShortInterval = hasPendingOrder()
        const expectedInterval = shouldUseShortInterval ? ORDER_STATUS_REFRESH_INTERVAL : REFRESH_INTERVAL
        
        console.log('刷新后检查状态:', {
          currentInterval: currentRefreshInterval,
          expectedInterval,
          shouldUseShortInterval,
          hasApplied: hasApplied.value,
          myOrderStatus: myOrder.value?.status,
          needRestart: currentRefreshInterval !== null && currentRefreshInterval !== expectedInterval
        })
        
        // 如果状态变化导致需要改变刷新间隔，重新启动定时器
        if (currentRefreshInterval !== null && currentRefreshInterval !== expectedInterval) {
          console.log('接单状态变化，重新启动定时器，新间隔:', expectedInterval, 'ms')
          // 间隔需要改变，重新启动定时器
          startAutoRefresh()
          return
        }
      } catch (error) {
        console.error('自动刷新失败:', error)
      }
    } else {
      console.log('页面不可见，跳过刷新')
    }
  }
  
  // 根据是否有待同意的接单，选择不同的刷新间隔
  const interval = hasPendingOrder() ? ORDER_STATUS_REFRESH_INTERVAL : REFRESH_INTERVAL
  currentRefreshInterval = interval
  
  console.log('启动自动刷新，间隔:', interval, 'ms', hasPendingOrder() ? '(待同意接单，快速刷新)' : '(正常刷新)')
  
  // 立即执行一次刷新
  doRefresh()
  
  // 设置定时刷新
  refreshTimer = window.setInterval(doRefresh, interval)
}

// 停止定时刷新
const stopAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
  refreshTimer = null
  currentRefreshInterval = null
}

// 监听页面可见性变化
const handleVisibilityChange = () => {
  if (document.visibilityState === 'visible') {
    console.log('页面变为可见，启动刷新')
    // 页面变为可见时，立即刷新一次，然后启动定时刷新
    loadData()
    startAutoRefresh()
  } else {
    console.log('页面不可见，停止刷新')
    // 页面不可见时，停止定时刷新以节省资源
    stopAutoRefresh()
  }
}

onMounted(() => {
  loadData()
  // 启动定时刷新
  startAutoRefresh()
  // 监听页面可见性变化
  document.addEventListener('visibilitychange', handleVisibilityChange)
})

// 当页面被激活时（从其他页面返回），刷新数据
onActivated(() => {
  loadData()
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
  <div class="flex flex-col h-full flex-1 overflow-hidden bg-background">
    <!-- 顶部导航 -->
    <div class="sticky top-0 z-10 bg-background/95 backdrop-blur border-b border-border px-4 py-3">
      <div class="flex items-center gap-3">
        <button 
          @click="goBack"
          class="inline-flex items-center justify-center rounded-lg text-sm font-medium transition-colors hover:bg-accent hover:text-accent-foreground h-9 w-9"
        >
          <icon-mdi:arrow-left class="w-5 h-5" />
        </button>
        <span class="text-lg font-semibold text-foreground">帖子详情</span>
      </div>
    </div>

    <!-- 内容区域 -->
    <div class="flex-1 overflow-y-auto px-4 py-4">
      <!-- 加载状态 -->
      <div v-if="loading" class="flex justify-center py-20">
        <el-icon class="is-loading text-4xl text-primary">
          <icon-mdi:loading />
        </el-icon>
      </div>

      <template v-else-if="post">
        <!-- 帖子详情 -->
        <div class="bg-card rounded-xl border border-border p-6 mb-6">
          <!-- 帖子头部 -->
          <div class="flex items-start justify-between mb-4">
            <div class="flex items-center gap-3">
              <el-avatar 
                :size="48" 
                :src="post.userAvatar || userAvatar"
                class="ring-2 ring-primary/20"
              />
              <div>
                <div class="font-medium text-foreground text-lg">{{ post.username || '匿名用户' }}</div>
                <div class="text-sm text-muted-foreground">{{ formatTime(post.createTime) }}</div>
              </div>
            </div>
            <!-- 操作按钮 -->
            <div class="flex items-center gap-2">
              <div v-if="post.isTop === 1" class="px-3 py-1 bg-primary/10 text-primary text-xs rounded-full font-medium">
                置顶
              </div>
              <button 
                v-if="isOwnContent(post.userId)"
                @click="handleDeletePost"
                class="inline-flex items-center justify-center rounded-lg text-sm font-medium transition-colors hover:bg-red-500/10 hover:text-red-500 h-9 px-3 text-muted-foreground"
              >
                <icon-mdi:delete-outline class="w-4 h-4 mr-1" />
                删除
              </button>
            </div>
          </div>

          <!-- 帖子标题 -->
          <h1 class="text-2xl font-bold text-foreground mb-4">{{ post.title }}</h1>

          <!-- 帖子内容 -->
          <div class="text-foreground leading-relaxed whitespace-pre-wrap mb-6">{{ post.content }}</div>

          <!-- 需求信息（仅需求类型显示） -->
          <div v-if="post.type === 1" class="mb-6 p-4 bg-muted/50 rounded-lg space-y-3">
            <!-- 接单状态（仅需求发布者可见） -->
            <div v-if="isOwnContent(post.userId)" class="flex items-center justify-between pb-3 border-b border-border">
              <div class="flex items-center gap-2">
                <span class="text-sm font-medium text-muted-foreground">接单状态：</span>
                <span 
                  class="text-sm font-medium px-3 py-1 rounded-full"
                  :class="post.isAccepted === 1 ? 'bg-green-500/10 text-green-600 dark:text-green-400' : 'bg-orange-500/10 text-orange-600 dark:text-orange-400'"
                >
                  {{ post.isAccepted === 1 ? '已接单' : '未接单' }}
                </span>
              </div>
            </div>
            <!-- 接单状态和接单按钮（其他用户可见） -->
            <div v-else class="flex items-center justify-between pb-3 border-b border-border">
              <div class="flex items-center gap-2">
                <span class="text-sm font-medium text-muted-foreground">接单状态：</span>
                <span 
                  class="text-sm font-medium px-3 py-1 rounded-full"
                  :class="post.isAccepted === 1 ? 'bg-green-500/10 text-green-600 dark:text-green-400' : 'bg-orange-500/10 text-orange-600 dark:text-orange-400'"
                >
                  {{ post.isAccepted === 1 ? '已接单' : '未接单' }}
                </span>
              </div>
              <!-- 接单按钮（未接单且未申请时显示，或者已拒绝时可以重新申请） -->
              <button
                v-if="!post.isAccepted && (!hasApplied || (myOrder && myOrder.status === 3)) && userStore.userInfo?.token"
                @click="handleApplyOrder"
                class="inline-flex items-center justify-center rounded-lg text-sm font-medium transition-colors bg-primary text-primary-foreground hover:bg-primary/90 h-8 px-4"
              >
                <icon-mdi:hand-extended class="w-4 h-4 mr-1" />
                {{ myOrder && myOrder.status === 3 ? '重新申请接单' : '申请接单' }}
              </button>
              <!-- 接单状态提示（已申请且未拒绝时显示） -->
              <div v-else-if="hasApplied && myOrder && myOrder.status !== 3" class="flex items-center gap-2">
                <span 
                  class="text-sm font-medium px-3 py-1 rounded-full"
                  :class="{
                    'bg-yellow-500/10 text-yellow-600 dark:text-yellow-400': myOrder.status === 0,
                    'bg-blue-500/10 text-blue-600 dark:text-blue-400': myOrder.status === 1,
                    'bg-green-500/10 text-green-600 dark:text-green-400': myOrder.status === 2
                  }"
                >
                  {{ myOrder.statusText || (myOrder.status === 0 ? '待同意' : myOrder.status === 1 ? '已接单未完成' : myOrder.status === 2 ? '已完成' : '未知') }}
                </span>
              </div>
              <!-- 已拒绝状态提示 -->
              <div v-else-if="myOrder && myOrder.status === 3" class="flex items-center gap-2">
                <span 
                  class="text-sm font-medium px-3 py-1 rounded-full bg-red-500/10 text-red-600 dark:text-red-400"
                >
                  {{ myOrder.statusText || '已拒绝' }}
                </span>
              </div>
              <!-- 已申请但无详细信息时显示 -->
              <span v-else-if="hasApplied" class="text-sm text-muted-foreground">
                已申请接单，等待同意
              </span>
            </div>
            
            <!-- 接单申请列表（仅需求发布者可见） -->
            <div v-if="isOwnContent(post.userId) && orderApplications.length > 0" class="pt-3 space-y-2">
              <div class="text-sm font-medium text-muted-foreground mb-2">接单申请列表：</div>
              <div v-for="order in orderApplications" :key="order.id" class="p-3 bg-background rounded-lg border border-border">
                <div class="flex items-center justify-between">
                  <div class="flex items-center gap-3">
                    <img 
                      :src="order.accepterAvatar || userAvatar" 
                      :alt="order.accepterName"
                      class="w-10 h-10 rounded-full object-cover"
                    />
                    <div>
                      <div class="text-sm font-medium text-foreground">{{ order.accepterName }}</div>
                      <div class="text-xs text-muted-foreground">
                        {{ formatTime(order.createTime) }}
                      </div>
                    </div>
                    <span 
                      class="text-xs font-medium px-2 py-1 rounded-full"
                      :class="{
                        'bg-yellow-500/10 text-yellow-600 dark:text-yellow-400': order.status === 0,
                        'bg-blue-500/10 text-blue-600 dark:text-blue-400': order.status === 1,
                        'bg-green-500/10 text-green-600 dark:text-green-400': order.status === 2,
                        'bg-red-500/10 text-red-600 dark:text-red-400': order.status === 3
                      }"
                    >
                      {{ order.statusText || (order.status === 0 ? '待同意' : order.status === 1 ? '已接单未完成' : order.status === 2 ? '已完成' : order.status === 3 ? '已拒绝' : '未知') }}
                    </span>
                  </div>
                  <div class="flex items-center gap-2">
                    <!-- 同意接单按钮（状态为待同意时显示） -->
                    <button
                      v-if="order.status === 0"
                      @click.stop="handleAcceptOrder(order.id)"
                      class="inline-flex items-center justify-center rounded-lg text-sm font-medium transition-colors bg-green-500 text-white hover:bg-green-600 h-8 px-3"
                    >
                      <icon-mdi:check class="w-4 h-4 mr-1" />
                      同意
                    </button>
                    <!-- 拒绝接单按钮（状态为待同意时显示） -->
                    <button
                      v-if="order.status === 0"
                      @click.stop="handleRejectOrder(order.id)"
                      class="inline-flex items-center justify-center rounded-lg text-sm font-medium transition-colors bg-red-500 text-white hover:bg-red-600 h-8 px-3"
                    >
                      <icon-mdi:close class="w-4 h-4 mr-1" />
                      拒绝
                    </button>
                    <!-- 标记为已完成按钮（状态为已接单未完成时显示） -->
                    <button
                      v-if="order.status === 1"
                      @click.stop="handleCompleteOrder(order.id)"
                      class="inline-flex items-center justify-center rounded-lg text-sm font-medium transition-colors bg-primary text-primary-foreground hover:bg-primary/90 h-8 px-3"
                    >
                      <icon-mdi:check-circle class="w-4 h-4 mr-1" />
                      标记为已完成
                    </button>
                  </div>
                </div>
              </div>
            </div>
            <div v-if="post.requirementType" class="flex items-center gap-2">
              <span class="text-sm font-medium text-muted-foreground">需求类型：</span>
              <span class="text-sm text-foreground">{{ post.requirementType }}</span>
            </div>
            <div v-if="post.timeRequirement" class="flex items-center gap-2">
              <span class="text-sm font-medium text-muted-foreground">时间要求：</span>
              <span class="text-sm text-foreground">{{ post.timeRequirement }}</span>
            </div>
            <div v-if="post.budget" class="flex items-center gap-2">
              <span class="text-sm font-medium text-muted-foreground">预算：</span>
              <span class="text-sm text-foreground">{{ post.budget }}</span>
            </div>
            <div v-if="post.styleDescription" class="flex items-start gap-2">
              <span class="text-sm font-medium text-muted-foreground">风格描述：</span>
              <span class="text-sm text-foreground flex-1 whitespace-pre-wrap">{{ post.styleDescription }}</span>
            </div>
          </div>

          <!-- 附件显示 -->
          <div v-if="post.referenceAttachment" class="mb-6">
            <div class="flex items-center gap-2 mb-2">
              <icon-mdi:attachment class="w-5 h-5 text-muted-foreground" />
              <span class="text-sm font-medium text-foreground">附件</span>
            </div>
            <div class="flex items-center gap-3 p-3 bg-muted/50 rounded-lg border border-border">
              <icon-mdi:file-document-outline class="w-6 h-6 text-primary" />
              <div class="flex-1 min-w-0">
                <a 
                  :href="post.referenceAttachment" 
                  target="_blank"
                  class="text-sm text-primary hover:underline break-all"
                >
                  {{ getFileName(post.referenceAttachment) }}
                </a>
                <p class="text-xs text-muted-foreground mt-1">点击下载或查看</p>
              </div>
              <a 
                :href="post.referenceAttachment" 
                target="_blank"
                class="inline-flex items-center justify-center rounded-lg text-sm font-medium transition-colors bg-primary text-primary-foreground hover:bg-primary/90 h-8 px-4"
              >
                <icon-mdi:download class="w-4 h-4 mr-1" />
                下载
              </a>
            </div>
            <!-- 如果是图片，显示预览 -->
            <div v-if="isImageFile(post.referenceAttachment)" class="mt-3">
              <img 
                :src="post.referenceAttachment" 
                alt="附件预览"
                class="max-w-full rounded-lg border border-border"
                @error="handleImageError"
              />
            </div>
            <!-- 如果是音频，显示播放器 -->
            <div v-if="isAudioFile(post.referenceAttachment)" class="mt-3">
              <audio 
                :src="post.referenceAttachment" 
                controls
                class="w-full"
              >
                您的浏览器不支持音频播放
              </audio>
            </div>
          </div>

          <!-- 帖子统计 -->
          <div class="flex items-center gap-6 pt-4 border-t border-border text-muted-foreground">
            <span class="flex items-center gap-1.5">
              <icon-mdi:eye-outline class="w-5 h-5" />
              {{ post.viewCount || 0 }} 浏览
            </span>
            <span class="flex items-center gap-1.5">
              <icon-mdi:comment-outline class="w-5 h-5" />
              {{ post.replyCount || 0 }} 回复
            </span>
            <button 
              @click="handleLikePost"
              class="flex items-center gap-1.5 hover:text-primary transition-colors"
              :class="{ 'text-primary': post.liked }"
            >
              <icon-mdi:thumb-up v-if="post.liked" class="w-5 h-5" />
              <icon-mdi:thumb-up-outline v-else class="w-5 h-5" />
              {{ post.likeCount || 0 }} 点赞
            </button>
          </div>
        </div>

        <!-- 回复输入区域 -->
        <div class="bg-card rounded-xl border border-border p-4 mb-6 reply-input-area">
          <div v-if="replyTo" class="flex items-center gap-2 mb-3 text-sm text-muted-foreground bg-muted/50 rounded-lg px-3 py-2">
            <span>回复 @{{ replyTo.username }}</span>
            <button @click="cancelReplyTo" class="ml-auto hover:text-foreground">
              <icon-mdi:close class="w-4 h-4" />
            </button>
          </div>
          <div class="flex gap-3">
            <el-avatar 
              :size="40" 
              :src="userStore.userInfo?.userAvatar || userAvatar"
            />
            <div class="flex-1">
              <textarea 
                v-model="replyContent"
                class="flex min-h-[80px] w-full rounded-lg border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-0 resize-none"
                :placeholder="replyTo ? `回复 @${replyTo.username}...` : '写下你的回复...'"
                maxlength="2000"
              />
              <div class="flex justify-end mt-3">
                <button 
                  @click="submitReply"
                  :disabled="submitting || !replyContent.trim()"
                  class="inline-flex items-center justify-center rounded-lg text-sm font-medium transition-colors bg-primary text-primary-foreground hover:bg-primary/90 h-9 px-6 disabled:opacity-50"
                >
                  {{ submitting ? '发送中...' : '发送回复' }}
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- 回复列表 -->
        <div class="bg-card rounded-xl border border-border">
          <div class="px-4 py-3 border-b border-border">
            <h3 class="font-semibold text-foreground">全部回复 ({{ total }})</h3>
          </div>
          
          <div class="divide-y divide-border">
            <template v-for="reply in replies" :key="reply.replyId">
              <!-- 一级回复 -->
              <div class="p-4">
                <div class="flex gap-3">
                  <el-avatar 
                    :size="40" 
                    :src="reply.userAvatar || userAvatar"
                  />
                  <div class="flex-1 min-w-0">
                    <div class="flex items-center gap-2 mb-1">
                      <span class="font-medium text-foreground">{{ reply.username || '匿名用户' }}</span>
                      <span class="text-xs text-muted-foreground">{{ formatTime(reply.createTime) }}</span>
                    </div>
                    <p class="text-foreground mb-2 whitespace-pre-wrap">{{ reply.content }}</p>
                    <div class="flex items-center gap-4 text-sm text-muted-foreground">
                      <button 
                        @click="handleLikeReply(reply)"
                        class="flex items-center gap-1 hover:text-primary transition-colors"
                        :class="{ 'text-primary': reply.liked }"
                      >
                        <icon-mdi:thumb-up v-if="reply.liked" class="w-4 h-4" />
                        <icon-mdi:thumb-up-outline v-else class="w-4 h-4" />
                        {{ reply.likeCount || 0 }}
                      </button>
                      <button 
                        @click="handleReplyTo(reply)"
                        class="flex items-center gap-1 hover:text-primary transition-colors"
                      >
                        <icon-mdi:comment-outline class="w-4 h-4" />
                        回复
                      </button>
                      <button 
                        v-if="isOwnContent(reply.userId)"
                        @click="handleDeleteReply(reply)"
                        class="flex items-center gap-1 hover:text-red-500 transition-colors"
                      >
                        <icon-mdi:delete-outline class="w-4 h-4" />
                        删除
                      </button>
                    </div>

                    <!-- 子回复（楼中楼） -->
                    <div v-if="reply.children && reply.children.length > 0" class="mt-3 space-y-3 pl-4 border-l-2 border-border">
                      <div v-for="child in reply.children" :key="child.replyId" class="pt-3">
                        <div class="flex gap-2">
                          <el-avatar 
                            :size="32" 
                            :src="child.userAvatar || userAvatar"
                          />
                          <div class="flex-1 min-w-0">
                            <div class="flex items-center gap-2 mb-1 flex-wrap">
                              <span class="font-medium text-foreground text-sm">{{ child.username || '匿名用户' }}</span>
                              <span v-if="child.parentUsername" class="text-xs text-muted-foreground">
                                回复 @{{ child.parentUsername }}
                              </span>
                              <span class="text-xs text-muted-foreground">{{ formatTime(child.createTime) }}</span>
                            </div>
                            <p class="text-foreground text-sm mb-2 whitespace-pre-wrap">{{ child.content }}</p>
                            <div class="flex items-center gap-4 text-xs text-muted-foreground">
                              <button 
                                @click="handleLikeReply(child)"
                                class="flex items-center gap-1 hover:text-primary transition-colors"
                                :class="{ 'text-primary': child.liked }"
                              >
                                <icon-mdi:thumb-up v-if="child.liked" class="w-3.5 h-3.5" />
                                <icon-mdi:thumb-up-outline v-else class="w-3.5 h-3.5" />
                                {{ child.likeCount || 0 }}
                              </button>
                              <button 
                                @click="handleReplyTo(child)"
                                class="flex items-center gap-1 hover:text-primary transition-colors"
                              >
                                <icon-mdi:comment-outline class="w-3.5 h-3.5" />
                                回复
                              </button>
                              <button 
                                v-if="isOwnContent(child.userId)"
                                @click="handleDeleteReply(child)"
                                class="flex items-center gap-1 hover:text-red-500 transition-colors"
                              >
                                <icon-mdi:delete-outline class="w-3.5 h-3.5" />
                                删除
                              </button>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </template>

            <!-- 空状态 -->
            <div v-if="replies.length === 0" class="flex flex-col items-center justify-center py-12 text-muted-foreground">
              <icon-mdi:comment-remove-outline class="w-12 h-12 mb-3 opacity-50" />
              <p>暂无回复</p>
              <p class="text-sm mt-1">快来发表第一条回复吧！</p>
            </div>
          </div>

          <!-- 分页 -->
          <div v-if="total > pageSize" class="px-4 py-3 border-t border-border flex justify-center">
            <el-pagination
              v-model:current-page="currentPage"
              :page-size="pageSize"
              :total="total"
              layout="prev, pager, next"
              @current-change="handlePageChange"
            />
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.whitespace-pre-wrap {
  white-space: pre-wrap;
  word-break: break-word;
}
</style>

