<template>
  <div class="flex flex-col h-full flex-1 overflow-hidden bg-background px-4 py-2">
    <div class="py-4 border-b border-border">
      <h2 class="text-xl font-bold text-foreground">我的接单</h2>
    </div>

    <div class="flex-1 overflow-y-auto py-4">
      <div v-if="!userStore.isLoggedIn" class="text-center py-20 text-muted-foreground">
        <p class="text-lg mb-4">请先登录</p>
        <el-button type="primary" @click="authVisible = true">登录</el-button>
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
            <el-option label="待同意" :value="0" />
            <el-option label="已接单未完成" :value="1" />
            <el-option label="已完成" :value="2" />
            <el-option label="已拒绝" :value="3" />
          </el-select>
        </div>

        <!-- 接单列表 -->
        <div v-loading="orderLoading" class="space-y-3">
          <div v-if="orders.length === 0" class="text-center py-20 text-muted-foreground">
            <icon-mdi:inbox-outline class="w-16 h-16 mx-auto mb-4 opacity-50" />
            <p class="text-lg">暂无接单记录</p>
          </div>
          <div 
            v-for="order in orders" 
            :key="order.id" 
            class="p-4 bg-card rounded-lg border border-border hover:border-primary/50 transition-colors cursor-pointer"
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
                  <div class="flex items-center gap-2">
                    <icon-mdi:account class="w-4 h-4" />
                    <span>需求发布者：{{ order.posterName }}</span>
                  </div>
                  <div class="flex items-center gap-2">
                    <icon-mdi:clock-outline class="w-4 h-4" />
                    <span>申请时间：{{ formatOrderTime(order.createTime) }}</span>
                  </div>
                  <div v-if="order.status === 2" class="flex items-center gap-2">
                    <icon-mdi:check-circle class="w-4 h-4" />
                    <span>完成时间：{{ formatOrderTime(order.updateTime) }}</span>
                  </div>
                  <div v-if="order.status === 3" class="flex items-center gap-2">
                    <icon-mdi:close-circle class="w-4 h-4" />
                    <span>拒绝时间：{{ formatOrderTime(order.updateTime) }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <div v-if="orderTotal > 0" class="mt-6 flex justify-center">
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
    </div>

    <!-- 登录对话框 -->
    <AuthTabs v-model="authVisible" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onActivated, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { UserStore } from '@/stores/modules/user'
import { getOrdersByAccepter } from '@/api/system'
import AuthTabs from '@/components/Auth/AuthTabs.vue'

const router = useRouter()
const route = useRoute()
const userStore = UserStore()
const authVisible = ref(false)

// 接单列表相关
const orders = ref<any[]>([])
const orderLoading = ref(false)
const orderCurrentPage = ref(1)
const orderPageSize = ref(10)
const orderTotal = ref(0)
const orderStatusFilter = ref<number | null>(null)

// 获取接单列表
const getOrders = async () => {
  if (!userStore.isLoggedIn) {
    return
  }
  
  orderLoading.value = true
  try {
    console.log('=== 获取接单列表 ===')
    console.log('请求参数:', {
      pageNum: orderCurrentPage.value,
      pageSize: orderPageSize.value,
      status: orderStatusFilter.value
    })
    const res = await getOrdersByAccepter(orderCurrentPage.value, orderPageSize.value, orderStatusFilter.value)
    console.log('API 响应:', res)
    if (res.code === 0) {
      orders.value = res.data.items || []
      orderTotal.value = res.data.total || 0
      console.log('接单列表数据:', {
        total: orderTotal.value,
        itemsCount: orders.value.length,
        items: orders.value.map((o: any) => ({
          id: o.id,
          postId: o.postId,
          status: o.status,
          statusText: o.statusText,
          postTitle: o.postTitle
        }))
      })
    } else {
      console.error('API 返回错误:', res.message)
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

// 跳转到帖子详情
const goToPostDetail = (postId: number) => {
  router.push(`/forum/${postId}`)
}

// 监听路由变化，当从帖子详情页返回时刷新
watch(() => route.path, (newPath, oldPath) => {
  // 如果从帖子详情页返回到我的接单页面，刷新数据
  if (newPath === '/my/orders' && oldPath && oldPath.startsWith('/forum/')) {
    if (userStore.isLoggedIn) {
      // 延迟一下，确保页面已经加载完成
      setTimeout(() => {
        getOrders()
      }, 100)
    }
  }
})

// 页面激活时刷新数据（从其他页面返回时）
onActivated(() => {
  if (userStore.isLoggedIn) {
    getOrders()
  }
})

// 初始化
onMounted(() => {
  if (userStore.isLoggedIn) {
    getOrders()
  }
})
</script>

<style scoped>
/* 样式已通过 Tailwind CSS 类实现 */
</style>

