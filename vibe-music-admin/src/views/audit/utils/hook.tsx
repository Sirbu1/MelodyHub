import "./reset.css";
import dayjs from "dayjs";
import { message } from "@/utils/message";
import type { PaginationProps } from "@pureadmin/table";
import { deviceDetection } from "@pureadmin/utils";
import {
  getPendingSongs,
  getPendingPosts,
  getPendingReplies,
  approveSong,
  rejectSong,
  approvePost,
  rejectPost,
  approveReply,
  rejectReply,
  getSongDetail,
  getForumPostDetail,
  getForumReplies
} from "@/api/system";
import { ElForm, ElMessageBox } from "element-plus";
import { type Ref, ref, toRaw, reactive, onMounted, onBeforeUnmount, watch } from "vue";

export function useAudit(tableRef: Ref, activeTab: Ref<string>) {
  const form = reactive({
    pageNum: 1,
    pageSize: 20
  });
  const dataList = ref([]);
  const loading = ref(true);
  const selectedNum = ref(0);
  const selectedIds = ref<number[]>([]);
  const pagination = reactive<PaginationProps>({
    total: 0,
    pageSize: 20,
    currentPage: 1,
    background: true
  });

  // 详情对话框相关
  const showDetailDialog = ref(false);
  const detailType = ref<"song" | "post" | "reply">("song");
  const detailData = ref<any>(null);
  const postReplies = ref<any[]>([]);
  const detailLoading = ref(false);

  // 根据当前标签页动态生成列
  const getColumns = (tab: string): TableColumnList => {
    const baseColumns: TableColumnList = [
      {
        label: "勾选列",
        type: "selection",
        fixed: "left",
        reserveSelection: true
      },
      {
        label: "用户头像",
        prop: "userAvatar",
        minWidth: 80,
        cellRenderer: ({ row }) => (
          <el-avatar size={40} src={row.userAvatar || row.user_avatar} />
        )
      },
      {
        label: "用户名",
        prop: "username",
        minWidth: 120
      }
    ];

    const songBaseColumns: TableColumnList = [
      {
        label: "勾选列",
        type: "selection",
        fixed: "left",
        reserveSelection: true
      }
    ];

    if (tab === "songs") {
      return [
        ...songBaseColumns,
        {
          label: "歌曲ID",
          prop: "songId",
          minWidth: 100
        },
        {
          label: "歌曲名称",
          prop: "songName",
          minWidth: 200
        },
        {
          label: "风格",
          prop: "style",
          minWidth: 100
        },
        {
          label: "创建者",
          prop: "creatorName",
          minWidth: 120
        },
        {
          label: "上传时间",
          minWidth: 180,
          prop: "createTime",
          formatter: ({ createTime }) =>
            dayjs(createTime).format("YYYY-MM-DD HH:mm:ss")
        },
        {
          label: "操作",
          fixed: "right",
          width: 200,
          slot: "operation"
        }
      ];
    } else if (tab === "posts") {
      return [
        ...baseColumns,
        {
          label: "帖子ID",
          prop: "postId",
          minWidth: 100
        },
        {
          label: "标题",
          prop: "title",
          minWidth: 200
        },
        {
          label: "类型",
          prop: "type",
          minWidth: 80,
          cellRenderer: ({ row }) => (row.type === 0 ? "交流" : "需求")
        },
        {
          label: "发帖人",
          prop: "username",
          minWidth: 120
        },
        {
          label: "发布时间",
          minWidth: 180,
          prop: "createTime",
          formatter: ({ createTime }) =>
            dayjs(createTime).format("YYYY-MM-DD HH:mm:ss")
        },
        {
          label: "操作",
          fixed: "right",
          width: 200,
          slot: "operation"
        }
      ];
    } else if (tab === "replies") {
      return [
        ...baseColumns,
        {
          label: "回复ID",
          prop: "replyId",
          minWidth: 100
        },
        {
          label: "帖子ID",
          prop: "postId",
          minWidth: 100
        },
        {
          label: "回复内容",
          prop: "content",
          minWidth: 300,
          cellRenderer: ({ row }) => {
            const content = row.content || "";
            return (
              <div
                style={{
                  "white-space": "normal",
                  "word-break": "break-all"
                }}
                title={content}
              >
                {content.length > 50 ? content.substring(0, 50) + "..." : content}
              </div>
            );
          }
        },
        {
          label: "回复时间",
          minWidth: 180,
          prop: "createTime",
          formatter: ({ createTime }) =>
            dayjs(createTime).format("YYYY-MM-DD HH:mm:ss")
        },
        {
          label: "操作",
          fixed: "right",
          width: 200,
          slot: "operation"
        }
      ];
    }
    
    // 默认返回空数组（不应该到达这里）
    return [];
  };

  const columns = ref<TableColumnList>(getColumns(activeTab.value));

  // 监听标签页切换
  watch(activeTab, (newTab) => {
    // 强制更新列定义
    columns.value = getColumns(newTab);
    form.pageNum = 1;
    pagination.currentPage = 1;
    selectedIds.value = [];
    selectedNum.value = 0;
    onSearch();
    // 重新启动定时刷新，确保新标签页的数据也能自动刷新
    startAutoRefresh();
  });

  function switchTab(tab: string) {
    activeTab.value = tab;
  }

  function handleApprove(row: any, isBatch: boolean = false) {
    const ids = isBatch ? selectedIds.value : [getId(row)];
    const type = activeTab.value;
    
    if (ids.length === 0) {
      message("请选择要审核的内容", { type: "warning" });
      return;
    }

    ElMessageBox.confirm(
      `确认要${isBatch ? "批量" : ""}通过这${ids.length}项内容吗?`,
      "系统提示",
      {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
        draggable: true
      }
    )
      .then(() => {
        const promises = ids.map(id => {
          if (type === "songs") return approveSong(id);
          if (type === "posts") return approvePost(id);
          if (type === "replies") return approveReply(id);
        });

        Promise.all(promises)
          .then(() => {
            message(`成功通过 ${ids.length} 项内容`, { type: "success" });
            onSearch();
            if (isBatch) {
              onSelectionCancel();
            }
          })
          .catch(err => {
            message("审核操作失败", { type: "error" });
            console.error("审核错误:", err);
          });
      })
      .catch(() => {});
  }

  function handleReject(row: any, isBatch: boolean = false) {
    const ids = isBatch ? selectedIds.value : [getId(row)];
    const type = activeTab.value;
    
    if (ids.length === 0) {
      message("请选择要审核的内容", { type: "warning" });
      return;
    }

    // 使用 ElMessageBox.prompt 来输入拒绝原因
    ElMessageBox.prompt(
      isBatch ? `请输入拒绝这${ids.length}项内容的原因（可选）` : "请输入拒绝原因（可选）",
      "审核拒绝",
      {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        inputType: "textarea",
        inputPlaceholder: "请输入拒绝原因，留空则无原因",
        inputValidator: (value) => {
          // 允许为空，但如果有值，长度不能超过500
          if (value && value.length > 500) {
            return "拒绝原因不能超过500个字符";
          }
          return true;
        },
        draggable: true
      }
    )
      .then(({ value }) => {
        // 如果用户输入了内容，即使 trim 后为空字符串，也传递空字符串（而不是 undefined）
        // 这样后端可以区分"没有输入"和"输入了空字符串"
        const reason = value !== null && value !== undefined ? value.trim() : undefined;
        console.log('拒绝原因:', reason, '类型:', typeof reason, '长度:', reason?.length);
        const promises = ids.map(id => {
          if (type === "songs") return rejectSong(id, reason);
          if (type === "posts") return rejectPost(id, reason);
          if (type === "replies") return rejectReply(id, reason);
        });

        Promise.all(promises)
          .then(() => {
            message(`成功拒绝 ${ids.length} 项内容`, { type: "success" });
            onSearch();
            if (isBatch) {
              onSelectionCancel();
            }
          })
          .catch(err => {
            message("审核操作失败", { type: "error" });
            console.error("审核错误:", err);
          });
      })
      .catch(() => {});
  }

  function getId(row: any): number {
    if (activeTab.value === "songs") return row.songId;
    if (activeTab.value === "posts") return row.postId;
    if (activeTab.value === "replies") return row.replyId;
    return 0;
  }

  // 查看详情
  async function handleViewDetail(row: any) {
    detailLoading.value = true;
    detailData.value = null;
    postReplies.value = [];
    
    try {
      if (activeTab.value === "songs") {
        detailType.value = "song";
        const res = await getSongDetail(row.songId);
        if (res.code === 0 && res.data) {
          detailData.value = res.data;
          showDetailDialog.value = true;
        } else {
          message("获取歌曲详情失败", { type: "error" });
        }
      } else if (activeTab.value === "posts") {
        detailType.value = "post";
        const res = await getForumPostDetail(row.postId);
        if (res.code === 0 && res.data) {
          detailData.value = res.data;
          // 获取帖子回复
          try {
            const repliesRes = await getForumReplies({
              postId: row.postId,
              pageNum: 1,
              pageSize: 100
            });
            if (repliesRes.code === 0 && repliesRes.data) {
              postReplies.value = repliesRes.data.items || [];
            }
          } catch (e) {
            console.error("获取帖子回复失败:", e);
          }
          showDetailDialog.value = true;
        } else {
          message("获取帖子详情失败", { type: "error" });
        }
      } else if (activeTab.value === "replies") {
        detailType.value = "reply";
        // 回复详情需要获取原帖子信息
        try {
          const postRes = await getForumPostDetail(row.postId);
          if (postRes.code === 0 && postRes.data) {
            detailData.value = {
              reply: row,
              post: postRes.data
            };
            // 获取该帖子的所有回复
            const repliesRes = await getForumReplies({
              postId: row.postId,
              pageNum: 1,
              pageSize: 100
            });
            if (repliesRes.code === 0 && repliesRes.data) {
              postReplies.value = repliesRes.data.items || [];
            }
            showDetailDialog.value = true;
          } else {
            message("获取回复详情失败", { type: "error" });
          }
        } catch (e) {
          console.error("获取回复详情失败:", e);
          message("获取回复详情失败", { type: "error" });
        }
      }
    } catch (e: any) {
      console.error("获取详情失败:", e);
      message(e.response?.data?.message || e.message || "获取详情失败", { type: "error" });
    } finally {
      detailLoading.value = false;
    }
  }

  function handleSizeChange(val: number) {
    pagination.pageSize = val;
    form.pageSize = val;
    onSearch();
  }

  function handleCurrentChange(val: number) {
    pagination.currentPage = val;
    form.pageNum = val;
    onSearch();
  }

  function handleSelectionChange(val) {
    if (tableRef) {
      selectedNum.value = val.length;
      selectedIds.value = val.map(item => getId(item));
    }
  }

  function onSelectionCancel() {
    selectedNum.value = 0;
    selectedIds.value = [];
    tableRef.value?.getTableRef()?.clearSelection();
  }

  async function onSearch() {
    loading.value = true;
    const params = toRaw(form);

    try {
      let res;
      if (activeTab.value === "songs") {
        res = await getPendingSongs(params.pageNum, params.pageSize);
      } else if (activeTab.value === "posts") {
        res = await getPendingPosts(params.pageNum, params.pageSize);
      } else if (activeTab.value === "replies") {
        res = await getPendingReplies(params.pageNum, params.pageSize);
      }

      console.log("审核列表响应:", res);

      // 检查响应格式
      if (!res) {
        message("获取待审核列表失败：响应为空", { type: "error" });
        dataList.value = [];
        pagination.total = 0;
        loading.value = false;
        return;
      }

      // 处理不同的响应格式
      if (res.code === 0) {
        if (res.data) {
          dataList.value = res.data.items || [];
          pagination.total = res.data.total || 0;
        } else {
          // 如果data为空，可能是空列表
          dataList.value = [];
          pagination.total = 0;
        }
      } else {
        message(res.message || "获取待审核列表失败", { type: "error" });
        dataList.value = [];
        pagination.total = 0;
      }
    } catch (e: any) {
      console.error("获取待审核列表错误:", e);
      console.error("错误详情:", {
        message: e.message,
        response: e.response,
        data: e.response?.data,
        status: e.response?.status
      });
      
      const errorMsg = e.response?.data?.message || e.message || "获取待审核列表失败";
      message(errorMsg, { type: "error" });
      dataList.value = [];
      pagination.total = 0;
    } finally {
      setTimeout(() => {
        loading.value = false;
      }, 500);
    }
  }

  const resetForm = (formEl: InstanceType<typeof ElForm> | undefined) => {
    if (!formEl) return;
    formEl.resetFields();
    onSearch();
  };

  // 定时刷新间隔（30秒）
  const REFRESH_INTERVAL = 30000;
  // 定时器引用
  let refreshTimer: number | null = null;

  // 启动定时刷新（用于检测新上传的待审核内容）
  const startAutoRefresh = () => {
    // 清除已存在的定时器
    if (refreshTimer) {
      clearInterval(refreshTimer);
    }
    // 每30秒自动刷新一次，以检测新上传的待审核内容
    refreshTimer = window.setInterval(() => {
      // 只在页面可见时刷新
      if (document.visibilityState === 'visible') {
        onSearch();
      }
    }, REFRESH_INTERVAL);
  };

  // 停止定时刷新
  const stopAutoRefresh = () => {
    if (refreshTimer) {
      clearInterval(refreshTimer);
    }
    refreshTimer = null;
  };

  // 监听页面可见性变化
  const handleVisibilityChange = () => {
    if (document.visibilityState === 'visible') {
      // 页面变为可见时，立即刷新一次，然后启动定时刷新
      onSearch();
      startAutoRefresh();
    } else {
      // 页面不可见时，停止定时刷新以节省资源
      stopAutoRefresh();
    }
  };

  onMounted(() => {
    columns.value = getColumns(activeTab.value);
    onSearch();
    // 启动定时刷新
    startAutoRefresh();
    // 监听页面可见性变化
    document.addEventListener('visibilitychange', handleVisibilityChange);
  });

  // 组件卸载时清理
  onBeforeUnmount(() => {
    stopAutoRefresh();
    document.removeEventListener('visibilitychange', handleVisibilityChange);
  });

  return {
    form,
    loading,
    columns,
    dataList,
    selectedNum,
    pagination,
    onSearch,
    resetForm,
    handleApprove,
    handleReject,
    handleSizeChange,
    handleCurrentChange,
    handleSelectionChange,
    onSelectionCancel,
    switchTab,
    deviceDetection,
    handleViewDetail,
    showDetailDialog,
    detailType,
    detailData,
    postReplies,
    detailLoading
  };
}

