/**
 * 响应时间监控工具
 * 用于收集和分析API请求的响应时间
 */

interface RequestTiming {
  url: string
  method: string
  startTime: number
  endTime?: number
  duration?: number
}

interface ResponseTimeStats {
  url: string
  method: string
  count: number
  totalTime: number
  averageTime: number
  minTime: number
  maxTime: number
  timings: number[]
}

class ResponseTimeMonitor {
  private requestMap = new Map<string, RequestTiming>()
  private statsMap = new Map<string, ResponseTimeStats>()
  private readonly STORAGE_KEY = 'api_response_time_stats'
  private readonly MAX_STORAGE_SIZE = 100 // 最多保存100条记录

  /**
   * 记录请求开始时间
   */
  recordRequestStart(config: { url?: string; method?: string }): string {
    const requestId = `${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
    const url = config.url || ''
    const method = (config.method || 'get').toUpperCase()

    this.requestMap.set(requestId, {
      url,
      method,
      startTime: performance.now(),
    })

    return requestId
  }

  /**
   * 记录请求结束时间并计算响应时间
   */
  recordRequestEnd(requestId: string, url?: string): number | null {
    const timing = this.requestMap.get(requestId)
    if (!timing) {
      return null
    }

    const endTime = performance.now()
    const duration = endTime - timing.startTime

    timing.endTime = endTime
    timing.duration = duration

    // 记录统计数据
    this.updateStats(timing.url || url || '', timing.method, duration)

    // 清理已完成的请求记录
    this.requestMap.delete(requestId)

    return duration
  }

  /**
   * 更新统计数据
   */
  private updateStats(url: string, method: string, duration: number): void {
    const key = `${method}:${url}`
    const existing = this.statsMap.get(key)

    if (existing) {
      existing.count++
      existing.totalTime += duration
      existing.averageTime = existing.totalTime / existing.count
      existing.minTime = Math.min(existing.minTime, duration)
      existing.maxTime = Math.max(existing.maxTime, duration)
      existing.timings.push(duration)
      
      // 保持最近N条记录
      if (existing.timings.length > this.MAX_STORAGE_SIZE) {
        existing.timings.shift()
      }
    } else {
      this.statsMap.set(key, {
        url,
        method,
        count: 1,
        totalTime: duration,
        averageTime: duration,
        minTime: duration,
        maxTime: duration,
        timings: [duration],
      })
    }

    // 保存到 localStorage
    this.saveToStorage()
  }

  /**
   * 获取特定URL的统计数据
   */
  getStats(urlPattern: string): ResponseTimeStats | null {
    for (const [key, stats] of this.statsMap.entries()) {
      if (stats.url.includes(urlPattern)) {
        return { ...stats }
      }
    }
    return null
  }

  /**
   * 获取所有统计数据
   */
  getAllStats(): ResponseTimeStats[] {
    return Array.from(this.statsMap.values()).map(stats => ({ ...stats }))
  }

  /**
   * 获取播放歌曲API的平均响应时间
   */
  getSongPlaybackStats(): ResponseTimeStats | null {
    return this.getStats('song/url/v1')
  }

  /**
   * 清除所有统计数据
   */
  clearStats(): void {
    this.statsMap.clear()
    this.requestMap.clear()
    localStorage.removeItem(this.STORAGE_KEY)
  }

  /**
   * 清除特定URL的统计数据
   */
  clearStatsForUrl(urlPattern: string): void {
    const keysToDelete: string[] = []
    for (const [key, stats] of this.statsMap.entries()) {
      if (stats.url.includes(urlPattern)) {
        keysToDelete.push(key)
      }
    }
    keysToDelete.forEach(key => this.statsMap.delete(key))
    this.saveToStorage()
  }

  /**
   * 保存统计数据到 localStorage
   */
  private saveToStorage(): void {
    try {
      const data = Array.from(this.statsMap.entries()).map(([key, stats]) => [
        key,
        {
          ...stats,
          timings: stats.timings.slice(-50), // 只保存最近50条
        },
      ])
      localStorage.setItem(this.STORAGE_KEY, JSON.stringify(data))
    } catch (error) {
      console.warn('保存响应时间统计数据失败:', error)
    }
  }

  /**
   * 从 localStorage 加载统计数据
   */
  loadFromStorage(): void {
    try {
      const data = localStorage.getItem(this.STORAGE_KEY)
      if (data) {
        const parsed = JSON.parse(data) as [string, ResponseTimeStats][]
        parsed.forEach(([key, stats]) => {
          this.statsMap.set(key, stats)
        })
      }
    } catch (error) {
      console.warn('加载响应时间统计数据失败:', error)
    }
  }

  /**
   * 格式化响应时间显示
   */
  formatDuration(ms: number): string {
    if (ms < 1000) {
      return `${ms.toFixed(2)}ms`
    }
    return `${(ms / 1000).toFixed(2)}s`
  }

  /**
   * 打印统计报告
   */
  printReport(urlPattern?: string): void {
    const stats = urlPattern ? this.getStats(urlPattern) : null
    const allStats = urlPattern ? (stats ? [stats] : []) : this.getAllStats()

    if (allStats.length === 0) {
      console.log('暂无响应时间统计数据')
      return
    }

    console.group('📊 API响应时间统计报告')
    allStats.forEach(stat => {
      console.log(`\n🔗 ${stat.method} ${stat.url}`)
      console.log(`   请求次数: ${stat.count}`)
      console.log(`   平均响应时间: ${this.formatDuration(stat.averageTime)}`)
      console.log(`   最短响应时间: ${this.formatDuration(stat.minTime)}`)
      console.log(`   最长响应时间: ${this.formatDuration(stat.maxTime)}`)
      if (stat.timings.length > 0) {
        const recent = stat.timings.slice(-10)
        console.log(`   最近10次: ${recent.map(t => this.formatDuration(t)).join(', ')}`)
      }
    })
    console.groupEnd()
  }
}

// 创建单例实例
const responseTimeMonitor = new ResponseTimeMonitor()

// 应用启动时加载历史数据
if (typeof window !== 'undefined') {
  responseTimeMonitor.loadFromStorage()
}

// 将监控器挂载到window对象，方便在控制台调试
if (typeof window !== 'undefined') {
  ;(window as any).responseTimeMonitor = responseTimeMonitor
}

export default responseTimeMonitor
