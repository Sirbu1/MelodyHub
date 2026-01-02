<script setup lang="ts">
import { getRecommendedSongs, getBanner } from '@/api/system'
// import { getRecommendedPlaylists } from '@/api/system' // 已注释，不再使用
import coverImg from '@/assets/cover.png'
import { formatTime, replaceUrlParams } from '@/utils'
import { ElNotification } from 'element-plus'
import { UserStore } from '@/stores/modules/user'
const router = useRouter()
const audio = AudioStore()
const user = UserStore()

const { loadTrack, play } = useAudioPlayer()

const bannerList = ref<{ bannerId: number; bannerUrl: string }[]>([])

// 推荐歌单（已注释，不再使用）
// const recommendedPlaylist = ref([])
// 今日为你推荐 - 推荐歌曲
const todayRecommendedSongs = ref([])
// 相似推荐 - 推荐歌曲
const similarRecommendedSongs = ref([])

// 监听用户登录状态
watch(
  () => user.isLoggedIn,
  (newVal) => {
    if (newVal) {
      // 用户登录后重新获取推荐数据
      getRecommendedData()
    }
  }
)

// 获取轮播图数据
const fetchBannerData = async () => {
  try {
    const result = await getBanner()
    if (result.code === 0 && Array.isArray(result.data)) {
      bannerList.value = result.data
    } else {
      ElNotification({
        type: 'error',
        message: '获取轮播图失败',
        duration: 2000,
      })
    }
  } catch (error) {
    console.error('Error fetching banner data:', error)
    ElNotification({
      type: 'error',
      message: '获取轮播图时发生错误',
      duration: 2000,
    })
  }
}

// 获取推荐数据
const getRecommendedData = async () => {
  // 获取推荐歌单（已注释，不再使用）
  // const result = await getRecommendedPlaylists()
  // if (result.code === 0 && Array.isArray(result.data)) {
  //   recommendedPlaylist.value = result.data.map((item) => ({
  //     playlistId: item.playlistId,
  //     title: item.title,
  //     coverUrl: item.coverUrl ?? coverImg,
  //   }))
  // } else {
  //   ElNotification({
  //     type: 'error',
  //     message: '获取推荐歌单失败',
  //     duration: 2000,
  //   })
  // }

  // 获取推荐歌曲
  await handleRefreshTodaySongs()
  await handleRefreshSimilarSongs()
}

onMounted(async () => {
  // 获取轮播图数据
  fetchBannerData()
  // 初始化时获取推荐数据
  getRecommendedData()
})

// 转换歌曲数据格式
const transformSongData = (data: any[]) => {
  return data.map((item) => ({
    id: item.songId,
    name: item.songName,
    artists: [
      {
        name: item.artistName,
      },
    ],
    duration: item.duration,
    audioUrl: item.audioUrl,
    coverUrl: item.coverUrl || '',  // 添加封面URL
    likeStatus: item.likeStatus || 0  // 从服务端获取收藏状态
  }))
}

// 刷新今日为你推荐
const handleRefreshTodaySongs = async () => {
  try {
    const result = await getRecommendedSongs()
    if (result.code === 0 && Array.isArray(result.data)) {
      const transformedSongs = transformSongData(result.data)
      // 今日为你推荐：取前10首
      todayRecommendedSongs.value = transformedSongs.slice(0, 10)
    } else {
      // 静默失败，不显示错误提示，避免干扰用户
      todayRecommendedSongs.value = []
    }
  } catch (error) {
    console.error('获取推荐歌曲失败:', error)
    // 静默失败，不显示错误提示
    todayRecommendedSongs.value = []
  }
}

// 刷新相似推荐
const handleRefreshSimilarSongs = async () => {
  try {
    const result = await getRecommendedSongs()
    if (result.code === 0 && Array.isArray(result.data)) {
      const transformedSongs = transformSongData(result.data)
      // 相似推荐：取第10首之后的歌曲，如果不足则重新获取一次
      if (transformedSongs.length > 10) {
        similarRecommendedSongs.value = transformedSongs.slice(10)
      } else {
        // 如果第一次获取的歌曲不足，再获取一次并排除已显示的歌曲
        try {
          const secondResult = await getRecommendedSongs()
          if (secondResult.code === 0 && Array.isArray(secondResult.data)) {
            const secondTransformedSongs = transformSongData(secondResult.data)
            // 排除今日推荐中已显示的歌曲ID
            const todaySongIds = new Set(todayRecommendedSongs.value.map(s => s.id))
            similarRecommendedSongs.value = secondTransformedSongs.filter(s => !todaySongIds.has(s.id))
          } else {
            similarRecommendedSongs.value = []
          }
        } catch (error) {
          console.error('获取相似推荐歌曲失败:', error)
          similarRecommendedSongs.value = []
        }
      }
    } else {
      // 静默失败，不显示错误提示
      similarRecommendedSongs.value = []
    }
  } catch (error) {
    console.error('获取相似推荐歌曲失败:', error)
    // 静默失败，不显示错误提示
    similarRecommendedSongs.value = []
  }
}

// 转换歌曲实体
const convertToTrackModel = (song: any) => {
  return {
    id: song.id.toString(),
    title: song.name,
    artist: song.artists.map((artist: any) => artist.name).join(', '),
    cover: song.coverUrl || '',
    url: song.audioUrl,
    duration: song.duration,
    likeStatus: song.likeStatus || 0  // 保持收藏状态
  }
}

const handlePlaylclick = async (row: any, source: 'today' | 'similar' = 'today') => {
  // 根据来源选择对应的推荐列表
  const sourceList = source === 'today' ? todayRecommendedSongs.value : similarRecommendedSongs.value
  
  // 将所有推荐歌曲转换为 trackModel
  const allTracks = sourceList
    .map(song => convertToTrackModel(song))
    .filter(track => track !== null)

  // 找到当前选中歌曲的索引
  const selectedIndex = sourceList.findIndex(song => song.id === row.id)

  // 清空现有播放列表并添加所有歌曲
  audio.setAudioStore('trackList', allTracks)
  // 设置当前播放索引为选中的歌曲
  audio.setAudioStore('currentSongIndex', selectedIndex)

  // 播放
  await loadTrack()
  play()
}

// 判断是否是当前播放的歌曲
const isCurrentPlaying = (songId: number) => {
  const currentTrack = audio.trackList[audio.currentSongIndex]
  return currentTrack && Number(currentTrack.id) === songId
}
</script>
<template>
  <div class="flex gap-6 p-4 w-full">
    <div class="flex-1">
      <div class="w-full flex flex-col overflow-hidden mb-8">
        <!-- banner -->
        <el-carousel :interval="4000" type="card" height="260px">
          <el-carousel-item v-for="item in bannerList" :key="item.bannerId">
            <img :src="item.bannerUrl" class="w-full h-full object-cover rounded-lg" />
          </el-carousel-item>
        </el-carousel>

        <!-- 今日为你推荐 - 推荐歌曲 -->
        <div class="mt-6">
          <div class="flex justify-between items-center mb-4">
            <h2 class="text-xl font-semibold">今日为你推荐</h2>
            <button @click="handleRefreshTodaySongs()"
              class="inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-md text-sm font-medium ring-offset-background transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 [&amp;_svg]:pointer-events-none [&amp;_svg]:size-4 [&amp;_svg]:shrink-0 text-primary underline-offset-4 hover:underline h-10 px-4 py-2">
              <icon-tabler:refresh class="text-lg" />
              刷新
            </button>
          </div>
          <el-scrollbar class="h-full" overflow-auto>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4 gap-x-16">
              <button v-for="item in todayRecommendedSongs" :key="item.id"
                class="grid grid-cols-[auto_2fr_1fr] items-center gap-4 transition duration-300 rounded-2xl w-full group"
                :class="[
                  isCurrentPlaying(item.id) ? 'bg-hoverMenuBg' : 'hover:bg-hoverMenuBg'
                ]" @click.stop="handlePlaylclick(item, 'today')">
                <!-- 专辑封面 -->
                <div class="w-16 h-16 rounded-2xl overflow-hidden relative">
                  <el-image :alt="item.name" width="64" height="64" class="w-full h-full object-cover"
                    :src="item.coverUrl ? (item.coverUrl + '?param=90y90') : coverImg" />
                  <!-- Play 按钮，使用 group-hover 控制透明度 -->
                  <button @click.stop="handlePlaylclick(item, 'today')"
                    class="absolute inset-0 flex items-center justify-center text-white opacity-0 transition-opacity duration-300 z-10 group-hover:opacity-100 group-hover:bg-black/50">
                    <icon-tabler:player-play-filled class="text-lg" />
                  </button>
                </div>

                <div class="truncate text-left ml-1">
                  <!-- 歌曲名称 -->
                  <h3 class="font-medium">{{ item.name }}</h3>
                  <!-- 艺术家 -->
                  <p class="text-sm text-muted-foreground line-clamp-1">
                    {{item.artists.map((item) => item.name).join(' ')}}
                  </p>
                </div>

                <!-- 时长 -->
                <div class="text-right mr-5">
                  <p class="text-sm text-muted-foreground line-clamp-1">
                    {{ formatTime(item.duration) }}
                  </p>
                </div>
              </button>
            </div>
          </el-scrollbar>
        </div>
      </div>

      <!-- 歌曲 -->
      <div class="w-full">
        <div class="flex justify-between items-center mb-4">
          <h2 class="text-xl font-semibold mb-4">相似推荐</h2>
          <button @click="handleRefreshSimilarSongs()"
            class="inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-md text-sm font-medium ring-offset-background transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 [&amp;_svg]:pointer-events-none [&amp;_svg]:size-4 [&amp;_svg]:shrink-0 text-primary underline-offset-4 hover:underline h-10 px-4 py-2">
            <icon-tabler:refresh class="text-lg" />
            刷新
          </button>
        </div>
        <el-scrollbar class="h-full" overflow-auto>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4 gap-x-16">
            <button v-for="item in similarRecommendedSongs" :key="item.id"
              class="grid grid-cols-[auto_2fr_1fr] items-center gap-4 transition duration-300 rounded-2xl w-full group"
              :class="[
                isCurrentPlaying(item.id) ? 'bg-hoverMenuBg' : 'hover:bg-hoverMenuBg'
              ]" @click.stop="handlePlaylclick(item, 'similar')">
              <!-- 专辑封面 -->
              <div class="w-16 h-16 rounded-2xl overflow-hidden relative">
                <el-image :alt="item.name" width="64" height="64" class="w-full h-full object-cover"
                  :src="item.coverUrl ? (item.coverUrl + '?param=90y90') : coverImg" />
                <!-- Play 按钮，使用 group-hover 控制透明度 -->
                <button @click.stop="handlePlaylclick(item, 'similar')"
                  class="absolute inset-0 flex items-center justify-center text-white opacity-0 transition-opacity duration-300 z-10 group-hover:opacity-100 group-hover:bg-black/50">
                  <icon-tabler:player-play-filled class="text-lg" />
                </button>
              </div>

              <div class="truncate text-left ml-1">
                <!-- 歌曲名称 -->
                <h3 class="font-medium">{{ item.name }}</h3>
                <!-- 艺术家 -->
                <p class="text-sm text-muted-foreground line-clamp-1">
                  {{item.artists.map((item) => item.name).join(' ')}}
                </p>
              </div>

              <!-- 时长 -->
              <div class="text-right mr-5">
                <p class="text-sm text-muted-foreground line-clamp-1">
                  {{ formatTime(item.duration) }}
                </p>
              </div>
            </button>
          </div>
        </el-scrollbar>
      </div>
    </div>
  </div>
</template>

<style scoped>
:deep(.el-carousel__item) {
  --el-carousel-item-scale: 1.2;
  /* 默认是 0.83，可以调整 */
}

/* 让所有图片撑满 */
.el-carousel__item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 20px;
  /* 圆角 */
}

.playlist-title {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  text-align: left;
  min-height: 2.5em;
  /* 固定两行高度 */
  line-height: 1.25;
  /* 行高 */
  overflow: hidden;
  text-overflow: ellipsis;
  display: flex;
  align-items: center;
  /* 单行时垂直居中 */
}
</style>
