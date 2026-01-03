import axios, {
  AxiosInstance,
  AxiosRequestConfig,
  InternalAxiosRequestConfig,
  AxiosRequestHeaders,
} from 'axios'
import NProgress from '@/config/nprogress'
import 'nprogress/nprogress.css'
import { UserStore } from '@/stores/modules/user'
import { ElMessage } from 'element-plus'
import responseTimeMonitor from './responseTimeMonitor'

const instance: AxiosInstance = axios.create({
  baseURL: 'http://localhost:8080', // 设置为后端服务地址
  timeout: 20000, // 设置超时时间 20秒
  headers: {
    Accept: 'application/json, text/plain, */*',
    'Content-Type': 'application/json',
    'X-Requested-With': 'XMLHttpRequest',
  },
  withCredentials: false,
})

// 请求拦截器
instance.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 开启进度条
    NProgress.start()

    // 记录请求开始时间
    const requestId = responseTimeMonitor.recordRequestStart({
      url: config.url,
      method: config.method,
    })
    // 将请求ID存储到config中，以便响应时使用
    ;(config as any).__requestId = requestId

    // 检查是否是FormData请求
    const isFormData = config.data instanceof FormData

    // 如果是FormData，删除Content-Type头，让axios自动设置multipart/form-data
    if (isFormData && config.headers) {
      delete config.headers['Content-Type']
      delete config.headers['content-type']
    }

    // 定义不需要添加token的公开接口
    const publicPaths = [
      '/user/login',
      '/user/register',
      '/user/sendVerificationCode',
      '/user/resetUserPassword',
      '/admin/login',
      '/admin/register',
    ]

    // 检查是否是公开接口
    const isPublicPath = publicPaths.some(path => config.url?.includes(path))
    
    if (isPublicPath) {
      return config
    }

    // 从 pinia 中获取token
    const userStore = UserStore()
    const token = userStore.userInfo?.token

    if (token) {
      // 确保headers对象存在并且是正确的类型
      if (!config.headers) {
        config.headers = {} as AxiosRequestHeaders
      }

      // 设置Authorization头
      config.headers.Authorization = `Bearer ${token}`
    }

    // 调试信息
    if (isFormData) {
      console.log('FormData请求:', config.url)
      console.log('Headers:', config.headers)
    }

    return config
  },
  (error) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
instance.interceptors.response.use(
  (response) => {
    // 关闭进度条
    NProgress.done()
    
    // 记录响应时间
    const requestId = (response.config as any).__requestId
    if (requestId) {
      const duration = responseTimeMonitor.recordRequestEnd(
        requestId,
        response.config.url
      )
      // 如果是播放歌曲的API，在控制台输出响应时间（可选）
      if (response.config.url?.includes('song/url/v1') && duration) {
        console.log(
          `🎵 歌曲播放请求响应时间: ${responseTimeMonitor.formatDuration(duration)}`
        )
      }
    }

    const { data } = response
    return data
  },
  (error) => {
    // 关闭进度条
    NProgress.done()

    // 记录错误请求的响应时间
    const requestId = (error.config as any)?.__requestId
    if (requestId) {
      responseTimeMonitor.recordRequestEnd(requestId, error.config?.url)
    }

    if (error.response) {
      // 定义公开接口路径
      const publicPaths = [
        '/user/login',
        '/user/register',
        '/user/sendVerificationCode',
        '/user/resetUserPassword',
        '/admin/login',
        '/admin/register',
      ]
      
      const isPublicPath = publicPaths.some(path => error.config.url?.includes(path))

      // 获取错误消息（优先使用服务器返回的消息）
      const errorMessage = error.response?.data?.message || error.response?.data?.msg || error.message || ''
      const requestUrl = error.config?.url || '未知接口'
      
      switch (error.response.status) {
        case 401:
          // 如果是登录请求
          if (error.config.url?.includes('/user/login') || error.config.url?.includes('/admin/login')) {
            ElMessage.error('邮箱或密码错误')
          } 
          // 如果是其他公开接口
          else if (isPublicPath) {
            ElMessage.error('请求失败，请稍后重试')
          } 
          // 如果是需要登录的接口
          else {
            const userStore = UserStore()
            userStore.clearUserInfo()
            ElMessage.error('登录已过期，请重新登录')
          }
          break
        case 403:
          ElMessage.error(errorMessage || '没有权限')
          break
        case 404:
          ElMessage.error(errorMessage || '请求的资源不存在')
          break
        case 500:
          // 显示详细的服务器错误信息
          console.error('服务器错误:', {
            url: requestUrl,
            status: error.response.status,
            message: errorMessage,
            data: error.response.data
          })
          ElMessage.error(errorMessage || `服务器错误: ${requestUrl}`)
          break
        default:
          ElMessage.error(errorMessage || '网络错误')
      }
    } else {
      ElMessage.error('网络连接失败')
    }

    return Promise.reject(error)
  }
)

// 封装request方法
export const http = <T>(
  method: 'get' | 'post' | 'put' | 'delete' | 'patch',
  url: string,
  config?: Omit<AxiosRequestConfig, 'method' | 'url'>
): Promise<T> => {
  return instance({ method, url, ...config })
}

// 封装get方法
export const httpGet = <T>(url: string, params?: object): Promise<T> =>
  instance.get(url, { params })

// 封装post方法
export const httpPost = <T>(
  url: string,
  data?: object,
  header?: object
): Promise<T> => instance.post(url, data, { headers: header })

// 封装upload方法
export const httpUpload = <T>(
  url: string,
  formData: FormData,
  header?: object
): Promise<T> => {
  return instance.post(url, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
      ...header,
    },
  })
}
