<script setup lang="ts">
import { ref, reactive, onMounted, onActivated, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UserStore } from '@/stores/modules/user'
import defaultAvatar from '@/assets/user.jpg'
import { updateUserInfo, updateUserAvatar, deleteUser, getUserInfo, getOrdersByAccepter } from '@/api/system'
import 'vue-cropper/dist/index.css'
import { VueCropper } from "vue-cropper";
import { useRouter, useRoute } from 'vue-router'
import AuthTabs from '@/components/Auth/AuthTabs.vue'
// import UploadSong from '@/components/UploadSong.vue' // 已注释，个人中心不再显示上传歌曲

const router = useRouter()
const route = useRoute()
const userStore = UserStore()
const loading = ref(false)
const userFormRef = ref<FormInstance>()
const cropperVisible = ref(false)
const cropperImg = ref('')
const cropper = ref<any>(null)
const authVisible = ref(false)
const activeTab = ref('profile')

// 接单列表相关
const orders = ref<any[]>([])
const orderLoading = ref(false)
const orderCurrentPage = ref(1)
const orderPageSize = ref(10)
const orderTotal = ref(0)
const orderStatusFilter = ref<number | null>(null)

const userForm = reactive({
  userId: userStore.userInfo.userId,
  username: userStore.userInfo.username || '',
  phone: userStore.userInfo.phone || '',
  email: userStore.userInfo.email || '',
  introduction: userStore.userInfo.introduction || '',
  gender: userStore.userInfo.gender || null,
  birth: userStore.userInfo.birth || '',
  area: userStore.userInfo.area || ''
})

// 监听登录状态变化，如果退出登录则清空表单
watch(() => userStore.isLoggedIn, (isLoggedIn) => {
  if (!isLoggedIn) {
    // 清空表单数据
    userForm.userId = undefined
    userForm.username = ''
    userForm.phone = ''
    userForm.email = ''
    userForm.introduction = ''
    userForm.gender = null
    userForm.birth = ''
    userForm.area = ''
    // 关闭裁剪弹窗
    cropperVisible.value = false
    cropperImg.value = ''
  } else {
    // 如果重新登录，更新表单数据
    userForm.userId = userStore.userInfo.userId
    userForm.username = userStore.userInfo.username || ''
    userForm.phone = userStore.userInfo.phone || ''
    userForm.email = userStore.userInfo.email || ''
    userForm.introduction = userStore.userInfo.introduction || ''
    userForm.gender = userStore.userInfo.gender || null
    userForm.birth = userStore.userInfo.birth || ''
    userForm.area = userStore.userInfo.area || ''
  }
})

// 表单验证规则
const userRules = reactive<FormRules>({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    {
      pattern: /^[a-zA-Z0-9_-]{4,16}$/,
      message: '用户名格式：4-16位字符（字母、数字、下划线、连字符）',
      trigger: 'blur',
    },
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
  introduction: [
    { max: 100, message: '简介不能超过100个字符', trigger: 'blur' },
  ],
})

// 检查登录状态
onMounted(() => {
  if (!userStore.isLoggedIn) {
    authVisible.value = true
  }
})

// 处理头像上传
const handleAvatarClick = () => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*'
  input.onchange = (e: Event) => {
    const target = e.target as HTMLInputElement
    const file = target.files?.[0]
    if (file) {
      const reader = new FileReader()
      reader.onload = (e) => {
        const result = e.target?.result
        if (typeof result === 'string') {
          cropperImg.value = result
          cropperVisible.value = true
        }
      }
      reader.readAsDataURL(file)
    }
  }
  input.click()
}

// 重置裁剪
const reset = () => {
  if (cropper.value) {
    cropper.value.refresh()
  }
}

// 缩放
const changeScale = (num: number) => {
  if (cropper.value) {
    cropper.value.changeScale(num)
  }
}

// 向左旋转
const rotateLeft = () => {
  if (cropper.value) {
    cropper.value.rotateLeft()
  }
}

// 向右旋转
const rotateRight = () => {
  if (cropper.value) {
    cropper.value.rotateRight()
  }
}

// 确认裁剪
const handleCropConfirm = async () => {
  if (!cropper.value) return
  cropper.value.getCropData(async (base64: string) => {
    try {
      const response = await fetch(base64)
      const blob = await response.blob()

      const formData = new FormData()
      formData.append('avatar', blob, 'avatar.png')

      const res = await updateUserAvatar(formData)

      if (res.code === 0) {
        // 重新获取用户信息以更新头像URL
        const userInfoResponse = await getUserInfo()
        if (userInfoResponse.code === 0) {
          userStore.setUserInfo(userInfoResponse.data, userStore.userInfo.token)
          ElMessage.success('头像更新成功')
          cropperVisible.value = false
          cropperImg.value = ''
        } else {
          ElMessage.error(userInfoResponse.message || '获取用户信息失败')
        }
      } else {
        ElMessage.error(res.message || '头像更新失败')
      }
    } catch (error: any) {
      console.error('头像更新错误:', error)
      ElMessage.error(error.message || '头像更新失败')
    }
  })
}

// 处理表单提交
const handleSubmit = async () => {
  if (!userFormRef.value) return
  await userFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const response = await updateUserInfo(userForm)
        if (response.code === 0) {
          const userInfoResponse = await getUserInfo()
          userStore.setUserInfo(userInfoResponse.data, userStore.userInfo.token)
          ElMessage.success('更新成功')
        } else {
          ElMessage.error(response.message || '更新失败')
        }
      } catch (error: any) {
        ElMessage.error(error.message || '更新失败')
      } finally {
        loading.value = false
      }
    }
  })
}

// 处理账号注销
const handleDelete = async () => {
  try {
    await ElMessageBox.confirm(
      '注销账号后，所有数据将被清除且无法恢复，是否确认注销？',
      '警告',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    loading.value = true
    const response = await deleteUser()
    if (response.code === 0) {
      userStore.clearUserInfo()
      ElMessage.success('账号已注销')
      router.push('/')
    } else {
      ElMessage.error(response.message || '注销失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '注销失败')
    }
  } finally {
    loading.value = false
  }
}

// 获取接单列表
const getOrders = async () => {
  if (!userStore.isLoggedIn) return
  
  orderLoading.value = true
  try {
    const res = await getOrdersByAccepter(orderCurrentPage.value, orderPageSize.value, orderStatusFilter.value)
    if (res.code === 0 && res.data) {
      orders.value = res.data.items || []
      orderTotal.value = res.data.total || 0
    }
  } catch (error) {
    console.error('获取接单列表失败', error)
  } finally {
    orderLoading.value = false
  }
}

// 接单状态筛选变化
const handleOrderStatusChange = () => {
  orderCurrentPage.value = 1
  getOrders()
}

// 接单列表分页变化
const handleOrderPageChange = () => {
  getOrders()
}

// 跳转到帖子详情
const goToPostDetail = (postId: number) => {
  router.push(`/forum/${postId}`)
}

// 格式化时间
const formatOrderTime = (time: string) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString('zh-CN', { 
    year: 'numeric', 
    month: '2-digit', 
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 获取状态文本
const getOrderStatusText = (status: number) => {
  switch (status) {
    case 0:
      return '待同意'
    case 1:
      return '已接单未完成'
    case 2:
      return '已完成'
    case 3:
      return '已拒绝'
    default:
      return '未知'
  }
}

// 获取状态颜色
const getOrderStatusColor = (status: number) => {
  switch (status) {
    case 0:
      return 'text-yellow-600 bg-yellow-500/10'
    case 1:
      return 'text-blue-600 bg-blue-500/10'
    case 2:
      return 'text-green-600 bg-green-500/10'
    case 3:
      return 'text-red-600 bg-red-500/10'
    default:
      return 'text-gray-600 bg-gray-500/10'
  }
}

// 监听标签页切换
watch(activeTab, (newTab) => {
  if (newTab === 'orders') {
    getOrders()
  }
})

// 监听路由变化，当从帖子详情页返回时刷新接单列表
watch(() => route.path, (newPath, oldPath) => {
  // 如果从帖子详情页返回到个人中心页面，且当前在接单标签页，刷新数据
  if (newPath === '/user' && oldPath && oldPath.startsWith('/forum/')) {
    if (activeTab.value === 'orders' && userStore.isLoggedIn) {
      // 延迟一下，确保页面已经加载完成
      setTimeout(() => {
        getOrders()
      }, 100)
    }
  }
})

// 页面激活时刷新数据（从其他页面返回时）
onActivated(() => {
  // 如果当前在接单标签页，刷新数据
  if (activeTab.value === 'orders' && userStore.isLoggedIn) {
    getOrders()
  }
})
</script>

<template>
  <div class="user-container">
    <h2 class="username">个人中心</h2>

    <!-- 选项卡导航 -->
    <el-tabs v-model="activeTab" class="user-tabs">
      <el-tab-pane label="个人信息" name="profile">
        <!-- 个人信息内容 -->

        <div class="section">
      <div class="section-title">头像</div>
      <div class="user-header">
        <div class="avatar-wrapper" @click="handleAvatarClick">
          <el-avatar :src="userStore.userInfo.avatarUrl || defaultAvatar" :size="100" />
          <div class="avatar-hover">
            <icon-ic:outline-photo-camera class="camera-icon" />
            <span>更新头像</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 头像裁剪弹窗 -->
    <el-dialog v-model="cropperVisible" title="裁剪头像" width="600px" :close-on-click-modal="false"
      :close-on-press-escape="false">
      <div class="cropper-container">
        <vue-cropper ref="cropper" :img="cropperImg" :info="true" :canScale="true" :autoCrop="true" :fixedBox="true"
          :canMove="true" :canMoveBox="true" :centerBox="true" :infoTrue="true" :fixed="true" :fixedNumber="[1, 1]"
          :high="true" mode="cover" :round="true" />
      </div>
      <template #footer>
        <div class="dialog-footer">
          <div class="flex justify-between items-center w-full">
            <div class="flex">
              <el-button size="mini" type="info" @click="reset" class="mr-1">重置</el-button>
              <el-button size="mini" plain @click="changeScale(1)" class="mr-1">
                <icon-ph:magnifying-glass-plus-light class="mr-0.5" />放大
              </el-button>
              <el-button size="mini" plain @click="changeScale(-1)" class="mr-1">
                <icon-ph:magnifying-glass-minus-light class="mr-0.5" />缩小
              </el-button>
              <el-button size="mini" plain @click="rotateLeft" class="mr-1">
                <icon-grommet-icons:rotate-left class="mr-0.5" />左旋转
              </el-button>
              <el-button size="mini" plain @click="rotateRight" class="mr-1">
                <icon-grommet-icons:rotate-right class="mr-0.5" />右旋转
              </el-button>
            </div>
            <div class="flex">
              <el-button size="mini" type="warning" plain @click="cropperVisible = false" class="mr-1">取消</el-button>
              <el-button size="mini" type="primary" @click="handleCropConfirm">确认</el-button>
            </div>
          </div>
        </div>
      </template>
    </el-dialog>

    <el-form ref="userFormRef" :model="userForm" :rules="userRules" label-width="0" size="large" class="user-form">
      <div class="section">
        <div class="section-title">用户名</div>
        <el-form-item prop="username">
          <el-input v-model="userForm.username" placeholder="用户名" />
        </el-form-item>
      </div>

      <div class="section">
        <div class="section-title">邮箱</div>
        <el-form-item prop="email">
          <el-input v-model="userForm.email" placeholder="邮箱" />
        </el-form-item>
      </div>

      <div class="section">
        <div class="section-title">联系电话</div>
        <el-form-item prop="phone">
          <el-input v-model="userForm.phone" placeholder="联系电话" />
        </el-form-item>
      </div>

      <div class="section">
        <div class="section-title">生日 <span class="text-red-500">*</span></div>
        <el-form-item prop="birth">
          <el-date-picker
            v-model="userForm.birth"
            type="date"
            placeholder="请选择生日"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            class="w-full"
          />
        </el-form-item>
      </div>

      <div class="section">
        <div class="section-title">国籍 <span class="text-red-500">*</span></div>
        <el-form-item prop="area">
          <el-input v-model="userForm.area" placeholder="请输入国籍" />
        </el-form-item>
      </div>

      <div class="section">
        <div class="section-title">简介 <span class="text-red-500">*</span></div>
        <el-form-item prop="introduction">
          <el-input v-model="userForm.introduction" type="textarea" :rows="4" placeholder="编辑个人简介" maxlength="100"
            show-word-limit />
        </el-form-item>
      </div>

      <el-form-item class="button-group">
        <div class="flex justify-between w-full">
          <el-button type="primary" :loading="loading" @click="handleSubmit" class="submit-btn">
            更新信息
          </el-button>
          <el-button type="danger" :loading="loading" @click="handleDelete" class="submit-btn">
            注销账号
          </el-button>
        </div>
      </el-form-item>
    </el-form>

        <!-- 登录对话框 -->
        <AuthTabs v-model="authVisible" />
      </el-tab-pane>

      <!-- 我的接单列表 -->
      <el-tab-pane label="我的接单" name="orders">
        <div v-if="!userStore.isLoggedIn" class="text-center py-10 text-muted-foreground">
          请先登录
        </div>
        <div v-else>
          <!-- 状态筛选 -->
          <div class="mb-4 flex items-center gap-2">
            <span class="text-sm text-muted-foreground">状态筛选：</span>
            <el-select 
              v-model="orderStatusFilter" 
              placeholder="全部" 
              clearable
              @change="handleOrderStatusChange"
              style="width: 150px"
            >
              <el-option label="全部" :value="null" />
              <el-option label="待同意" :value="0" />
              <el-option label="已接单未完成" :value="1" />
              <el-option label="已完成" :value="2" />
              <el-option label="已拒绝" :value="3" />
            </el-select>
          </div>

          <!-- 接单列表 -->
          <div v-loading="orderLoading" class="space-y-3">
            <div v-if="orders.length === 0" class="text-center py-10 text-muted-foreground">
              暂无接单记录
            </div>
            <div 
              v-for="order in orders" 
              :key="order.id" 
              class="p-4 bg-background rounded-lg border border-border hover:border-primary/50 transition-colors cursor-pointer"
              @click="goToPostDetail(order.postId)"
            >
              <div class="flex items-start justify-between">
                <div class="flex-1">
                  <div class="flex items-center gap-3 mb-2">
                    <h3 class="text-base font-medium text-foreground hover:text-primary">
                      {{ order.postTitle }}
                    </h3>
                    <span 
                      class="text-xs font-medium px-2 py-1 rounded-full"
                      :class="getOrderStatusColor(order.status)"
                    >
                      {{ getOrderStatusText(order.status) }}
                    </span>
                  </div>
                  <div class="text-sm text-muted-foreground space-y-1">
                    <div>需求发布者：{{ order.posterName }}</div>
                    <div>申请时间：{{ formatOrderTime(order.createTime) }}</div>
                    <div v-if="order.status === 2">完成时间：{{ formatOrderTime(order.updateTime) }}</div>
                    <div v-if="order.status === 3">拒绝时间：{{ formatOrderTime(order.updateTime) }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 分页 -->
          <div v-if="orderTotal > 0" class="mt-4 flex justify-center">
            <el-pagination
              v-model:current-page="orderCurrentPage"
              v-model:page-size="orderPageSize"
              :total="orderTotal"
              :page-sizes="[10, 20, 30, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleOrderPageChange"
              @current-change="handleOrderPageChange"
            />
          </div>
        </div>
      </el-tab-pane>

      <!-- 上传歌曲标签页已删除 -->
      <!-- <el-tab-pane label="上传歌曲" name="upload">
        <UploadSong />
      </el-tab-pane> -->
    </el-tabs>
  </div>
</template>

<style scoped>
.user-container {
  max-width: 1000px;
  margin: 30px auto;
  padding: 30px 40px 15px;
  background-color: var(--el-bg-color);
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.user-header {
  text-align: left;
  margin-bottom: 20px;
  display: flex;
}

.username {
  margin: 0 0 20px 0;
  font-size: 20px;
  color: var(--el-text-color-primary);
  font-weight: normal;
}

.user-form {
  max-width: 100%;
  margin: 0;
}

:deep(.el-form-item) {
  margin-bottom: 24px;
}

:deep(.el-input__wrapper) {
  border-radius: 8px;
  background-color: var(--el-fill-color-blank);
  box-shadow: 0 0 0 1px var(--el-border-color) inset !important;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--el-border-color-hover) inset !important;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--el-color-primary) inset !important;
}

.submit-btn {
  border-radius: 8px;
  width: 140px;
}

:deep(.el-textarea__inner) {
  border-radius: 8px;
  resize: none;
  background-color: var(--el-fill-color-blank);
  box-shadow: 0 0 0 1px var(--el-border-color) inset !important;
}

:deep(.el-textarea__inner:hover) {
  box-shadow: 0 0 0 1px var(--el-border-color-hover) inset !important;
}

:deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 1px var(--el-color-primary) inset !important;
}

:deep(.el-input.is-disabled .el-input__wrapper) {
  background-color: var(--el-fill-color-blank);
  box-shadow: 0 0 0 1px var(--el-border-color-light) inset !important;
  cursor: not-allowed;
}

.section {
  margin-bottom: 24px;
}

.section-title {
  margin-bottom: 8px;
  color: var(--el-text-color-regular);
  font-size: 14px;
}

.avatar-wrapper {
  position: relative;
  cursor: pointer;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  overflow: hidden;
}

.avatar-hover {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  opacity: 0;
  transition: opacity 0.3s;
  color: white;
  font-size: 14px;
}

.avatar-hover .camera-icon {
  font-size: 24px;
  margin-bottom: 4px;
}

.avatar-wrapper:hover .avatar-hover {
  opacity: 1;
}

.button-group {
  margin-top: 40px;
}

.cropper-container {
  width: 100%;
  height: 400px;
}

:deep(.el-dialog__body) {
  padding-top: 10px;
}

.user-tabs {
  margin-top: 20px;
}

:deep(.el-tabs__header) {
  margin: 0 0 24px 0;
}

:deep(.el-tabs__nav-wrap::after) {
  display: none;
}

:deep(.el-tabs__item) {
  font-size: 16px;
  font-weight: 500;
}

:deep(.el-tabs__item.is-active) {
  color: var(--el-color-primary);
}

:deep(.el-tabs__active-bar) {
  background-color: var(--el-color-primary);
}
</style>
