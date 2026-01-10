<template>
  <div class="flex flex-col h-full flex-1 overflow-hidden bg-background px-4 py-4">
    <!-- 页面标题 -->
    <div class="mb-6">
      <h1 class="text-2xl font-semibold text-foreground mb-2 flex items-center gap-2">
        <icon-mdi:music-note class="text-primary" />
        上传原创歌曲
      </h1>
      <p class="text-sm text-muted-foreground">分享您的音乐作品，让更多人听到您的声音</p>
    </div>

    <div class="flex-1 overflow-y-auto">
      <div class="max-w-3xl mx-auto">
        <div class="bg-card rounded-xl border border-border p-6 shadow-sm">
      <form @submit.prevent="handleSubmit">
          <!-- 歌曲标题 -->
          <div class="mb-6">
            <label for="songName" class="block text-sm font-medium text-foreground mb-2 flex items-center gap-2">
              <icon-mdi:music class="text-primary" />
              歌曲标题 <span class="text-red-500">*</span>
            </label>
            <input
              id="songName"
              v-model="form.songName"
              type="text"
              required
              placeholder="请输入歌曲标题"
              class="flex h-10 w-full rounded-lg border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-0"
            />
          </div>

          <!-- 歌曲风格 -->
          <div class="mb-6">
            <label for="style" class="block text-sm font-medium text-foreground mb-2 flex items-center gap-2">
              <icon-mdi:playlist-music class="text-primary" />
              歌曲风格 <span class="text-red-500">*</span>
            </label>
            <select 
              id="style" 
              v-model="form.style" 
              required 
              class="flex h-10 w-full rounded-lg border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-0"
            >
              <option value="">请选择风格</option>
              <option value="流行">流行</option>
              <option value="摇滚">摇滚</option>
              <option value="电子">电子</option>
              <option value="古典">古典</option>
              <option value="爵士">爵士</option>
              <option value="民谣">民谣</option>
              <option value="嘻哈">嘻哈</option>
              <option value="其他">其他</option>
            </select>
          </div>

          <!-- 封面文件 -->
          <div class="mb-6">
            <label class="block text-sm font-medium text-foreground mb-2 flex items-center gap-2">
              <icon-mdi:image class="text-primary" />
              歌曲封面
            </label>
            <div 
              class="relative border-2 border-dashed border-border rounded-xl p-8 text-center cursor-pointer transition-all hover:border-primary/50 hover:bg-accent/50"
              @click="coverInput?.click()"
            >
              <input
                id="coverFile"
                ref="coverInput"
                type="file"
                accept="image/*"
                @change="handleCoverChange"
                class="hidden"
              />
              <div v-if="!coverPreview" class="flex flex-col items-center gap-3">
                <icon-mdi:cloud-upload class="text-4xl text-muted-foreground" />
                <p class="text-sm font-medium text-foreground">点击选择封面图片</p>
                <p class="text-xs text-muted-foreground">支持 JPG、PNG 格式，最大 10MB</p>
              </div>
              <div v-else class="relative inline-block">
                <img :src="coverPreview" alt="封面预览" class="max-w-[200px] max-h-[200px] rounded-lg object-cover shadow-md" />
                <button 
                  type="button" 
                  @click.stop="removeCover" 
                  class="absolute -top-2 -right-2 bg-red-500 text-white rounded-full w-7 h-7 flex items-center justify-center hover:bg-red-600 transition-colors shadow-md"
                >
                  <icon-mdi:close-circle class="text-lg" />
                </button>
              </div>
            </div>
          </div>

          <!-- 音频文件 -->
          <div class="mb-6">
            <label class="block text-sm font-medium text-foreground mb-2 flex items-center gap-2">
              <icon-mdi:file-music class="text-primary" />
              音频文件 <span class="text-red-500">*</span>
            </label>
            <div 
              class="relative border-2 border-dashed border-border rounded-xl p-8 text-center cursor-pointer transition-all hover:border-primary/50 hover:bg-accent/50"
              @click="audioInput?.click()"
            >
              <input
                id="audioFile"
                ref="audioInput"
                type="file"
                accept=".mp3,.wav"
                required
                @change="handleAudioChange"
                class="hidden"
              />
              <div v-if="!audioFile" class="flex flex-col items-center gap-3">
                <icon-mdi:cloud-upload class="text-4xl text-muted-foreground" />
                <p class="text-sm font-medium text-foreground">点击选择音频文件</p>
                <p class="text-xs text-muted-foreground">支持 MP3、WAV 格式，最大 100MB</p>
              </div>
              <div v-else class="flex items-center justify-between w-full p-4 bg-accent rounded-lg border border-border">
                <div class="flex items-center gap-4 flex-1 min-w-0">
                  <icon-mdi:file-music class="text-3xl text-primary flex-shrink-0" />
                  <div class="flex-1 min-w-0">
                    <p class="text-sm font-medium text-foreground truncate">{{ audioFileName }}</p>
                    <p class="text-xs text-muted-foreground">{{ formatFileSize(audioFileSize) }}</p>
                    <p v-if="audioDuration > 0" class="text-xs text-muted-foreground">
                      时长: {{ formatDuration(audioDuration) }}
                    </p>
                  </div>
                </div>
                <button 
                  type="button" 
                  @click.stop="removeAudio" 
                  class="ml-4 bg-red-500 text-white rounded-full w-7 h-7 flex items-center justify-center hover:bg-red-600 transition-colors flex-shrink-0"
                >
                  <icon-mdi:close-circle class="text-lg" />
                </button>
              </div>
            </div>
          </div>

          <!-- 打赏设置 -->
          <div class="mb-6">
            <label class="flex items-center gap-3 p-4 bg-accent rounded-lg cursor-pointer hover:bg-accent/80 transition-colors">
              <input
                v-model="form.isRewardEnabled"
                type="checkbox"
                @change="handleRewardToggle"
                class="w-4 h-4 rounded border-input text-primary focus:ring-2 focus:ring-ring focus:ring-offset-0"
              />
              <span class="flex items-center gap-2 text-sm font-medium text-foreground">
                <icon-mdi:gift class="text-primary" />
                开启打赏功能
              </span>
            </label>
          </div>

          <!-- 收款码文件 -->
          <div v-if="form.isRewardEnabled" class="mb-6 p-4 bg-primary/5 rounded-xl border border-primary/20">
            <label class="block text-sm font-medium text-foreground mb-2 flex items-center gap-2">
              <icon-mdi:qrcode class="text-primary" />
              收款码图片 <span class="text-red-500">*</span>
            </label>
            <div 
              class="relative border-2 border-dashed border-border rounded-xl p-6 text-center cursor-pointer transition-all hover:border-primary/50 hover:bg-accent/50"
              @click="qrInput?.click()"
            >
              <input
                id="rewardQrFile"
                ref="qrInput"
                type="file"
                accept="image/*"
                required
                @change="handleQrChange"
                class="hidden"
              />
              <div v-if="!qrPreview" class="flex flex-col items-center gap-3">
                <icon-mdi:cloud-upload class="text-4xl text-muted-foreground" />
                <p class="text-sm font-medium text-foreground">点击选择收款码图片</p>
                <p class="text-xs text-muted-foreground">支持 JPG、PNG 格式，最大 5MB</p>
              </div>
              <div v-else class="relative inline-block">
                <img :src="qrPreview" alt="收款码预览" class="max-w-[150px] max-h-[150px] rounded-lg object-cover shadow-md" />
                <button 
                  type="button" 
                  @click.stop="removeQr" 
                  class="absolute -top-2 -right-2 bg-red-500 text-white rounded-full w-7 h-7 flex items-center justify-center hover:bg-red-600 transition-colors shadow-md"
                >
                  <icon-mdi:close-circle class="text-lg" />
                </button>
              </div>
            </div>
          </div>

          <!-- 上传按钮 -->
          <div class="mt-8">
            <button 
              type="submit" 
              :disabled="isUploading" 
              class="inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-lg text-sm font-medium transition-colors bg-primary text-primary-foreground hover:bg-primary/90 h-11 px-8 w-full disabled:opacity-50 disabled:cursor-not-allowed shadow-md hover:shadow-lg"
            >
              <icon-mdi:upload v-if="!isUploading" class="text-lg" />
              <icon-mdi:loading v-else class="text-lg animate-spin" />
              {{ isUploading ? '上传中...' : '上传歌曲' }}
            </button>
          </div>
        </form>

        <!-- 上传进度 -->
        <div v-if="uploadProgress > 0" class="mt-6 p-4 bg-accent rounded-lg">
          <div class="flex justify-between items-center mb-3">
            <span class="text-sm font-medium text-foreground">上传进度</span>
            <span class="text-sm font-semibold text-primary">{{ uploadProgress }}%</span>
          </div>
          <div class="w-full h-2 bg-muted rounded-full overflow-hidden">
            <div
              class="h-full bg-primary transition-all duration-300 rounded-full"
              :style="{ width: uploadProgress + '%' }"
            ></div>
          </div>
        </div>

        <!-- 上传结果 -->
        <div v-if="uploadResult" class="mt-6">
          <div 
            v-if="uploadResult.success" 
            class="flex items-start gap-3 p-4 bg-green-500/10 border border-green-500/20 rounded-lg text-green-600 dark:text-green-400"
          >
            <icon-mdi:check-circle class="text-xl flex-shrink-0 mt-0.5" />
            <div class="flex-1">
              <p class="font-semibold mb-1">上传成功！</p>
              <p class="text-sm">{{ uploadResult.message }}</p>
            </div>
          </div>
          <div 
            v-else 
            class="flex items-start gap-3 p-4 bg-red-500/10 border border-red-500/20 rounded-lg text-red-600 dark:text-red-400"
          >
            <icon-mdi:alert-circle class="text-xl flex-shrink-0 mt-0.5" />
            <div class="flex-1">
              <p class="font-semibold mb-1">上传失败</p>
              <p class="text-sm">{{ uploadResult.message }}</p>
            </div>
          </div>
        </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { uploadOriginalSong } from '@/api/system'
import { ElNotification } from 'element-plus'
import { UserStore } from '@/stores/modules/user'

// 定义emit事件
const emit = defineEmits(['upload-success', 'show-login'])

// 用户store
const userStore = UserStore()

// 表单数据
const form = reactive({
  songName: '',
  style: '',
  isRewardEnabled: false
})

// 文件引用
const coverInput = ref(null)
const audioInput = ref(null)
const qrInput = ref(null)

// 预览图片
const coverPreview = ref('')
const qrPreview = ref('')

// 音频文件信息（用于响应式显示）
const audioFile = ref(null)
const audioFileName = ref('')
const audioFileSize = ref(0)

// 音频时长
const audioDuration = ref(0)

// 上传状态
const isUploading = ref(false)
const uploadProgress = ref(0)
const uploadResult = ref(null)

// 处理封面文件选择
const handleCoverChange = (event) => {
  const file = event.target.files[0]
  if (file) {
    // 验证文件大小（10MB）
    if (file.size > 10 * 1024 * 1024) {
      alert('封面文件大小不能超过10MB')
      if (coverInput.value) {
        coverInput.value.value = ''
      }
      return
    }

    // 生成预览
    const reader = new FileReader()
    reader.onload = (e) => {
      coverPreview.value = e.target.result
    }
    reader.readAsDataURL(file)
  } else {
    coverPreview.value = ''
  }
}

// 处理音频文件选择
const handleAudioChange = (event) => {
  const file = event.target.files[0]
  if (file) {
    // 验证文件格式
    const allowedTypes = ['audio/mpeg', 'audio/wav', 'audio/x-wav']
    if (!allowedTypes.includes(file.type) && !file.name.toLowerCase().endsWith('.mp3') && !file.name.toLowerCase().endsWith('.wav')) {
      alert('仅支持MP3和WAV格式的音频文件')
      if (audioInput.value) {
        audioInput.value.value = ''
      }
      audioFile.value = null
      audioFileName.value = ''
      audioFileSize.value = 0
      audioDuration.value = 0
      return
    }

    // 验证文件大小（100MB）
    if (file.size > 100 * 1024 * 1024) {
      alert('文件大于100M，请压缩后上传')
      if (audioInput.value) {
        audioInput.value.value = ''
      }
      audioFile.value = null
      audioFileName.value = ''
      audioFileSize.value = 0
      audioDuration.value = 0
      return
    }

    // 更新响应式变量，立即显示文件信息
    audioFile.value = file
    audioFileName.value = file.name
    audioFileSize.value = file.size

    // 获取音频时长
    const audio = new Audio()
    const objectUrl = URL.createObjectURL(file)
    audio.src = objectUrl
    
    audio.addEventListener('loadedmetadata', () => {
      if (audio.duration && isFinite(audio.duration)) {
        // 将秒数转换为字符串格式（保留2位小数）
        audioDuration.value = parseFloat(audio.duration.toFixed(2))
        console.log('音频时长:', audioDuration.value, '秒')
      } else {
        audioDuration.value = 0
        console.warn('无法获取音频时长')
      }
      URL.revokeObjectURL(objectUrl)
    })

    audio.addEventListener('error', () => {
      console.error('音频文件加载失败')
      audioDuration.value = 0
      URL.revokeObjectURL(objectUrl)
    })
  } else {
    audioFile.value = null
    audioFileName.value = ''
    audioFileSize.value = 0
    audioDuration.value = 0
  }
}

// 处理打赏开关
const handleRewardToggle = () => {
  if (!form.isRewardEnabled) {
    // 关闭打赏时清空收款码
    if (qrInput.value) {
      qrInput.value.value = ''
    }
    qrPreview.value = ''
  }
}

// 移除封面
const removeCover = () => {
  if (coverInput.value) {
    coverInput.value.value = ''
  }
  coverPreview.value = ''
}

// 移除音频
const removeAudio = () => {
  if (audioInput.value) {
    audioInput.value.value = ''
  }
  audioFile.value = null
  audioFileName.value = ''
  audioFileSize.value = 0
  audioDuration.value = 0
}

// 移除收款码
const removeQr = () => {
  if (qrInput.value) {
    qrInput.value.value = ''
  }
  qrPreview.value = ''
}

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 Bytes'
  const k = 1024
  const sizes = ['Bytes', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

// 格式化时长
const formatDuration = (seconds) => {
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins}:${secs.toString().padStart(2, '0')}`
}

// 处理收款码文件选择
const handleQrChange = (event) => {
  const file = event.target.files[0]
  if (file) {
    // 验证文件大小（5MB）
    if (file.size > 5 * 1024 * 1024) {
      alert('收款码图片大小不能超过5MB')
      if (qrInput.value) {
        qrInput.value.value = ''
      }
      return
    }

    // 生成预览
    const reader = new FileReader()
    reader.onload = (e) => {
      qrPreview.value = e.target.result
    }
    reader.readAsDataURL(file)
  } else {
    qrPreview.value = ''
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!form.songName.trim()) {
    alert('请输入歌曲标题')
    return
  }

  if (!form.style) {
    alert('请选择歌曲风格')
    return
  }

  if (!audioInput.value || !audioInput.value.files || !audioInput.value.files[0]) {
    alert('请上传音频文件')
    return
  }

  if (form.isRewardEnabled && (!qrInput.value || !qrInput.value.files || !qrInput.value.files[0])) {
    alert('开启打赏功能必须上传收款码图片')
    return
  }

  // 检查是否已获取音频时长
  if (audioDuration.value === 0) {
    alert('正在解析音频时长，请稍候...')
    return
  }

  // 准备FormData
  const formData = new FormData()
  formData.append('songName', form.songName.trim())
  formData.append('style', form.style)
  formData.append('isRewardEnabled', form.isRewardEnabled.toString())
  formData.append('duration', audioDuration.value.toString()) // 添加时长

  if (coverInput.value && coverInput.value.files && coverInput.value.files[0]) {
    formData.append('coverFile', coverInput.value.files[0])
  }

  formData.append('audioFile', audioInput.value.files[0])

  if (form.isRewardEnabled && qrInput.value && qrInput.value.files && qrInput.value.files[0]) {
    formData.append('rewardQrFile', qrInput.value.files[0])
  }

  // 调试信息
  console.log('FormData contents:')
  for (let [key, value] of formData.entries()) {
    console.log(key, value)
  }

  // 检查用户登录状态
  if (!userStore.isLoggedIn || !userStore.userInfo?.token) {
    ElNotification({
      type: 'warning',
      title: '请先登录',
      message: '您需要登录后才能上传歌曲',
      duration: 3000,
    })
    // 通知父组件显示登录对话框
    emit('show-login')
    return
  }

  // 检查用户积分
  const userScore = userStore.userInfo?.score ?? 100
  if (userScore <= 0) {
    ElNotification({
      type: 'warning',
      title: '发布权限不足',
      message: '当前账号无发布权限，积分不足（积分为0时无法发帖、发歌、回复）',
      duration: 3000,
    })
    return
  }

  try {
    isUploading.value = true
    uploadProgress.value = 0

    // 模拟上传进度（实际项目中可以通过axios的onUploadProgress实现）
    const progressInterval = setInterval(() => {
      if (uploadProgress.value < 90) {
        uploadProgress.value += 10
      }
    }, 200)

    const response = await uploadOriginalSong(formData)

    clearInterval(progressInterval)
    uploadProgress.value = 100

    // 检查响应状态码
    if (response.code !== 0) {
      // 如果返回错误，显示错误信息
      let errorMessage = response.message || '上传失败，请重试'
      
      // 如果是个人信息不完整的错误，显示用户友好的提示
      if (errorMessage.includes('个人信息') || errorMessage.includes('生日') || errorMessage.includes('国籍') || errorMessage.includes('简介')) {
        errorMessage = '请填写个人完整信息后再上传'
      }
      
      uploadResult.value = {
        success: false,
        message: errorMessage
      }
      return
    }

    uploadResult.value = {
      success: true,
      message: response.message || '上传成功！您的歌曲已提交审核，审核通过后将在曲库中显示。'
    }

    // 重置表单
    resetForm()

    // 通知父组件上传成功
    emit('upload-success')

  } catch (error) {
    console.error('Upload error:', error)
    console.error('Error response:', error.response)
    console.error('Error message:', error.message)

    let errorMessage = '上传失败，请重试'
    if (error.response?.data?.message) {
      errorMessage = error.response.data.message
    } else if (error.response?.status === 401) {
      errorMessage = '登录已过期，请重新登录'
    } else if (error.response?.status === 500) {
      errorMessage = '服务器错误，请稍后重试'
    } else if (error.message) {
      errorMessage = error.message
    }

    uploadResult.value = {
      success: false,
      message: errorMessage
    }
  } finally {
    isUploading.value = false
    uploadProgress.value = 0
  }
}

// 组件挂载时检查登录状态
onMounted(() => {
  if (!userStore.isLoggedIn || !userStore.userInfo?.token) {
    ElNotification({
      type: 'warning',
      title: '请先登录',
      message: '您需要登录后才能上传歌曲',
      duration: 3000,
    })
    // 通知父组件显示登录对话框
    emit('show-login')
  }
})

// 重置表单
const resetForm = () => {
  form.songName = ''
  form.style = ''
  form.isRewardEnabled = false

  if (coverInput.value) {
    coverInput.value.value = ''
  }
  if (audioInput.value) {
    audioInput.value.value = ''
  }
  if (qrInput.value) {
    qrInput.value.value = ''
  }

  coverPreview.value = ''
  qrPreview.value = ''
  audioFile.value = null
  audioFileName.value = ''
  audioFileSize.value = 0
  audioDuration.value = 0
}
</script>

<style scoped>
/* 使用 Tailwind CSS 类，这里只保留必要的自定义样式 */
</style>
