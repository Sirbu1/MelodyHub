<template>
  <div class="flex flex-col h-full flex-1 overflow-hidden bg-background px-4 py-2">
    <!-- 导航栏：切换模块 -->
    <div class="py-4 border-b border-border">
      <div class="flex items-center gap-2">
        <button
          @click="switchTab('songs')"
          :class="[
            'px-6 py-2 rounded-lg text-sm font-medium transition-all duration-300',
            activeTab === 'songs'
              ? 'bg-primary text-primary-foreground shadow-md'
              : 'bg-transparent text-muted-foreground hover:text-foreground hover:bg-accent'
          ]"
        >
          <icon-ri:music-line class="inline mr-2" />
          我的歌曲
        </button>
        <button
          @click="switchTab('posts')"
          :class="[
            'px-6 py-2 rounded-lg text-sm font-medium transition-all duration-300',
            activeTab === 'posts'
              ? 'bg-primary text-primary-foreground shadow-md'
              : 'bg-transparent text-muted-foreground hover:text-foreground hover:bg-accent'
          ]"
        >
          <icon-mdi:forum-outline class="inline mr-2" />
          我的帖子
        </button>
        <button
          @click="switchTab('replies')"
          :class="[
            'px-6 py-2 rounded-lg text-sm font-medium transition-all duration-300',
            activeTab === 'replies'
              ? 'bg-primary text-primary-foreground shadow-md'
              : 'bg-transparent text-muted-foreground hover:text-foreground hover:bg-accent'
          ]"
        >
          <icon-mdi:comment-outline class="inline mr-2" />
          我的回复
        </button>
      </div>
    </div>

    <!-- 状态筛选 -->
    <div class="py-3 border-b border-border">
      <div class="flex items-center gap-2">
        <span class="text-sm text-muted-foreground">状态筛选：</span>
        <el-radio-group v-model="selectedStatus" @change="handleStatusChange" size="small">
          <el-radio-button :label="null">全部</el-radio-button>
          <el-radio-button :label="0">待审核</el-radio-button>
          <el-radio-button :label="1">已通过</el-radio-button>
          <el-radio-button :label="2">未通过</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- 内容区域 -->
    <div class="flex-1 overflow-y-auto min-h-0">
      <!-- 我的歌曲 -->
      <div v-if="activeTab === 'songs'" v-loading="loading">
        <div v-if="songs.length === 0" class="empty-state">
          <icon-ri:music-line class="empty-icon" />
          <h3>暂无歌曲</h3>
          <p>您还没有上传任何歌曲</p>
        </div>
        <div v-else class="song-list">
          <div
            v-for="song in songs"
            :key="song.songId"
            class="song-item"
            @click="viewSongDetail(song)"
          >
            <div class="song-info">
              <div class="song-name">{{ song.songName }}</div>
              <div class="song-meta">
                <span>风格：{{ song.style }}</span>
                <span>上传时间：{{ formatTime(song.createTime) }}</span>
              </div>
            </div>
            <div class="song-actions">
              <el-tag
                :type="getStatusType(song.auditStatus)"
                size="small"
              >
                {{ getStatusText(song.auditStatus) }}
              </el-tag>
              <el-button
                type="primary"
                link
                size="small"
                @click.stop="viewSongDetail(song)"
                class="ml-2"
              >
                查看详情
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 我的帖子 -->
      <div v-if="activeTab === 'posts'" v-loading="loading">
        <div v-if="posts.length === 0" class="empty-state">
          <icon-mdi:forum-outline class="empty-icon" />
          <h3>暂无帖子</h3>
          <p>您还没有发布任何帖子</p>
        </div>
        <div v-else class="post-list">
          <div
            v-for="post in posts"
            :key="post.postId"
            class="post-item"
            @click="viewPostDetail(post)"
          >
            <div class="post-info">
              <div class="post-title">{{ post.title }}</div>
              <div class="post-meta">
                <span>类型：{{ post.type === 0 ? '交流' : '需求' }}</span>
                <span>发布时间：{{ formatTime(post.createTime) }}</span>
              </div>
            </div>
            <div class="post-actions">
              <el-tag
                :type="getStatusType(post.auditStatus)"
                size="small"
              >
                {{ getStatusText(post.auditStatus) }}
              </el-tag>
              <el-button
                type="primary"
                link
                size="small"
                @click.stop="viewPostDetail(post)"
                class="ml-2"
              >
                查看详情
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 我的回复 -->
      <div v-if="activeTab === 'replies'" v-loading="loading">
        <div v-if="replies.length === 0" class="empty-state">
          <icon-mdi:comment-outline class="empty-icon" />
          <h3>暂无回复</h3>
          <p>您还没有发布任何回复</p>
        </div>
        <div v-else class="reply-list">
          <div
            v-for="reply in replies"
            :key="reply.replyId"
            class="reply-item"
            @click="viewReplyDetail(reply)"
          >
            <div class="reply-info">
              <div class="reply-content">{{ reply.content }}</div>
              <div class="reply-meta">
                <span>回复时间：{{ formatTime(reply.createTime) }}</span>
              </div>
            </div>
            <div class="reply-actions">
              <el-tag
                :type="getStatusType(reply.auditStatus)"
                size="small"
              >
                {{ getStatusText(reply.auditStatus) }}
              </el-tag>
              <el-button
                type="primary"
                link
                size="small"
                @click.stop="viewReplyDetail(reply)"
                class="ml-2"
              >
                查看详情
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 登录提示 -->
    <AuthTabs v-model="authVisible" />

    <!-- DrawerMusic 播放页面 -->
    <DrawerMusic v-model="showDrawerMusic" />

    <!-- 帖子详情对话框 -->
    <el-dialog
      v-model="showPostDialog"
      title="帖子详情"
      width="70%"
      :close-on-click-modal="false"
    >
      <div v-if="postDetail" class="post-detail-dialog">
        <div class="post-header mb-4">
          <h2 class="text-2xl font-bold mb-2">{{ postDetail.title }}</h2>
          <div class="flex items-center gap-4 text-sm text-muted-foreground">
            <span>类型：{{ postDetail.type === 0 ? '交流' : '需求' }}</span>
            <span>发布时间：{{ formatTime(postDetail.createTime) }}</span>
            <el-tag
              :type="getStatusType(postDetail.auditStatus)"
              size="small"
            >
              {{ getStatusText(postDetail.auditStatus) }}
            </el-tag>
          </div>
        </div>
        
        <div class="post-content mb-6">
          <div class="prose max-w-none" v-html="postDetail.content"></div>
        </div>

        <!-- 回复列表 -->
        <div v-if="postReplies.length > 0" class="replies-section">
          <h3 class="text-lg font-semibold mb-4">回复 ({{ postReplies.length }})</h3>
          <div class="space-y-4">
            <div
              v-for="reply in postReplies"
              :key="reply.replyId"
              class="reply-item-dialog bg-card rounded-lg p-4 border"
            >
              <div class="flex gap-3">
                <el-avatar :src="reply.userAvatar" :size="40" />
                <div class="flex-1">
                  <div class="flex items-center gap-2 mb-2">
                    <span class="font-medium">{{ reply.username }}</span>
                    <span class="text-xs text-muted-foreground">{{ formatTime(reply.createTime) }}</span>
                    <el-tag
                      :type="getStatusType(reply.auditStatus)"
                      size="small"
                    >
                      {{ getStatusText(reply.auditStatus) }}
                    </el-tag>
                  </div>
                  <div class="text-foreground">{{ reply.content }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="text-center text-muted-foreground py-8">
          暂无回复
        </div>
      </div>
    </el-dialog>

    <!-- 回复详情对话框 -->
    <el-dialog
      v-model="showReplyDialog"
      title="回复详情"
      width="70%"
      :close-on-click-modal="false"
    >
      <div v-if="replyDetail && replyPostDetail" class="reply-detail-dialog">
        <!-- 原帖子信息 -->
        <div class="post-section mb-6 pb-6 border-b">
          <h3 class="text-lg font-semibold mb-3">原帖子</h3>
          <div class="bg-card rounded-lg p-4">
            <h4 class="text-xl font-bold mb-2">{{ replyPostDetail.title }}</h4>
            <div class="text-sm text-muted-foreground mb-3">
              <span>类型：{{ replyPostDetail.type === 0 ? '交流' : '需求' }}</span>
              <span class="ml-4">发布时间：{{ formatTime(replyPostDetail.createTime) }}</span>
            </div>
            <div class="prose max-w-none" v-html="replyPostDetail.content"></div>
          </div>
        </div>

        <!-- 我的回复 -->
        <div class="reply-section mb-6">
          <h3 class="text-lg font-semibold mb-3">我的回复</h3>
          <div class="bg-card rounded-lg p-4 border">
            <div class="flex items-center gap-2 mb-2">
              <span class="text-sm text-muted-foreground">回复时间：{{ formatTime(replyDetail.createTime) }}</span>
              <el-tag
                :type="getStatusType(replyDetail.auditStatus)"
                size="small"
              >
                {{ getStatusText(replyDetail.auditStatus) }}
              </el-tag>
            </div>
            <div class="text-foreground">{{ replyDetail.content }}</div>
          </div>
        </div>

        <!-- 其他回复 -->
        <div v-if="postReplies.length > 0" class="other-replies-section">
          <h3 class="text-lg font-semibold mb-4">其他回复 ({{ postReplies.length }})</h3>
          <div class="space-y-4">
            <div
              v-for="reply in postReplies"
              :key="reply.replyId"
              class="reply-item-dialog bg-card rounded-lg p-4 border"
            >
              <div class="flex gap-3">
                <el-avatar :src="reply.userAvatar" :size="40" />
                <div class="flex-1">
                  <div class="flex items-center gap-2 mb-2">
                    <span class="font-medium">{{ reply.username }}</span>
                    <span class="text-xs text-muted-foreground">{{ formatTime(reply.createTime) }}</span>
                    <el-tag
                      :type="getStatusType(reply.auditStatus)"
                      size="small"
                    >
                      {{ getStatusText(reply.auditStatus) }}
                    </el-tag>
                  </div>
                  <div class="text-foreground">{{ reply.content }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { UserStore } from '@/stores/modules/user'
import { getUserOriginalSongs, getUserPosts, getUserReplies, getForumPostDetail, getForumReplies } from '@/api/system'
import AuthTabs from '@/components/Auth/AuthTabs.vue'
import DrawerMusic from '@/components/DrawerMusic/index.vue'
import { ElMessage } from 'element-plus'
import { AudioStore } from '@/stores/modules/audio'
import { useAudioPlayer } from '@/hooks/useAudioPlayer'
import default_album from '@/assets/default_album.jpg'

const route = useRoute()
const router = useRouter()
const userStore = UserStore()
const audio = AudioStore()
const { loadTrack, play } = useAudioPlayer()
const activeTab = ref('songs')
const selectedStatus = ref<number | null>(null)
const loading = ref(false)
const authVisible = ref(false)

const songs = ref<any[]>([])
const posts = ref<any[]>([])
const replies = ref<any[]>([])

// DrawerMusic 控制
const showDrawerMusic = ref(false)

// 帖子详情对话框
const showPostDialog = ref(false)
const postDetail = ref<any>(null)
const postReplies = ref<any[]>([])

// 回复详情对话框
const showReplyDialog = ref(false)
const replyDetail = ref<any>(null)
const replyPostDetail = ref<any>(null)

// 切换标签页
const switchTab = (tab: string) => {
  activeTab.value = tab
  selectedStatus.value = null
  loadData()
}

// 状态变化处理
const handleStatusChange = () => {
  loadData()
}

// 获取状态文本
const getStatusText = (status: number | null | undefined) => {
  if (status === null || status === undefined) return '待审核'
  switch (status) {
    case 0:
      return '待审核'
    case 1:
      return '已通过'
    case 2:
      return '未通过'
    default:
      return '未知'
  }
}

// 获取状态类型
const getStatusType = (status: number | null | undefined) => {
  if (status === null || status === undefined) return 'warning'
  switch (status) {
    case 0:
      return 'warning'
    case 1:
      return 'success'
    case 2:
      return 'danger'
    default:
      return 'info'
  }
}

// 格式化时间
const formatTime = (time: string) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString('zh-CN')
}

// 加载数据
const loadData = async () => {
  if (!userStore.isLoggedIn || !userStore.userInfo?.userId) {
    authVisible.value = true
    return
  }

  loading.value = true
  try {
    if (activeTab.value === 'songs') {
      const response = await getUserOriginalSongs(
        userStore.userInfo.userId,
        1,
        100,
        selectedStatus.value ?? undefined
      )
      if (response.code === 0 && response.data) {
        songs.value = response.data.items || []
      }
    } else if (activeTab.value === 'posts') {
      const response = await getUserPosts(
        userStore.userInfo.userId,
        1,
        100,
        selectedStatus.value ?? undefined
      )
      if (response.code === 0 && response.data) {
        posts.value = response.data.items || []
      }
    } else if (activeTab.value === 'replies') {
      const response = await getUserReplies(
        userStore.userInfo.userId,
        1,
        100,
        selectedStatus.value ?? undefined
      )
      if (response.code === 0 && response.data) {
        replies.value = response.data.items || []
      }
    }
  } catch (error: any) {
    console.error('加载数据失败:', error)
    if (error.response?.status === 404) {
      ElMessage.error('请求的资源不存在，请检查API路径')
    } else {
      ElMessage.error(error.message || '加载数据失败')
    }
  } finally {
    loading.value = false
  }
}

// 查看歌曲详情 - 打开 DrawerMusic 播放页面
const viewSongDetail = async (song: any) => {
  try {
    // 转换歌曲为 trackModel 格式
    const track = {
      id: song.songId.toString(),
      title: song.songName,
      artist: song.artistName || song.creatorName || '',
      album: song.album || '',
      cover: song.coverUrl || default_album,
      url: song.audioUrl || '',
      duration: parseFloat(song.duration || '0') * 1000,
      likeStatus: song.likeStatus || 0
    }
    
    // 设置播放列表并播放
    audio.setAudioStore('trackList', [track])
    audio.setAudioStore('currentSongIndex', 0)
    
    // 如果有音频URL，加载并播放
    if (track.url) {
      await loadTrack()
      play()
    }
    
    // 打开 DrawerMusic 抽屉
    showDrawerMusic.value = true
  } catch (error) {
    console.error('打开歌曲播放页面失败:', error)
    ElMessage.error('打开歌曲播放页面失败')
  }
}

// 查看帖子详情 - 弹出对话框
const viewPostDetail = async (post: any) => {
  try {
    loading.value = true
    // 获取帖子详情
    const postRes = await getForumPostDetail(post.postId)
    if (postRes.code === 0 && postRes.data) {
      postDetail.value = postRes.data
      
      // 获取帖子回复
      const repliesRes = await getForumReplies({
        postId: post.postId,
        pageNum: 1,
        pageSize: 100
      })
      if (repliesRes.code === 0 && repliesRes.data) {
        postReplies.value = repliesRes.data.items || []
      }
      
      showPostDialog.value = true
    } else {
      ElMessage.error('获取帖子详情失败')
    }
  } catch (error) {
    console.error('获取帖子详情失败:', error)
    ElMessage.error('获取帖子详情失败')
  } finally {
    loading.value = false
  }
}

// 查看回复详情 - 弹出对话框
const viewReplyDetail = async (reply: any) => {
  try {
    loading.value = true
    replyDetail.value = reply
    
    // 获取对应的帖子详情
    const postRes = await getForumPostDetail(reply.postId)
    if (postRes.code === 0 && postRes.data) {
      replyPostDetail.value = postRes.data
      
      // 获取帖子回复
      const repliesRes = await getForumReplies({
        postId: reply.postId,
        pageNum: 1,
        pageSize: 100
      })
      if (repliesRes.code === 0 && repliesRes.data) {
        postReplies.value = repliesRes.data.items || []
      }
      
      showReplyDialog.value = true
    } else {
      ElMessage.error('获取帖子详情失败')
    }
  } catch (error) {
    console.error('获取回复详情失败:', error)
    ElMessage.error('获取回复详情失败')
  } finally {
    loading.value = false
  }
}

// 检查登录状态
onMounted(() => {
  if (!userStore.isLoggedIn) {
    authVisible.value = true
  } else {
    loadData()
  }
})
</script>

<style scoped>
.empty-state {
  text-align: center;
  padding: 80px 20px;
  color: #999;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 20px;
  opacity: 0.5;
}

.empty-state h3 {
  margin: 0 0 10px 0;
  color: #666;
}

.empty-state p {
  margin: 0;
}

.song-list,
.post-list,
.reply-list {
  padding: 20px 0;
}

.song-item,
.post-item,
.reply-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  margin-bottom: 12px;
  background: hsl(var(--card));
  border-radius: 8px;
  border: 1px solid hsl(var(--border));
  transition: all 0.3s;
}

.song-item:hover,
.post-item:hover,
.reply-item:hover {
  border-color: hsl(var(--primary));
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.song-info,
.post-info,
.reply-info {
  flex: 1;
}

.song-name,
.post-title {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 8px;
  color: hsl(var(--foreground));
}

.reply-content {
  font-size: 14px;
  color: hsl(var(--foreground));
  margin-bottom: 8px;
  line-height: 1.5;
}

.song-meta,
.post-meta,
.reply-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: hsl(var(--muted-foreground));
}

.song-actions,
.post-actions,
.reply-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: 16px;
}

.song-item,
.post-item,
.reply-item {
  cursor: pointer;
}

.post-detail-dialog,
.reply-detail-dialog {
  max-height: 70vh;
  overflow-y: auto;
}

.post-content {
  line-height: 1.8;
}

.reply-item-dialog {
  transition: all 0.3s;
}

.reply-item-dialog:hover {
  border-color: hsl(var(--primary));
}
</style>

