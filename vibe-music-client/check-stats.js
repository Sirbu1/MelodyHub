/**
 * 快速检查响应时间统计数据的脚本
 * 这个脚本需要在实际浏览器环境中运行
 */

// 在浏览器控制台中运行此代码来查看统计数据
const checkStats = () => {
  try {
    const storageKey = 'api_response_time_stats';
    const data = localStorage.getItem(storageKey);
    
    if (!data) {
      console.log('📊 暂无响应时间数据');
      console.log('💡 请先播放一些歌曲来收集数据');
      return null;
    }
    
    const parsed = JSON.parse(data);
    let songStats = null;
    
    // 查找播放歌曲API的统计数据
    for (const [key, stats] of parsed) {
      if (stats.url && stats.url.includes('song/url/v1')) {
        songStats = stats;
        break;
      }
    }
    
    if (!songStats || songStats.count === 0) {
      console.log('📊 暂无播放歌曲API的响应时间数据');
      console.log('💡 请先播放一些歌曲来收集数据');
      return null;
    }
    
    const formatDuration = (ms) => {
      if (ms < 1000) {
        return `${ms.toFixed(2)}ms`;
      }
      return `${(ms / 1000).toFixed(2)}s`;
    };
    
    console.log('📊 播放歌曲API响应时间统计');
    console.log('=====================================');
    console.log(`🔗 API: GET ${songStats.url}`);
    console.log(`📈 请求次数: ${songStats.count}`);
    console.log(`⏱️  平均响应时间: ${formatDuration(songStats.averageTime)}`);
    console.log(`⚡ 最短响应时间: ${formatDuration(songStats.minTime)}`);
    console.log(`🐌 最长响应时间: ${formatDuration(songStats.maxTime)}`);
    console.log('=====================================');
    
    return {
      averageTime: songStats.averageTime,
      count: songStats.count,
      minTime: songStats.minTime,
      maxTime: songStats.maxTime,
      formattedAverage: formatDuration(songStats.averageTime),
    };
  } catch (error) {
    console.error('❌ 读取统计数据时出错:', error);
    return null;
  }
};

// 如果在浏览器环境中，立即执行
if (typeof window !== 'undefined') {
  checkStats();
}

// 如果在Node.js环境中，提供使用说明
if (typeof module !== 'undefined' && module.exports) {
  console.log(`
使用说明：
1. 打开浏览器开发者工具（F12）
2. 在控制台中运行以下代码：

   // 方法1：直接使用window对象
   window.responseTimeMonitor.getSongPlaybackStats()
   
   // 方法2：使用localStorage
   ${checkStats.toString()}
   checkStats()
  `);
}


