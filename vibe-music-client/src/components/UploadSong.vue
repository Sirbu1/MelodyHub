<template>
  <div class="upload-song-container">
    <h2>上传原创歌曲</h2>

    <div class="upload-form">
      <form @submit.prevent="handleSubmit">
        <!-- 歌曲标题 -->
        <div class="form-group">
          <label for="songName">歌曲标题 *</label>
          <input
            id="songName"
            v-model="form.songName"
            type="text"
            required
            placeholder="请输入歌曲标题"
          />
        </div>

        <!-- 歌曲风格 -->
        <div class="form-group">
          <label for="style">歌曲风格 *</label>
          <select id="style" v-model="form.style" required>
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
        <div class="form-group">
          <label for="coverFile">歌曲封面</label>
          <input
            id="coverFile"
            ref="coverInput"
            type="file"
            accept="image/*"
            @change="handleCoverChange"
          />
          <div v-if="coverPreview" class="image-preview">
            <img :src="coverPreview" alt="封面预览" />
          </div>
        </div>

        <!-- 音频文件 -->
        <div class="form-group">
          <label for="audioFile">音频文件 *</label>
          <input
            id="audioFile"
            ref="audioInput"
            type="file"
            accept=".mp3,.wav"
            required
            @change="handleAudioChange"
          />
          <small class="file-hint">支持MP3、WAV格式，文件大小不超过100MB</small>
        </div>

        <!-- 打赏设置 -->
        <div class="form-group">
          <label class="checkbox-label">
            <input
              v-model="form.isRewardEnabled"
              type="checkbox"
              @change="handleRewardToggle"
            />
            开启打赏功能
          </label>
        </div>

        <!-- 收款码文件 -->
        <div v-if="form.isRewardEnabled" class="form-group">
          <label for="rewardQrFile">收款码图片 *</label>
          <input
            id="rewardQrFile"
            ref="qrInput"
            type="file"
            accept="image/*"
            required
            @change="handleQrChange"
          />
          <div v-if="qrPreview" class="image-preview">
            <img :src="qrPreview" alt="收款码预览" />
          </div>
        </div>

        <!-- 上传按钮 -->
        <div class="form-actions">
          <button type="submit" :disabled="isUploading" class="upload-btn">
            {{ isUploading ? '上传中...' : '上传歌曲' }}
          </button>
        </div>
      </form>

      <!-- 上传进度 -->
      <div v-if="uploadProgress > 0" class="upload-progress">
        <div class="progress-bar">
          <div
            class="progress-fill"
            :style="{ width: uploadProgress + '%' }"
          ></div>
        </div>
        <span>{{ uploadProgress }}%</span>
      </div>
    </div>

    <!-- 上传结果 -->
    <div v-if="uploadResult" class="upload-result">
      <div v-if="uploadResult.success" class="success-message">
        ✅ {{ uploadResult.message }}
      </div>
      <div v-else class="error-message">
        ❌ {{ uploadResult.message }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { uploadOriginalSong } from '@/api/system'

// 定义emit事件
const emit = defineEmits(['upload-success'])

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
      audioDuration.value = 0
      return
    }

    // 验证文件大小（100MB）
    if (file.size > 100 * 1024 * 1024) {
      alert('文件大于100M，请压缩后上传')
      if (audioInput.value) {
        audioInput.value.value = ''
      }
      audioDuration.value = 0
      return
    }

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
  const { UserStore } = await import('@/stores/modules/user')
  const userStore = UserStore()
  console.log('User token:', userStore.userInfo?.token)
  console.log('Is logged in:', userStore.isLoggedIn)

  if (!userStore.isLoggedIn || !userStore.userInfo?.token) {
    alert('请先登录后再上传歌曲')
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
  audioDuration.value = 0
}
</script>

<style scoped>
.upload-song-container {
  max-width: 600px;
  margin: 0 auto;
  padding: 20px;
  min-height: 100%;
  box-sizing: border-box;
}

.upload-form {
  background: #f9f9f9;
  padding: 20px;
  border-radius: 8px;
  margin-top: 20px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: bold;
}

.form-group input[type="text"],
.form-group select {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.form-group input[type="file"] {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.checkbox-label {
  display: flex;
  align-items: center;
  cursor: pointer;
  font-weight: normal;
}

.checkbox-label input {
  margin-right: 8px;
}

.file-hint {
  display: block;
  margin-top: 5px;
  color: #666;
  font-size: 12px;
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

.form-actions {
  margin-top: 30px;
}

.upload-btn {
  background: #007bff;
  color: white;
  padding: 12px 24px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
  width: 100%;
}

.upload-btn:hover:not(:disabled) {
  background: #0056b3;
}

.upload-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.upload-progress {
  margin-top: 20px;
  text-align: center;
}

.progress-bar {
  width: 100%;
  height: 20px;
  background: #e9ecef;
  border-radius: 10px;
  overflow: hidden;
  margin-bottom: 10px;
}

.progress-fill {
  height: 100%;
  background: #007bff;
  transition: width 0.3s ease;
}

.upload-result {
  margin-top: 20px;
  padding: 15px;
  border-radius: 4px;
}

.success-message {
  background: #d4edda;
  color: #155724;
  border: 1px solid #c3e6cb;
}

.error-message {
  background: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
}
</style>
