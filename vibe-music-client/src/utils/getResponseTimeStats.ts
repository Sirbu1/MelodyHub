/**
 * 获取响应时间统计信息的工具脚本
 * 可以直接在浏览器控制台运行
 */

import responseTimeMonitor from './responseTimeMonitor'

/**
 * 获取并显示播放歌曲API的平均响应时间
 */
export function displaySongPlaybackStats() {
  const stats = responseTimeMonitor.getSongPlaybackStats()
  
  if (!stats) {
    console.log('📊 暂无播放歌曲API的响应时间数据')
    console.log('💡 提示：请先播放一些歌曲来收集数据')
    return null
  }

  console.log('📊 播放歌曲API响应时间统计')
  console.log('=====================================')
  console.log(`🔗 API: GET ${stats.url}`)
  console.log(`📈 请求次数: ${stats.count}`)
  console.log(`⏱️  平均响应时间: ${responseTimeMonitor.formatDuration(stats.averageTime)}`)
  console.log(`⚡ 最短响应时间: ${responseTimeMonitor.formatDuration(stats.minTime)}`)
  console.log(`🐌 最长响应时间: ${responseTimeMonitor.formatDuration(stats.maxTime)}`)
  console.log('=====================================')

  if (stats.timings && stats.timings.length > 0) {
    const recent = stats.timings.slice(-10)
    console.log('📋 最近10次响应时间:')
    recent.forEach((time, index) => {
      console.log(`   ${index + 1}. ${responseTimeMonitor.formatDuration(time)}`)
    })
  }

  return {
    averageTime: stats.averageTime,
    count: stats.count,
    minTime: stats.minTime,
    maxTime: stats.maxTime,
    formattedAverage: responseTimeMonitor.formatDuration(stats.averageTime),
  }
}

// 导出到window对象，方便在控制台使用
if (typeof window !== 'undefined') {
  ;(window as any).displaySongPlaybackStats = displaySongPlaybackStats
}



