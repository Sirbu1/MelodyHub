import { http } from '@/utils/http'

export type Result = {
  code: number
  message: string
  data?: Array<any> | number | string | object
}

export type ResultTable = {
  code: number
  message: string
  data?: {
    /** 列表数据 */
    items: Array<any>
    /** 总条目数 */
    total?: number
    /** 每页显示条目个数 */
    pageSize?: number
    /** 当前页数 */
    currentPage?: number
  }
}

/** 用户登录 */
export const login = (data: object) => {
  return http<Result>('post', '/user/login', { data })
}

/** 用户登出 */
export const logout = () => {
  return http<Result>('post', '/user/logout')
}

/** 发送邮箱验证码 */
export const sendEmailCode = (email: string) => {
  return http<Result>('get', '/user/sendVerificationCode', {
    params: { email },
  })
}

/** 用户注册 */
export const register = (data: object) => {
  return http<Result>('post', '/user/register', { data })
}

/** 重置密码 */
export const resetPassword = (data: object) => {
  return http<Result>('patch', '/user/resetUserPassword', { data })
}

/** 获取用户信息 */
export const getUserInfo = () => {
  return http<Result>('get', '/user/getUserInfo')
}

/** 更新用户信息 */
export const updateUserInfo = (data: object) => {
  return http<Result>('put', '/user/updateUserInfo', { data })
}

/** 更新用户头像 */
export const updateUserAvatar = (formData: FormData) => {
  return http<Result>('patch', '/user/updateUserAvatar', {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
    data: formData,
    transformRequest: [(data) => data], // 防止 axios 处理 FormData
  })
}

/** 注销账号 */
export const deleteUser = () => {
  return http<Result>('delete', '/user/deleteAccount')
}

/** 获取轮播图 */
export const getBanner = () => {
  return http<Result>('get', '/banner/getBannerList')
}

/** 获取推荐歌单 */
export const getRecommendedPlaylists = () => {
  return http<Result>('get', '/playlist/getRecommendedPlaylists')
}

/** 获取推荐歌曲 */
export const getRecommendedSongs = () => {
  return http<Result>('get', '/song/getRecommendedSongs')
}

/** 获取所有歌曲 */
export const getAllSongs = (data: object) => {
  return http<ResultTable>('post', '/song/getAllSongs', { data })
}

/** 获取歌曲详情 */
export const getSongDetail = (id: number) => {
  return http<ResultTable>('get', `/song/getSongDetail/${id}`)
}

/** 上传原创歌曲 */
export const uploadOriginalSong = (formData: FormData) => {
  return http<Result>('post', '/song/uploadOriginalSong', {
    data: formData,
    // 不手动设置Content-Type，让axios自动处理multipart/form-data
    // transformRequest: [(data) => data], // 移除这个，让axios正常处理
  })
}

/** 获取用户原创歌曲 */
export const getUserOriginalSongs = (userId: number, pageNum: number = 1, pageSize: number = 10, auditStatus?: number | null) => {
  const params: any = { pageNum, pageSize }
  if (auditStatus !== undefined && auditStatus !== null) {
    params.auditStatus = auditStatus
  }
  return http<ResultTable>('get', `/song/getUserOriginalSongs/${userId}`, { params })
}

/** 获取所有原创歌曲 */
export const getAllOriginalSongs = (pageNum: number = 1, pageSize: number = 10) => {
  return http<ResultTable>('get', '/song/getAllOriginalSongs', {
    params: { pageNum, pageSize },
  })
}

/** 删除原创歌曲 */
export const deleteOriginalSong = (songId: number) => {
  return http<Result>('delete', `/song/deleteOriginalSong/${songId}`)
}

/** 更新原创歌曲并重新提交审核 */
export const updateOriginalSong = (songId: number, formData: FormData) => {
  return http<Result>('post', `/song/updateOriginalSong/${songId}`, {
    data: formData,
  })
}

/** 获取所有歌手 */
export const getAllArtists = (data: object) => {
  return http<ResultTable>('post', '/artist/getAllArtists', { data })
}

/** 获取歌手详情 */
export const getArtistDetail = (id: number) => {
  return http<Result>('get', `/artist/getArtistDetail/${id}`)
}

/** 获取所有歌单 */
export const getAllPlaylists = (data: object) => {
  return http<ResultTable>('post', '/playlist/getAllPlaylists', { data })
}

/** 获取歌单详情 */
export const getPlaylistDetail = (id: number) => {
  return http<Result>('get', `/playlist/getPlaylistDetail/${id}`)
}

/** 获取用户收藏的歌曲 */
export const getFavoriteSongs = (data: object) => {
  return http<Result>('post', '/favorite/getFavoriteSongs', { data })
}

/** 收藏歌曲 */
export const collectSong = (songId: number) => {
  return http<Result>('post', '/favorite/collectSong', { params: { songId } })
}

/** 取消收藏歌曲 */
export const cancelCollectSong = (songId: number) => {
  return http<Result>('delete', '/favorite/cancelCollectSong', {
    params: { songId },
  })
}

/** 获取用户收藏的歌单 */
export const getFavoritePlaylists = (data: object) => {
  return http<Result>('post', '/favorite/getFavoritePlaylists', { data })
}

/** 收藏歌单 */
export const collectPlaylist = (playlistId: number) => {
  return http<Result>('post', '/favorite/collectPlaylist', {
    params: { playlistId },
  })
}

/** 取消收藏歌单 */
export const cancelCollectPlaylist = (playlistId: number) => {
  return http<Result>('delete', '/favorite/cancelCollectPlaylist', {
    params: { playlistId },
  })
}

/** 新增歌曲评论 */
export const addSongComment = (data: object) => {
  return http<Result>('post', '/comment/addSongComment', { data })
}

/** 新增歌单评论 */
export const addPlaylistComment = (data: object) => {
  return http<Result>('post', '/comment/addPlaylistComment', { data })
}

/** 点赞评论 */
export const likeComment = (commentId: number) => {
  return http<Result>('patch', `/comment/likeComment/${commentId}`)
}

/** 取消点赞评论 */
export const cancelLikeComment = (commentId: number) => {
  return http<Result>('patch', `/comment/cancelLikeComment/${commentId}`)
}

/** 删除评论 */
export const deleteComment = (commentId: number) => {
  return http<Result>('delete', `/comment/deleteComment/${commentId}`)
}

/** 新增反馈 */
export const addFeedback = (data: { content: string }) => {
  return http<Result>('post', '/feedback/addFeedback', { params: data })
}

// ==================== 论坛相关接口 ====================

/** 获取帖子列表 */
export const getForumPosts = (data: object) => {
  return http<ResultTable>('post', '/forum/posts', { data })
}

/** 获取帖子详情 */
export const getForumPostDetail = (id: number) => {
  return http<Result>('get', `/forum/postDetail/${id}`)
}

/** 发布帖子 */
export const addForumPost = (data: { title: string; content: string; type?: number }) => {
  return http<Result>('post', '/forum/post', { data })
}

/** 发布帖子（支持文件上传，用于需求发布） */
export const addForumPostWithFile = (formData: FormData) => {
  return http<Result>('post', '/forum/post', { data: formData })
}

/** 删除帖子 */
export const deleteForumPost = (id: number) => {
  return http<Result>('delete', `/forum/post/${id}`)
}

/** 点赞帖子 */
export const likeForumPost = (id: number) => {
  return http<Result>('patch', `/forum/post/like/${id}`)
}

/** 取消点赞帖子 */
export const cancelLikeForumPost = (id: number) => {
  return http<Result>('patch', `/forum/post/cancelLike/${id}`)
}

/** 更新需求帖子接单状态 */
export const updateForumPostAcceptStatus = (id: number, isAccepted: number) => {
  return http<Result>('patch', `/forum/post/acceptStatus/${id}`, { params: { isAccepted } })
}

/** 更新帖子并重新提交审核 */
export const updateForumPost = (postId: number, formData: FormData) => {
  return http<Result>('post', `/forum/post/${postId}/update`, { data: formData })
}

/** 获取帖子回复列表 */
export const getForumReplies = (data: object) => {
  return http<ResultTable>('post', '/forum/replies', { data })
}

/** 发布回复 */
export const addForumReply = (data: { postId: number; content: string; parentId?: number }) => {
  return http<Result>('post', '/forum/reply', { data })
}

/** 删除回复 */
export const deleteForumReply = (id: number) => {
  return http<Result>('delete', `/forum/reply/${id}`)
}

/** 更新回复并重新提交审核 */
export const updateForumReply = (replyId: number, data: { content: string }) => {
  return http<Result>('put', `/forum/reply/${replyId}`, { data })
}

/** 点赞回复 */
export const likeForumReply = (id: number) => {
  return http<Result>('patch', `/forum/reply/like/${id}`)
}

/** 取消点赞回复 */
export const cancelLikeForumReply = (id: number) => {
  return http<Result>('patch', `/forum/reply/cancelLike/${id}`)
}

/** 获取用户帖子列表（按状态筛选） */
export const getUserPosts = (userId: number, pageNum: number = 1, pageSize: number = 10, auditStatus?: number | null) => {
  const params: any = { pageNum, pageSize }
  if (auditStatus !== undefined && auditStatus !== null) {
    params.auditStatus = auditStatus
  }
  return http<ResultTable>('get', `/forum/getUserPosts/${userId}`, { params })
}

/** 获取用户回复列表（按状态筛选） */
export const getUserReplies = (userId: number, pageNum: number = 1, pageSize: number = 10, auditStatus?: number | null) => {
  const params: any = { pageNum, pageSize }
  if (auditStatus !== undefined && auditStatus !== null) {
    params.auditStatus = auditStatus
  }
  return http<ResultTable>('get', `/forum/getUserReplies/${userId}`, { params })
}

// ==================== 接单相关接口 ====================

/** 申请接单 */
export const applyOrder = (postId: number) => {
  return http<Result>('post', `/forum/order/apply/${postId}`)
}

/** 获取需求发布者的接单申请列表 */
export const getOrderApplicationsByPoster = (pageNum: number = 1, pageSize: number = 10, status?: number | null) => {
  const params: any = { pageNum, pageSize }
  if (status !== undefined && status !== null) {
    params.status = status
  }
  return http<ResultTable>('get', '/forum/order/applications', { params })
}

/** 同意接单 */
export const acceptOrder = (orderId: number) => {
  return http<Result>('patch', `/forum/order/accept/${orderId}`)
}

/** 拒绝接单 */
export const rejectOrder = (orderId: number) => {
  return http<Result>('patch', `/forum/order/reject/${orderId}`)
}

/** 标记为已完成 */
export const completeOrder = (orderId: number) => {
  return http<Result>('patch', `/forum/order/complete/${orderId}`)
}

/** 获取接单者的接单列表 */
export const getOrdersByAccepter = (pageNum: number = 1, pageSize: number = 10, status?: number | null) => {
  const params: any = { pageNum, pageSize }
  if (status !== undefined && status !== null) {
    params.status = status
  }
  return http<ResultTable>('get', '/forum/order/myOrders', { params })
}

/** 获取接单详情 */
export const getOrderDetail = (orderId: number) => {
  return http<Result>('get', `/forum/order/detail/${orderId}`)
}