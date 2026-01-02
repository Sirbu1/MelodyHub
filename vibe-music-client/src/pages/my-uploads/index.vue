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
                v-if="song.auditStatus === 2"
                type="warning"
                link
                size="small"
                @click.stop="handleReupload(song)"
                class="ml-2"
              >
                重新上传
              </el-button>
              <el-button
                type="primary"
                link
                size="small"
                @click.stop="viewSongDetail(song)"
                class="ml-2"
              >
                查看详情
              </el-button>
              <el-button
                type="danger"
                link
                size="small"
                @click.stop="handleDeleteSong(song)"
                class="ml-2"
              >
                删除
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
            @click="handlePostClick(post)"
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
                v-if="post.auditStatus === 2"
                type="warning"
                link
                size="small"
                @click.stop="handleReuploadPost(post)"
                class="ml-2"
              >
                重新上传
              </el-button>
              <el-button
                v-if="post.auditStatus === 1"
                type="success"
                link
                size="small"
                @click.stop="goToForumPost(post.postId)"
                class="ml-2"
              >
                查看帖子
              </el-button>
              <el-button
                v-else
                type="primary"
                link
                size="small"
                @click.stop="viewPostDetail(post)"
                class="ml-2"
              >
                查看详情
              </el-button>
              <el-button
                type="danger"
                link
                size="small"
                @click.stop="handleDeletePost(post)"
                class="ml-2"
              >
                删除
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
            @click="handleReplyClick(reply)"
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
                v-if="reply.auditStatus === 2"
                type="warning"
                link
                size="small"
                @click.stop="handleReuploadReply(reply)"
                class="ml-2"
              >
                重新上传
              </el-button>
              <el-button
                v-if="reply.auditStatus === 1"
                type="success"
                link
                size="small"
                @click.stop="goToForumPost(reply.postId)"
                class="ml-2"
              >
                查看帖子
              </el-button>
              <el-button
                v-else
                type="primary"
                link
                size="small"
                @click.stop="viewReplyDetail(reply)"
                class="ml-2"
              >
                查看详情
              </el-button>
              <el-button
                type="danger"
                link
                size="small"
                @click.stop="handleDeleteReply(reply)"
                class="ml-2"
              >
                删除
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

    <!-- 重新上传帖子对话框 -->
    <el-dialog
      v-model="showReuploadPostDialog"
      title="重新上传帖子"
      width="700px"
      :close-on-click-modal="false"
    >
      <div v-if="reuploadPost" class="reupload-form">
        <el-form :model="reuploadPostForm" label-width="100px">
          <el-form-item label="帖子标题" required>
            <el-input v-model="reuploadPostForm.title" placeholder="请输入帖子标题" />
          </el-form-item>
          
          <el-form-item label="帖子内容" required>
            <el-input
              v-model="reuploadPostForm.content"
              type="textarea"
              :rows="6"
              placeholder="请输入帖子内容"
              maxlength="10000"
              show-word-limit
            />
          </el-form-item>
          
          <el-form-item label="帖子类型">
            <el-radio-group v-model="reuploadPostForm.type">
              <el-radio :value="0">交流</el-radio>
              <el-radio :value="1">需求</el-radio>
            </el-radio-group>
          </el-form-item>
          
          <template v-if="reuploadPostForm.type === 1">
            <el-form-item label="需求类型">
              <el-input v-model="reuploadPostForm.requirementType" placeholder="如：作曲、编曲、混音、作词等" />
            </el-form-item>
            
            <el-form-item label="时间要求">
              <el-input v-model="reuploadPostForm.timeRequirement" placeholder="请输入时间要求" />
            </el-form-item>
            
            <el-form-item label="预算">
              <el-input v-model="reuploadPostForm.budget" placeholder="请输入预算" />
            </el-form-item>
            
            <el-form-item label="风格描述">
              <el-input
                v-model="reuploadPostForm.styleDescription"
                type="textarea"
                :rows="3"
                placeholder="请输入风格描述"
                maxlength="2000"
                show-word-limit
              />
            </el-form-item>
          </template>
          
          <el-form-item label="参考附件">
            <el-upload
              :auto-upload="false"
              :on-change="handlePostAttachmentChange"
              :show-file-list="false"
            >
              <el-button type="primary">选择附件</el-button>
            </el-upload>
            <div v-if="reuploadPost.referenceAttachment && !reuploadPostForm.attachmentFile" class="text-sm text-muted-foreground mt-2">
              当前附件：{{ reuploadPost.referenceAttachment.split('/').pop() }}
            </div>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showReuploadPostDialog = false">取消</el-button>
          <el-button type="primary" :loading="reuploadingPost" @click="handleReuploadPostSubmit">
            重新提交审核
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 重新上传回复对话框 -->
    <el-dialog
      v-model="showReuploadReplyDialog"
      title="重新上传回复"
      width="600px"
      :close-on-click-modal="false"
    >
      <div v-if="reuploadReply" class="reupload-form">
        <el-form :model="reuploadReplyForm" label-width="100px">
          <el-form-item label="回复内容" required>
            <el-input
              v-model="reuploadReplyForm.content"
              type="textarea"
              :rows="6"
              placeholder="请输入回复内容"
              maxlength="2000"
              show-word-limit
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showReuploadReplyDialog = false">取消</el-button>
          <el-button type="primary" :loading="reuploadingReply" @click="handleReuploadReplySubmit">
            重新提交审核
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 重新上传歌曲对话框 -->
    <el-dialog
      v-model="showReuploadDialog"
      title="重新上传歌曲"
      width="600px"
      :close-on-click-modal="false"
    >
      <div v-if="reuploadSong" class="reupload-form">
        <el-form :model="reuploadForm" label-width="100px">
          <el-form-item label="歌曲标题" required>
            <el-input v-model="reuploadForm.songName" placeholder="请输入歌曲标题" />
          </el-form-item>
          
          <el-form-item label="歌曲风格" required>
            <el-select v-model="reuploadForm.style" placeholder="请选择风格" style="width: 100%">
              <el-option label="流行" value="流行" />
              <el-option label="摇滚" value="摇滚" />
              <el-option label="电子" value="电子" />
              <el-option label="古典" value="古典" />
              <el-option label="爵士" value="爵士" />
              <el-option label="民谣" value="民谣" />
              <el-option label="嘻哈" value="嘻哈" />
              <el-option label="其他" value="其他" />
            </el-select>
          </el-form-item>
          
          <el-form-item label="封面图片">
            <el-upload
              :auto-upload="false"
              :on-change="handleCoverChange"
              :show-file-list="false"
              accept="image/*"
            >
              <el-button type="primary">选择封面</el-button>
              <div v-if="reuploadForm.coverPreview" class="image-preview">
                <img :src="reuploadForm.coverPreview" alt="封面预览" />
              </div>
            </el-upload>
            <div v-if="reuploadSong.coverUrl && !reuploadForm.coverFile" class="current-cover">
              <p class="text-sm text-muted-foreground">当前封面：</p>
              <img :src="reuploadSong.coverUrl" alt="当前封面" class="current-cover-img" />
            </div>
          </el-form-item>
          
          <el-form-item label="音频文件">
            <el-upload
              :auto-upload="false"
              :on-change="handleAudioChange"
              :show-file-list="false"
              accept=".mp3,.wav"
            >
              <el-button type="primary">选择音频</el-button>
            </el-upload>
            <div v-if="reuploadSong.audioUrl && !reuploadForm.audioFile" class="text-sm text-muted-foreground mt-2">
              当前音频：{{ reuploadSong.audioUrl.split('/').pop() }}
            </div>
            <small class="file-hint">支持MP3、WAV格式，文件大小不超过100MB</small>
          </el-form-item>
          
          <el-form-item label="打赏设置">
            <el-checkbox v-model="reuploadForm.isRewardEnabled" @change="handleRewardToggle">
              开启打赏功能
            </el-checkbox>
          </el-form-item>
          
          <el-form-item v-if="reuploadForm.isRewardEnabled" label="收款码" required>
            <el-upload
              :auto-upload="false"
              :on-change="handleQrChange"
              :show-file-list="false"
              accept="image/*"
            >
              <el-button type="primary">选择收款码</el-button>
              <div v-if="reuploadForm.qrPreview" class="image-preview">
                <img :src="reuploadForm.qrPreview" alt="收款码预览" />
              </div>
            </el-upload>
            <div v-if="reuploadSong.rewardQrUrl && !reuploadForm.rewardQrFile && reuploadForm.isRewardEnabled" class="current-cover">
              <p class="text-sm text-muted-foreground">当前收款码：</p>
              <img :src="reuploadSong.rewardQrUrl" alt="当前收款码" class="current-cover-img" />
            </div>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showReuploadDialog = false">取消</el-button>
          <el-button type="primary" :loading="reuploading" @click="handleReuploadSubmit">
            重新提交审核
          </el-button>
        </div>
      </template>
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
import { ref, onMounted, onActivated, watch } from 'vue'
import { useRoute, useRouter, onBeforeRouteUpdate } from 'vue-router'
import { UserStore } from '@/stores/modules/user'
import { getUserOriginalSongs, getUserPosts, getUserReplies, getForumPostDetail, getForumReplies, updateOriginalSong, updateForumPost, updateForumReply, deleteOriginalSong, deleteForumPost, deleteForumReply } from '@/api/system'
import AuthTabs from '@/components/Auth/AuthTabs.vue'
import DrawerMusic from '@/components/DrawerMusic/index.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
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

// 重新上传对话框
const showReuploadDialog = ref(false)
const reuploadSong = ref<any>(null)
const reuploading = ref(false)
const reuploadForm = ref({
  songName: '',
  style: '',
  coverFile: null as File | null,
  audioFile: null as File | null,
  isRewardEnabled: false,
  rewardQrFile: null as File | null,
  coverPreview: '',
  qrPreview: '',
  audioDuration: 0
})

// 重新上传帖子对话框
const showReuploadPostDialog = ref(false)
const reuploadPost = ref<any>(null)
const reuploadingPost = ref(false)
const reuploadPostForm = ref({
  title: '',
  content: '',
  type: 0,
  requirementType: '',
  timeRequirement: '',
  budget: '',
  styleDescription: '',
  attachmentFile: null as File | null
})

// 重新上传回复对话框
const showReuploadReplyDialog = ref(false)
const reuploadReply = ref<any>(null)
const reuploadingReply = ref(false)
const reuploadReplyForm = ref({
  content: ''
})

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

// 跳转到论坛帖子详情页
const goToForumPost = (postId: number) => {
  router.push(`/forum/${postId}`)
}

// 处理帖子点击
const handlePostClick = (post: any) => {
  // 如果已通过审核，直接跳转到论坛帖子详情页
  if (post.auditStatus === 1) {
    goToForumPost(post.postId)
  } else {
    // 否则显示详情对话框
    viewPostDetail(post)
  }
}

// 处理回复点击
const handleReplyClick = (reply: any) => {
  // 如果已通过审核，跳转到对应的论坛帖子详情页
  if (reply.auditStatus === 1) {
    goToForumPost(reply.postId)
  } else {
    // 否则显示详情对话框
    viewReplyDetail(reply)
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

// 处理重新上传
const handleReupload = (song: any) => {
  reuploadSong.value = song
  reuploadForm.value = {
    songName: song.songName || '',
    style: song.style || '',
    coverFile: null,
    audioFile: null,
    isRewardEnabled: song.isRewardEnabled || false,
    rewardQrFile: null,
    coverPreview: '',
    qrPreview: '',
    audioDuration: 0
  }
  showReuploadDialog.value = true
}

// 处理封面文件选择
const handleCoverChange = (file: any) => {
  const fileObj = file.raw || file
  if (fileObj) {
    if (fileObj.size > 10 * 1024 * 1024) {
      ElMessage.error('封面文件大小不能超过10MB')
      return
    }
    const reader = new FileReader()
    reader.onload = (e) => {
      reuploadForm.value.coverPreview = e.target?.result as string
    }
    reader.readAsDataURL(fileObj)
    reuploadForm.value.coverFile = fileObj
  }
}

// 处理音频文件选择
const handleAudioChange = (file: any) => {
  const fileObj = file.raw || file
  if (fileObj) {
    const allowedTypes = ['audio/mpeg', 'audio/wav', 'audio/x-wav']
    if (!allowedTypes.includes(fileObj.type) && !fileObj.name.toLowerCase().endsWith('.mp3') && !fileObj.name.toLowerCase().endsWith('.wav')) {
      ElMessage.error('仅支持MP3和WAV格式的音频文件')
      return
    }
    if (fileObj.size > 100 * 1024 * 1024) {
      ElMessage.error('文件大于100M，请压缩后上传')
      return
    }
    
    // 获取音频时长
    const audio = new Audio()
    const objectUrl = URL.createObjectURL(fileObj)
    audio.src = objectUrl
    
    audio.addEventListener('loadedmetadata', () => {
      if (audio.duration && isFinite(audio.duration)) {
        reuploadForm.value.audioDuration = parseFloat(audio.duration.toFixed(2))
      } else {
        reuploadForm.value.audioDuration = 0
      }
      URL.revokeObjectURL(objectUrl)
    })
    
    audio.addEventListener('error', () => {
      reuploadForm.value.audioDuration = 0
      URL.revokeObjectURL(objectUrl)
    })
    
    reuploadForm.value.audioFile = fileObj
  }
}

// 处理打赏开关
const handleRewardToggle = () => {
  if (!reuploadForm.value.isRewardEnabled) {
    reuploadForm.value.rewardQrFile = null
    reuploadForm.value.qrPreview = ''
  }
}

// 处理收款码文件选择
const handleQrChange = (file: any) => {
  const fileObj = file.raw || file
  if (fileObj) {
    if (fileObj.size > 5 * 1024 * 1024) {
      ElMessage.error('收款码图片大小不能超过5MB')
      return
    }
    const reader = new FileReader()
    reader.onload = (e) => {
      reuploadForm.value.qrPreview = e.target?.result as string
    }
    reader.readAsDataURL(fileObj)
    reuploadForm.value.rewardQrFile = fileObj
  }
}

// 提交重新上传
const handleReuploadSubmit = async () => {
  if (!reuploadForm.value.songName.trim()) {
    ElMessage.error('请输入歌曲标题')
    return
  }
  
  if (!reuploadForm.value.style) {
    ElMessage.error('请选择歌曲风格')
    return
  }
  
  // 如果没有上传新音频，检查是否有旧音频
  if (!reuploadForm.value.audioFile && !reuploadSong.value.audioUrl) {
    ElMessage.error('请上传音频文件')
    return
  }
  
  // 如果开启了打赏但没有上传新收款码，检查是否有旧收款码
  if (reuploadForm.value.isRewardEnabled) {
    if (!reuploadForm.value.rewardQrFile && !reuploadSong.value.rewardQrUrl) {
      ElMessage.error('开启打赏功能必须上传收款码图片')
      return
    }
  }
  
  // 如果上传了新音频，需要等待时长解析
  if (reuploadForm.value.audioFile && reuploadForm.value.audioDuration === 0) {
    ElMessage.warning('正在解析音频时长，请稍候...')
    return
  }
  
  try {
    reuploading.value = true
    
    const formData = new FormData()
    formData.append('songName', reuploadForm.value.songName.trim())
    formData.append('style', reuploadForm.value.style)
    formData.append('isRewardEnabled', reuploadForm.value.isRewardEnabled.toString())
    
    if (reuploadForm.value.coverFile) {
      formData.append('coverFile', reuploadForm.value.coverFile)
    }
    
    if (reuploadForm.value.audioFile) {
      formData.append('audioFile', reuploadForm.value.audioFile)
      formData.append('duration', reuploadForm.value.audioDuration.toString())
    }
    
    if (reuploadForm.value.isRewardEnabled && reuploadForm.value.rewardQrFile) {
      formData.append('rewardQrFile', reuploadForm.value.rewardQrFile)
    }
    
    const response = await updateOriginalSong(reuploadSong.value.songId, formData)
    
    if (response.code === 0) {
      ElMessage.success(response.message || '歌曲更新成功，已重新提交审核')
      showReuploadDialog.value = false
      // 重新加载数据
      loadData()
    } else {
      ElMessage.error(response.message || '更新失败')
    }
  } catch (error: any) {
    console.error('更新歌曲失败:', error)
    ElMessage.error(error.message || '更新失败，请重试')
  } finally {
    reuploading.value = false
  }
}

// 处理重新上传帖子
const handleReuploadPost = (post: any) => {
  reuploadPost.value = post
  reuploadPostForm.value = {
    title: post.title || '',
    content: post.content || '',
    type: post.type || 0,
    requirementType: post.requirementType || '',
    timeRequirement: post.timeRequirement || '',
    budget: post.budget || '',
    styleDescription: post.styleDescription || '',
    attachmentFile: null
  }
  showReuploadPostDialog.value = true
}

// 处理帖子附件文件选择
const handlePostAttachmentChange = (file: any) => {
  const fileObj = file.raw || file
  if (fileObj) {
    reuploadPostForm.value.attachmentFile = fileObj
  }
}

// 提交重新上传帖子
const handleReuploadPostSubmit = async () => {
  if (!reuploadPostForm.value.title.trim()) {
    ElMessage.error('请输入帖子标题')
    return
  }
  
  if (!reuploadPostForm.value.content.trim()) {
    ElMessage.error('请输入帖子内容')
    return
  }
  
  try {
    reuploadingPost.value = true
    
    const formData = new FormData()
    // 确保必填字段不为空
    const title = reuploadPostForm.value.title.trim()
    const content = reuploadPostForm.value.content.trim()
    
    if (!title || !content) {
      ElMessage.error('标题和内容不能为空')
      reuploadingPost.value = false
      return
    }
    
    formData.append('title', title)
    formData.append('content', content)
    formData.append('type', String(reuploadPostForm.value.type))
    
    if (reuploadPostForm.value.type === 1) {
      if (reuploadPostForm.value.requirementType && reuploadPostForm.value.requirementType.trim()) {
        formData.append('requirementType', reuploadPostForm.value.requirementType.trim())
      }
      if (reuploadPostForm.value.timeRequirement && reuploadPostForm.value.timeRequirement.trim()) {
        formData.append('timeRequirement', reuploadPostForm.value.timeRequirement.trim())
      }
      if (reuploadPostForm.value.budget && reuploadPostForm.value.budget.trim()) {
        formData.append('budget', reuploadPostForm.value.budget.trim())
      }
      if (reuploadPostForm.value.styleDescription && reuploadPostForm.value.styleDescription.trim()) {
        formData.append('styleDescription', reuploadPostForm.value.styleDescription.trim())
      }
    }
    
    if (reuploadPostForm.value.attachmentFile) {
      formData.append('referenceAttachmentFile', reuploadPostForm.value.attachmentFile)
    }
    
    console.log('提交更新帖子，postId:', reuploadPost.value.postId)
    console.log('FormData内容:', {
      title,
      content,
      type: reuploadPostForm.value.type,
      hasAttachment: !!reuploadPostForm.value.attachmentFile
    })
    
    const response = await updateForumPost(reuploadPost.value.postId, formData)
    
    if (response.code === 0) {
      ElMessage.success(response.message || '帖子更新成功，已重新提交审核')
      showReuploadPostDialog.value = false
      loadData()
    } else {
      ElMessage.error(response.message || '更新失败')
    }
  } catch (error: any) {
    console.error('更新帖子失败:', error)
    ElMessage.error(error.message || '更新失败，请重试')
  } finally {
    reuploadingPost.value = false
  }
}

// 处理重新上传回复
const handleReuploadReply = (reply: any) => {
  reuploadReply.value = reply
  reuploadReplyForm.value = {
    content: reply.content || ''
  }
  showReuploadReplyDialog.value = true
}

// 提交重新上传回复
const handleReuploadReplySubmit = async () => {
  if (!reuploadReplyForm.value.content.trim()) {
    ElMessage.error('请输入回复内容')
    return
  }
  
  try {
    reuploadingReply.value = true
    
    const response = await updateForumReply(reuploadReply.value.replyId, {
      content: reuploadReplyForm.value.content.trim()
    })
    
    if (response.code === 0) {
      ElMessage.success(response.message || '回复更新成功，已重新提交审核')
      showReuploadReplyDialog.value = false
      loadData()
    } else {
      ElMessage.error(response.message || '更新失败')
    }
  } catch (error: any) {
    console.error('更新回复失败:', error)
    ElMessage.error(error.message || '更新失败，请重试')
  } finally {
    reuploadingReply.value = false
  }
}

// 删除歌曲
const handleDeleteSong = async (song: any) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除歌曲《${song.songName}》吗？删除后不可恢复`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    
    const response = await deleteOriginalSong(song.songId)
    if (response.code === 0) {
      ElMessage.success('删除成功')
      loadData()
    } else {
      ElMessage.error(response.message || '删除失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除歌曲失败:', error)
      ElMessage.error(error.message || '删除失败，请重试')
    }
  }
}

// 删除帖子
const handleDeletePost = async (post: any) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除帖子《${post.title}》吗？删除后不可恢复`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    
    const response = await deleteForumPost(post.postId)
    if (response.code === 0) {
      ElMessage.success('删除成功')
      loadData()
    } else {
      ElMessage.error(response.message || '删除失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除帖子失败:', error)
      ElMessage.error(error.message || '删除失败，请重试')
    }
  }
}

// 删除回复
const handleDeleteReply = async (reply: any) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除这条回复吗？删除后不可恢复',
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    
    const response = await deleteForumReply(reply.replyId)
    if (response.code === 0) {
      ElMessage.success('删除成功')
      loadData()
    } else {
      ElMessage.error(response.message || '删除失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除回复失败:', error)
      ElMessage.error(error.message || '删除失败，请重试')
    }
  }
}

// 监听登录状态变化，登录后刷新数据
watch(() => userStore.isLoggedIn, (isLoggedIn) => {
  if (isLoggedIn && userStore.userInfo?.userId) {
    loadData()
  }
})

// 检查登录状态
onMounted(() => {
  if (!userStore.isLoggedIn) {
    authVisible.value = true
  } else {
    loadData()
  }
})

// 页面激活时刷新数据（从其他页面返回时）
onActivated(() => {
  if (userStore.isLoggedIn && userStore.userInfo?.userId) {
    loadData()
  }
})

// 监听路由变化，确保从其他页面返回时刷新
watch(() => route.path, (newPath, oldPath) => {
  // 如果路由变化到当前页面，且用户已登录，则刷新数据
  if (newPath === '/my/uploads' && userStore.isLoggedIn && userStore.userInfo?.userId) {
    // 延迟一下，确保页面已经加载完成
    setTimeout(() => {
      loadData()
    }, 100)
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

.reupload-form {
  padding: 20px 0;
}

.image-preview {
  margin-top: 10px;
}

.image-preview img {
  max-width: 200px;
  max-height: 200px;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.current-cover {
  margin-top: 10px;
}

.current-cover-img {
  max-width: 200px;
  max-height: 200px;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.file-hint {
  display: block;
  margin-top: 5px;
  color: #666;
  font-size: 12px;
}
</style>

