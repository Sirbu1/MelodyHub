import { http } from "@/utils/http";
import { getToken } from "@/utils/auth";

export type Result = {
  code: number;
  message: string;
  data?: Array<any> | number | string | object;
};

export type ResultTable = {
  code: number;
  message: string;
  data?: {
    /** 列表数据 */
    items: Array<any>;
    /** 总条目数 */
    total?: number;
    /** 每页显示条目个数 */
    pageSize?: number;
    /** 当前页数 */
    currentPage?: number;
  };
};

/** 用户管理-获取用户列表 */
export const getUserList = (data: object) => {
  const userData = getToken();
  return http.request<ResultTable>("post", "/admin/getAllUsers", {
    headers: {
      "Content-Type": "application/json",
      Authorization: userData.accessToken
    }, // 确保是 JSON
    data // 直接传 JSON 对象
  });
};

/** 用户管理-新增用户 */
export const addUser = (data: object) => {
  const userData = getToken();
  return http.request<Result>("post", "/admin/addUser", {
    headers: {
      "Content-Type": "application/json",
      Authorization: userData.accessToken
    },
    data
  });
};

/** 用户管理-编辑用户 */
export const updateUser = (data: object) => {
  const userData = getToken();
  return http.request<Result>("put", "/admin/updateUser", {
    headers: {
      "Content-Type": "application/json",
      Authorization: userData.accessToken
    },
    data
  });
};

/** 用户管理-更新用户状态 */
export const updateUserStatus = (id: number, status: number) => {
  const userData = getToken();
  return http.request<Result>(
    "patch",
    `/admin/updateUserStatus/${id}/${status}`,
    {
      headers: {
        Authorization: userData.accessToken
      }
    }
  );
};

/** 用户管理-删除用户 */
export const deleteUser = (id: number) => {
  const userData = getToken();
  return http.request<Result>("delete", `/admin/deleteUser/${id}`, {
    headers: { Authorization: userData.accessToken }
  });
};

/** 用户管理-批量删除用户 */
export const deleteUsers = (ids: Array<number>) => {
  const userData = getToken();
  return http.request<Result>("delete", `/admin/deleteUsers`, {
    headers: {
      "Content-Type": "application/json",
      Authorization: userData.accessToken
    },
    data: ids
  });
};

/** 歌手管理-获取歌手列表 */
export const getArtistList = (data: object) => {
  const userData = getToken();
  return http.request<ResultTable>("post", "/admin/getAllArtists", {
    headers: {
      "Content-Type": "application/json",
      Authorization: userData.accessToken
    },
    data
  });
};

/** 歌手管理-新增歌手 */
export const addArtist = (data: object) => {
  const userData = getToken();
  return http.request<Result>("post", "/admin/addArtist", {
    headers: {
      "Content-Type": "application/json",
      Authorization: userData.accessToken
    },
    data
  });
};

/** 歌手管理-编辑歌手 */
export const updateArtist = (data: object) => {
  const userData = getToken();
  return http.request<Result>("put", "/admin/updateArtist", {
    headers: {
      "Content-Type": "application/json",
      Authorization: userData.accessToken
    },
    data
  });
};

/** 歌手管理-更新歌手头像 */
export const updateArtistAvatar = (id: number, data: object) => {
  const userData = getToken();
  return http.request<Result>("patch", `/admin/updateArtistAvatar/${id}`, {
    headers: {
      "Content-Type": "multipart/form-data",
      Authorization: userData.accessToken
    },
    data,
    responseType: "json" // 确保使用正确的响应类型（可以使用 'json' 或 'blob'）
  });
};

/** 歌手管理-删除歌手 */
export const deleteArtist = (id: number) => {
  const userData = getToken();
  return http.request<Result>("delete", `/admin/deleteArtist/${id}`, {
    headers: { Authorization: userData.accessToken }
  });
};

/** 歌手管理-批量删除歌手 */
export const deleteArtists = (ids: Array<number>) => {
  const userData = getToken();
  return http.request<Result>("delete", `/admin/deleteArtists`, {
    headers: {
      "Content-Type": "application/json",
      Authorization: userData.accessToken
    },
    data: ids
  });
};

/** 歌曲管理-获取所有歌手 */
export const getAllArtists = () => {
  const userData = getToken();
  return http.request<Result>("get", "/admin/getAllArtistNames", {
    headers: {
      "Content-Type": "application/json",
      Authorization: userData.accessToken
    }
  });
};

/** 歌曲管理-获取歌曲列表 */
export const getSongList = (data: object) => {
  const userData = getToken();
  return http.request<ResultTable>("post", "/admin/getAllSongsByArtist", {
    headers: {
      "Content-Type": "application/json",
      Authorization: userData.accessToken
    },
    data
  });
};

/** 歌曲管理-新增歌曲 */
export const addSong = (data: object) => {
  const userData = getToken();
  return http.request<Result>("post", "/admin/addSong", {
    headers: {
      "Content-Type": "application/json",
      Authorization: userData.accessToken
    },
    data
  });
};

/** 歌曲管理-编辑歌曲 */
export const updateSong = (data: object) => {
  const userData = getToken();
  return http.request<Result>("put", "/admin/updateSong", {
    headers: {
      "Content-Type": "application/json",
      Authorization: userData.accessToken
    },
    data
  });
};

/** 歌曲管理-更新歌曲封面 */
export const updateSongCover = (id: number, data: object) => {
  const userData = getToken();
  return http.request<Result>("patch", `/admin/updateSongCover/${id}`, {
    headers: {
      "Content-Type": "multipart/form-data",
      Authorization: userData.accessToken
    },
    data,
    responseType: "json" // 确保使用正确的响应类型（可以使用 'json' 或 'blob'）
  });
};

/** 歌曲管理-更新歌曲音频 */
export const updateSongAudio = (id: number, data: FormData) => {
  const userData = getToken();
  return http.request<Result>("patch", `/admin/updateSongAudio/${id}`, {
    headers: {
      "Content-Type": "multipart/form-data",
      Authorization: userData.accessToken
    },
    data,
    responseType: "json" // 确保使用正确的响应类型（可以使用 'json' 或 'blob'）
  });
};

/** 歌曲管理-删除歌曲 */
export const deleteSong = (id: number) => {
  const userData = getToken();
  return http.request<Result>("delete", `/admin/deleteSong/${id}`, {
    headers: { Authorization: userData.accessToken }
  });
};

/** 歌曲管理-批量删除歌曲 */
export const deleteSongs = (ids: Array<number>) => {
  const userData = getToken();
  return http.request<Result>("delete", `/admin/deleteSongs`, {
    headers: {
      "Content-Type": "application/json",
      Authorization: userData.accessToken
    },
    data: ids
  });
};

/** 歌单管理-获取歌单列表 */
export const getPlaylistList = (data: object) => {
  const userData = getToken();
  return http.request<ResultTable>("post", "/admin/getAllPlaylists", {
    headers: {
      "Content-Type": "application/json",
      Authorization: userData.accessToken
    },
    data
  });
};

/** 歌单管理-新增歌单 */
export const addPlaylist = (data: object) => {
  const userData = getToken();
  return http.request<Result>("post", "/admin/addPlaylist", {
    headers: {
      "Content-Type": "application/json",
      Authorization: userData.accessToken
    },
    data
  });
};

/** 歌单管理-编辑歌单 */
export const updatePlaylist = (data: object) => {
  const userData = getToken();
  return http.request<Result>("put", "/admin/updatePlaylist", {
    headers: {
      "Content-Type": "application/json",
      Authorization: userData.accessToken
    },
    data
  });
};

/** 歌单管理-更新歌单封面 */
export const updatePlaylistCover = (id: number, data: object) => {
  const userData = getToken();
  return http.request<Result>("patch", `/admin/updatePlaylistCover/${id}`, {
    headers: {
      "Content-Type": "multipart/form-data",
      Authorization: userData.accessToken
    },
    data,
    responseType: "json" // 确保使用正确的响应类型（可以使用 'json' 或 'blob'）
  });
};

/** 歌单管理-删除歌单 */
export const deletePlaylist = (id: number) => {
  const userData = getToken();
  return http.request<Result>("delete", `/admin/deletePlaylist/${id}`, {
    headers: { Authorization: userData.accessToken }
  });
};

/** 歌单管理-批量删除歌单 */
export const deletePlaylists = (ids: Array<number>) => {
  const userData = getToken();
  return http.request<Result>("delete", `/admin/deletePlaylists`, {
    headers: {
      "Content-Type": "application/json",
      Authorization: userData.accessToken
    },
    data: ids
  });
};

/** 反馈管理-获取反馈列表 */
export const getFeedbackList = (data: object) => {
  const userData = getToken();
  return http.request<ResultTable>("post", "/admin/getAllFeedbacks", {
    headers: {
      "Content-Type": "application/json",
      Authorization: userData.accessToken
    },
    data
  });
};

/** 反馈管理-删除反馈 */
export const deleteFeedback = (id: number) => {
  const userData = getToken();
  return http.request<Result>("delete", `/admin/deleteFeedback/${id}`, {
    headers: { Authorization: userData.accessToken }
  });
};

/** 反馈管理-批量删除反馈 */
export const deleteFeedbacks = (ids: Array<number>) => {
  const userData = getToken();
  return http.request<Result>("delete", `/admin/deleteFeedbacks`, {
    headers: {
      "Content-Type": "application/json",
      Authorization: userData.accessToken
    },
    data: ids
  });
};

/** 轮播图管理-获取轮播图列表 */
export const getBannerList = (data: object) => {
  const userData = getToken();
  return http.request<ResultTable>("post", "/admin/getAllBanners", {
    headers: {
      "Content-Type": "application/json",
      Authorization: userData.accessToken
    },
    data
  });
};

/** 轮播图管理-新增轮播图 */
export const addBanner = (data: object) => {
  const userData = getToken();
  return http.request<Result>("post", "/admin/addBanner", {
    headers: {
      "Content-Type": "multipart/form-data",
      Authorization: userData.accessToken
    },
    data
  });
};

/** 轮播图管理-编辑轮播图 */
export const updateBanner = (id: number, data: object) => {
  const userData = getToken();
  return http.request<Result>("patch", `/admin/updateBanner/${id}`, {
    headers: {
      "Content-Type": "multipart/form-data",
      Authorization: userData.accessToken
    },
    data
  });
};

/** 轮播图管理-编辑轮播图状态 */
export const updateBannerStatus = (id: number, status: number) => {
  const userData = getToken();
  return http.request<Result>("patch", `/admin/updateBannerStatus/${id}`, {
    headers: {
      Authorization: userData.accessToken
    },
    params: { status }
  });
};

/** 轮播图管理-删除轮播图 */
export const deleteBanner = (id: number) => {
  const userData = getToken();
  return http.request<Result>("delete", `/admin/deleteBanner/${id}`, {
    headers: { Authorization: userData.accessToken }
  });
};

/** 轮播图管理-批量删除轮播图 */
export const deleteBanners = (ids: Array<number>) => {
  const userData = getToken();
  return http.request<Result>("delete", `/admin/deleteBanners`, {
    headers: {
      "Content-Type": "application/json",
      Authorization: userData.accessToken
    },
    data: ids
  });
};

/** 审核管理-获取待审核歌曲列表 */
export const getPendingSongs = (pageNum: number = 1, pageSize: number = 10) => {
  const userData = getToken();
  return http.request<ResultTable>("get", `/admin/audit/pending/songs`, {
    headers: {
      Authorization: userData.accessToken
    },
    params: { pageNum, pageSize }
  });
};

/** 审核管理-获取待审核帖子列表 */
export const getPendingPosts = (pageNum: number = 1, pageSize: number = 10) => {
  const userData = getToken();
  return http.request<ResultTable>("get", `/admin/audit/pending/posts`, {
    headers: {
      Authorization: userData.accessToken
    },
    params: { pageNum, pageSize }
  });
};

/** 审核管理-获取待审核回复列表 */
export const getPendingReplies = (pageNum: number = 1, pageSize: number = 10) => {
  const userData = getToken();
  return http.request<ResultTable>("get", `/admin/audit/pending/replies`, {
    headers: {
      Authorization: userData.accessToken
    },
    params: { pageNum, pageSize }
  });
};

/** 审核管理-获取待审核评论列表 */
export const getPendingComments = (pageNum: number = 1, pageSize: number = 10) => {
  const userData = getToken();
  return http.request<ResultTable>("get", `/admin/audit/pending/comments`, {
    headers: {
      Authorization: userData.accessToken
    },
    params: { pageNum, pageSize }
  });
};

/** 审核管理-审核通过歌曲 */
export const approveSong = (id: number) => {
  const userData = getToken();
  return http.request<Result>("patch", `/admin/audit/song/approve/${id}`, {
    headers: { Authorization: userData.accessToken }
  });
};

/** 审核管理-审核拒绝歌曲 */
export const rejectSong = (id: number, reason?: string) => {
  const userData = getToken();
  const params: any = {};
  // 如果 reason 不是 undefined，就传递它（包括空字符串）
  if (reason !== undefined) {
    params.reason = reason;
  }
  console.log('rejectSong - id:', id, 'reason:', reason, 'params:', params);
  return http.request<Result>("patch", `/admin/audit/song/reject/${id}`, {
    headers: { Authorization: userData.accessToken },
    params
  });
};

/** 审核管理-审核通过帖子 */
export const approvePost = (id: number) => {
  const userData = getToken();
  return http.request<Result>("patch", `/admin/audit/post/approve/${id}`, {
    headers: { Authorization: userData.accessToken }
  });
};

/** 审核管理-审核拒绝帖子 */
export const rejectPost = (id: number, reason?: string) => {
  const userData = getToken();
  const params: any = {};
  // 如果 reason 不是 undefined，就传递它（包括空字符串）
  if (reason !== undefined) {
    params.reason = reason;
  }
  console.log('rejectPost - id:', id, 'reason:', reason, 'params:', params);
  return http.request<Result>("patch", `/admin/audit/post/reject/${id}`, {
    headers: { Authorization: userData.accessToken },
    params
  });
};

/** 审核管理-审核通过回复 */
export const approveReply = (id: number) => {
  const userData = getToken();
  return http.request<Result>("patch", `/admin/audit/reply/approve/${id}`, {
    headers: { Authorization: userData.accessToken }
  });
};

/** 审核管理-审核拒绝回复 */
export const rejectReply = (id: number, reason?: string) => {
  const userData = getToken();
  const params: any = {};
  // 如果 reason 不是 undefined，就传递它（包括空字符串）
  if (reason !== undefined) {
    params.reason = reason;
  }
  console.log('rejectReply - id:', id, 'reason:', reason, 'params:', params);
  return http.request<Result>("patch", `/admin/audit/reply/reject/${id}`, {
    headers: { Authorization: userData.accessToken },
    params
  });
};

/** 获取歌曲详情 */
export const getSongDetail = (id: number) => {
  const userData = getToken();
  return http.request<Result>("get", `/song/getSongDetail/${id}`, {
    headers: { Authorization: userData.accessToken }
  });
};

/** 获取帖子详情 */
export const getForumPostDetail = (id: number) => {
  const userData = getToken();
  return http.request<Result>("get", `/forum/postDetail/${id}`, {
    headers: { Authorization: userData.accessToken }
  });
};

/** 获取帖子回复列表 */
export const getForumReplies = (data: { postId: number; pageNum: number; pageSize: number }) => {
  const userData = getToken();
  return http.request<ResultTable>("post", `/forum/replies`, {
    headers: {
      "Content-Type": "application/json",
      Authorization: userData.accessToken
    },
    data
  });
};

/** 审核管理-审核通过评论 */
export const approveComment = (id: number) => {
  const userData = getToken();
  return http.request<Result>("patch", `/admin/audit/comment/approve/${id}`, {
    headers: { Authorization: userData.accessToken }
  });
};

/** 审核管理-审核拒绝评论 */
export const rejectComment = (id: number, reason?: string) => {
  const userData = getToken();
  const params: any = {};
  if (reason) {
    params.reason = reason;
  }
  return http.request<Result>("patch", `/admin/audit/comment/reject/${id}`, {
    headers: { Authorization: userData.accessToken },
    params
  });
};