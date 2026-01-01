import {
  createRouter,
  createWebHistory,
  createWebHashHistory,
} from 'vue-router'

const mode = import.meta.env.VITE_ROUTER_MODE

const routerMode = {
  hash: () => createWebHashHistory(),
  history: () => createWebHistory(),
}

const router = createRouter({
  history: routerMode[mode](),
  strict: false,
  scrollBehavior: () => ({ left: 0, top: 0 }),
  routes: [
    {
      path: '/',
      component: () => import('@/pages/index.vue'),
    },
    {
      path: '/library',
      component: () => import('@/pages/library/index.vue'),
    },
    {
      path: '/artist',
      component: () => import('@/pages/artist/index.vue'),
    },
    {
      path: '/artist/:id',
      component: () => import('@/pages/artist/[id].vue'),
    },
    {
      path: '/playlist',
      component: () => import('@/pages/playlist/index.vue'),
    },
    {
      path: '/playlist/:id',
      component: () => import('@/pages/playlist/[id].vue'),
    },
    {
      path: '/like',
      component: () => import('@/pages/like/index.vue'),
    },
    {
      path: '/user',
      component: () => import('@/pages/user/index.vue'),
    },
    {
      path: '/forum',
      component: () => import('@/pages/forum/index.vue'),
    },
    {
      path: '/forum/:id',
      component: () => import('@/pages/forum/[id].vue'),
    },
    {
      path: '/original',
      component: () => import('@/pages/original/index.vue'),
    },
    {
      path: '/original/upload',
      component: () => import('@/pages/original/index.vue'),
    },
    {
      path: '/original/library',
      component: () => import('@/pages/original/index.vue'),
    },
    {
      path: '/my/uploads',
      component: () => import('@/pages/my-uploads/index.vue'),
    },
  ],
})

export default router
