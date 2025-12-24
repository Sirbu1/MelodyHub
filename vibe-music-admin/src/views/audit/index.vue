<script setup lang="ts">
import { ref, computed } from "vue";
import { useAudit } from "./utils/hook";
import { PureTableBar } from "@/components/RePureTableBar";
import { useRenderIcon } from "@/components/ReIcon/src/hooks";

import Check from "@iconify-icons/ep/check";
import Close from "@iconify-icons/ep/close";
import Refresh from "@iconify-icons/ep/refresh";
import View from "@iconify-icons/ep/view";

defineOptions({
  name: "AuditManagement"
});

const formRef = ref();
const tableRef = ref();
const activeTab = ref("songs");

const {
  form,
  loading,
  columns,
  dataList,
  selectedNum,
  pagination,
  deviceDetection,
  onSearch,
  resetForm,
  handleApprove,
  handleReject,
  handleSizeChange,
  onSelectionCancel,
  handleCurrentChange,
  handleSelectionChange,
  switchTab,
  handleViewDetail,
  showDetailDialog,
  detailType,
  detailData,
  postReplies,
  detailLoading
} = useAudit(tableRef, activeTab);

// 根据当前标签页动态设置 row-key
const rowKey = computed(() => {
  switch (activeTab.value) {
    case "songs":
      return "songId";
    case "posts":
      return "postId";
    case "replies":
      return "replyId";
    default:
      return "id";
  }
});
</script>

<template>
  <div :class="['flex', 'justify-between', deviceDetection() && 'flex-wrap']">
    <div
      :class="[deviceDetection() ? ['w-full', 'mt-2'] : 'w-[calc(100%-0px)]']"
    >
      <!-- 标签页切换 -->
      <el-tabs v-model="activeTab" @tab-change="switchTab" class="mb-4">
        <el-tab-pane label="待审核歌曲" name="songs" />
        <el-tab-pane label="待审核帖子" name="posts" />
        <el-tab-pane label="待审核回复" name="replies" />
      </el-tabs>

      <PureTableBar 
        :title="`${activeTab === 'songs' ? '歌曲' : activeTab === 'posts' ? '帖子' : '回复'}审核管理`" 
        :columns="columns" 
        @refresh="onSearch"
      >
        <template v-slot="{ size, dynamicColumns }">
          <div
            v-if="selectedNum > 0"
            v-motion-fade
            class="bg-[var(--el-fill-color-light)] w-full h-[46px] mb-2 pl-4 flex items-center"
          >
            <div class="flex-auto">
              <span
                style="font-size: var(--el-font-size-base)"
                class="text-[rgba(42,46,54,0.5)] dark:text-[rgba(220,220,242,0.5)]"
              >
                已选择 {{ selectedNum }} 项
              </span>
            </div>
            <el-popconfirm title="是否确认批量通过？" @confirm="handleApprove(null, true)">
              <template #reference>
                <el-button
                  :disabled="selectedNum === 0"
                  :icon="useRenderIcon(Check)"
                  text
                  type="primary"
                >
                  批量通过
                </el-button>
              </template>
            </el-popconfirm>
            <el-popconfirm title="是否确认批量拒绝？" @confirm="handleReject(null, true)">
              <template #reference>
                <el-button
                  :disabled="selectedNum === 0"
                  :icon="useRenderIcon(Close)"
                  text
                  type="danger"
                >
                  批量拒绝
                </el-button>
              </template>
            </el-popconfirm>
          </div>
          <pure-table
            ref="tableRef"
            :row-key="rowKey"
            border
            align-whole="center"
            showOverflowTooltip
            table-layout="auto"
            :loading="loading"
            :size="size"
            :data="dataList"
            :columns="dynamicColumns"
            :pagination="pagination"
            :paginationSmall="size === 'small'"
            :header-cell-style="{
              background: 'var(--el-table-row-hover-bg-color)',
              color: 'var(--el-text-color-primary)'
            }"
            @page-size-change="handleSizeChange"
            @page-current-change="handleCurrentChange"
            @selection-change="handleSelectionChange"
          >
            <template #operation="{ row }">
              <el-button
                :icon="useRenderIcon(View)"
                link
                type="info"
                :size="size"
                @click="handleViewDetail(row)"
              >
                查看
              </el-button>
              <el-button
                :icon="useRenderIcon(Check)"
                link
                type="primary"
                :size="size"
                @click="handleApprove(row)"
              >
                通过
              </el-button>
              <el-button
                :icon="useRenderIcon(Close)"
                link
                type="danger"
                :size="size"
                @click="handleReject(row)"
              >
                拒绝
              </el-button>
            </template>
          </pure-table>
        </template>
      </PureTableBar>
    </div>
  </div>

  <!-- 详情对话框 -->
  <el-dialog
    v-model="showDetailDialog"
    :title="detailType === 'song' ? '歌曲详情' : detailType === 'post' ? '帖子详情' : '回复详情'"
    width="80%"
    top="5vh"
    destroy-on-close
    v-loading="detailLoading"
  >
    <!-- 歌曲详情 -->
    <div v-if="detailType === 'song' && detailData" class="song-detail">
      <div class="flex gap-6 mb-6">
        <el-image
          :src="detailData.coverUrl || detailData.cover_url"
          :alt="detailData.songName || detailData.song_name"
          class="w-48 h-48 rounded-lg"
          fit="cover"
        />
        <div class="flex-1">
          <h3 class="text-2xl font-bold mb-4">{{ detailData.songName || detailData.song_name }}</h3>
          <div class="space-y-2 text-sm">
            <p><span class="font-semibold">艺术家：</span>{{ detailData.artistName || detailData.artist_name || '未知' }}</p>
            <p><span class="font-semibold">专辑：</span>{{ detailData.album || '未知' }}</p>
            <p><span class="font-semibold">风格：</span>{{ detailData.style || '未知' }}</p>
            <p><span class="font-semibold">时长：</span>{{ detailData.duration ? (detailData.duration / 60).toFixed(2) + ' 分钟' : '未知' }}</p>
            <p><span class="font-semibold">创建者：</span>{{ detailData.creatorName || detailData.creator_name || '未知' }}</p>
            <p><span class="font-semibold">上传时间：</span>{{ detailData.createTime || detailData.create_time }}</p>
          </div>
        </div>
      </div>
      <div v-if="detailData.lyric || detailData.lyrics" class="lyric-section">
        <h4 class="text-lg font-semibold mb-3">歌词</h4>
        <div class="lyric-content bg-gray-50 dark:bg-gray-800 p-4 rounded-lg max-h-96 overflow-y-auto">
          <pre class="whitespace-pre-wrap">{{ detailData.lyric || detailData.lyrics }}</pre>
        </div>
      </div>
      <div v-if="detailData.audioUrl || detailData.audio_url" class="audio-section mt-6">
        <h4 class="text-lg font-semibold mb-3">音频预览</h4>
        <audio :src="detailData.audioUrl || detailData.audio_url" controls class="w-full" />
      </div>
    </div>

    <!-- 帖子详情 -->
    <div v-if="detailType === 'post' && detailData" class="post-detail">
      <div class="mb-4 text-sm text-gray-600 dark:text-gray-400">
        <span>类型：{{ detailData.type === 0 ? '交流' : '需求' }}</span>
        <span class="ml-4">发布时间：{{ detailData.createTime || detailData.create_time }}</span>
        <span class="ml-4">发帖人：{{ detailData.username }}</span>
      </div>
      <h3 class="text-xl font-semibold mb-4">{{ detailData.title }}</h3>
      <div class="post-content bg-gray-50 dark:bg-gray-800 p-4 rounded-lg mb-6">
        <p class="whitespace-pre-wrap">{{ detailData.content }}</p>
      </div>
      <div v-if="detailData.type === 1" class="requirement-info mb-6">
        <h4 class="text-lg font-semibold mb-3">需求信息</h4>
        <div class="grid grid-cols-2 gap-4 text-sm">
          <p><span class="font-semibold">需求类型：</span>{{ detailData.requirementType || '未知' }}</p>
          <p><span class="font-semibold">时间要求：</span>{{ detailData.timeRequirement || '未知' }}</p>
          <p><span class="font-semibold">预算：</span>{{ detailData.budget || '未知' }}</p>
          <p><span class="font-semibold">风格描述：</span>{{ detailData.styleDescription || '未知' }}</p>
        </div>
      </div>
      <div class="replies-section">
        <h4 class="text-lg font-semibold mb-3">回复列表 ({{ postReplies.length }})</h4>
        <div v-if="postReplies.length > 0" class="space-y-4">
          <div
            v-for="reply in postReplies"
            :key="reply.replyId || reply.reply_id"
            class="reply-item bg-gray-50 dark:bg-gray-800 rounded-lg p-4"
          >
            <div class="flex gap-3">
              <el-avatar :src="reply.userAvatar || reply.user_avatar" :size="40" />
              <div class="flex-1">
                <div class="flex items-center gap-2 mb-2">
                  <span class="font-medium">{{ reply.username }}</span>
                  <span class="text-xs text-gray-500">{{ reply.createTime || reply.create_time }}</span>
                </div>
                <div class="text-gray-700 dark:text-gray-300">{{ reply.content }}</div>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="text-center text-gray-500 py-8">暂无回复</div>
      </div>
    </div>

    <!-- 回复详情 -->
    <div v-if="detailType === 'reply' && detailData" class="reply-detail">
      <h4 class="text-lg font-semibold mb-3">原帖子信息</h4>
      <div v-if="detailData.post" class="post-info bg-gray-50 dark:bg-gray-800 rounded-lg p-4 mb-6">
        <h3 class="text-xl font-semibold mb-2">{{ detailData.post.title }}</h3>
        <div class="text-sm text-gray-600 dark:text-gray-400 mb-2">
          <span>类型：{{ detailData.post.type === 0 ? '交流' : '需求' }}</span>
          <span class="ml-4">发布时间：{{ detailData.post.createTime || detailData.post.create_time }}</span>
          <span class="ml-4">发帖人：{{ detailData.post.username }}</span>
        </div>
        <p class="text-gray-700 dark:text-gray-300 line-clamp-3">{{ detailData.post.content }}</p>
      </div>
      <h4 class="text-lg font-semibold mb-3">回复内容</h4>
      <div class="my-reply-content bg-blue-50 dark:bg-blue-900/20 rounded-lg p-4 mb-6 border border-blue-200 dark:border-blue-800">
        <div class="flex gap-3">
          <el-avatar :src="detailData.reply.userAvatar || detailData.reply.user_avatar" :size="40" />
          <div class="flex-1">
            <div class="flex items-center gap-2 mb-2">
              <span class="font-medium">{{ detailData.reply.username }}</span>
              <span class="text-xs text-gray-500">{{ detailData.reply.createTime || detailData.reply.create_time }}</span>
            </div>
            <div class="text-gray-700 dark:text-gray-300">{{ detailData.reply.content }}</div>
          </div>
        </div>
      </div>
      <h4 class="text-lg font-semibold mb-3">该帖子的其他回复</h4>
      <div v-if="postReplies.length > 0" class="space-y-4">
        <div
          v-for="reply in postReplies.filter(r => (r.replyId || r.reply_id) !== (detailData.reply.replyId || detailData.reply.reply_id))"
          :key="reply.replyId || reply.reply_id"
          class="reply-item bg-gray-50 dark:bg-gray-800 rounded-lg p-4"
        >
          <div class="flex gap-3">
            <el-avatar :src="reply.userAvatar || reply.user_avatar" :size="40" />
            <div class="flex-1">
              <div class="flex items-center gap-2 mb-2">
                <span class="font-medium">{{ reply.username }}</span>
                <span class="text-xs text-gray-500">{{ reply.createTime || reply.create_time }}</span>
              </div>
              <div class="text-gray-700 dark:text-gray-300">{{ reply.content }}</div>
            </div>
          </div>
        </div>
      </div>
      <div v-else class="text-center text-gray-500 py-8">暂无其他回复</div>
    </div>
  </el-dialog>
</template>

