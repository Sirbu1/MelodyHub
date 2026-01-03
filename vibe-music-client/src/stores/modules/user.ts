import { defineStore } from 'pinia'
import piniaPersistConfig from '@/stores/helper/persist'
import { UserState } from '@/stores/interface'
import { login, logout, getUserInfo } from '@/api/system'
import { AudioStore } from './audio'

interface UserInfo {
  userId?: number
  username?: string
  phone?: string
  email?: string
  avatarUrl?: string
  introduction?: string
  gender?: number | null
  birth?: string
  area?: string
  score?: number
  token?: string
}

/**
 * 用户信息
 */
export const UserStore = defineStore('UserStore', {
  state: (): UserState => ({
    userInfo: {} as UserInfo,
    isLoggedIn: false,
  }),
  getters: {
    // 验证token和用户信息是否匹配
    isUserInfoValid(): boolean {
      if (!this.userInfo?.token || !this.userInfo?.userId) {
        return false
      }
      return true
    }
  },
  actions: {
    // 设置用户信息
    setUserInfo(userInfo: any, token?: string) {
      this.userInfo = {
        userId: userInfo.userId,
        username: userInfo.username,
        phone: userInfo.phone,
        email: userInfo.email,
        avatarUrl: userInfo.userAvatar,
        introduction: userInfo.introduction,
        gender: userInfo.gender,
        birth: userInfo.birth,
        area: userInfo.area,
        score: userInfo.score,
        token: token
      }
      this.isLoggedIn = true
    },
    // 更新头像
    updateUserAvatar(avatarUrl: string) {
      if (this.userInfo) {
        this.userInfo.avatarUrl = avatarUrl
      }
    },
    // 清除用户信息
    clearUserInfo() {
      this.userInfo = {}
      this.isLoggedIn = false

      // 清空所有歌曲的喜欢状态
      const audioStore = AudioStore()
      // 清空播放列表中的喜欢状态
      audioStore.trackList.forEach(track => {
        track.likeStatus = 0
      })
      // 清空当前页面歌曲列表中的喜欢状态
      if (audioStore.currentPageSongs) {
        audioStore.currentPageSongs.forEach(song => {
          song.likeStatus = 0
        })
      }
    },
    // 用户登录
    async userLogin(loginData: { email: string; password: string }) {
      try {
        // 先清除旧的用户信息，避免数据混淆
        this.clearUserInfo()
        
        const response = await login(loginData)

        if (response.code === 0) {
          // 先保存token
          const token = response.data
          
          // 临时设置token，用于后续获取用户信息
          this.userInfo = { token }
          this.isLoggedIn = false // 先不设置为已登录，等获取到用户信息后再设置

          try {
            // 再获取用户信息
            const userInfoResponse = await getUserInfo()
            
            if (userInfoResponse.code === 0) {
              // 使用setUserInfo设置完整的用户信息（包括token）
              this.setUserInfo(userInfoResponse.data, token)
              return { success: true, message: '登录成功' }
            }
            // 如果获取用户信息失败，清除token
            this.clearUserInfo()
            return { success: false, message: userInfoResponse.message || '获取用户信息失败' }
          } catch (error: any) {
            // 如果获取用户信息失败，清除token
            this.clearUserInfo()
            return { success: false, message: error.message || '获取用户信息失败' }
          }
        }
        return { success: false, message: response.message || '登录失败' }
      } catch (error: any) {
        // 登录失败，确保清除所有用户信息
        this.clearUserInfo()
        return { success: false, message: error.message || '登录失败' }
      }
    },
    // 用户退出
    async userLogout() {
      try {
        const response = await logout()
        if (response.code === 0) {
          this.clearUserInfo()
          return { success: true, message: '退出成功' }
        }
        return { success: false, message: response.message }
      } catch (error: any) {
        return { success: false, message: error.message || '退出失败' }
      }
    },
    // 验证并同步用户信息（页面刷新时调用）
    async validateAndSyncUserInfo() {
      // 如果没有token，清除用户信息
      if (!this.userInfo?.token) {
        this.clearUserInfo()
        return
      }

      // 如果有token但没有用户ID，或者用户信息不完整，重新获取
      if (!this.userInfo?.userId) {
        try {
          const userInfoResponse = await getUserInfo()
          if (userInfoResponse.code === 0) {
            // 使用当前token更新用户信息
            this.setUserInfo(userInfoResponse.data, this.userInfo.token)
          } else {
            // 如果获取失败，可能是token过期，清除用户信息
            this.clearUserInfo()
          }
        } catch (error: any) {
          // 如果请求失败，可能是token过期，清除用户信息
          console.error('验证用户信息失败:', error)
          this.clearUserInfo()
        }
      } else {
        // 如果有token和用户ID，验证token是否有效
        try {
          const userInfoResponse = await getUserInfo()
          if (userInfoResponse.code === 0) {
            // 验证返回的用户ID是否与存储的用户ID一致
            if (userInfoResponse.data.userId !== this.userInfo.userId) {
              // 如果不一致，说明token对应的用户已经改变，更新用户信息
              console.warn('用户ID不匹配，更新用户信息')
              this.setUserInfo(userInfoResponse.data, this.userInfo.token)
            } else {
              // 如果一致，确保用户信息是最新的
              this.setUserInfo(userInfoResponse.data, this.userInfo.token)
            }
          } else {
            // 如果获取失败，可能是token过期，清除用户信息
            this.clearUserInfo()
          }
        } catch (error: any) {
          // 如果请求失败，可能是token过期，清除用户信息
          console.error('验证用户信息失败:', error)
          // 401错误表示token过期，清除用户信息
          if (error.response?.status === 401) {
            this.clearUserInfo()
          }
        }
      }
    }
  },
  persist: piniaPersistConfig('UserStore'),
})
