# 播放歌曲API响应时间监控使用说明

## 功能说明

系统已自动集成了响应时间监控功能，会记录每次调用 `song/url/v1` API（播放歌曲的GET请求）的响应时间。

## 查看平均响应时间的方法

### 方法1：浏览器控制台（推荐）

1. 打开浏览器开发者工具（按 `F12`）
2. 切换到"控制台"（Console）标签
3. 输入以下代码并按回车：

```javascript
// 获取播放歌曲API的统计数据（注意：函数名是 getSongPlaybackStats，不是 getSongPalybackStats）
window.responseTimeMonitor.getSongPlaybackStats()
```

4. 或者查看详细报告：

```javascript
// 打印详细的统计报告
window.responseTimeMonitor.printReport('song/url/v1')
```

### 方法2：使用测试页面

在浏览器中打开 `test-response-time.html` 文件，会自动显示统计数据。

### 方法3：在代码中使用

```typescript
import { getSongPlaybackAverageResponseTime, printSongPlaybackStats } from '@/utils'

// 获取平均响应时间（毫秒）
const avgTime = getSongPlaybackAverageResponseTime()

// 打印统计报告
printSongPlaybackStats()
```

## 输出示例

```javascript
{
  url: "song/url/v1?id=123&level=standard",
  method: "GET",
  count: 15,              // 请求次数
  averageTime: 125.5,     // 平均响应时间（毫秒）
  minTime: 89.2,          // 最短响应时间
  maxTime: 234.8,         // 最长响应时间
  timings: [120, 134, ...] // 历史响应时间数组
}
```

## 数据持久化

统计数据会自动保存到浏览器的 localStorage 中，刷新页面后数据仍然保留。

## 清除数据

如果需要清除统计数据：

```javascript
// 清除所有统计数据
window.responseTimeMonitor.clearStats()

// 只清除播放歌曲API的统计数据
window.responseTimeMonitor.clearStatsForUrl('song/url/v1')
```

## 注意事项

- 只有在实际播放歌曲时才会产生统计数据
- 如果歌曲URL已经存在，可能不会再次请求API
- 统计数据最多保存最近100条记录

