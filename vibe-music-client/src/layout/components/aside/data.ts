export const MenuData = [
  {
    title: '',
    children: [{ title: '推荐', icon: 'ri:home-smile-2-line', router: '/' }],
  },
  {
    title: '发现',
    children: [
      { title: '曲库', icon: 'ri:music-2-line', router: '/library' },
      {
        title: '歌手',
        icon: 'ri:mic-line',
        router: '/artist'
      },
      // {
      //   title: '歌单',
      //   icon: 'ri:album-line',
      //   router: '/playlist'
      // },
    ],
  },
  {
    title: '社区',
    children: [
      {
        title: '论坛',
        icon: 'ri:discuss-line',
        router: '/forum'
      },
    ],
  },
  {
    title: '原创',
    children: [
      {
        title: '上传歌曲',
        icon: 'ri:upload-line',
        router: '/original/upload'
      },
    ],
  },
  {
    title: '我的',
    children: [
      { title: '喜欢', icon: 'ri:heart-line', router: '/like' },
      {
        title: '个人中心',
        icon: 'mi:user',
        router: '/user'
      },
      {
        title: '我的上传',
        icon: 'ri:creative-commons-line',
        router: '/my/uploads'
      },
      {
        title: '我的接单',
        icon: 'ri:hand-heart-line',
        router: '/my/orders'
      },
    ],
  },
]
