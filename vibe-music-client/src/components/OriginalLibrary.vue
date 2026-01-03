<template>
  <div class="original-library">
    <!-- 搜索和筛选 -->
    <div class="library-header">
      <div class="search-section">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索原创歌曲..."
          clearable
          @clear="handleSearch"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <icon-ri:search-line />
          </template>
        </el-input>
      </div>

      <div class="filter-section">
        <el-select v-model="selectedStyle" placeholder="选择风格" clearable @change="handleFilter">
          <el-option
            v-for="style in styleOptions"
            :key="style.value"
            :label="style.label"
            :value="style.value"
          />
        </el-select>
      </div>
    </div>

    <!-- 使用Table组件样式显示歌曲列表 -->
    <div class="flex-1 h-full flex flex-col overflow-hidden" v-loading="loading">
      <el-table :data="tableSongList" style="
          --el-table-border: none;
          --el-table-border-color: none;
          --el-table-tr-bg-color: none;
          --el-table-header-bg-color: none;
          --el-table-row-hover-bg-color: transparent;
        " class="!rounded-lg !h-full transition duration-300">
        <el-table-column>
          <template #header>
            <div class="grid grid-cols-[auto_4fr_3fr_1fr_2fr_1fr_1fr] items-center gap-6 w-full text-left mt-2">
              <div class="ml-3">标题</div>
              <div class="w-12"></div>
              <div class="ml-1">歌手</div>
              <div>喜欢</div>
              <div class="ml-7">时长</div>
              <div>下载</div>
              <div>操作</div>
            </div>
          </template>
          <template #default="{ row }">
            <div
              class="grid grid-cols-[auto_4fr_3fr_1fr_2fr_1fr_1fr] items-center gap-6 w-full group transition duration-300 rounded-2xl p-2"
              :class="[
                isCurrentPlaying(row.songId) ? 'bg-[hsl(var(--hover-menu-bg))]' : 'hover:bg-[hsl(var(--hover-menu-bg))]',
                'cursor-pointer'
              ]"
              @click="handlePlay(row)">
              <!-- 标题和封面 -->
              <div class="w-10 h-10 relative">
                <el-image 
                  :src="row.coverUrl || default_album" 
                  fit="cover" 
                  lazy 
                  :alt="row.songName" 
                  class="w-full h-full rounded-md"
                  @error="handleCoverError"
                />
                <!-- Play 按钮，使用 group-hover 控制透明度 -->
                <div
                  class="absolute inset-0 flex items-center justify-center text-white opacity-0 transition-opacity duration-300 z-10 group-hover:opacity-100 group-hover:bg-black/50 rounded-md">
                  <icon-tabler:player-play-filled class="text-lg" />
                </div>
              </div>

              <!-- 歌曲名称 -->
              <div class="text-left">
                <div class="flex-1 line-clamp-1">{{ row.songName }}</div>
              </div>

              <!-- 歌手 -->
              <div class="text-left">
                <div class="line-clamp-1 w-48">{{ row.artistName }}</div>
              </div>

              <!-- 喜欢 -->
              <div class="flex items-center ml-1">
                <el-button text circle @click="handleLike(row, $event)">
                  <icon-mdi:cards-heart-outline v-if="!userStore.isLoggedIn || row.likeStatus === 0" class="text-lg" />
                  <icon-mdi:cards-heart v-else class="text-lg text-red-500" />
                </el-button>
              </div>

              <!-- 时长 -->
              <div class="text-left ml-8">
                <span v-if="row.duration && !isNaN(Number(row.duration)) && Number(row.duration) > 0">
                  {{ formatMillisecondsToTime(Number(row.duration) * 1000) }}
                </span>
                <span v-else class="text-muted-foreground">--</span>
              </div>

              <!-- 下载 -->
              <div class="flex items-center ml-1">
                <el-button text circle @click.stop="downLoadMusic(row, $event)">
                  <icon-material-symbols:download class="text-lg" />
                </el-button>
              </div>

              <!-- 删除 -->
              <div class="flex items-center ml-1">
                <el-button text circle @click.stop="handleDelete(row, $event)" class="text-red-500 hover:text-red-600">
                  <icon-mdi:delete-outline class="text-lg" />
                </el-button>
              </div>
            </div>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 空状态 -->
      <div v-if="!loading && tableSongList.length === 0" class="empty-state">
        <icon-ri:music-line class="empty-icon" />
        <h3>暂无原创歌曲</h3>
        <p>快来上传你的第一首原创歌曲吧！</p>
        <el-button type="primary" @click="switchToUpload">
          立即上传
        </el-button>
      </div>

      <!-- 分页 -->
      <nav class="mx-auto flex w-full justify-center mt-3" v-if="total > pageSize">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[20, 30, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
          class="mb-3"
        />
      </nav>
    </div>

    <!-- 打赏二维码弹窗 -->
    <el-dialog
      v-model="qrDialogVisible"
      title="支持创作者"
      width="400px"
      center
    >
      <div class="qr-dialog-content">
        <p class="qr-description">
          扫描二维码支持 <strong>{{ currentSong?.creatorName }}</strong> 的创作
        </p>
        <div class="qr-image">
          <el-image
            :src="currentSong?.rewardQrUrl"
            alt="收款码"
            fit="contain"
            class="qr-code"
          />
        </div>
        <p class="qr-tip">
          <icon-ri:information-line />
          您的支持将鼓励更多原创音乐诞生
        </p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserOriginalSongs, deleteOriginalSong, collectSong, cancelCollectSong } from '@/api/system'
import { Song } from '@/api/interface'
import { UserStore } from '@/stores/modules/user'
import { formatMillisecondsToTime } from '@/utils'
import default_album from '@/assets/default_album.jpg'

const router = useRouter()
const userStore = UserStore()
const audio = AudioStore()
const { loadTrack, play } = useAudioPlayer()

// 响应式数据
const loading = ref(false)
const originalSongList = ref<any[]>([])
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const searchKeyword = ref('')
const selectedStyle = ref('')
const qrDialogVisible = ref(false)
const currentSong = ref<any>(null)

// 风格选项
const styleOptions = [
  { label: '流行', value: '流行' },
  { label: '摇滚', value: '摇滚' },
  { label: '电子', value: '电子' },
  { label: '古典', value: '古典' },
  { label: '爵士', value: '爵士' },
  { label: '民谣', value: '民谣' },
  { label: '嘻哈', value: '嘻哈' },
  { label: '其他', value: '其他' }
]

// 将原创歌曲数据转换为Table组件期望的Song格式
const tableSongList = computed(() => {
  return originalSongList.value.map(song => {
    // 将原创歌曲格式转换为Song格式
    const tableSong: Song = {
      songId: song.songId,
      songName: song.songName,
      artistName: song.creatorName || '原创音乐人', // 使用creatorName作为artistName
      duration: song.duration || '0',
      coverUrl: song.coverUrl || '',
      audioUrl: song.audioUrl || '',
      likeStatus: song.likeStatus || 0,
      releaseTime: song.releaseTime || song.createTime || '',
      isRewardEnabled: song.isRewardEnabled === true || song.isRewardEnabled === 1,
      rewardQrUrl: song.rewardQrUrl && song.rewardQrUrl.trim() !== '' ? song.rewardQrUrl : undefined
    }
    
    return tableSong
  })
})

// 监听数据变化，更新当前页面的歌曲列表
watch(() => tableSongList.value, (newData) => {
  audio.setCurrentPageSongs(newData)
}, { immediate: true })

// 获取歌曲列表（只获取当前用户上传的歌曲）
const fetchSongs = async () => {
  if (!userStore.isLoggedIn || !userStore.userInfo?.userId) {
    ElMessage.warning('请先登录')
    originalSongList.value = []
    total.value = 0
    return
  }

  loading.value = true
  try {
    const response = await getUserOriginalSongs(userStore.userInfo.userId, currentPage.value, pageSize.value)

    console.log('获取我的上传歌曲响应:', response)

    if (response.code === 0) {
      // 处理返回的数据结构
      if (response.data) {
        if (response.data.items) {
          // ResultTable格式
          let songs = response.data.items || []
          
          // 应用搜索和筛选
          if (searchKeyword.value) {
            songs = songs.filter((song: any) => 
              song.songName?.toLowerCase().includes(searchKeyword.value.toLowerCase()) ||
              song.creatorName?.toLowerCase().includes(searchKeyword.value.toLowerCase())
            )
          }
          
          if (selectedStyle.value) {
            songs = songs.filter((song: any) => song.style === selectedStyle.value)
          }
          
          originalSongList.value = songs
          total.value = response.data.total || 0
        } else if (Array.isArray(response.data)) {
          originalSongList.value = response.data
          total.value = response.data.length
        } else {
          originalSongList.value = []
          total.value = 0
        }
      } else {
        originalSongList.value = []
        total.value = 0
      }
      
      console.log('歌曲列表:', originalSongList.value)
      console.log('总数:', total.value)
    } else {
      ElMessage.error(response.message || '获取歌曲列表失败')
      originalSongList.value = []
      total.value = 0
    }
  } catch (error: any) {
    console.error('获取我的上传歌曲失败:', error)
    ElMessage.error('获取歌曲列表失败')
    originalSongList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 处理搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchSongs()
}

// 处理筛选
const handleFilter = () => {
  currentPage.value = 1
  fetchSongs()
}

// 处理分页
const handlePageChange = (page: number) => {
  currentPage.value = page
  fetchSongs()
}

// 处理每页大小变化
const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  fetchSongs()
}

// 显示打赏二维码
const showRewardQr = (song: any) => {
  currentSong.value = song
  qrDialogVisible.value = true
}

// 使用router跳转到上传页面
const switchToUpload = () => {
  router.push('/original/upload')
}

// 转换歌曲实体
const convertToTrackModel = (song: Song) => {
  if (!song.songId || !song.songName || !song.audioUrl) {
    console.error('歌曲数据不完整:', song)
    return null
  }
  return {
    id: song.songId.toString(),
    title: song.songName,
    artist: song.artistName,
    cover: song.coverUrl || default_album,
    url: song.audioUrl,
    duration: Number(song.duration) || 0,
    likeStatus: song.likeStatus || 0,
  }
}

// 播放音乐
const handlePlay = async (row: Song) => {
  // 先将所有表格数据转换为 trackModel
  const allTracks = tableSongList.value
    .map(song => convertToTrackModel(song))
    .filter(track => track !== null)

  // 找到当前选中歌曲的索引
  const selectedIndex = tableSongList.value.findIndex(song => song.songId === row.songId)

  // 清空现有播放列表并添加所有歌曲
  audio.setAudioStore('trackList', allTracks)
  // 设置当前播放索引为选中的歌曲
  audio.setAudioStore('currentSongIndex', selectedIndex)

  // 加载并播放选中的歌曲
  await loadTrack()
  play()
}

// 判断是否是当前播放的歌曲
const isCurrentPlaying = (songId: number) => {
  const currentTrack = audio.trackList[audio.currentSongIndex]
  return currentTrack && Number(currentTrack.id) === songId
}

// 处理封面图片加载失败
const handleCoverError = (event: Event) => {
  const target = event.target as HTMLImageElement
  if (target && target.src !== default_album) {
    target.src = default_album
  }
}

// 更新所有相同歌曲的喜欢状态
const updateAllSongLikeStatus = (songId: number, status: number) => {
  // 更新播放列表中的状态
  audio.trackList.forEach(track => {
    if (Number(track.id) === songId) {
      track.likeStatus = status
    }
  })

  // 更新当前页面的歌曲列表状态
  if (audio.currentPageSongs) {
    audio.currentPageSongs.forEach(song => {
      if (song.songId === songId) {
        song.likeStatus = status
      }
    })
  }

  // 更新原始数据
  originalSongList.value.forEach(song => {
    if (song.songId === songId) {
      song.likeStatus = status
    }
  })
}

// 处理喜欢/取消喜欢
const handleLike = async (row: Song, e: Event) => {
  e.stopPropagation() // 阻止事件冒泡
  
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }

  try {
    if (row.likeStatus === 0) {
      // 收藏歌曲
      const res = await collectSong(row.songId)
      if (res.code === 0) {
        updateAllSongLikeStatus(row.songId, 1)
        ElMessage.success('已添加到我的喜欢')
      } else {
        ElMessage.error(res.message || '添加到我的喜欢失败')
      }
    } else {
      // 取消收藏
      const res = await cancelCollectSong(row.songId)
      if (res.code === 0) {
        updateAllSongLikeStatus(row.songId, 0)
        ElMessage.success('已取消喜欢')
      } else {
        ElMessage.error(res.message || '取消喜欢失败')
      }
    }
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

// 下载音乐
const downLoadMusic = (row: Song, e: Event) => {
  e.stopPropagation() // 阻止事件冒泡
  const link = document.createElement('a')
  link.href = row.audioUrl
  link.setAttribute('download', `${row.songName} - ${row.artistName}`)
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

// 删除歌曲
const handleDelete = async (song: Song, event: Event) => {
  event.stopPropagation()
  
  try {
    await ElMessageBox.confirm(
      `确定要删除歌曲《${song.songName}》吗？此操作不可恢复。`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )

    loading.value = true
    console.log('准备删除歌曲，songId:', song.songId)
    const response = await deleteOriginalSong(song.songId)
    console.log('删除响应:', response)
    
    if (response.code === 0) {
      ElMessage.success('删除成功')
      // 刷新列表
      fetchSongs()
    } else {
      ElMessage.error(response.message || '删除失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除歌曲失败:', error)
      console.error('错误详情:', error.response || error.message)
      // 如果错误响应中有message，显示它
      if (error.response?.data?.message) {
        ElMessage.error(error.response.data.message)
      } else if (error.message) {
        ElMessage.error(error.message)
      } else {
        ElMessage.error('删除失败，请稍后重试')
      }
    }
  } finally {
    loading.value = false
  }
}

// 刷新数据
const refreshData = () => {
  fetchSongs()
}

// 暴露方法给父组件
defineExpose({
  refreshData
})

// 组件挂载时获取数据
onMounted(() => {
  if (userStore.isLoggedIn) {
    fetchSongs()
  }
})
</script>

<style scoped>
.original-library {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  min-height: 500px;
}

.library-header {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
  align-items: center;
  flex-shrink: 0;
}

.search-section {
  flex: 1;
  max-width: 400px;
}

.filter-section {
  min-width: 150px;
}

:deep(.el-input__wrapper) {
  border-radius: 25px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

:deep(.el-select .el-input__wrapper) {
  border-radius: 20px;
}

:deep(.el-table__row) {
  background: transparent !important;
}

:deep(.el-table__row:hover) td {
  background: transparent !important;
}

:deep(.el-table__cell) {
  padding: 0 !important;
}

.empty-state {
  text-align: center;
  padding: 80px 20px;
  color: #999;
  flex: 1;
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
  margin: 0 0 30px 0;
}

.qr-dialog-content {
  text-align: center;
}

.qr-description {
  margin-bottom: 20px;
  font-size: 1rem;
  color: #666;
}

.qr-image {
  margin: 20px 0;
}

.qr-code {
  max-width: 200px;
  max-height: 200px;
  margin: 0 auto;
  border-radius: 10px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.qr-tip {
  font-size: 0.9rem;
  color: #999;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
}

@media (max-width: 768px) {
  .library-header {
    flex-direction: column;
    gap: 15px;
  }
}
</style>