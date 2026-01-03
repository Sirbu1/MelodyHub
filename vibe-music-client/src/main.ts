import { createApp } from 'vue'
import App from './App.vue'
import router from './routers/index'
import Store from '@/stores'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import './style/index.scss'
// 导入响应时间统计工具（使其在控制台可用）
import '@/utils/responseTimeMonitor'
import '@/utils/getResponseTimeStats'

const app = createApp(App)

// 路由
app.use(router)
// 状态管理
app.use(Store)
// ElementPlus
app.use(ElementPlus, {
  locale: zhCn,
})
app.mount('#app')
